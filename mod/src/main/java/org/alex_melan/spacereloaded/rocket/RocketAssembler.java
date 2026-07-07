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
            // MVP: баки полные (см. javadoc)
            double fill = properties.get().propellantCapacityKg();
            parts.add(new PlacedPart(local, properties.get(), fill));
        }

        return new Result.Ok(blocks, origin, new RocketStructure(parts), commandPos.immutable());
    }
}
