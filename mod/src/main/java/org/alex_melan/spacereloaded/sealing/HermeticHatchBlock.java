package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Герметичный люк (T034). Шлюз возникает из ГЕОМЕТРИИ: тамбур с двумя люками —
 * это просто маленькая комната; 26-направленный fill честно обсчитывает её как
 * зону. Интерлок: люк не откроется, пока в радиусе открыт другой люк — оба
 * сразу открыть нельзя, основная зона не разгерметизируется (сценарий US2-1/2).
 * Открытие — через цикл выравнивания давления (задержка + шипение).
 *
 * <p>Закрытый люк герметичен (тег #spacereloaded:airtight); открытый пропускает
 * газ через свойство OPEN — резолвер проверяет его раньше тега.
 */
public class HermeticHatchBlock extends Block {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty CYCLING = BooleanProperty.create("cycling");

    public HermeticHatchBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(OPEN, false)
                .setValue(CYCLING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, CYCLING);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        if (state.getValue(OPEN)) {
            // Закрытие мгновенно и всегда разрешено
            level.setBlock(pos, state.setValue(OPEN, false).setValue(CYCLING, false), 3);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
            ZoneManager.markBlockChanged(serverLevel, pos);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (state.getValue(CYCLING)) {
            return InteractionResult.SUCCESS_SERVER; // цикл уже идёт
        }
        if (findOpenHatchNearby(serverLevel, pos)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(
                        Component.translatable("message.spacereloaded.airlock_interlock"));
            }
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.6f);
            return InteractionResult.SUCCESS_SERVER;
        }
        // Цикл выравнивания давления
        level.setBlock(pos, state.setValue(CYCLING, true), 3);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
        serverLevel.scheduleTick(pos, this, SpaceReloaded.config().airlockCycleTicks);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CYCLING)) {
            return;
        }
        // Повторная проверка интерлока: другой люк могли открыть за время цикла
        if (findOpenHatchNearby(level, pos)) {
            level.setBlock(pos, state.setValue(CYCLING, false), 3);
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, true).setValue(CYCLING, false), 3);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        // scheduleTick-путь не проходит через события использования — метим сами
        ZoneManager.markBlockChanged(level, pos);
    }

    /** Интерлок: есть ли другой ОТКРЫТЫЙ (или открывающийся) люк в радиусе тамбура. */
    private boolean findOpenHatchNearby(ServerLevel level, BlockPos pos) {
        int radius = SpaceReloaded.config().airlockInterlockRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState other = level.getBlockState(cursor);
                    if (other.getBlock() instanceof HermeticHatchBlock
                            && (other.getValue(OPEN) || other.getValue(CYCLING))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
