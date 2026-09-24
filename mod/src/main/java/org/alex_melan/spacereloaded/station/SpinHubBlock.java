package org.alex_melan.spacereloaded.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.core.station.SpinGravity;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

import java.util.Locale;

/** Ступица кольца: горизонтальная ось (по взгляду при установке); ПКМ — отчёт. */
public class SpinHubBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public SpinHubBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpinHubBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.SPIN_HUB) {
            return null;
        }
        return (l, pos, s, be) -> SpinHubBlockEntity.serverTick((ServerLevel) l, pos, (SpinHubBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SpinHubBlockEntity hub
                && player instanceof ServerPlayer sp) {
            var a = hub.assembly();
            if (!hub.isolated() || a == null) {
                sp.sendSystemMessage(Component.translatable("message.spacereloaded.spin_hub.attached"));
            } else {
                double w = hub.omega();
                sp.sendSystemMessage(Component.translatable("message.spacereloaded.spin_hub.report",
                        String.format(Locale.ROOT, "%.2f", SpinGravity.rpm(Math.abs(w))),
                        String.format(Locale.ROOT, "%.0f", a.totalMass()),
                        String.format(Locale.ROOT, "%.3g", a.inertia()),
                        String.format(Locale.ROOT, "%.3g", a.inertia() * Math.abs(w)),
                        String.format(Locale.ROOT, "%.0f", a.bearingForce(w)),
                        String.format(Locale.ROOT, "%.0f", SpinHubBlockEntity.BEARING_LIMIT_N)));
            }
        }
        return InteractionResult.SUCCESS;
    }

}
