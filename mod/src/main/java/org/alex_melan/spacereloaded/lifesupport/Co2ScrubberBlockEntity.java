package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.lifesupport.Scrubber;
import org.alex_melan.spacereloaded.electronics.ProcessMachineBlockEntity;
import org.alex_melan.spacereloaded.electronics.ProcessMenu;
import org.alex_melan.spacereloaded.machine.ChemicalProcess;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.sealing.SealedZone;

import java.util.List;

/**
 * Поглотитель CO₂ (007, FR-505, D73): вентилятор 1 м³/мин прокачивает воздух зоны через картридж
 * (эффективность прохода 0.9) — объёмное удаление k = Q·η. Поглощённая масса — k·Δ∫c dt по
 * накопленному интегралу зоны, точно при любых событиях между отметками.
 * <ul>
 *   <li>LiOH (Аполлон): прочность картриджа — граммы CO₂ (750); исчерпан — рассыпается.</li>
 *   <li>Цеолит 5A (CDRA): вечный, десорбция 2 МДж на кг CO₂ (133 E/кг), CO₂ — баллонами по 1 кг в
 *       выход или прямо в соседний реактор Сабатье; без энергии слой насыщается и не работает.</li>
 * </ul>
 * Слоты: 0 — картридж, 1 — CO₂.
 */
public class Co2ScrubberBlockEntity extends ProcessMachineBlockEntity {

    public static final double ZEOLITE_E_PER_KG = EnergyScale.fromJoules(Scrubber.ZEOLITE_MJ_PER_KG * 1e6);

    private double lastExposure = -1;
    private long lastZone = Long.MIN_VALUE;
    private double co2Kg;
    private int working;

    public Co2ScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CO2_SCRUBBER, pos, state, ProcessMenu.Layout.CO2_SCRUBBER, 8000, 400);
    }

    private boolean lioh() {
        return items.get(0).is(ModItems.LIOH_CARTRIDGE);
    }

    private boolean zeolite() {
        return items.get(0).is(ModItems.ZEOLITE_BED);
    }

    @Override
    protected void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        SealedZone zone = LifeSupportState.zoneAround(level, getBlockPos());
        boolean active = zone != null && zone.isSealed() && (lioh() || (zeolite() && energy.amount >= 1));
        double k = Scrubber.removalM3PerDay(Scrubber.FAN_M3_PER_MIN, Scrubber.EFFICIENCY);
        LifeSupportState.contribute(level, getBlockPos(), active
                ? new LifeSupportState.Contribution(0, 0, k, 0) : LifeSupportState.Contribution.NONE);
        if (zone == null) {
            lastExposure = -1;
            working = 0;
            return;
        }
        double exposure = LifeSupportState.co2Exposure(level, zone);
        long key = zone.controllerPos().asLong();
        double removed = lastExposure >= 0 && key == lastZone && active ? Math.max(0, k * (exposure - lastExposure)) : 0;
        lastExposure = exposure;
        lastZone = key;
        working = active ? 40 : Math.max(0, working - 20);
        if (removed <= 0) {
            return;
        }
        if (lioh()) {
            ItemStack cartridge = items.get(0);
            int grams = (int) Math.round(removed * 1000);
            cartridge.setDamageValue(cartridge.getDamageValue() + grams);
            if (cartridge.getDamageValue() >= cartridge.getMaxDamage()) {
                items.set(0, ItemStack.EMPTY);
            }
        } else {
            long cost = (long) Math.ceil(removed * ZEOLITE_E_PER_KG);
            energy.amount = Math.max(0, energy.amount - cost);
            co2Kg += removed;
            emit(level);
        }
        setChanged();
    }

    /** Целые килограммы CO₂ — в соседний Сабатье, иначе в выходной слот. */
    private void emit(ServerLevel level) {
        int whole = (int) Math.floor(co2Kg + 1e-9);
        for (Direction d : Direction.values()) {
            if (whole <= 0) {
                break;
            }
            if (level.getBlockEntity(getBlockPos().relative(d)) instanceof org.alex_melan.spacereloaded.machine.SabatierReactorBlockEntity sabatier) {
                ItemStack slot = sabatier.getItem(0);
                if (slot.isEmpty() || (slot.is(ModItems.CARBON_DIOXIDE) && slot.getCount() < slot.getMaxStackSize())) {
                    int move = Math.min(whole, slot.isEmpty() ? 64 : slot.getMaxStackSize() - slot.getCount());
                    if (slot.isEmpty()) {
                        sabatier.setItem(0, new ItemStack(ModItems.CARBON_DIOXIDE, move));
                    } else {
                        slot.grow(move);
                    }
                    whole -= move;
                    co2Kg -= move;
                }
            }
        }
        if (whole > 0 && ChemicalProcess.fits(items, new int[] {1}, List.of(new ItemStack(ModItems.CARBON_DIOXIDE, whole)))) {
            ChemicalProcess.distribute(items, new int[] {1}, new ItemStack(ModItems.CARBON_DIOXIDE, whole));
            co2Kg -= whole;
        }
    }

    @Override
    protected int progress() {
        return working;
    }

    @Override
    protected int maxProgress() {
        return 40;
    }

    @Override
    protected int reserve() {
        ItemStack c = items.get(0);
        return lioh() ? c.getMaxDamage() - c.getDamageValue() : -1;
    }

    @Override
    protected MenuType<ProcessMenu> menuType() {
        return ModMenus.CO2_SCRUBBER;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("co2", co2Kg);
        output.putDouble("exposure", lastExposure);
        output.putLong("zone", lastZone);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        co2Kg = input.getDoubleOr("co2", 0);
        lastExposure = input.getDoubleOr("exposure", -1);
        lastZone = input.getLongOr("zone", Long.MIN_VALUE);
    }
}
