package org.alex_melan.spacereloaded.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.metallurgy.ArcFurnace;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.industry.IndustryStructures;
import org.alex_melan.spacereloaded.lifesupport.EnergyScale;
import org.alex_melan.spacereloaded.lifesupport.GasKind;
import org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.FormedStructure;
import org.alex_melan.spacereloaded.multiblock.HammerTarget;
import org.alex_melan.spacereloaded.multiblock.StatusProvider;
import org.alex_melan.spacereloaded.multiblock.StructurePorts;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;
import org.alex_melan.spacereloaded.registry.ItemMasses;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Дуговая сталеплавильная печь (008, US5, D86): партия — свод открыт, в ванну закладывается шихта
 * (железо, лом, метеоритное железо) и известь (кальцит — флюс шлака); свод закрывается, электроды
 * опускаются, дуга вводит энергию {@link ArcFurnace#meltJ} при мощности трансформатора 5 МВт;
 * кислородная продувка (баллон O₂ у корпуса) выжигает углерод и экономит электричество; затем печь
 * наклоняется и сливает сталь через носок. Электроды расходуются 1.8 кг на тонну.
 */
public class ArcFurnaceBlockEntity extends MachineBlockEntity
        implements HammerTarget, StatusProvider, ControllerBlock.ItemAcceptor, IndustryStructures.StructureOwner {

    public enum Phase { OPEN, CLOSING, MELT, REFINE, TAP, OPENING }

    public static final double RATING_W = 5e6;
    public static final double CAPACITY_KG = 5000;
    public static final int CLOSE_TICKS = 60;
    public static final int REFINE_TICKS = 100;
    public static final int TAP_TICKS = 120;
    public static final double ELECTRODE_KG = 40;
    public static final double LIME_FRACTION = 0.02;
    private static final TagKey<net.minecraft.world.item.Item> CHARGE = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("spacereloaded", "eaf_charge"));

    private final FormedStructure structure = new FormedStructure();
    private boolean claimed;
    private Phase phase = Phase.OPEN;
    private long phaseStart;
    private double chargeKg;
    private double limeKg;
    private double meltedJ;
    private double needJ;
    private double electrodeKg;
    private boolean oxygenBlow;
    private double powerW;
    private double energyDebt;
    private String lastTap = "";
    public Object clientAnim;

    public ArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EAF, pos, state, 200_000, 20_000, 0);
    }

    private Direction face() {
        return getBlockState().getValue(ControllerBlock.FACING);
    }

    public Phase phase() { return phase; }
    public long phaseStart() { return phaseStart; }
    public double meltFraction() { return needJ > 0 ? Math.min(1, meltedJ / needJ) : 0; }
    public double chargeKg() { return chargeKg; }
    public double electrodeKg() { return electrodeKg; }
    public boolean formed() { return getBlockState().getValue(ControllerBlock.FORMED); }
    public double powerW() { return powerW; }

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

    public static void serverTick(ArcFurnaceBlockEntity be, ServerLevel level) {
        be.tick(level);
    }

    private void setPhase(ServerLevel level, Phase next) {
        phase = next;
        phaseStart = level.getGameTime();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        setChanged();
    }

    private void tick(ServerLevel level) {
        if (!claimed) {
            claimed = true;
            structure.reclaim(level, getBlockPos(), face());
        }
        if (structure.revalidate(level, getBlockPos(), face())) {
            setChanged();
        }
        long now = level.getGameTime();
        long age = now - phaseStart;
        if (!structure.formed()) {
            if (phase != Phase.OPEN) {
                // структура разрушена: расплав уходит в шлак
                chargeKg = 0;
                meltedJ = 0;
                setPhase(level, Phase.OPEN);
            }
            setFormed(level);
            return;
        }
        switch (phase) {
            case CLOSING -> {
                if (age >= CLOSE_TICKS) {
                    needJ = ArcFurnace.meltJ(chargeKg, oxygenBlow);
                    meltedJ = 0;
                    setPhase(level, Phase.MELT);
                }
            }
            case MELT -> melt(level, now);
            case REFINE -> {
                if (age >= REFINE_TICKS) {
                    setPhase(level, Phase.TAP);
                }
            }
            case TAP -> {
                if (age == TAP_TICKS / 2) {
                    tap(level);
                }
                if (age >= TAP_TICKS) {
                    setPhase(level, Phase.OPENING);
                }
            }
            case OPENING -> {
                if (age >= CLOSE_TICKS) {
                    setPhase(level, Phase.OPEN);
                }
            }
            default -> {
            }
        }
        if (now % 20 == 0) {
            ensureAdjacentCableNetworks(level);
            setFormed(level);
        }
        boolean active = phase == Phase.MELT && powerW > 0;
        if (getBlockState().getValue(ControllerBlock.ACTIVE) != active) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    /** Дуга: энергия из сети до мощности трансформатора; продувка O₂ тратит кислород по мере плавки. */
    private void melt(ServerLevel level, long now) {
        // долг энергии: целые E списываются по мере накопления — средняя мощность ровно номинал
        energyDebt += EnergyScale.fromJoules(RATING_W * 0.05);
        long take = Math.min(energy.amount, (long) Math.floor(energyDebt));
        energyDebt -= take;
        if (take == 0 && energy.amount == 0) {
            energyDebt = Math.min(energyDebt, EnergyScale.fromJoules(RATING_W * 0.05));
        }
        double got = take / EnergyScale.E_PER_KWH * 3.6e6;
        energy.amount -= take;
        double before = meltFraction();
        meltedJ += got;
        powerW += (got / 0.05 - powerW) * 0.1;   // сглаженная мощность для экрана
        double dFrac = meltFraction() - before;
        electrodeKg = Math.max(0, electrodeKg - ArcFurnace.electrodeKg(chargeKg, org.alex_melan.spacereloaded.SpaceReloaded.config().eafElectrodeKgPerTonne) * dFrac);
        if (oxygenBlow) {
            double need = ArcFurnace.oxygenKg(chargeKg) * dFrac;
            double pulled = 0;
            for (BlockPos p : StructurePorts.around(level, getBlockPos(), face(), 0)) {
                if (pulled >= need) {
                    break;
                }
                if (level.getBlockEntity(p) instanceof GasTankBlockEntity t && t.kind() == GasKind.OXYGEN) {
                    pulled += t.extract(GasKind.OXYGEN, need - pulled);
                }
            }
            if (pulled < need * 0.999) {
                // кислород кончился — остаток плавки только дугой
                oxygenBlow = false;
                needJ = meltedJ + ArcFurnace.meltJ(chargeKg, false) * (1 - meltFraction());
            }
        }
        if (now % 10 == 0 && powerW > 0) {
            level.playSound(null, getBlockPos().above(2), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.8f,
                    0.5f + level.getRandom().nextFloat() * 0.3f);
        }
        if (electrodeKg <= 0) {
            powerW = 0;
            meltedJ -= got; // без электродов дуги нет
        }
        if (meltedJ >= needJ) {
            powerW = 0;
            setPhase(level, Phase.REFINE);
        }
        if (now % 40 == 0) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Слив через носок: сталь слитками у носка (справа от лица трансформатора), шлак отдельно. */
    private void tap(ServerLevel level) {
        double steelIngot = ItemMasses.massOf(level.registryAccess(), new ItemStack(ModItems.STEEL_INGOT));
        double loss = limeKg >= LIME_FRACTION * chargeKg ? ArcFurnace.SLAG_LOSS : 0.15;
        double steel = chargeKg * (1 - loss);
        int ingots = (int) Math.floor(steel / steelIngot);
        double slagItem = ItemMasses.massOf(level.registryAccess(), new ItemStack(ModItems.SLAG));
        int slag = (int) Math.max(1, Math.round((chargeKg * loss + limeKg) / Math.max(1, slagItem)));
        BlockPos out = org.alex_melan.spacereloaded.multiblock.MultiblockTemplates.worldPos(getBlockPos(), face(), 3, 0, 2);
        while (ingots > 0) {
            int n = Math.min(64, ingots);
            level.addFreshEntity(new ItemEntity(level, out.getX() + 0.5, out.getY() + 0.5, out.getZ() + 0.5,
                    new ItemStack(ModItems.STEEL_INGOT, n)));
            ingots -= n;
        }
        level.addFreshEntity(new ItemEntity(level, out.getX() + 0.5, out.getY() + 0.5, out.getZ() + 0.5,
                new ItemStack(ModItems.SLAG, Math.min(64, slag))));
        level.sendParticles(ParticleTypes.LAVA, out.getX() + 0.5, out.getY() + 1.2, out.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, out, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
        lastTap = String.format(Locale.ROOT, "%.0f/%.0f", steel, chargeKg);
        org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 32, org.alex_melan.spacereloaded.industry.IndustryAdvancements.FIRST_MELT);
        chargeKg = 0;
        limeKg = 0;
        meltedJ = 0;
        needJ = 0;
    }

    private void setFormed(ServerLevel level) {
        boolean formed = structure.formed();
        if (getBlockState().getValue(ControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean accept(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (stack.is(ModItems.GRAPHITE_ELECTRODE)) {
            int n = (int) Math.min(stack.getCount(), Math.floor((3 * ELECTRODE_KG - electrodeKg) / ELECTRODE_KG));
            if (n <= 0) {
                return false;
            }
            electrodeKg += n * ELECTRODE_KG;
            stack.shrink(n);
            setChanged();
            return true;
        }
        if (phase != Phase.OPEN) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.eaf.closed"));
            return true;
        }
        double per = ItemMasses.massOf(level.registryAccess(), stack);
        boolean lime = stack.is(Items.CALCITE);
        if (!lime && !stack.is(CHARGE)) {
            return false;
        }
        int n = (int) Math.min(stack.getCount(), Math.floor((CAPACITY_KG - chargeKg - limeKg) / Math.max(0.1, per)));
        if (n <= 0) {
            return false;
        }
        if (lime) {
            limeKg += n * per;
        } else {
            chargeKg += n * per;
        }
        stack.shrink(n);
        setChanged();
        return true;
    }

    @Override
    public void action(ServerLevel level, ServerPlayer player, String action, double value) {
        switch (action) {
            case "melt" -> {
                if (phase == Phase.OPEN && structure.formed() && chargeKg > 0 && electrodeKg > 0) {
                    setPhase(level, Phase.CLOSING);
                    level.playSound(null, getBlockPos().above(2), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.5f);
                }
            }
            case "oxygen" -> {
                if (phase == Phase.OPEN || phase == Phase.CLOSING) {
                    oxygenBlow = !oxygenBlow;
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
            lines.add(Component.translatable("status.spacereloaded.eaf.not_formed").withColor(0xE0B23C));
        }
        lines.add(Component.translatable("status.spacereloaded.eaf.phase." + phase.name().toLowerCase(Locale.ROOT)));
        lines.add(Component.translatable("status.spacereloaded.eaf.charge", f0(chargeKg), f0(limeKg), f0(CAPACITY_KG),
                limeKg >= LIME_FRACTION * chargeKg ? "5" : "15"));
        double need = phase == Phase.OPEN ? ArcFurnace.meltJ(chargeKg, oxygenBlow) : needJ;
        lines.add(Component.translatable("status.spacereloaded.eaf.energy", f0(meltedJ / 3.6e6), f0(need / 3.6e6),
                String.format(Locale.ROOT, "%.2f", powerW / 1e6),
                String.format(Locale.ROOT, "%.1f", Math.max(0, need - meltedJ) / RATING_W / 60)));
        lines.add(Component.translatable(oxygenBlow ? "status.spacereloaded.eaf.o2_on" : "status.spacereloaded.eaf.o2_off",
                f0(ArcFurnace.oxygenKg(chargeKg))));
        lines.add(Component.translatable("status.spacereloaded.eaf.electrodes", f0(electrodeKg), f0(3 * ELECTRODE_KG)));
        if (!lastTap.isEmpty()) {
            lines.add(Component.translatable("status.spacereloaded.eaf.last", lastTap));
        }
        List<MachineStatusPayload.Gauge> gauges = List.of(
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.melt"), (float) meltFraction(), 0xFF8A30),
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.charge"), (float) (chargeKg / CAPACITY_KG), 0x9CA0A8));
        List<MachineStatusPayload.Action> actions = List.of(
                new MachineStatusPayload.Action("melt", Component.translatable("action.spacereloaded.eaf.melt"), 0),
                new MachineStatusPayload.Action("oxygen", Component.translatable("action.spacereloaded.eaf.oxygen"), 0));
        return new MachineStatusPayload(getBlockPos(), Component.translatable("screen.spacereloaded.eaf"), lines, gauges, actions);
    }

    private static String f0(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }

    /** Стенд: шихта, известь, электроды. */
    public void testCharge(double metalKg, double lime, double electrodes) {
        chargeKg = metalKg;
        limeKg = lime;
        electrodeKg = electrodes;
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
        output.putInt("phase", phase.ordinal());
        output.putLong("phase_start", phaseStart);
        output.putDouble("charge", chargeKg);
        output.putDouble("lime", limeKg);
        output.putDouble("melted", meltedJ);
        output.putDouble("need", needJ);
        output.putDouble("electrodes", electrodeKg);
        output.putBoolean("oxygen", oxygenBlow);
        output.putDouble("power", powerW);
        output.putString("last", lastTap);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        structure.load(input);
        claimed = false;
        phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, input.getIntOr("phase", 0)))];
        phaseStart = input.getLongOr("phase_start", 0);
        chargeKg = input.getDoubleOr("charge", 0);
        limeKg = input.getDoubleOr("lime", 0);
        meltedJ = input.getDoubleOr("melted", 0);
        needJ = input.getDoubleOr("need", 0);
        electrodeKg = input.getDoubleOr("electrodes", 0);
        oxygenBlow = input.getBooleanOr("oxygen", false);
        powerW = input.getDoubleOr("power", 0);
        lastTap = input.getStringOr("last", "");
    }
}
