package org.alex_melan.spacereloaded.eclss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.eclss.EclssBalance;
import org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere;
import org.alex_melan.spacereloaded.core.lifesupport.Metabolism;
import org.alex_melan.spacereloaded.core.lifesupport.Scrubber;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.industry.IndustryStructures;
import org.alex_melan.spacereloaded.lifesupport.EnergyScale;
import org.alex_melan.spacereloaded.lifesupport.GasKind;
import org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity;
import org.alex_melan.spacereloaded.lifesupport.LifeSupportState;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.FormedStructure;
import org.alex_melan.spacereloaded.multiblock.HammerTarget;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.multiblock.StatusProvider;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;
import org.alex_melan.spacereloaded.registry.ItemMasses;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;
import org.alex_melan.spacereloaded.sealing.SealedZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Контроллер стойки жизнеобеспечения (008, US1, D82) — ECLSS МКС в стене герметичной зоны.
 * Раз в секунду (биология — игровые сутки, как в 007): OGS держит pO₂ на уставке, разлагая воду;
 * CDRA снимает CO₂ зоны; Сабатье восстанавливает CO₂ водородом OGS (его хватает на 57 %), вода
 * возвращается, метан с кислородом соседнего баллона идёт в топливный бак метаноксом (O/F 3.6) или
 * в сброс; WRS возвращает метаболическую воду экипажа. Энергия — по {@link EclssBalance}.
 */
