package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.electronics.CrystalGrowth;
import org.alex_melan.spacereloaded.core.electronics.Purity;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.energy.EnergyUtil;
import org.alex_melan.spacereloaded.kinetics.KineticMachineBlockEntity;
import org.alex_melan.spacereloaded.kinetics.KineticNetworks;
import org.alex_melan.spacereloaded.machine.ChemicalProcess;
import org.alex_melan.spacereloaded.machine.recipe.MachiningRecipe;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModRecipes;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;
import java.util.Locale;

/**
 * Установка Чохральского (006, FR-414, D62). Загрузка — 3 куска поликремния (массы — по таблице
 * {@code item_mass}: кусок = 1/9 блока 10 л, 2.59 кг) в кварцевый тигель,
 * нагрев от сети (резистивный нагреватель), затравка вытягивается с вращением от вала.
 * <ul>
 *   <li>Устойчивость вращения — коэффициент вариации оборотов за операцию (Уэлфорд): ≤ 5 % —
 *       монокристалл, иначе фронт срывается и растёт поликристалл (солнечные элементы).</li>
 *   <li>Легирование: фосфор (n, k = 0.35) или алюминий (p, k = 0.002). Годная доля слитка по
 *       Шайлю g = 1 − r^(−1/(1−k)) при допуске сопротивления r = 1.5: фосфор — 46 % (5 слитков
 *       Ø50 × 150 мм по 0.69 кг из 7.8 кг), алюминий — 33 % (3 слитка): честный урок, почему p-тип
 *       в Cz легируют бором, а не алюминием. Доза лигатуры мала — одного предмета хватает на 64 загрузки.</li>
 *   <li>Остаток расплава (хвост) застывает в тигле; примесь бора (k = 0.8) по Шайлю уходит в
 *       него — слиток чуть чище загрузки, хвост грязнее. Хвост возвращается поликремнием
 *       своей чистоты (масса и примеси сохраняются), целыми кусками.</li>
 *   <li>Сорвался фронт (нестабильное вращение) — годная часть застывает поликристаллом: тот же
 *       расчёт массы, продукт — мультикремниевый слиток.</li>
 *   <li>Кварцевый тигель растворяется в расплаве и трескается при остывании — один на загрузку,
 *       как на настоящем производстве.</li>
 * </ul>
 * Слоты: 0 — поликремний, 1 — слитки, 2 — тигель, 3 — лигатура, 4 — хвост. Слиток выдаётся
 * атомарно: если выход занят другим видом кристалла, установка ждёт с готовым слитком.
 */
public class CrystalPullerBlockEntity extends KineticMachineBlockEntity<MachiningRecipe.Lathe> {

    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_OUT = 1;
    public static final int SLOT_CRUCIBLE = 2;
    public static final int SLOT_DOPANT = 3;
    public static final int SLOT_TAIL = 4;

    public static final int PULL_TICKS = 600;
    /** Загрузка тигля, кусков поликремния. */
    public static final int CHARGE = 3;
    public static final int DOSES_PER_DOPANT = 64;
    /** Нагреватель: ~150 кВт·ч на кг загрузки (7.8 кг) → 36 000 E за вытягивание. */
    private static final long HEATER_PER_TICK = 60;
    private static final double PULL_TORQUE = 150;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(20_000, 400, 0);
    private int work;
    private double mean;
    private double m2;
    private int samples;
    private boolean pulling;
    private int doses;
    private double dopantK;
    private double tailMass;
    private double tailImpurity;

