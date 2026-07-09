package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Герметичный люк (T034). Шлюз возникает из геометрии: тамбур с двумя люками —
 * обычная зона для 26-направленного fill.
 *
 * <p><b>Группы люков</b>: смежные люки (до {@value #MAX_GROUP} блоков — проём
 * 1×2/2×1/2×2) работают как одна дверь: открываются/закрываются вместе, друг
 * друга НЕ интерлочат. Интерлок действует только между разными группами:
 * пока открыта чужая группа в радиусе — эта не откроется.
 * Открытие — через цикл выравнивания давления (задержка + шипение).
 *
 * <p>Закрытый люк герметичен (тег #spacereloaded:airtight); открытый пропускает
 * газ через свойство OPEN — резолвер проверяет его раньше тега.
 */
public class HermeticHatchBlock extends Block {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty CYCLING = BooleanProperty.create("cycling");
    /**
     * Запомненный редстоун-сигнал (как у ванильной двери). Без него любое
     * обновление соседа — включая наш собственный setBlock по членам группы —
     * выглядит как «сигнал снят» и мгновенно захлопывает открытую дверь.
     */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** Максимум люков в группе-«двери» (2×2 проём). */
    private static final int MAX_GROUP = 4;

    public HermeticHatchBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(OPEN, false)
                .setValue(CYCLING, false)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, CYCLING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED,
                context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
        List<BlockPos> group = collectGroup(serverLevel, pos);

        boolean anyOpen = group.stream().anyMatch(p -> serverLevel.getBlockState(p).getValue(OPEN));
        if (anyOpen) {
            // Закрытие всей группы мгновенно и всегда разрешено
            setGroup(serverLevel, group, false, false);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS_SERVER;
        }
        boolean anyCycling = group.stream().anyMatch(p -> serverLevel.getBlockState(p).getValue(CYCLING));
        if (anyCycling) {
            return InteractionResult.SUCCESS_SERVER; // цикл уже идёт
        }
        if (!beginCycle(serverLevel, pos, group) && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.spacereloaded.airlock_interlock"));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Запуск цикла выравнивания давления (общий для ПКМ и редстоуна).
     * @return false, если интерлок не пустил (рядом открыт другой люк)
     */
    private boolean beginCycle(ServerLevel level, BlockPos pos, List<BlockPos> group) {
        if (findOpenHatchNearby(level, pos, group)) {
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.6f);
            return false;
        }
        for (BlockPos member : group) {
            level.setBlock(member, level.getBlockState(member).setValue(CYCLING, true), 3);
        }
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
        level.scheduleTick(pos, this, SpaceReloaded.config().airlockCycleTicks);
        return true;
    }

    /**
     * Редстоун-управление шлюзом (UX-обвязка): сигнал открывает группу через
     * цикл (с интерлоком), снятие сигнала — мгновенно закрывает. Клик по люку
     * работает как прежде; редстоун — для автоматики шлюзовых камер.
     *
     * <p>Реагируем только на ФРОНТ сигнала (POWERED != hasNeighborSignal).
     * Иначе setBlock с UPDATE_NEIGHBORS по соседнему люку группы приходит сюда
     * как «сигнала нет, а дверь открыта» и захлопывает её: дверь 2×2
     * открывалась одним блоком или ни одним, и только рычаг держал её открытой.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock,
                                   net.minecraft.world.level.redstone.Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, sourceBlock, orientation, movedByPiston);
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState current = serverLevel.getBlockState(pos);
        if (!(current.getBlock() instanceof HermeticHatchBlock)) {
            return;
        }
        boolean powered = serverLevel.hasNeighborSignal(pos);
        if (powered == current.getValue(POWERED)) {
            return; // сигнал не менялся: обновление соседа нас не касается
        }
        serverLevel.setBlock(pos, current.setValue(POWERED, powered), Block.UPDATE_CLIENTS);

        boolean open = current.getValue(OPEN);
        boolean cycling = current.getValue(CYCLING);
        List<BlockPos> group = collectGroup(serverLevel, pos);
        if (powered && !open && !cycling) {
            beginCycle(serverLevel, pos, group);
        } else if (!powered && open) {
            setGroup(serverLevel, group, false, false);
            serverLevel.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CYCLING)) {
            return;
        }
        List<BlockPos> group = collectGroup(level, pos);
        // Повторная проверка интерлока: чужой люк могли открыть за время цикла
        if (findOpenHatchNearby(level, pos, group)) {
            setGroup(level, group, false, false);
            return;
        }
        setGroup(level, group, true, false);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private void setGroup(ServerLevel level, List<BlockPos> group, boolean open, boolean cycling) {
        for (BlockPos member : group) {
            BlockState memberState = level.getBlockState(member);
            if (memberState.getBlock() instanceof HermeticHatchBlock) {
                level.setBlock(member, memberState.setValue(OPEN, open).setValue(CYCLING, cycling), 3);
                // setBlock/scheduleTick не проходят через события использования — метим сами
                ZoneManager.markBlockChanged(level, member);
            }
        }
    }

    /** Группа-«дверь»: смежные люки (6-связность), лимит MAX_GROUP. */
    private static List<BlockPos> collectGroup(ServerLevel level, BlockPos start) {
        List<BlockPos> group = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        visited.add(start.immutable());
        while (!queue.isEmpty() && group.size() < MAX_GROUP) {
            BlockPos current = queue.poll();
            group.add(current);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.add(neighbor)
                        && level.getBlockState(neighbor).getBlock() instanceof HermeticHatchBlock) {
                    queue.add(neighbor);
                }
            }
        }
        return group;
    }

    /** Интерлок: открытый/открывающийся люк ЧУЖОЙ группы в радиусе тамбура. */
    private boolean findOpenHatchNearby(ServerLevel level, BlockPos pos, List<BlockPos> ownGroup) {
        Set<BlockPos> own = new HashSet<>(ownGroup);
        int radius = SpaceReloaded.config().airlockInterlockRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (own.contains(cursor)) {
                        continue;
                    }
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
