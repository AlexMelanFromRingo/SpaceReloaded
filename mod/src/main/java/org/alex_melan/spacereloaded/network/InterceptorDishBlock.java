package org.alex_melan.spacereloaded.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Тарелка-перехватчик: ПКМ — статус (слушаемый канал). Настройка ключом связи. */
public class InterceptorDishBlock extends Block implements EntityBlock {

    public InterceptorDishBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InterceptorDishBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.INTERCEPTOR_DISH) {
            return null;
        }
        return (tickLevel, pos, tickState, be) ->
                InterceptorDishBlockEntity.serverTick((ServerLevel) tickLevel, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof InterceptorDishBlockEntity dish
                && player instanceof ServerPlayer serverPlayer) {
            int f = dish.listenFrequency();
            serverPlayer.sendSystemMessage(Component.translatable(
                    f == 0 ? "message.spacereloaded.interceptor.open"
                           : "message.spacereloaded.interceptor.tuned", f));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
