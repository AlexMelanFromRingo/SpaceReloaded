package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.rocketry.PartProperties;
import org.alex_melan.spacereloaded.core.rocketry.PlacedPart;
import org.alex_melan.spacereloaded.core.rocketry.RocketStructure;
import org.alex_melan.spacereloaded.registry.ModTags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сборщик ракеты (T051, FR-020): BFS по 6 граням от командного модуля по тегу
 * {@code #spacereloaded:rocket_parts}. Структурная связность — гранями
 * (26-направленность — только про газ). Деталь без записи в part_properties —
 * ошибка с позицией. Баки при сборке заполняются полностью
 * (TODO US6: честная заправка через ISRU).
 */
public final class RocketAssembler {

    /** Блок структуры: мировая позиция + состояние + локальная позиция + свойства. */
    public record ScannedBlock(BlockPos worldPos, BlockState state, long localPos, PartProperties properties) {
    }

    public sealed interface Result {
        record Ok(List<ScannedBlock> blocks, BlockPos origin, RocketStructure structure,
                  BlockPos commandWorldPos) implements Result {
        }

        record Error(String translationKey, BlockPos pos) implements Result {
        }
    }

    private RocketAssembler() {
    }

    /**
     * Сборка со стартовой площадки (AR-стиль): объём = клетки площадки ×
     * высота пилона. Требования: ровно один командный модуль, все детали
     * связаны гранями с командным модулем.
     */
    public static Result scanVolume(ServerLevel level, Iterable<BlockPos> padCells,
                                    int minY, int maxY, PartPropertiesResolver resolver,
                                    int maxBlocks) {
        List<BlockPos> collected = new ArrayList<>();
        Set<BlockPos> collectedSet = new HashSet<>();
        BlockPos commandPos = null;
        int commandCount = 0;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (BlockPos cell : padCells) {
            for (int y = minY; y <= maxY; y++) {
                cursor.set(cell.getX(), y, cell.getZ());
                BlockState state = level.getBlockState(cursor);
                if (!state.is(ModTags.ROCKET_PARTS)) {
                    continue;
                }
                BlockPos pos = cursor.immutable();
                collected.add(pos);
                collectedSet.add(pos);
                if (collected.size() > maxBlocks) {
                    return new Result.Error("message.spacereloaded.assembly.too_big", pos);
                }
                Optional<PartProperties> props = resolver.resolve(state);
                if (props.isPresent() && props.get().role() == org.alex_melan.spacereloaded.core.rocketry.PartRole.COMMAND) {
                    commandCount++;
                    commandPos = pos;
                }
            }
        }
        if (collected.isEmpty()) {
            return new Result.Error("message.spacereloaded.assembly.no_parts", padCells.iterator().next());
        }
        if (commandCount == 0) {
            return new Result.Error("message.spacereloaded.assembly.no_command", collected.get(0));
        }
        if (commandCount > 1) {
            return new Result.Error("message.spacereloaded.assembly.multiple_command", commandPos);
        }

        // Связность: BFS от командного модуля внутри собранного множества
        Set<BlockPos> reachable = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(commandPos);
        reachable.add(commandPos);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (collectedSet.contains(neighbor) && reachable.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        for (BlockPos pos : collected) {
            if (!reachable.contains(pos)) {
                return new Result.Error("message.spacereloaded.assembly.disconnected", pos);
            }
        }
        return buildResult(level, collected, resolver, commandPos);
    }

    public static Result scan(ServerLevel level, BlockPos commandPos,
                              PartPropertiesResolver resolver, int maxBlocks) {
        BlockState commandState = level.getBlockState(commandPos);
        if (!commandState.is(ModTags.ROCKET_PARTS)) {
            return new Result.Error("message.spacereloaded.assembly.not_command", commandPos);
        }

        List<BlockPos> collected = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(commandPos.immutable());
        visited.add(commandPos.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            collected.add(current);
            if (collected.size() > maxBlocks) {
                return new Result.Error("message.spacereloaded.assembly.too_big", current);
            }
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) {
                    continue;
                }
                if (level.getBlockState(neighbor).is(ModTags.ROCKET_PARTS)) {
                    visited.add(neighbor.immutable());
                    queue.add(neighbor.immutable());
                }
            }
        }

        return buildResult(level, collected, resolver, commandPos);
    }

    private static Result buildResult(ServerLevel level, List<BlockPos> collected,
                                      PartPropertiesResolver resolver, BlockPos commandPos) {
        // Локальные координаты — от минимального угла AABB структуры
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos pos : collected) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        BlockPos origin = new BlockPos(minX, minY, minZ);

        List<ScannedBlock> blocks = new ArrayList<>(collected.size());
        List<PlacedPart> parts = new ArrayList<>(collected.size());
        for (BlockPos pos : collected) {
            BlockState state = level.getBlockState(pos);
            Optional<PartProperties> properties = resolver.resolve(state);
            if (properties.isEmpty()) {
                return new Result.Error("message.spacereloaded.assembly.unknown_part", pos);
            }
            long local = PackedPos.pack(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
            blocks.add(new ScannedBlock(pos.immutable(), state, local, properties.get()));
            // MVP: баки полные (честная заправка — вместе с ISRU)
            double fill = properties.get().propellantCapacityKg();
            parts.add(new PlacedPart(local, properties.get(), fill));
        }

        return new Result.Ok(blocks, origin, new RocketStructure(parts), commandPos.immutable());
    }
}
