package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.electronics.DieYield;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.kinetics.KineticMachineBlockEntity;
import org.alex_melan.spacereloaded.kinetics.KineticNetworks;
import org.alex_melan.spacereloaded.machine.ChemicalProcess;
import org.alex_melan.spacereloaded.machine.recipe.MachiningRecipe;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * Прецизионная пила (006, FR-415, D63): алмазный диск с внутренней режущей кромкой (ID-пила) от
 * вала, окно 1000–3500 об/мин.
 * <ul>
 *   <li>Слиток ставится на оправку целиком и режется пластина за пластиной: шаг — толщина
 *       пластины 0.275 мм плюс пропил 0.25 мм. Длина слитка — из его массы по таблице масс и
 *       сечения (Ø50 мм — кремниевый и сапфировый слитки, квадрат 50 × 50 мм — мультикремниевый
 *       брусок): слиток Ø50 × 150 мм (0.69 кг) даёт 287 пластин, 48 % массы уходит в пропил —
 *       кремниевая пыль (в шихту), целыми кусками по таблице масс.</li>
 *   <li>Сапфир (T3) режется так же; пыль корунда не собирается (абразив).</li>
 *   <li>Пластина, прошедшая весь маршрут фаба, → скрайбирование: годных Binomial(DPW, e^(−D·A)),
 *       D — дефекты операций + примеси недочищенного кремния.</li>
 * </ul>
 * Слоты: 0 — вход, 1 — продукт, 2 — пыль пропила. Выдача атомарная — пила ждёт места.
 */
public class WaferSawBlockEntity extends KineticMachineBlockEntity<MachiningRecipe.Lathe> {

    public static final int SLOT_DUST = 2;
    public static final double WAFER_THICKNESS_MM = 0.275;
    public static final double KERF_MM = 0.25;
    public static final double DIAMETER_CM = 5;
    public static final int CUT_TICKS = 60;
    public static final int DICE_TICKS = 200;
    private static final double CUT_TORQUE = 60;
    private int work;
    private boolean cutting;
    private double dustMass;
    /** Слиток на оправке: предмет, остаток длины (мм), чистота. */
    private Item mounted = Items.AIR;
    private double mountedLength;
    private float mountedPurity = -1;

