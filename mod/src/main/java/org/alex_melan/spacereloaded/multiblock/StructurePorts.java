package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Порты собранного мультиблока (008): клетки снаружи структуры, граничащие с любым её блоком. Баки,
 * баллоны и сеть подключаются к стойке, реактору или печи в любом месте корпуса — как трубопроводы и
 * шины настоящей установки, а не только к грани контроллера.
 */
public final class StructurePorts {

    private StructurePorts() {
    }

    public static List<BlockPos> around(ServerLevel level, BlockPos key, Direction face, int repeats) {
        var template = MultiblockTemplates.forKey(level.registryAccess(), level.getBlockState(key).getBlock());
        if (template.isEmpty()) {
            return List.of();
        }
        List<BlockPos> cells = MultiblockTemplates.cells(key, face, template.get(), repeats);
        Set<Long> inside = new HashSet<>();
        for (BlockPos c : cells) {
            inside.add(c.asLong());
        }
        Set<BlockPos> out = new LinkedHashSet<>();
        for (BlockPos c : cells) {
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (!inside.contains(n.asLong())) {
                    out.add(n);
                }
            }
        }
        return new ArrayList<>(out);
    }
}
