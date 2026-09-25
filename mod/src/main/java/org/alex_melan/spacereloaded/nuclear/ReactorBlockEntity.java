package org.alex_melan.spacereloaded.nuclear;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.nuclear.PointKinetics;
import org.alex_melan.spacereloaded.core.nuclear.ReactorRegulator;
import org.alex_melan.spacereloaded.core.nuclear.ReactorThermal;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.industry.IndustryStructures;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.FormedStructure;
import org.alex_melan.spacereloaded.multiblock.HammerTarget;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.multiblock.StatusProvider;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;
import org.alex_melan.spacereloaded.network.Thermal;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Реактор деления Kilopower (008, US2, D83); ключ — привод стержня внизу мачты. Каждый тик —
 * точечная кинетика ({@link PointKinetics}) и тепло ({@link ReactorThermal}) со временем ×10
 * (пуск KRUSTY занимал часы, форма процессов сохраняется). Реактивность: запас корзины − эффективность
 * стержня (S-кривая) − обратная связь α_T·(T − T₀). Режимы: ручной (цель стержня) и регулятор
 * ({@link ReactorRegulator}, уставка температуры, ввод ≤ +0.05 $). SCRAM — кнопкой или сигналом
 * редстоуна: стержень падает под пружиной, остаточное тепло — по Вэю–Вигнеру. ρ ≥ 1 $ — мгновенная
 * критичность и расплав. Электричество — в сеть по шкале генераторов мода (калибровка по РИТЭГу
 * MMRTG: 4 E/тик ≈ 110 Вт(э)).
 */
