package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModRegistries;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Реестр многоблочных структур 004 (принцип III, D32): карта «клетка → структуры-владельцы»
 * по измерению. Изменение блока (ServerLevelMixin) — O(1) поиск по карте; только владелец
 * клетки помечается «грязным» и пересобирает себя в своём тике. Никакого поллинга.
 */
public final class IndustryStructures {

    /** Владелец структуры: блок-сущность, которая умеет пересобраться. */
    public interface StructureOwner {
        void markStructureDirty();
    }

    /**
     * Только для гейм-теста: подменяет профиль тела катапульты (игрок стенда не покидает
     * оверворлд). Выставляется и сбрасывается сценарием в {@code finally}.
     */
    public static volatile ModRegistries.PlanetProfile testBodyOverride;

    private static final Map<ResourceKey<Level>, Map<Long, Set<Long>>> CELL_OWNERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<Long, Set<Long>>> OWNER_CELLS = new HashMap<>();

    private IndustryStructures() {
    }

    /** Профиль тела для механизмов 004 (с тестовой подменой). */
    public static Optional<ModRegistries.PlanetProfile> bodyFor(ServerLevel level) {
        ModRegistries.PlanetProfile override = testBodyOverride;
        return override != null ? Optional.of(override) : PlanetManager.profileFor(level);
    }

    /** Заменяет набор клеток владельца (старые снимаются). */
    public static synchronized void claim(ServerLevel level, BlockPos owner, Collection<BlockPos> cells) {
        release(level, owner);
        long ownerKey = owner.asLong();
        Set<Long> keys = new HashSet<>(cells.size() * 2);
        Map<Long, Set<Long>> cellOwners = CELL_OWNERS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        for (BlockPos cell : cells) {
            long key = cell.asLong();
            keys.add(key);
            cellOwners.computeIfAbsent(key, k -> new HashSet<>(2)).add(ownerKey);
        }
        OWNER_CELLS.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(ownerKey, keys);
    }

    public static synchronized void release(ServerLevel level, BlockPos owner) {
        Map<Long, Set<Long>> ownerCells = OWNER_CELLS.get(level.dimension());
        if (ownerCells == null) {
            return;
        }
        Set<Long> keys = ownerCells.remove(owner.asLong());
        Map<Long, Set<Long>> cellOwners = CELL_OWNERS.get(level.dimension());
        if (keys == null || cellOwners == null) {
            return;
        }
        for (long key : keys) {
            Set<Long> owners = cellOwners.get(key);
            if (owners != null) {
                owners.remove(owner.asLong());
                if (owners.isEmpty()) {
                    cellOwners.remove(key);
                }
            }
        }
    }

    /** Хук изменения блока (ServerLevelMixin): будит только владельцев клетки. */
    public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        // Смена свойств катушки (IN_RAIL/AXIS) — наш же визуал, не изменение структуры
        if (oldState.getBlock() == newState.getBlock() && newState.getBlock() instanceof CoilBlock) {
            return;
        }
        Set<Long> owners;
        synchronized (IndustryStructures.class) {
            Map<Long, Set<Long>> cellOwners = CELL_OWNERS.get(level.dimension());
            if (cellOwners == null) {
                return;
            }
            Set<Long> found = cellOwners.get(pos.asLong());
            if (found == null) {
                return;
            }
            owners = Set.copyOf(found);
        }
        for (long owner : owners) {
            if (level.getBlockEntity(BlockPos.of(owner)) instanceof StructureOwner structure) {
                structure.markStructureDirty();
            }
        }
    }

    public static synchronized void clearAll() {
        CELL_OWNERS.clear();
        OWNER_CELLS.clear();
    }
}
