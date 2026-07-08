package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Экран телеметрии (backlog: Telemetry Unit + стена экранов). Панель на стене,
 * лицо которой светится по статусу ближайшей герметичной зоны:
 * 0 — нет данных (серый), 1 — ЗАМКНУТО (зелёный), 2 — УТЕЧКА (красный).
 * ПКМ — подробный отчёт в чат. Данные — из ZoneManager (честная герметичность).
 */
public class TelemetryScreenBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 2);

    public TelemetryScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH).setValue(STATUS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TelemetryScreenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.TELEMETRY_SCREEN) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                TelemetryScreenBlockEntity.serverTick((ServerLevel) tickLevel, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            SealedZone zone = ZoneManager.nearestZone(serverLevel, pos, 16);
            boolean vacuum = ZoneManager.isVacuumWorld(serverLevel);
            if (zone == null) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.telemetry.no_zone", vacuum
                                ? Component.translatable("message.spacereloaded.telemetry.env_vacuum")
                                : Component.translatable("message.spacereloaded.telemetry.env_air")));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.telemetry.report",
                        Component.translatable("sealing.spacereloaded.status."
                                + zone.status().name().toLowerCase(java.util.Locale.ROOT)),
                        zone.volume().size(),
                        vacuum ? Component.translatable("message.spacereloaded.telemetry.env_vacuum")
                               : Component.translatable("message.spacereloaded.telemetry.env_air")));
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
