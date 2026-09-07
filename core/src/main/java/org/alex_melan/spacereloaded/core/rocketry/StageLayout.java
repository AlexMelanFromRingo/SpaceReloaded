package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.LongHashSet;
import org.alex_melan.spacereloaded.core.geometry.LongQueue;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Раскладка ступеней (Полёт 2.0, FR-061/FR-062/FR-064): структура делится
 * горизонтальными плоскостями разделителей ({@link PartRole#SEPARATOR}).
 *
 * <p>Правила (документированные упрощения, принцип I/VIII):
 * <ul>
 *   <li>ступень i — детали с высотой y ∈ (topY[i−1], topY[i]], где topY —
 *       уникальные Y разделителей по возрастанию; верхняя ступень — всё выше
 *       последнего разделителя. Разделители на одном ярусе — одна плоскость;
 *       сам разделитель уходит вниз вместе с отброшенной ступенью;</li>
 *   <li>последовательная схема: радиальные ускорители и параллельная работа
 *       ступеней не поддерживаются (бэклог);</li>
 *   <li>внутри ступени баки дренируются пропорционально ёмкости — как и в
 *       одноступенчатой модели ({@link RocketStructure#withTotalPropellant}).</li>
 * </ul>
 *
 * <p>{@link #activeView} даёт структуру для существующего интегратора: ступени
 * ниже активной отсутствуют, активная — как есть, верхние — груз (двигатели без
 * тяги, баки без ёмкости, но с массой своего топлива). Так {@code FlightIntegrator}
 * и {@code PerformanceCalculator} не знают о ступенях вовсе.
 */
public record StageLayout(List<Stage> stages, RocketStructure structure) {

    /** Ключи ошибок валидации (переводятся в сообщения слоем мода). */
    public static final String ERROR_ABOVE_COMMAND = "stage_above_command";
    public static final String ERROR_EMPTY_BELOW = "stage_empty_below";
    public static final String ERROR_DISCONNECTED = "stage_disconnected";

    /**
     * Ступень.
     *
     * @param index                 номер снизу (0 — зажигается первой)
     * @param parts                 детали ступени (с топливом из скана)
     * @param topY                  Y плоскости разделителя ступени (у верхней — максимум стека)
     * @param dryMassKg             сухая масса ступени (включая её разделитель)
     * @param propellantCapacityKg  суммарная ёмкость баков ступени
     * @param hasEngines            есть ли двигатели
     */
    public record Stage(int index, List<PlacedPart> parts, int topY, double dryMassKg,
                        double propellantCapacityKg, boolean hasEngines) {
        public Stage {
            parts = List.copyOf(parts);
        }

        /** Только разделители (ступень без полезных деталей). */
        boolean isSeparatorsOnly() {
            for (PlacedPart part : parts) {
                if (part.properties().role() != PartRole.SEPARATOR) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Ошибка раскладки: ключ + позиция проблемной детали. */
    public record StageError(String key, long packedPos) {
    }

    public StageLayout {
        stages = List.copyOf(stages);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("layout needs at least one stage");
        }
    }

    /** Деление структуры на ступени по плоскостям разделителей (≥ 1 ступень всегда). */
    public static StageLayout of(RocketStructure structure) {
        TreeSet<Integer> planes = new TreeSet<>();
        int maxY = Integer.MIN_VALUE;
        for (PlacedPart part : structure.parts()) {
            int y = PackedPos.unpackY(part.packedPos());
            maxY = Math.max(maxY, y);
            if (part.properties().role() == PartRole.SEPARATOR) {
                planes.add(y);
            }
        }
        // Плоскость на самом верху стека не отделяет ничего сверху — верхняя ступень
        // всегда содержит вершину стека (валидация отдельно потребует командный модуль выше)
        List<Integer> tops = new ArrayList<>(planes.headSet(maxY, false));
        tops.add(maxY);

        List<List<PlacedPart>> buckets = new ArrayList<>(tops.size());
        for (int i = 0; i < tops.size(); i++) {
            buckets.add(new ArrayList<>());
        }
        for (PlacedPart part : structure.parts()) {
            int y = PackedPos.unpackY(part.packedPos());
            int index = 0;
            while (y > tops.get(index)) {
                index++;
            }
            buckets.get(index).add(part);
        }

        List<Stage> stages = new ArrayList<>(tops.size());
        for (int i = 0; i < tops.size(); i++) {
            double dry = 0;
            double capacity = 0;
            boolean engines = false;
            for (PlacedPart part : buckets.get(i)) {
                dry += part.properties().massKg();
                capacity += part.properties().propellantCapacityKg();
                engines |= part.properties().role() == PartRole.ENGINE;
            }
            stages.add(new Stage(i, buckets.get(i), tops.get(i), dry, capacity, engines));
        }
        return new StageLayout(stages, structure);
    }

    /**
     * Проверка раскладки (FR-062): разделитель не выше командного модуля, под каждой
     * плоскостью есть детали, после отсечения любой нижней ступени остаток связан
     * гранями с командным модулем.
     *
     * @param commandPos локальная позиция командного модуля
     * @return ошибка с позицией виновной детали, либо empty
     */
    public static Optional<StageError> validate(RocketStructure structure, long commandPos) {
        int commandY = PackedPos.unpackY(commandPos);
        for (PlacedPart part : structure.parts()) {
            if (part.properties().role() == PartRole.SEPARATOR
                    && PackedPos.unpackY(part.packedPos()) >= commandY) {
                return Optional.of(new StageError(ERROR_ABOVE_COMMAND, part.packedPos()));
            }
        }
        StageLayout layout = of(structure);
        if (layout.stageCount() == 1) {
            return Optional.empty();
        }
        for (int i = 0; i < layout.stageCount() - 1; i++) {
            Stage stage = layout.stage(i);
            if (stage.isSeparatorsOnly()) {
                return Optional.of(new StageError(ERROR_EMPTY_BELOW, stage.parts().get(0).packedPos()));
            }
        }
        // Связность остатка после каждого отсечения: BFS по 6 граням от командного модуля
        for (int cut = 0; cut < layout.stageCount() - 1; cut++) {
            int topY = layout.stage(cut).topY();
            LongHashSet remaining = new LongHashSet(structure.parts().size());
            for (PlacedPart part : structure.parts()) {
                if (PackedPos.unpackY(part.packedPos()) > topY) {
                    remaining.add(part.packedPos());
                }
            }
            LongHashSet visited = new LongHashSet(structure.parts().size());
            LongQueue queue = new LongQueue(Math.max(16, structure.parts().size()));
            if (remaining.contains(commandPos)) {
                visited.add(commandPos);
                queue.enqueue(commandPos);
            }
            while (!queue.isEmpty()) {
                long current = queue.dequeue();
                int x = PackedPos.unpackX(current);
                int y = PackedPos.unpackY(current);
                int z = PackedPos.unpackZ(current);
                long[] neighbours = {
                        PackedPos.pack(x + 1, y, z), PackedPos.pack(x - 1, y, z),
                        PackedPos.pack(x, y + 1, z), PackedPos.pack(x, y - 1, z),
                        PackedPos.pack(x, y, z + 1), PackedPos.pack(x, y, z - 1)
                };
                for (long next : neighbours) {
                    if (remaining.contains(next) && visited.add(next)) {
                        queue.enqueue(next);
                    }
                }
            }
            for (PlacedPart part : structure.parts()) {
                if (PackedPos.unpackY(part.packedPos()) > topY && !visited.contains(part.packedPos())) {
                    return Optional.of(new StageError(ERROR_DISCONNECTED, part.packedPos()));
                }
            }
        }
        return Optional.empty();
    }

    public int stageCount() {
        return stages.size();
    }

    public Stage stage(int index) {
        return stages.get(index);
    }

    /**
     * Структура для интегратора при активной ступени {@code active}: нижние
     * ступени отброшены, верхние — груз.
     *
     * @param stagePropellantKg топливо по ступеням (кг), индексы совпадают со ступенями
     */
    public RocketStructure activeView(int active, double[] stagePropellantKg) {
        if (active < 0 || active >= stages.size()) {
            throw new IllegalArgumentException("active stage out of range: " + active);
        }
        List<PlacedPart> view = new ArrayList<>();
        for (int i = active; i < stages.size(); i++) {
            Stage stage = stages.get(i);
            double propellant = i < stagePropellantKg.length ? stagePropellantKg[i] : 0;
            double fraction = stage.propellantCapacityKg() <= 0 ? 0
                    : Math.clamp(propellant / stage.propellantCapacityKg(), 0, 1);
            for (PlacedPart part : stage.parts()) {
                PartProperties props = part.properties();
                long pos = part.packedPos();
                if (i == active) {
                    view.add(new PlacedPart(pos, props, props.propellantCapacityKg() * fraction));
                    continue;
                }
                switch (props.role()) {
                    case ENGINE -> view.add(new PlacedPart(pos, PartProperties.hull(props.massKg()), 0));
                    case TANK -> view.add(new PlacedPart(pos,
                            PartProperties.hull(props.massKg() + props.propellantCapacityKg() * fraction), 0));
                    default -> view.add(new PlacedPart(pos, props, 0));
                }
            }
        }
        return new RocketStructure(view);
    }

    /** Детали ступеней выше {@code dropped} (координаты исходные, топливо из скана). */
    public RocketStructure remainingAfter(int dropped) {
        List<PlacedPart> parts = new ArrayList<>();
        for (int i = dropped + 1; i < stages.size(); i++) {
            parts.addAll(stages.get(i).parts());
        }
        return new RocketStructure(parts);
    }

    /** Топливо каждой ступени по фактической заправке деталей (скан баков). */
    public double[] propellantByStage() {
        double[] result = new double[stages.size()];
        for (int i = 0; i < stages.size(); i++) {
            for (PlacedPart part : stages.get(i).parts()) {
                result[i] += part.propellantKg();
            }
        }
        return result;
    }

    /** Распределение суммарного топлива по ступеням пропорционально ёмкости (стыковка, старые сохранения). */
    public double[] distributeByCapacity(double totalPropellantKg) {
        double capacity = 0;
        for (Stage stage : stages) {
            capacity += stage.propellantCapacityKg();
        }
        double[] result = new double[stages.size()];
        if (capacity <= 0) {
            return result;
        }
        double total = Math.clamp(totalPropellantKg, 0, capacity);
        for (int i = 0; i < stages.size(); i++) {
            result[i] = total * stages.get(i).propellantCapacityKg() / capacity;
        }
        return result;
    }
}