public class ReactorBlockEntity extends MachineBlockEntity
        implements HammerTarget, StatusProvider, ControllerBlock.ItemAcceptor, IndustryStructures.StructureOwner {

    public static final double TIME_SCALE = 10;
    public static final double T_REF = 300;
    /** Скорость привода, доля хода в секунду физического времени (полный ход — 100 с). */
    public static double rodSpeed() { return org.alex_melan.spacereloaded.SpaceReloaded.config().reactorRodSpeedPerS; }
    public static double rodWorth() { return org.alex_melan.spacereloaded.SpaceReloaded.config().reactorRodWorthDollars; }
    public static double alphaPerK() { return org.alex_melan.spacereloaded.SpaceReloaded.config().reactorAlphaCentsPerK / 100; }
    /** Падение стержня при SCRAM: полный ход за 1 с физического времени. */
    public static final double SCRAM_SPEED = 1.0;
    public static final double GRID_E_PER_TICK_PER_W = 4.0 / 110;
    public static final int[][] POWER_SLOTS = {{1, 2, 0}, {-1, 2, 0}, {0, 2, 1}, {0, 2, -1}};
    /** Пассивные потери зоны в конструкцию и опору, Вт/K (**оценка**). */
    public static final double PASSIVE_W_PER_K = 3;

    private final FormedStructure structure = new FormedStructure();
    private boolean claimed;
    private double rodPos;
    private double rodTarget;
    private boolean auto;
    private double setpoint = 1073;
    private boolean scram;
    private double power = PointKinetics.SOURCE_W;
    private double temperature = T_REF;
    private double prevRho = Double.NaN;
    private double u235 = -1;
    private float enrichment;
    private double burnupMwd;
    private int damage;
    private int stirlings;
    private int panels;
    private double envK = T_REF;
    private double electricW;
    private double heatOutW;
    private double coldK;
    private double decayW;
    private double scramPower;
    private double sinceScram;
    private double operatingS;
    private double energyFraction;
    private int syncCooldown;
    private double sentRod = -1, sentTemp = -1, sentElectric = -1;
    /** Анимация — только в клиентском экземпляре. */
    public Object clientAnim;

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR, pos, state, 200_000, 0, 20_000);
    }

    /** Топливная корзина с заданной массой U-235 и обогащением. */
    public static ItemStack basket(double u235Kg, float enrichment) {
        ItemStack stack = new ItemStack(ModItems.FUEL_BASKET);
        stack.set(ModDataComponents.FUEL_U235, (float) u235Kg);
        stack.set(ModDataComponents.ENRICHMENT, enrichment);
        stack.set(ModDataComponents.FUEL_BURNUP, 0f);
        return stack;
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
        readStructure(level);
        return ok;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            structure.dismantle(serverLevel, pos, state.getValue(ControllerBlock.FACING), state.getBlock());
            if (u235 >= 0) {
                ItemStack b = basket(u235, enrichment);
                b.set(ModDataComponents.FUEL_BURNUP, (float) burnupMwd);
                net.minecraft.world.Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, b);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    // --- чтение для рендера и стенда ---
    public double rodPosition() { return rodPos; }
    public double temperature() { return temperature; }
    public double electricW() { return electricW; }
    public double powerW() { return power + decayW; }
    public boolean scrammed() { return scram; }
    public int damage() { return damage; }
    public int stirlings() { return stirlings; }
    public int radiatorSegments() { return structure.repeats(); }
    public boolean formed() { return getBlockState().getValue(ControllerBlock.FORMED); }
    /** Какие гнёзда заняты Стирлингами (битовая маска по {@link #POWER_SLOTS}). */
    private int stirlingMask;
    public int stirlingMask() { return stirlingMask; }

    public double reactivity() {
        if (u235 < 0 || damage >= 2) {
            return -9;
        }
        return PointKinetics.fuelExcess(u235) - rodWorth() + PointKinetics.rodWorth(rodPos, rodWorth())
                - alphaPerK() * (temperature - T_REF);
    }

    public static void serverTick(ReactorBlockEntity be, ServerLevel level) {
        be.tick(level);
    }

    private void tick(ServerLevel level) {
        if (!claimed) {
            claimed = true;
            structure.reclaim(level, getBlockPos(), face());
            readStructure(level);
        }
        if (structure.revalidate(level, getBlockPos(), face())) {
            readStructure(level);
            setChanged();
        }
        long now = level.getGameTime();
        if (now % 20 == 0) {
            ensureAdjacentCableNetworks(level);
            readStructure(level);
            envK = Thermal.temperature(level, getBlockPos()) + 273.15;
        }
        double dt = 0.05 * TIME_SCALE;
        boolean formed = structure.formed();
        if (level.hasNeighborSignal(getBlockPos()) && !scram) {
            scram(level, "redstone");
        }
        // привод стержня
        if (scram || !formed) {
            rodTarget = 0;
            rodPos = Math.max(0, rodPos - SCRAM_SPEED * dt);
        } else {
            if (auto) {
                double dTdt = (power + decayW - heatOutW - PASSIVE_W_PER_K * (temperature - envK))
                        / ReactorThermal.HEAT_CAPACITY_J_PER_K;
                // следящий привод: сдвиг на Δρ по местному наклону S-кривой, не быстрее привода
                double lo = Math.max(0, rodPos - 1e-3), hi = Math.min(1, rodPos + 1e-3);
                double slope = (PointKinetics.rodWorth(hi, rodWorth()) - PointKinetics.rodWorth(lo, rodWorth())) / (hi - lo);
                rodTarget = Math.max(0, Math.min(1, rodPos
                        + ReactorRegulator.rodDelta(reactivity(), temperature, dTdt, setpoint, slope, rodSpeed() * dt)));
            }
            double step = rodSpeed() * dt;
            rodPos += Math.max(-step, Math.min(step, rodTarget - rodPos));
        }
        // кинетика
        double rho = reactivity();
        if (u235 >= 0 && damage < 2) {
            if (PointKinetics.promptCritical(rho)) {
                meltdown(level);
            } else {
                if (!Double.isNaN(prevRho)) {
                    power = PointKinetics.jump(power, prevRho, rho);
                }
                power = PointKinetics.evolve(power, rho, dt);
                prevRho = rho;
            }
        } else {
            power = 0;
            prevRho = Double.NaN;
        }
        if (scram || damage >= 2) {
            sinceScram += dt;
            decayW = scramPower * PointKinetics.decayHeatFraction(sinceScram, operatingS);
        } else {
            decayW = 0;
        }
        // тепло: Стирлинги → электричество и радиатор; пассивные потери
        int workingStirlings = damage >= 2 ? 0 : stirlings;
        double[] out = formed ? ReactorThermal.extract(temperature, workingStirlings, panels, envK) : new double[] {0, 0, envK};
        heatOutW = out[0];
        electricW = out[1] * (damage == 1 ? 0.5 : 1);
        coldK = out[2];
        double passive = PASSIVE_W_PER_K * (temperature - envK);
        temperature = ReactorThermal.step(temperature, power + decayW, heatOutW + passive, dt);
        if (temperature > ReactorThermal.MELT_K && damage < 2) {
            meltdown(level);
        } else if (temperature > ReactorThermal.DAMAGE_K && damage < 1) {
            damage = 1;
            level.playSound(null, getBlockPos(), SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
        }
        if (u235 >= 0 && power > 0) {
            u235 = Math.max(0, u235 - PointKinetics.burnedKg(power * dt));
            burnupMwd += power * dt / 8.64e10;
            if (power > 100) {
                operatingS += dt;
            }
        }
        // энергия в сеть
        energyFraction += electricW * GRID_E_PER_TICK_PER_W;
        long e = (long) Math.floor(energyFraction);
        if (e > 0) {
            energyFraction -= e;
            energy.amount = Math.min(energy.capacity, energy.amount + e);
        }
        pushEnergyToNeighbors(level);
        boolean active = power + decayW > 100;
        if (getBlockState().getValue(ControllerBlock.ACTIVE) != active) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        if (electricW > 500 && now % 100 == 0) {
            org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 32, org.alex_melan.spacereloaded.industry.IndustryAdvancements.CRITICALITY);
        }
        if (electricW > 10 && now % 60 == 0) {
            level.playSound(null, getBlockPos().above(2), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4f,
                    0.5f + (float) Math.min(0.5, electricW / 2000));
        }
        sync(level);
        setChanged();
    }

    private void scram(ServerLevel level, String reason) {
        scram = true;
        scramPower = power;
        sinceScram = 0;
        auto = false;
        level.playSound(null, getBlockPos(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    private void meltdown(ServerLevel level) {
        if (damage >= 2) {
            return;
        }
        damage = 2;
        scramPower = Math.max(power, scramPower);
        scram = true;
        sinceScram = 0;
        power = 0;
        temperature = Math.max(temperature, ReactorThermal.MELT_K + 100);
        level.playSound(null, getBlockPos(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 0.6f);
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(getBlockPos(), 64)) {
                p.sendSystemMessage(Component.translatable("message.spacereloaded.reactor.meltdown").withColor(0xDD4B4B));
            }
        }
    }

    private void readStructure(ServerLevel level) {
        boolean formed = structure.formed();
        if (getBlockState().getValue(ControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        }
        int n = 0;
        int mask = 0;
        if (formed) {
            for (int i = 0; i < POWER_SLOTS.length; i++) {
                int[] o = POWER_SLOTS[i];
                if (level.getBlockState(MultiblockTemplates.worldPos(getBlockPos(), face(), o[0], o[1], o[2]))
                        .is(ModBlocks.STIRLING_CONVERTOR)) {
                    n++;
                    mask |= 1 << i;
                }
            }
        }
        stirlings = n;
        stirlingMask = mask;
        panels = formed ? 4 * structure.repeats() : 0;
    }

    private void sync(ServerLevel level) {
        if (--syncCooldown > 0) {
            return;
        }
        if (Math.abs(rodPos - sentRod) > 0.002 || Math.abs(temperature - sentTemp) > 3
                || Math.abs(electricW - sentElectric) > 20 || scram && sentRod != rodPos) {
            sentRod = rodPos;
            sentTemp = temperature;
            sentElectric = electricW;
            syncCooldown = 4;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean accept(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.FUEL_BASKET)) {
            return false;
        }
        if (u235 >= 0 || rodPos > 0.001 || temperature > 400) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.reactor.basket_refused"));
            return true;
        }
        u235 = stack.getOrDefault(ModDataComponents.FUEL_U235, 0f);
        enrichment = stack.getOrDefault(ModDataComponents.ENRICHMENT, 0f);
        burnupMwd = stack.getOrDefault(ModDataComponents.FUEL_BURNUP, 0f);
        stack.shrink(1);
        power = PointKinetics.SOURCE_W;
        prevRho = Double.NaN;
        player.sendSystemMessage(Component.translatable("message.spacereloaded.reactor.basket_in",
                String.format(Locale.ROOT, "%.1f", u235), String.format(Locale.ROOT, "%.1f", enrichment * 100),
                String.format(Locale.ROOT, "%.2f", PointKinetics.fuelExcess(u235))));
        setChanged();
        return true;
    }

    @Override
    public void action(ServerLevel level, ServerPlayer player, String action, double value) {
        switch (action) {
            case "rod" -> {
                if (!scram) {
                    auto = false;
                    rodTarget = Math.max(0, Math.min(1, rodTarget + value));
                }
            }
            case "auto" -> {
                if (!scram && damage < 2) {
                    auto = !auto;
                    rodTarget = rodPos;
                }
            }
            case "setpoint" -> setpoint = Math.max(500, Math.min(1250, setpoint + value));
            case "scram" -> scram(level, "operator");
            case "reset" -> {
                if (damage < 2 && rodPos <= 0.001) {
                    scram = false;
                    decayW = 0;
                }
            }
            case "unload" -> {
                if (u235 >= 0 && rodPos <= 0.001 && temperature < 400 && damage < 2) {
                    ItemStack b = basket(u235, enrichment);
                    b.set(ModDataComponents.FUEL_BURNUP, (float) burnupMwd);
                    if (!player.getInventory().add(b)) {
                        player.drop(b, false);
                    }
                    u235 = -1;
                    power = 0;
                } else {
                    player.sendSystemMessage(Component.translatable("message.spacereloaded.reactor.basket_refused"));
                }
            }
            default -> {
            }
        }
        setChanged();
    }

    @Override
    public MachineStatusPayload status(ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        if (!structure.formed()) {
            lines.add(Component.translatable("status.spacereloaded.reactor.not_formed").withColor(0xE0B23C));
        }
        if (damage >= 2) {
            lines.add(Component.translatable("status.spacereloaded.reactor.melted").withColor(0xDD4B4B));
        } else if (damage == 1) {
            lines.add(Component.translatable("status.spacereloaded.reactor.damaged").withColor(0xE0B23C));
        }
        if (scram) {
            lines.add(Component.translatable("status.spacereloaded.reactor.scram").withColor(0xDD4B4B));
        }
        lines.add(auto ? Component.translatable("status.spacereloaded.reactor.auto", f0(setpoint))
                : Component.translatable("status.spacereloaded.reactor.manual"));
        lines.add(Component.translatable("status.spacereloaded.reactor.power", f2((power + decayW) / 1000),
                f2(electricW / 1000), stirlings));
        lines.add(Component.translatable("status.spacereloaded.reactor.temp", f0(temperature), f0(coldK), f0(envK)));
        double rho = reactivity();
        lines.add(Component.translatable("status.spacereloaded.reactor.rho", rho < -8 ? "—" : f0(rho * 100),
                rho <= 0 || rho < -8 ? "∞" : f0(PointKinetics.period(rho) / TIME_SCALE)));
        lines.add(Component.translatable("status.spacereloaded.reactor.rod", f0(rodPos * 100), f0(rodTarget * 100)));
        lines.add(u235 < 0 ? Component.translatable("status.spacereloaded.reactor.no_fuel")
                : Component.translatable("status.spacereloaded.reactor.fuel", f2(u235), f0(enrichment * 100), f2(burnupMwd)));
        lines.add(Component.translatable("status.spacereloaded.reactor.radiator", panels * 2, f2(decayW / 1000)));
        List<MachineStatusPayload.Gauge> gauges = List.of(
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.rod"), (float) rodPos, 0xE0B23C),
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.core_temp"),
                        (float) (temperature / ReactorThermal.MELT_K),
                        temperature > ReactorThermal.DAMAGE_K ? 0xDD4B4B : temperature > setpoint + 50 ? 0xE0B23C : 0x57C4C4),
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.electric"),
                        (float) (electricW / Math.max(1, stirlings * 250.0)), 0x8FE0A8));
        List<MachineStatusPayload.Action> actions = List.of(
                new MachineStatusPayload.Action("rod", Component.literal("▼5%"), -0.05),
                new MachineStatusPayload.Action("rod", Component.literal("▲5%"), 0.05),
                new MachineStatusPayload.Action("auto", Component.translatable("action.spacereloaded.reactor.auto"), 0),
                new MachineStatusPayload.Action("setpoint", Component.literal("−25K"), -25),
                new MachineStatusPayload.Action("setpoint", Component.literal("+25K"), 25),
                new MachineStatusPayload.Action("scram", Component.literal("SCRAM"), 0),
                new MachineStatusPayload.Action("reset", Component.translatable("action.spacereloaded.reactor.reset"), 0),
                new MachineStatusPayload.Action("unload", Component.translatable("action.spacereloaded.reactor.unload"), 0));
        return new MachineStatusPayload(getBlockPos(), Component.translatable("screen.spacereloaded.reactor"), lines, gauges,
                actions);
    }

    private static String f0(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }

    private static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Стенд: быстрые установки. */
    public void testSetup(double u235Kg, boolean autoMode, double setpointK) {
        u235 = u235Kg;
        enrichment = 0.93f;
        auto = autoMode;
        setpoint = setpointK;
        power = PointKinetics.SOURCE_W;
        prevRho = Double.NaN;
    }

    public void testRodTarget(double h) {
        auto = false;
        rodTarget = h;
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
        output.putDouble("rod", rodPos);
        output.putDouble("rod_target", rodTarget);
        output.putBoolean("auto", auto);
        output.putDouble("setpoint", setpoint);
        output.putBoolean("scram", scram);
        output.putDouble("power", power);
        output.putDouble("temp", temperature);
        output.putDouble("u235", u235);
        output.putFloat("enrichment", enrichment);
        output.putDouble("burnup", burnupMwd);
        output.putInt("damage", damage);
        output.putDouble("electric", electricW);
        output.putDouble("scram_power", scramPower);
        output.putDouble("since_scram", sinceScram);
        output.putDouble("operating", operatingS);
        output.putInt("stirling_mask", stirlingMask);
        output.putInt("stirlings", stirlings);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        structure.load(input);
        claimed = false;
        rodPos = input.getDoubleOr("rod", 0);
        rodTarget = input.getDoubleOr("rod_target", 0);
        auto = input.getBooleanOr("auto", false);
        setpoint = input.getDoubleOr("setpoint", 1073);
        scram = input.getBooleanOr("scram", false);
        power = input.getDoubleOr("power", PointKinetics.SOURCE_W);
        temperature = input.getDoubleOr("temp", T_REF);
        u235 = input.getDoubleOr("u235", -1);
        enrichment = input.getFloatOr("enrichment", 0);
        burnupMwd = input.getDoubleOr("burnup", 0);
        damage = input.getIntOr("damage", 0);
        electricW = input.getDoubleOr("electric", 0);
        scramPower = input.getDoubleOr("scram_power", 0);
        sinceScram = input.getDoubleOr("since_scram", 0);
        operatingS = input.getDoubleOr("operating", 0);
        stirlingMask = input.getIntOr("stirling_mask", 0);
        stirlings = input.getIntOr("stirlings", 0);
        prevRho = Double.NaN;
    }
}
