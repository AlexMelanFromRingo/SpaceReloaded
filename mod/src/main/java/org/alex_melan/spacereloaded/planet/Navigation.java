package org.alex_melan.spacereloaded.planet;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.registry.ModRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Навигация по карте планет. Цель перелёта выбирается ЛЮБАЯ (не только прямой
 * сосед текущего измерения): маршрут считается поиском в ширину по графу
 * transition_targets, а ракета летит к ближайшему хопу на пути. Так с Земли
 * можно назначить Марс: борт уйдёт на орбиту, а оттуда дальше по маршруту.
 *
 * <p>Узлы графа — id записей реестра планет (у Земли это {@code spacereloaded:earth},
 * а измерение у неё {@code minecraft:overworld}), рёбра — их transition_targets.
 */
public final class Navigation {

    private Navigation() {
    }

    /** Все планеты реестра (id записей), отсортированы детерминированно. */
    public static List<Identifier> planetIds(RegistryAccess access) {
        var registry = access.lookupOrThrow(ModRegistries.PLANETS);
        List<Identifier> ids = new ArrayList<>();
        for (ModRegistries.PlanetProfile profile : registry) {
            Identifier key = registry.getKey(profile);
            if (key != null) {
                ids.add(key);
            }
        }
        ids.sort(Identifier::compareTo);
        return ids;
    }

    /** id записи планеты по её измерению (обратное отображение). */
    public static Identifier entryIdFor(RegistryAccess access, Identifier dimensionId) {
        var registry = access.lookupOrThrow(ModRegistries.PLANETS);
        for (ModRegistries.PlanetProfile profile : registry) {
            if (profile.dimension().equals(dimensionId)) {
                return registry.getKey(profile);
            }
        }
        return null;
    }

    private static ModRegistries.PlanetProfile profile(RegistryAccess access, Identifier entryId) {
        return access.lookupOrThrow(ModRegistries.PLANETS).getValue(entryId);
    }

    /**
     * Следующий хоп на пути от {@code from} к {@code to} (поиск в ширину).
     *
     * @return id записи следующей планеты, либо {@code null}: уже на месте,
     *         маршрута нет, или входные данные неизвестны
     */
    public static Identifier nextHop(RegistryAccess access, Identifier from, Identifier to) {
        if (from == null || to == null || from.equals(to)) {
            return null;
        }
        Map<Identifier, Identifier> cameFrom = new HashMap<>();
        ArrayDeque<Identifier> queue = new ArrayDeque<>();
        queue.add(from);
        cameFrom.put(from, null);
        while (!queue.isEmpty()) {
            Identifier current = queue.poll();
            ModRegistries.PlanetProfile profile = profile(access, current);
            if (profile == null) {
                continue;
            }
            for (Identifier next : profile.transitionTargets()) {
                if (cameFrom.containsKey(next)) {
                    continue;
                }
                cameFrom.put(next, current);
                if (next.equals(to)) {
                    // Разворачиваем путь назад до первого шага от from
                    Identifier step = next;
                    while (!from.equals(cameFrom.get(step))) {
                        step = cameFrom.get(step);
                        if (step == null) {
                            return null;
                        }
                    }
                    return step;
                }
                queue.add(next);
            }
        }
        return null;
    }
}
