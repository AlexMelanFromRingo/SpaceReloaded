package org.alex_melan.spacereloaded.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Целеуказатель (US7): ПКМ по блоку на поверхности — запомнить метку
 * (измерение + позиция), затем ПКМ по пушке на орбите — навести.
 * Метка на игрока, как связка заправочного рукава (v1: живёт до перезапуска;
 * персистентность меток — вместе с GUI пушки).
 */
public class TargetingDesignatorItem extends Item {

    private static final Map<UUID, GlobalPos> MARKS = new HashMap<>();

    public TargetingDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        // По пушке метку не перезаписываем — этим кликом она наводится (block wins)
        if (context.getLevel().getBlockState(context.getClickedPos())
                .is(org.alex_melan.spacereloaded.registry.ModBlocks.ORBITAL_CANNON)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        MARKS.put(player.getUUID(), GlobalPos.of(context.getLevel().dimension(), pos.immutable()));
        player.sendSystemMessage(Component.translatable(
                "message.spacereloaded.designator.marked",
                pos.toShortString(), context.getLevel().dimension().identifier().toString()));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Последняя метка игрока (для пушки). */
    @Nullable
    public static GlobalPos mark(ServerPlayer player) {
        return MARKS.get(player.getUUID());
    }

    /** SERVER_STOPPING: метки не должны протекать между мирами (integrated server). */
    public static void clearAll() {
        MARKS.clear();
    }
}
