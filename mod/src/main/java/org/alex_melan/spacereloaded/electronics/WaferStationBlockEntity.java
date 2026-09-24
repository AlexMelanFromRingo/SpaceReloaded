package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.electronics.CleanroomAir;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.industry.IndustryAdvancements;
import org.alex_melan.spacereloaded.machine.ChemicalProcess;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

import java.util.List;

/**
 * База машин фаба (006, FR-423, D65): пластина проходит операцию своего маршрута
 * ({@link WaferProcess}); в начале и в конце операции берётся отметка экспозиции чистой комнаты,
 * и пластина получает дефекты по точной средней концентрации за операцию. Выдача атомарная:
 * операция не стартует, пока результат не помещается в выход. Слот 0 — пластина, выход — последний.
 */
public abstract class WaferStationBlockEntity extends ProcessMachineBlockEntity {

    /** Работа над пластиной: ключ (смена — сброс), длительность, энергия в тик, учёт чистоты. */
    protected record Job(String key, int ticks, long energyPerTick, boolean cleanroom) {
    }

    private int progress;
    private int maxProgress = 1;
    private String jobKey = "";
    private CleanroomTracker.Exposure start;
    private double lastConcentration = -1;

    protected WaferStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                      ProcessMenu.Layout layout) {
        super(type, pos, state, layout, 20_000, 400);
    }

    /** Что станция сделает с пластиной сейчас (null — нечего). */
    protected abstract Job plan(ItemStack wafer);

    /** Результат операции (без изъятия реагентов) — для проверки места. */
    protected abstract ItemStack preview(Job job, ItemStack wafer);

    /** Завершить операцию: изъять реагенты, вернуть результат. */
    protected abstract ItemStack complete(ServerLevel level, Job job, ItemStack wafer);

    private int outSlot() {
        return layout.outputs()[0];
    }

    @Override
    protected void tick(ServerLevel level) {
        ItemStack wafer = items.get(0);
        Job job = wafer.isEmpty() ? null : plan(wafer);
        if (job == null || !ChemicalProcess.fits(items, layout.outputs(), List.of(preview(job, wafer.copyWithCount(1))))) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (progress > 0 && !job.key().equals(jobKey)) {
            progress = 0;
        }
        if (energy.amount < job.energyPerTick()) {
            return;
        }
        if (progress == 0) {
            jobKey = job.key();
            start = CleanroomTracker.exposure(level, getBlockPos());
        }
        maxProgress = job.ticks();
        energy.amount -= job.energyPerTick();
        progress++;
        if (level.getGameTime() % 20 == 0) {
            lastConcentration = CleanroomTracker.concentration(level, getBlockPos());
        }
        if (progress >= job.ticks()) {
            ItemStack piece = wafer.split(1);
            if (job.cleanroom()) {
                CleanroomTracker.Exposure end = CleanroomTracker.exposure(level, getBlockPos());
                double mean = start == null ? CleanroomTracker.outside(level)
                        : end.meanSince(start, CleanroomTracker.outside(level));
                lastConcentration = CleanroomTracker.concentration(level, getBlockPos());
                piece.set(ModDataComponents.WAFER_DEFECTS, (float) (piece.getOrDefault(ModDataComponents.WAFER_DEFECTS, 0f)
                        + WaferProcess.operationDefects(mean)));
                if (CleanroomAir.isoClass(mean) <= 5) {
                    IndustryAdvancements.awardNearby(level, getBlockPos(), 16, IndustryAdvancements.ISO5);
                }
            }
            ChemicalProcess.distribute(items, layout.outputs(), complete(level, job, piece));
            progress = 0;
            start = null;
        }
        setChanged();
    }

    /** Пластина после операции маршрута: шаг +1. */
    protected static ItemStack advanced(ItemStack wafer) {
        ItemStack next = wafer.copy();
        next.set(ModDataComponents.WAFER_STEP, wafer.getOrDefault(ModDataComponents.WAFER_STEP, 0) + 1);
        return next;
    }

    @Override
    protected int progress() {
        return progress;
    }

    @Override
    protected int maxProgress() {
        return maxProgress;
    }

    @Override
    protected int isoClass() {
        return lastConcentration < 0 ? 0 : CleanroomAir.isoClass(lastConcentration);
    }

    public double lastConcentration() {
        return lastConcentration;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putString("job", jobKey);
        output.putDouble("c_last", lastConcentration);
        if (start != null) {
            output.putLong("x_zone", start.zone());
            output.putDouble("x_integral", start.integral());
            output.putDouble("x_minutes", start.minutes());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
        jobKey = input.getStringOr("job", "");
        lastConcentration = input.getDoubleOr("c_last", -1);
        double minutes = input.getDoubleOr("x_minutes", -1);
        start = minutes < 0 ? null : new CleanroomTracker.Exposure(input.getLongOr("x_zone", CleanroomTracker.OUTSIDE),
                input.getDoubleOr("x_integral", 0), minutes);
        if (progress > 0 && start == null) {
            progress = 0;
        }
    }
}