    public CrystalPullerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_PULLER, pos, state, ModRecipes.MACHINING);
    }

    public EnergyStorage energyStorage() {
        return energy;
    }

    @Override
    protected int slotCount() {
        return 5;
    }

    private static double rpm(double value) {
        return value * 2 * Math.PI / 60;
    }

    @Override
    protected double nominalOmega() {
        return rpm(40);
    }

    @Override
    protected double minOmega() {
        return rpm(20);
    }

    @Override
    protected double maxOmega() {
        return rpm(60);
    }

    @Override
    protected double machineDeltaUm() {
        return 0;
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        return new NodeLoad(0, 0, SpaceReloaded.config().kineticFrictionTorqueNm + (pulling ? PULL_TORQUE : 0), 30);
    }

    /** Коэффициент сегрегации лигатуры или −1. */
    private static double dopantK(ItemStack stack) {
        if (stack.is(ModItems.PHOSPHORUS)) {
            return CrystalGrowth.K_PHOSPHORUS;
        }
        if (stack.is(ModItems.ALUMINIUM_INGOT)) {
            return CrystalGrowth.K_ALUMINIUM;
        }
        return -1;
    }

    /** Доза лигатуры на загрузку: остаток дозы или новый предмет. */
    private double availableDopant() {
        if (doses > 0) {
            return dopantK;
        }
        return dopantK(items.get(SLOT_DOPANT));
    }

    private double mass(net.minecraft.world.item.Item item) {
        return org.alex_melan.spacereloaded.registry.ItemMasses.massOf(level.registryAccess(), new ItemStack(item));
    }

    /** Масса загрузки, кг. */
    private double chargeMass() {
        return CHARGE * mass(ModItems.POLYSILICON);
    }

    /** Кристалл, который вырастет при текущей устойчивости вращения. */
    private net.minecraft.world.item.Item crystal() {
        double cv = samples > 1 && mean > 0 ? Math.sqrt(m2 / (samples - 1)) / mean : 0;
        return CrystalGrowth.monocrystalline(cv) ? ModItems.SILICON_BOULE : ModItems.MULTICRYSTALLINE_SILICON;
    }

    /** Сколько целых кристаллов даёт годная доля загрузки. */
    private int crystals(double k, net.minecraft.world.item.Item crystal) {
        double g = CrystalGrowth.usableFraction(k, CrystalGrowth.RESISTIVITY_TOLERANCE);
        return (int) Math.floor(chargeMass() * g / mass(crystal) + 1e-9);
    }

    /** Хвост в кусках поликремния после вытягивания (худший случай — с накопленным). */
    private int tailPieces(int n, net.minecraft.world.item.Item crystal) {
        double tail = tailMass + chargeMass() - n * mass(crystal);
        return (int) Math.floor(tail / mass(ModItems.POLYSILICON) + 1e-9);
    }

    private boolean ready() {
        double k = availableDopant();
        if (k < 0 || !items.get(SLOT_CHARGE).is(ModItems.POLYSILICON) || items.get(SLOT_CHARGE).getCount() < CHARGE
                || !items.get(SLOT_CRUCIBLE).is(ModItems.QUARTZ_CRUCIBLE)) {
            return false;
        }
        return outputsFit(k);
    }

    /** Поместятся ли кристалл ожидаемого исхода и хвост (исход сменится — выдача подождёт места). */
    private boolean outputsFit(double k) {
        var crystal = crystal();
        int n = crystals(k, crystal);
        int tail = tailPieces(n, crystal);
        ItemStack probe = new ItemStack(crystal, Math.max(1, n));
        probe.set(ModDataComponents.PURITY, (float) Purity.CEILING);
        ItemStack tailProbe = new ItemStack(ModItems.POLYSILICON, Math.max(1, tail));
        tailProbe.set(ModDataComponents.PURITY, (float) Purity.CEILING);
        return (n == 0 || ChemicalProcess.fits(items, new int[] {SLOT_OUT}, List.of(probe)))
                && (tail == 0 || ChemicalProcess.fits(items, new int[] {SLOT_TAIL}, List.of(tailProbe)));
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
        }
        if (work >= PULL_TICKS) {
            if (finishPull()) {
                setChanged();
            }
            return; // слиток готов и ждёт места
        }
        boolean ready = ready() && inWindow() && energy.amount >= HEATER_PER_TICK;
        boolean was = pulling;
        pulling = ready;
        if (ready) {
            energy.amount -= HEATER_PER_TICK;
            double w = Math.abs(omega);
            samples++;
            double delta = w - mean;
            mean += delta / samples;
            m2 += delta * (w - mean);
            if (++work >= PULL_TICKS) {
                finishPull();
            }
        } else if (!items.get(SLOT_CHARGE).is(ModItems.POLYSILICON)) {
            reset();
        }
        if (was != pulling) {
            KineticNetworks.wake(level, getBlockPos());
        }
        setChanged();
    }

    private boolean finishPull() {
        double k = availableDopant();
        if (k < 0 || items.get(SLOT_CHARGE).getCount() < CHARGE || !items.get(SLOT_CRUCIBLE).is(ModItems.QUARTZ_CRUCIBLE)) {
            reset(); // загрузку или тигель вынули — цикл сорван
            return true;
        }
        var crystal = crystal();
        int n = crystals(k, crystal);
        if (!outputsFit(k)) {
            if (pulling) {
                pulling = false;
                if (level instanceof ServerLevel serverLevel) {
                    KineticNetworks.wake(serverLevel, getBlockPos());
                }
            }
            return false; // кристалл готов и ждёт места
        }
        if (doses <= 0) {
            dopantK = dopantK(items.get(SLOT_DOPANT));
            items.get(SLOT_DOPANT).shrink(1);
            doses = DOSES_PER_DOPANT;
        }
        doses--;
        ItemStack charge = items.get(SLOT_CHARGE);
        double feed = Purity.impurityFraction(ChemicalProcess.purityOf(charge));
        double total = chargeMass();
        double solidMass = n * mass(crystal);
        double[] split = CrystalGrowth.segregate(CrystalGrowth.K_BORON, solidMass / total);
        if (n > 0) {
            ItemStack out = new ItemStack(crystal, n);
            out.set(ModDataComponents.PURITY, (float) Purity.fromImpurity(feed * split[0]));
            ChemicalProcess.distribute(items, new int[] {SLOT_OUT}, out);
        }
        double rest = total - solidMass;
        tailMass += rest;
        tailImpurity += rest * feed * split[1];
        double piece = mass(ModItems.POLYSILICON);
        int whole = (int) Math.floor(tailMass / piece + 1e-9);
        if (whole > 0) {
            ItemStack tailStack = new ItemStack(ModItems.POLYSILICON, whole);
            tailStack.set(ModDataComponents.PURITY, (float) Purity.fromImpurity(tailImpurity / tailMass));
            double share = whole * piece / tailMass;
            tailImpurity -= tailImpurity * share;
            tailMass -= whole * piece;
            ChemicalProcess.distribute(items, new int[] {SLOT_TAIL}, tailStack);
        }
        charge.shrink(CHARGE);
        items.get(SLOT_CRUCIBLE).shrink(1);
        reset();
        return true;
    }

    private void reset() {
        work = 0;
        mean = 0;
        m2 = 0;
        samples = 0;
    }

    public int work() {
        return work;
    }

    @Override
    protected void extraReport(java.util.List<Component> lines) {
        double cv = samples > 1 && mean > 0 ? Math.sqrt(m2 / (samples - 1)) / mean : 0;
        lines.add(Component.translatable("message.spacereloaded.crystal_puller.report",
                work * 100 / PULL_TICKS, String.format(Locale.ROOT, "%.1f", cv * 100), doses,
                String.format(Locale.ROOT, "%.2f", tailMass), energy.amount));
    }

    // ---------------- слоты ----------------

    @Override
    public boolean insert(ItemStack stack) {
        int slot = stack.is(ModItems.QUARTZ_CRUCIBLE) ? SLOT_CRUCIBLE
                : dopantK(stack) >= 0 ? SLOT_DOPANT : SLOT_CHARGE;
        ItemStack in = items.get(slot);
        if (in.isEmpty()) {
            items.set(slot, stack.split(stack.getCount()));
        } else if (ChemicalProcess.canStack(in, stack) && in.getCount() < in.getMaxStackSize()) {
            int move = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
            ChemicalProcess.merge(in, stack, move);
            stack.shrink(move);
        } else {
            return false;
        }
        setChanged();
        return true;
    }

    @Override
    public ItemStack takeOutput() {
        for (int slot : new int[] {SLOT_OUT, SLOT_TAIL, SLOT_CHARGE}) {
            if (!items.get(slot).isEmpty()) {
                ItemStack taken = items.get(slot);
                items.set(slot, ItemStack.EMPTY);
                setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[] {SLOT_OUT, SLOT_TAIL}
                : new int[] {SLOT_CHARGE, SLOT_CRUCIBLE, SLOT_DOPANT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return switch (slot) {
            case SLOT_CHARGE -> stack.is(ModItems.POLYSILICON);
            case SLOT_CRUCIBLE -> stack.is(ModItems.QUARTZ_CRUCIBLE);
            case SLOT_DOPANT -> dopantK(stack) >= 0;
            default -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUT || slot == SLOT_TAIL;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("work", work);
        output.putDouble("mean", mean);
        output.putDouble("m2", m2);
        output.putInt("samples", samples);
        output.putInt("doses", doses);
        output.putDouble("dopant_k", dopantK);
        output.putDouble("tail_mass", tailMass);
        output.putDouble("tail_impurity", tailImpurity);
        output.putLong("energy", energy.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        work = input.getIntOr("work", 0);
        mean = input.getDoubleOr("mean", 0);
        m2 = input.getDoubleOr("m2", 0);
        samples = input.getIntOr("samples", 0);
        doses = input.getIntOr("doses", 0);
        dopantK = input.getDoubleOr("dopant_k", 0);
        tailMass = input.getDoubleOr("tail_mass", 0);
        tailImpurity = input.getDoubleOr("tail_impurity", 0);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
    }

    /** Строки для Jade/отчёта. */
    public List<Component> status() {
        return List.of(Component.translatable("message.spacereloaded.crystal_puller.status",
                work * 100 / PULL_TICKS, doses));
    }
}
