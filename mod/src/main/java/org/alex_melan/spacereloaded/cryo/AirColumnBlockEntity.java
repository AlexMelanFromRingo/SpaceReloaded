package org.alex_melan.spacereloaded.cryo;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.cryo.AirSeparation;
import org.alex_melan.spacereloaded.industry.IndustryStructures;
import org.alex_melan.spacereloaded.lifesupport.GasKind;
import org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.FormedStructure;
import org.alex_melan.spacereloaded.multiblock.HammerTarget;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.multiblock.StatusProvider;
import org.alex_melan.spacereloaded.multiblock.StructurePorts;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Криогенная воздухоразделительная колонна (008, US4, D85). Компрессор на валу даёт поток воздуха
 * P/0.061 кВт·ч/кг; число тарелок N — чистоту продуктов по Фенске ({@link AirSeparation}); отбор
 * кислорода и азота — по правилу рычага от чистот (мало тарелок — мало и нечисто), потери 5 %.
 * Кислород — в баллоны O₂, азот — в баллоны N₂ у любого блока колонны, аргон — в баллоны при N ≥ 20.
 * Работает только в воздухе с кислородом (Земля): у CO₂-атмосферы Марса кислорода для колонны нет.
 */
public class AirColumnBlockEntity extends BlockEntity
        implements HammerTarget, StatusProvider, IndustryStructures.StructureOwner {

    public static final double ARGON_KG_PER_CANISTER = 10;
    /** Массовая доля «тяжёлой» части (O₂ + Ar) воздуха для правила рычага. */
    private static final double HEAVY = AirSeparation.O2_MASS + AirSeparation.AR_MASS;

    private final FormedStructure structure = new FormedStructure();
    private boolean claimed;
    private boolean running;
    private double shaftW, airKgS, o2KgS, n2KgS, argonKgS, argonKg, ventedKgS;
    private String problem = "";
    public Object clientAnim;

    public AirColumnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASU, pos, state);
    }

    private Direction face() {
        return getBlockState().getValue(ControllerBlock.FACING);
    }

    public int trays() {
        return structure.formed() ? structure.repeats() : 0;
    }

    public boolean running() {
        return running;
    }

    public boolean formed() {
        return getBlockState().getValue(ControllerBlock.FORMED);
    }

    public double airKgS() {
        return airKgS;
    }

    public double o2KgS() {
        return o2KgS;
    }

    /** Настоящая чистота кислорода: без бокового отбора аргона (N < 20) он остаётся в кислороде. */
    public static double oxygenPurity(int n) {
        double p = AirSeparation.oxygenPurity(n);
        return n >= AirSeparation.ARGON_TRAYS ? p : p * AirSeparation.O2_MASS / HEAVY;
    }

    public double argonKg() {
        return argonKg;
    }

    @Override
    public void markStructureDirty() {
        structure.markDirty();
    }

    @Override
    public boolean hammer(ServerLevel level, ServerPlayer player) {
        boolean ok = structure.hammer(level, getBlockPos(), face(), player);
        setFormed(level);
        return ok;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            structure.dismantle(serverLevel, pos, state.getValue(ControllerBlock.FACING), state.getBlock());
        }
        super.preRemoveSideEffects(pos, state);
    }

    public static void serverTick(AirColumnBlockEntity be, ServerLevel level) {
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
        setFormed(level);
        boolean was = running;
        running = false;
        shaftW = airKgS = o2KgS = n2KgS = argonKgS = ventedKgS = 0;
        problem = "";
        if (!structure.formed()) {
            problem = "not_formed";
        } else if (!PlanetManager.isBreathable(level)) {
            problem = "no_air";
        } else if (level.getBlockEntity(MultiblockTemplates.worldPos(getBlockPos(), face(), 2, 0, 0))
                instanceof AsuCompressorBlockEntity compressor) {
            shaftW = compressor.absorbedW();
            if (shaftW < 5_000) {
                problem = "no_shaft";
            } else {
                separate(level, trays());
            }
        }
        if (running && level.getGameTime() % 60 == 0) {
            level.playSound(null, getBlockPos().above(trays()), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 0.6f);
        }
        if (was != running) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.ACTIVE, running), Block.UPDATE_CLIENTS);
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    /** Поток за 1 с: правило рычага по чистотам верха (N₂) и низа (O₂ + Ar), потери 5 %. */
    private void separate(ServerLevel level, int n) {
        double feed = AirSeparation.airFlow(shaftW);
        double p = AirSeparation.oxygenPurity(n);
        double q = AirSeparation.nitrogenPurity(n);
        double bottom = feed * (HEAVY - (1 - q)) / (p - (1 - q));
        double top = feed - bottom;
        double argon = n >= AirSeparation.ARGON_TRAYS ? feed * AirSeparation.AR_MASS * AirSeparation.AR_RECOVERY : 0;
        // «тяжёлый» продукт — O₂ с аргоном; кислород — его доля 0.231/0.244, аргон без бокового отбора — примесь
        double o2 = bottom * p * AirSeparation.O2_MASS / HEAVY * AirSeparation.RECOVERY;
        double n2 = Math.max(0, top * q * AirSeparation.RECOVERY);
        double stored = 0;
        List<GasTankBlockEntity> tanks = new ArrayList<>();
        for (BlockPos port : StructurePorts.around(level, getBlockPos(), face(), structure.repeats())) {
            if (level.getBlockEntity(port) instanceof GasTankBlockEntity tank) {
                tanks.add(tank);
            }
        }
        double o2Left = o2, n2Left = n2;
        for (GasTankBlockEntity t : tanks) {
            if (t.kind() == GasKind.OXYGEN) {
                o2Left -= t.insert(GasKind.OXYGEN, o2Left);
            } else if (t.kind() == GasKind.NITROGEN) {
                n2Left -= t.insert(GasKind.NITROGEN, n2Left);
            }
        }
        for (GasTankBlockEntity t : tanks) {
            if (t.kind() == GasKind.NONE && o2Left > 1e-9) {
                o2Left -= t.insert(GasKind.OXYGEN, o2Left);
            } else if (t.kind() == GasKind.NONE && n2Left > 1e-9) {
                n2Left -= t.insert(GasKind.NITROGEN, n2Left);
            }
        }
        running = true;
        airKgS = feed;
        o2KgS = o2 - o2Left;
        n2KgS = n2 - n2Left;
        argonKgS = argon;
        if (argon > 0) {
            org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 32, org.alex_melan.spacereloaded.industry.IndustryAdvancements.ARGON);
        }
        argonKg = Math.min(argonKg + argon, 64 * ARGON_KG_PER_CANISTER);
        ventedKgS = feed - o2KgS - n2KgS - argon;
    }

    private void setFormed(ServerLevel level) {
        boolean formed = structure.formed();
        if (getBlockState().getValue(ControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void action(ServerLevel level, ServerPlayer player, String action, double value) {
        if ("argon".equals(action)) {
            int n = (int) Math.floor(argonKg / ARGON_KG_PER_CANISTER);
            if (n > 0) {
                ItemStack s = new ItemStack(ModItems.ARGON_CANISTER, n);
                if (!player.getInventory().add(s)) {
                    player.drop(s, false);
                }
                argonKg -= n * ARGON_KG_PER_CANISTER;
            }
        }
        setChanged();
    }

    @Override
    public MachineStatusPayload status(ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        if (!problem.isEmpty()) {
            lines.add(Component.translatable("status.spacereloaded.asu." + problem).withColor(0xE0B23C));
        }
        int n = trays();
        lines.add(Component.translatable("status.spacereloaded.asu.trays", n,
                String.format(Locale.ROOT, "%.2f", oxygenPurity(n) * 100),
                String.format(Locale.ROOT, "%.4f", AirSeparation.nitrogenPurity(n) * 100),
                AirSeparation.ARGON_TRAYS));
        lines.add(Component.translatable("status.spacereloaded.asu.flow", f1(shaftW / 1000), f2(airKgS)));
        lines.add(Component.translatable("status.spacereloaded.asu.products", f1(o2KgS * 60), f1(n2KgS * 60),
                f2(argonKgS * 60), f1(ventedKgS * 60)));
        lines.add(Component.translatable(n >= AirSeparation.ARGON_TRAYS ? "status.spacereloaded.asu.argon"
                : "status.spacereloaded.asu.argon_need", f1(argonKg), AirSeparation.ARGON_TRAYS));
        List<MachineStatusPayload.Gauge> gauges = List.of(
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.o2_purity"),
                        (float) ((oxygenPurity(n) - 0.2) / 0.8), 0x57C4C4),
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.shaft"),
                        (float) (shaftW / AsuCompressorBlockEntity.NOMINAL_W), 0x8FE0A8));
        return new MachineStatusPayload(getBlockPos(), Component.translatable("screen.spacereloaded.asu"), lines, gauges,
                List.of(new MachineStatusPayload.Action("argon", Component.translatable("action.spacereloaded.asu.argon"), 0)));
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
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
        output.putBoolean("running", running);
        output.putDouble("argon", argonKg);
        output.putDouble("air", airKgS);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        structure.load(input);
        claimed = false;
        running = input.getBooleanOr("running", false);
        argonKg = input.getDoubleOr("argon", 0);
        airKgS = input.getDoubleOr("air", 0);
    }
}