    public WaferSawBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAFER_SAW, pos, state, ModRecipes.MACHINING);
    }

    @Override
    protected int slotCount() {
        return 3;
    }

    private static double rpm(double value) {
        return value * 2 * Math.PI / 60;
    }

    @Override
    protected double nominalOmega() {
        return rpm(1500);
    }

    @Override
    protected double minOmega() {
        return rpm(1000);
    }

    @Override
    protected double maxOmega() {
        return rpm(3500);
    }

    @Override
    protected double machineDeltaUm() {
        return 0;
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        return new NodeLoad(0, 0, SpaceReloaded.config().kineticFrictionTorqueNm + (cutting ? CUT_TORQUE : 0), 5);
    }

    private static boolean finishedWafer(ItemStack stack) {
        return WaferKind.next(stack) == WaferProcess.Operation.DONE;
    }

    private static boolean ingot(ItemStack stack) {
        return stack.is(ModItems.SILICON_BOULE) || stack.is(ModItems.MULTICRYSTALLINE_SILICON) || stack.is(ModItems.SAPPHIRE);
    }

    /** Плотность материала слитка, г/см³. */
    private static double density(Item ingot) {
        return ingot == ModItems.SAPPHIRE ? 3.98 : 2.33;
    }

    /** Сечение слитка, см²: круг Ø50 мм или квадрат 50 × 50 мм (мультикремний). */
    private static double section(Item ingot) {
        return ingot == ModItems.MULTICRYSTALLINE_SILICON ? DIAMETER_CM * DIAMETER_CM
                : Math.PI * DIAMETER_CM * DIAMETER_CM / 4;
    }

    private static Item waferOf(Item ingot) {
        return ingot == ModItems.SILICON_BOULE ? ModItems.SILICON_WAFER
                : ingot == ModItems.MULTICRYSTALLINE_SILICON ? ModItems.MULTICRYSTALLINE_WAFER : ModItems.SAPPHIRE_WAFER;
    }

    /** Длина слитка по массе из таблицы масс, мм. */
    private double lengthOf(Item ingot) {
        double kg = org.alex_melan.spacereloaded.registry.ItemMasses.massOf(level.registryAccess(), new ItemStack(ingot));
        return kg * 1000 / (density(ingot) * section(ingot)) * 10;
    }

    /** Масса пропила одной пластины, кг. */
    private static double kerfMass(Item ingot) {
        return section(ingot) * KERF_MM / 10 * density(ingot) / 1000;
    }

    private boolean mountedReady() {
        return mounted != Items.AIR && mountedLength >= WAFER_THICKNESS_MM;
    }

    private ItemStack nextWafer() {
        ItemStack wafer = new ItemStack(waferOf(mounted));
        if (mountedPurity >= 0 && mounted != ModItems.SAPPHIRE) {
            wafer.set(ModDataComponents.PURITY, mountedPurity);
        }
        return wafer;
    }

    private int dustAfterCut(boolean worstCase) {
        if (mounted == ModItems.SAPPHIRE) {
            return 0;
        }
        double piece = org.alex_melan.spacereloaded.registry.ItemMasses.massOf(level.registryAccess(),
                new ItemStack(ModItems.SILICON_DUST));
        double total = (dustMass + kerfMass(mounted)) / piece;
        return worstCase ? (int) Math.ceil(total - 1e-9) : (int) Math.floor(total + 1e-9);
    }

    private boolean sliceFits() {
        int dust = dustAfterCut(true);
        return ChemicalProcess.fits(items, new int[] {1}, List.of(nextWafer()))
                && (dust == 0 || ChemicalProcess.fits(items, new int[] {SLOT_DUST},
                        List.of(new ItemStack(ModItems.SILICON_DUST, dust))));
    }

    private boolean diceFits(ItemStack wafer) {
        WaferKind kind = WaferKind.byIndex(wafer.getOrDefault(ModDataComponents.WAFER_KIND, 0));
        ItemStack dies = new ItemStack(kind.die(), DieYield.diesPerWafer(WaferKind.WAFER_DIAMETER_CM, kind.areaCm2));
        return ChemicalProcess.fits(items, new int[] {1}, List.of(dies));
    }

    /** Поставить слиток на оправку, если она пуста. */
    private void mount() {
        if (mountedReady()) {
            return;
        }
        if (mounted != Items.AIR) {
            mounted = Items.AIR; // обрезок короче пластины — в пыль уже учтён пропилом
        }
        ItemStack input = items.get(0);
        if (ingot(input)) {
            mounted = input.getItem();
            Float left = input.get(ModDataComponents.INGOT_LENGTH);
            mountedLength = left == null ? lengthOf(mounted) : left;
            Float purity = input.get(ModDataComponents.PURITY);
            mountedPurity = purity == null ? -1 : purity;
            input.shrink(1);
            setChanged();
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        ItemStack input = items.get(0);
        boolean dicing = !mountedReady() && finishedWafer(input);
        if (!dicing) {
            mount();
        }
        boolean workable = dicing ? diceFits(input) : mountedReady() && sliceFits();
        boolean ready = workable && inWindow();
        boolean was = cutting;
        cutting = ready;
        if (ready && ++work >= (dicing ? DICE_TICKS : CUT_TICKS)) {
            work = 0;
            if (dicing) {
                dice(level, input);
            } else {
                slice();
            }
        } else if (!workable) {
            work = 0;
        }
        if (was != cutting) {
            KineticNetworks.wake(level, getBlockPos());
        }
        setChanged();
    }

    private void slice() {
        ChemicalProcess.distribute(items, new int[] {1}, nextWafer());
        if (mounted != ModItems.SAPPHIRE) {
            dustMass += kerfMass(mounted);
            double piece = org.alex_melan.spacereloaded.registry.ItemMasses.massOf(level.registryAccess(),
                    new ItemStack(ModItems.SILICON_DUST));
            int whole = (int) Math.floor(dustMass / piece + 1e-9);
            if (whole > 0) {
                dustMass -= whole * piece;
                ChemicalProcess.distribute(items, new int[] {SLOT_DUST}, new ItemStack(ModItems.SILICON_DUST, whole));
            }
        }
        mountedLength -= WAFER_THICKNESS_MM + KERF_MM;
    }

    private void dice(ServerLevel level, ItemStack input) {
        WaferKind kind = WaferKind.byIndex(input.getOrDefault(ModDataComponents.WAFER_KIND, 0));
        double defects = input.getOrDefault(ModDataComponents.WAFER_DEFECTS, 0f)
                + DieYield.impurityDefects(ChemicalProcess.purityOf(input));
        int dies = DieYield.diesPerWafer(WaferKind.WAFER_DIAMETER_CM, kind.areaCm2);
        int good = DieYield.sampleGood(level.getRandom().nextLong(), dies, DieYield.yield(defects, kind.areaCm2));
        if (good > 0) {
            ChemicalProcess.distribute(items, new int[] {1}, new ItemStack(kind.die(), good));
            org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 16,
                    org.alex_melan.spacereloaded.industry.IndustryAdvancements.THINKING_SAND);
        }
        // бракованные кристаллы отбраковываются зондом — масса ничтожна (граммы)
        input.shrink(1);
    }

    /** Сломали пилу — недорезанный слиток падает с остатком длины. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide() && mountedReady()) {
            ItemStack rest = new ItemStack(mounted);
            rest.set(ModDataComponents.INGOT_LENGTH, (float) mountedLength);
            if (mountedPurity >= 0) {
                rest.set(ModDataComponents.PURITY, mountedPurity);
            }
            net.minecraft.world.level.block.Block.popResource(level, pos, rest);
            mounted = Items.AIR;
        }
        super.preRemoveSideEffects(pos, state);
    }

    /** Остаток слитка на оправке, пластин. */
    public int wafersLeft() {
        return mountedReady() ? (int) Math.floor((mountedLength + KERF_MM) / (WAFER_THICKNESS_MM + KERF_MM)) : 0;
    }

    @Override
    public ItemStack takeOutput() {
        for (int slot : new int[] {1, SLOT_DUST, 0}) {
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
        return side == Direction.DOWN ? new int[] {1, SLOT_DUST} : new int[] {0};
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 || slot == SLOT_DUST;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("work", work);
        output.putDouble("dust_mass", dustMass);
        output.putString("mounted", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mounted).toString());
        output.putDouble("mounted_length", mountedLength);
        output.putFloat("mounted_purity", mountedPurity);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        work = input.getIntOr("work", 0);
        dustMass = input.getDoubleOr("dust_mass", 0);
        var id = net.minecraft.resources.Identifier.tryParse(input.getStringOr("mounted", "minecraft:air"));
        mounted = id == null ? Items.AIR : net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
        mountedLength = input.getDoubleOr("mounted_length", 0);
        mountedPurity = input.getFloatOr("mounted_purity", -1);
    }
}
