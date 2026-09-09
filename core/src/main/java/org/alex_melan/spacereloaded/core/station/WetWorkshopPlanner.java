package org.alex_melan.spacereloaded.core.station;

import org.alex_melan.spacereloaded.core.geometry.LongHashSet;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.rocketry.PartRole;
import org.alex_melan.spacereloaded.core.rocketry.PlacedPart;
import org.alex_melan.spacereloaded.core.rocketry.RocketStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Wet workshop (003, FR-122/FR-123, D26): план переоборудования борта в модуль
 * станции. Детали-баки и корпус (роль TANK/HULL — включая грузовой отсек) с хотя бы
 * одной из 6 граней наружу становятся оболочкой; баки/корпус, окружённые такими же
 * деталями со всех сторон, — внутренним объёмом; прочие роли (двигатели, командный
 * модуль, кресла, гиродины, разделители, узлы) сохраняются своими блоками и НЕ
 * входят в гермооболочку: бак, граничащий с двигателем, — стенка, а не пустота
 * (иначе интерьер упирался бы в негерметичный двигатель).
 *
 * <p>Физический смысл: выведенная на орбиту масса баков и есть материал оболочки
 * (Skylab / S-IVB); внутренний объём — бывшая тара топлива, которое борт сжёг,
 * чтобы сюда добраться.
 */
public final class WetWorkshopPlanner {

    public enum Kind {
        SHELL, INTERIOR, KEEP
    }

    public record Cell(long packedPos, Kind kind) {
    }

    public record Plan(List<Cell> cells, int shellCount, int interiorCount, int keepCount) {
        public Plan {
            cells = List.copyOf(cells);
        }

        /** Тип клетки плана либо {@code null}, если клетка не принадлежит борту. */
        public Kind kindAt(int x, int y, int z) {
            long packed = PackedPos.pack(x, y, z);
            for (Cell cell : cells) {
                if (cell.packedPos() == packed) {
                    return cell.kind();
                }
            }
            return null;
        }
    }

    private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private WetWorkshopPlanner() {
    }

    public static Plan plan(RocketStructure structure) {
        // «Внутри» — только конвертируемые детали: гермооболочка строится из стенок баков/корпуса
        LongHashSet occupied = new LongHashSet(structure.parts().size());
        for (PlacedPart part : structure.parts()) {
            if (convertible(part.properties().role())) {
                occupied.add(part.packedPos());
            }
        }
        List<Cell> cells = new ArrayList<>(structure.parts().size());
        int shell = 0;
        int interior = 0;
        int keep = 0;
        for (PlacedPart part : structure.parts()) {
            long pos = part.packedPos();
            Kind kind;
            if (!convertible(part.properties().role())) {
                kind = Kind.KEEP;
                keep++;
            } else if (exposed(occupied, pos)) {
                kind = Kind.SHELL;
                shell++;
            } else {
                kind = Kind.INTERIOR;
                interior++;
            }
            cells.add(new Cell(pos, kind));
        }
        return new Plan(cells, shell, interior, keep);
    }

    /** Клетка плана в (x, y, z), если она — оболочка (кандидат в люк перехода). */
    public static OptionalLong hatchCell(Plan plan, int x, int y, int z) {
        Kind kind = plan.kindAt(x, y, z);
        return kind == Kind.SHELL ? OptionalLong.of(PackedPos.pack(x, y, z)) : OptionalLong.empty();
    }

    private static boolean convertible(PartRole role) {
        return role == PartRole.TANK || role == PartRole.HULL;
    }

    private static boolean exposed(LongHashSet occupied, long pos) {
        for (int[] face : FACES) {
            if (!occupied.contains(PackedPos.offset(pos, face[0], face[1], face[2]))) {
                return true;
            }
        }
        return false;
    }

    /** Карта «позиция → тип» для адаптеров (быстрый доступ при расстановке блоков). */
    public static Map<Long, Kind> index(Plan plan) {
        Map<Long, Kind> map = new HashMap<>(plan.cells().size() * 2);
        for (Cell cell : plan.cells()) {
            map.put(cell.packedPos(), cell.kind());
        }
        return map;
    }
}
