package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * Ловушка масс (004, FR-220…FR-226): ПКМ — инвентарь и отчёт; ПКМ полётной программой —
 * привязать программу к ловушке (цель катапульты).
 */
public class MassCatcherBlock extends Block implements EntityBlock {

    public MassCatcherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MassCatcherBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.MASS_CATCHER) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                MassCatcherBlockEntity.serverTick((MassCatcherBlockEntity) blockEntity, (ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.FLIGHT_PROGRAM)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MassCatcherBlockEntity catcher
                && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (!catcher.operational(serverLevel)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.spacereloaded.mass_catcher.orbit_only"));
                return InteractionResult.SUCCESS_SERVER;
            }
            stack.set(ModDataComponents.PROGRAM_CATCHER, GlobalPos.of(level.dimension(), pos.immutable()));
            serverPlayer.sendSystemMessage(Component.translatable("message.spacereloaded.mass_catcher.program_bound",
                    pos.toShortString(), level.dimension().identifier().toString()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MassCatcherBlockEntity catcher
                && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            serverPlayer.openMenu(catcher);
            serverPlayer.sendSystemMessage(Component.translatable("message.spacereloaded.mass_catcher.header",
                    pos.toShortString()));
            catcher.report(serverLevel).forEach(serverPlayer::sendSystemMessage);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
