package org.alex_melan.spacereloaded.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.sealing.HermeticHatchBlock;

/**
 * Стыковочный порт (003, FR-120, D26): герметичный люк с направлением. ПКМ —
 * люк (цикл выравнивания, интерлок, группы — как у обычного люка); Sneak+ПКМ —
 * стыковка-конверсия припаркованного борта у передней грани в модуль станции.
 * После конверсии стенка модуля напротив порта становится люком: два люка
 * подряд — переход из геометрии, без отдельной механики шлюза.
 */
public class DockingPortBlock extends HermeticHatchBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DockingPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(OPEN, false)
                .setValue(CYCLING, false)
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        return (base == null ? defaultBlockState() : base)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!player.isSecondaryUseActive()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(WetWorkshop.convert(serverLevel, pos, state.getValue(FACING)));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