public class EclssControllerBlockEntity extends MachineBlockEntity
        implements HammerTarget, StatusProvider, ControllerBlock.ItemAcceptor, IndustryStructures.StructureOwner {

    /** Гнёзда модулей в локальных координатах шаблона (x, y). */
    public static final int[][] SOCKETS = {{-1, 1}, {-1, 2}, {1, 1}, {1, 2}};
    public static final int NONE = 0, OGS = 1, SABATIER = 2, CDRA = 3, WRS = 4;
    public static final double WATER_CAPACITY_KG = 2000;
    /** OGA МКС: 5.4 кг O₂ в сутки в непрерывном режиме. */
    public static final double OGS_KG_PER_DAY = 5.4;
    public static final double O2_SETPOINT_KPA = 21.3;
    public static final double O_F_METHALOX = 3.6;
    private static final double DT_DAYS = 20.0 / 24000.0;

    private final FormedStructure structure = new FormedStructure();
    private boolean claimed;
    private double waterKg;
    private double h2Kg;
    private double ch4Kg;
    private double energyDebt;
    /** Модули гнёзд (по 4 бита) и работающие (битовая маска по гнёздам) — для анимации. */
    private int sockets;
    private int running;
    // сглаженные скорости за сутки для экрана
    private double o2Rate, co2Rate, reducedFraction, waterBack, makeup, ch4Rate, powerKw;
    private boolean ch4ToFuel;
    private String problem = "";
    /** Состояние анимации — только в клиентском экземпляре (роторы, фазы), задаёт рендер. */
    public Object clientAnim;

    public EclssControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ECLSS_CONTROLLER, pos, state, 20_000, 2_000, 0);
    }

    private Direction face() {
        return getBlockState().getValue(ControllerBlock.FACING);
    }

    @Override
    public void markStructureDirty() {
        structure.markDirty();
    }

    @Override
    public boolean hammer(ServerLevel level, ServerPlayer player) {
        boolean ok = structure.hammer(level, getBlockPos(), face(), player);
        setFormedState(level);
        return ok;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            structure.dismantle(serverLevel, pos, state.getValue(ControllerBlock.FACING), state.getBlock());
            LifeSupportState.contribute(serverLevel, pos, LifeSupportState.Contribution.NONE);
        }
        super.preRemoveSideEffects(pos, state);
    }

    public int socketKind(int i) {
        return (sockets >> (4 * i)) & 15;
    }

    public boolean socketRunning(int i) {
        return (running & (1 << i)) != 0;
    }

    public boolean formed() {
        return getBlockState().getValue(ControllerBlock.FORMED);
    }

    public double waterKg() {
        return waterKg;
    }

    public static void serverTick(EclssControllerBlockEntity be, ServerLevel level) {
        be.tick(level);
    }

    private void tick(ServerLevel level) {
        if (!claimed) {
            claimed = true;
            structure.reclaim(level, getBlockPos(), face());
        }
        if (structure.revalidate(level, getBlockPos(), face())) {
            setChanged();
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);
        setFormedState(level);
        int nextSockets = structure.formed() ? readSockets(level) : 0;
        int nextRunning = 0;
        LifeSupportState.Contribution contribution = LifeSupportState.Contribution.NONE;
        SealedZone zone = structure.formed() ? LifeSupportState.zoneAround(level, getBlockPos()) : null;
        LifeSupportState.Gas gas = zone != null && zone.isSealed() ? LifeSupportState.now(level, zone) : null;
        problem = !structure.formed() ? "not_formed" : gas == null ? "no_zone" : "";
        if (gas != null) {
            int[] count = new int[5];
            for (int i = 0; i < 4; i++) {
                count[(nextSockets >> (4 * i)) & 15]++;
            }
            int people = 0;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && zone.volume().contains(p.blockPosition().asLong())) {
                    people++;
                }
            }
            // OGS: выработка O₂ = дыхание экипажа + добор до уставки, не выше номинала модулей и запаса воды
            double rate = 0;
            if (count[OGS] > 0 && gas.pO2() < O2_SETPOINT_KPA + 0.5) {
                double deficit = CabinAtmosphere.massFor(O2_SETPOINT_KPA, CabinAtmosphere.M_O2, gas.volume(),
                        CabinAtmosphere.T_CABIN) - gas.mO2();
                rate = Math.max(0, Math.min(count[OGS] * OGS_KG_PER_DAY, people * Metabolism.O2_PER_DAY + Math.max(0, deficit) / 0.05));
                rate = Math.min(rate, waterKg / (EclssBalance.WATER_PER_O2 * DT_DAYS));
                if (waterKg < 0.01) {
                    problem = "no_water";
                }
            }
            double k = count[CDRA] * Scrubber.removalM3PerDay(Scrubber.FAN_M3_PER_MIN, Scrubber.EFFICIENCY);
            double removed = k * gas.mCo2() / gas.volume() * DT_DAYS;
            double o2 = rate * DT_DAYS;
            double metabolic = count[WRS] > 0 ? people * EclssBalance.METABOLIC_WATER_PER_DAY * EclssBalance.WRS_RECOVERY * DT_DAYS : 0;
            double kwh = o2 * EclssBalance.OGS_KWH_PER_KG_O2 + removed * EclssBalance.CDRA_KWH_PER_KG_CO2
                    + metabolic * EclssBalance.WRS_KWH_PER_KG;
            energyDebt += EnergyScale.fromJoules(kwh * 3.6e6);
            long cost = (long) Math.floor(energyDebt);
            if (energy.amount < cost) {
                problem = "no_power";
                energyDebt = Math.min(energyDebt, cost);
            } else {
                energy.amount -= cost;
                energyDebt -= cost;
                waterKg -= o2 * EclssBalance.WATER_PER_O2;
                h2Kg += o2 * EclssBalance.H2_PER_O2;
                double reduced = 0;
                if (count[SABATIER] > 0 && removed > 0) {
                    reduced = Math.min(removed, h2Kg / EclssBalance.H2_PER_CO2);
                    h2Kg -= reduced * EclssBalance.H2_PER_CO2;
                    waterKg += reduced * EclssBalance.WATER_PER_CO2;
                    ch4Kg += reduced * EclssBalance.CH4_PER_CO2;
                }
                // водород не копится: объём линии OGS → Сабатье ~1 г, лишнее стравливается (как на МКС); без Сабатье — всё
                h2Kg = Math.min(h2Kg, count[SABATIER] > 0 ? 0.001 : 0);
                waterKg = Math.min(WATER_CAPACITY_KG, waterKg + metabolic);
                ch4ToFuel = storeMethane(level);
                smooth(rate, removed / DT_DAYS, removed > 0 ? reduced / removed : 0,
                        (reduced * EclssBalance.WATER_PER_CO2 + metabolic) / DT_DAYS,
                        Math.max(0, o2 * EclssBalance.WATER_PER_O2 - reduced * EclssBalance.WATER_PER_CO2 - metabolic) / DT_DAYS,
                        reduced * EclssBalance.CH4_PER_CO2 / DT_DAYS, kwh * 3600);
                contribution = new LifeSupportState.Contribution(rate, 0, k, 0);
                for (int i = 0; i < 4; i++) {
                    int kind = (nextSockets >> (4 * i)) & 15;
                    boolean on = switch (kind) {
                        case OGS -> rate > 0;
                        case SABATIER -> reduced > 0;
                        case CDRA -> k > 0;
                        case WRS -> metabolic > 0 || rate > 0;
                        default -> false;
                    };
                    if (on) {
                        nextRunning |= 1 << i;
                    }
                }
            }
        }
        LifeSupportState.contribute(level, getBlockPos(), contribution);
        boolean active = nextRunning != 0;
        if (getBlockState().getValue(ControllerBlock.ACTIVE) != active) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        if (nextSockets != sockets || nextRunning != running) {
            sockets = nextSockets;
            running = nextRunning;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    private void smooth(double o2, double co2, double frac, double back, double mk, double ch4, double kw) {
        double a = 0.1;
        o2Rate += (o2 - o2Rate) * a;
        co2Rate += (co2 - co2Rate) * a;
        reducedFraction += (frac - reducedFraction) * a;
        waterBack += (back - waterBack) * a;
        makeup += (mk - makeup) * a;
        ch4Rate += (ch4 - ch4Rate) * a;
        powerKw += (kw - powerKw) * a;
    }

    /**
     * Метан — в топливный бак метаноксом (O/F 3.6) с кислородом баллона: бак и баллон подключаются к
     * любому блоку стойки (трубопроводы рамы). Нет бака, баллона или кислорода — сброс, как на МКС.
     */
    private boolean storeMethane(ServerLevel level) {
        if (ch4Kg < 0.01) {
            return ch4ToFuel;
        }
        double needO2 = ch4Kg * O_F_METHALOX;
        FuelTankBlockEntity fuel = null;
        List<GasTankBlockEntity> oxygen = new ArrayList<>();
        double available = 0;
        for (BlockPos p : org.alex_melan.spacereloaded.multiblock.StructurePorts.around(level, getBlockPos(), face(), 0)) {
            var be = level.getBlockEntity(p);
            if (be instanceof FuelTankBlockEntity tank && fuel == null
                    && (tank.propellantKg() <= 0 || "spacereloaded:methalox".equals(tank.fuelType()))) {
                fuel = tank;
            } else if (be instanceof GasTankBlockEntity gasTank && gasTank.kind() == GasKind.OXYGEN) {
                oxygen.add(gasTank);
                available += gasTank.mass();
            }
        }
        double methane = ch4Kg;
        ch4Kg = 0;
        if (fuel == null || available < needO2) {
            return false;
        }
        double got = 0;
        for (GasTankBlockEntity t : oxygen) {
            got += t.extract(GasKind.OXYGEN, needO2 - got);
            if (got >= needO2 - 1e-9) {
                break;
            }
        }
        double put = fuel.fill(methane + got, "spacereloaded:methalox");
        if (put < methane + got - 1e-6 && !oxygen.isEmpty()) {
            oxygen.get(0).insert(GasKind.OXYGEN, got * (1 - put / (methane + got)));
        }
        return put > 0;
    }

    private void setFormedState(ServerLevel level) {
        boolean formed = structure.formed();
        if (getBlockState().getValue(ControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    private int readSockets(ServerLevel level) {
        int packed = 0;
        for (int i = 0; i < SOCKETS.length; i++) {
            BlockPos p = MultiblockTemplates.worldPos(getBlockPos(), face(), SOCKETS[i][0], SOCKETS[i][1], 0);
            BlockState s = level.getBlockState(p);
            int kind = s.is(ModBlocks.OGS_MODULE) ? OGS : s.is(ModBlocks.SABATIER_MODULE) ? SABATIER
                    : s.is(ModBlocks.CDRA_MODULE) ? CDRA : s.is(ModBlocks.WRS_MODULE) ? WRS : NONE;
            packed |= kind << (4 * i);
        }
        return packed;
    }

    @Override
    public boolean accept(ServerLevel level, ServerPlayer player, ItemStack stack) {
        double per;
        if (stack.is(Items.WATER_BUCKET)) {
            per = 1000; // ведро = блок воды 1 м³ (как гидролоток 007)
        } else if (stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("spacereloaded", "electrolyzer_input")))) {
            per = ItemMasses.massOf(level.registryAccess(), stack);
        } else {
            return false;
        }
        int n = (int) Math.min(stack.getCount(), Math.floor((WATER_CAPACITY_KG - waterKg) / per));
        if (n <= 0) {
            return false;
        }
        waterKg += n * per;
        if (stack.is(Items.WATER_BUCKET)) {
            player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.BUCKET));
        } else {
            stack.shrink(n);
        }
        player.sendSystemMessage(Component.translatable("message.spacereloaded.eclss.water",
                String.format(Locale.ROOT, "%.1f", n * per), String.format(Locale.ROOT, "%.0f", waterKg)));
        setChanged();
        return true;
    }

    @Override
    public MachineStatusPayload status(ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        List<MachineStatusPayload.Gauge> gauges = new ArrayList<>();
        if (!problem.isEmpty()) {
            lines.add(Component.translatable("status.spacereloaded.eclss." + problem).withColor(0xE0B23C));
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int kind = socketKind(i);
            if (kind != NONE) {
                names.add(Component.translatable("module.spacereloaded." + switch (kind) {
                    case OGS -> "ogs";
                    case SABATIER -> "sabatier";
                    case CDRA -> "cdra";
                    default -> "wrs";
                }).getString());
            }
        }
        lines.add(Component.translatable("status.spacereloaded.eclss.modules",
                names.isEmpty() ? Component.translatable("module.spacereloaded.none").getString() : String.join(", ", names)));
        SealedZone zone = LifeSupportState.zoneAround(level, getBlockPos());
        LifeSupportState.Gas gas = zone != null && zone.isSealed() ? LifeSupportState.now(level, zone) : null;
        if (gas != null) {
            int people = 0;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && zone.volume().contains(p.blockPosition().asLong())) {
                    people++;
                }
            }
            lines.add(Component.translatable("status.spacereloaded.eclss.crew", people, f2(gas.pO2()), f2(gas.pCo2())));
            gauges.add(new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.po2"),
                    (float) (gas.pO2() / 25.0), gas.pO2() < 19.5 ? 0xDD4B4B : 0x57C4C4));
            gauges.add(new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.pco2"),
                    (float) (gas.pCo2() / 1.0), gas.pCo2() >= 0.53 ? 0xDD4B4B : 0x8FE0A8));
        }
        lines.add(Component.translatable("status.spacereloaded.eclss.o2", f3(o2Rate), f3(o2Rate * EclssBalance.WATER_PER_O2)));
        lines.add(Component.translatable("status.spacereloaded.eclss.co2", f3(co2Rate),
                String.format(Locale.ROOT, "%.0f", 100 * reducedFraction)));
        lines.add(Component.translatable("status.spacereloaded.eclss.water", f2(waterKg), f3(waterBack), f3(makeup)));
        lines.add(Component.translatable(ch4ToFuel ? "status.spacereloaded.eclss.ch4_fuel" : "status.spacereloaded.eclss.ch4_vent",
                f3(ch4Rate)));
        lines.add(Component.translatable("status.spacereloaded.eclss.power", f2(powerKw)));
        gauges.add(0, new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.water"),
                (float) (waterKg / WATER_CAPACITY_KG), 0x3C8CD8));
        return new MachineStatusPayload(getBlockPos(), Component.translatable("screen.spacereloaded.eclss"), lines, gauges,
                List.of());
    }

    private static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String f3(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        structure.save(output);
        output.putDouble("water", waterKg);
        output.putDouble("h2", h2Kg);
        output.putDouble("ch4", ch4Kg);
        output.putInt("sockets", sockets);
        output.putInt("running", running);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        structure.load(input);
        claimed = false;
        waterKg = input.getDoubleOr("water", 0);
        h2Kg = input.getDoubleOr("h2", 0);
        ch4Kg = input.getDoubleOr("ch4", 0);
        sockets = input.getIntOr("sockets", 0);
        running = input.getIntOr("running", 0);
    }

    /** Стенд: положить воду. */
    public void testWater(double kg) {
        waterKg = kg;
    }
}
