package org.alex_melan.spacereloaded.orbit;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

/** Орбитальный снимок в ожидании: ПКМ после срока проявляет его в карту (007, US5). */
public class OrbitalImageItem extends Item {

    public OrbitalImageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        var stack = player.getItemInHand(hand);
        ImageOrder order = stack.get(ModDataComponents.IMAGE_ORDER);
        if (order == null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.blank"));
            return InteractionResult.FAIL;
        }
        OrbitalImages.develop(serverPlayer, stack, order);
        return InteractionResult.SUCCESS_SERVER;
    }
}
