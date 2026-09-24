package org.alex_melan.spacereloaded.multiblock;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Ключевой блок мультиблоков 008: лицо к игроку при установке, свойства {@code formed}/{@code active};
 * ПКМ — экран состояния ({@link StatusProvider}), ПКМ предметом — передать его контроллеру
 * ({@link ItemAcceptor}: лёд в стойку, шихта в печь…), тик — только на сервере.
 */
public class ControllerBlock<T extends BlockEntity> extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = FormableBlock.FORMED;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** Серверный тик контроллера. */
    public interface Ticker<T> {
        void tick(T be, ServerLevel level);
    }

    /** Контроллер, принимающий предмет из руки. */
    public interface ItemAcceptor {
        boolean accept(ServerLevel level, ServerPlayer player, ItemStack stack);
    }

    private final BiFunction<BlockPos, BlockState, T> factory;
    private final Supplier<BlockEntityType<T>> type;
    private final Ticker<T> ticker;

    public ControllerBlock(Properties properties, BiFunction<BlockPos, BlockState, T> factory,
                           Supplier<BlockEntityType<T>> type, Ticker<T> ticker) {
        super(properties);
        this.factory = factory;
        this.type = type;
        this.ticker = ticker;
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(FORMED, false)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends BlockEntity> BlockEntityTicker<B> getTicker(Level level, BlockState state, BlockEntityType<B> t) {
        if (level.isClientSide() || t != type.get()) {
            return null;
        }
        return (l, p, s, be) -> ticker.tick((T) be, (ServerLevel) l);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof StatusProvider provider) {
            ServerPlayNetworking.send(serverPlayer, provider.status(serverLevel));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || stack.getItem() instanceof EngineerHammerItem) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ItemAcceptor acceptor) {
            return acceptor.accept(serverLevel, serverPlayer, stack) ? InteractionResult.SUCCESS_SERVER
                    : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
