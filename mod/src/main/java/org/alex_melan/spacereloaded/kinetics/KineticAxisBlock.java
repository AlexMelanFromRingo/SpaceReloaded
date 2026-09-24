package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Узел сети с осью (005): вал, шестерня, муфта, мотор, маховик, станки, ступица. Ось задаётся
 * при установке гранью клика (как у брёвен). Вращающиеся части рисует BER; у «роторных» блоков
 * (вал, шестерни, маховик) модель самого блока невидима — видимый корпус есть только у машин.
 * ПКМ пустой рукой — отчёт узла: об/мин, момент, мощность, состояние сети.
 */
public class KineticAxisBlock extends RotatedPillarBlock implements EntityBlock, KineticBlock {

    private final Kind kind;
    private final double inertia;
    private final Material material;
    private final boolean rotorOnly;
    private final double shapeRadius;
    private final BiFunction<BlockPos, BlockState, ? extends KineticBlockEntity> factory;
    private final Supplier<BlockEntityType<? extends KineticBlockEntity>> type;

    public KineticAxisBlock(Properties properties, Kind kind, double inertia, Material material, boolean rotorOnly,
                            double shapeRadius,
                            BiFunction<BlockPos, BlockState, ? extends KineticBlockEntity> factory,
                            Supplier<BlockEntityType<? extends KineticBlockEntity>> type) {
        super(properties);
        this.kind = kind;
        this.inertia = inertia;
        this.material = material;
        this.rotorOnly = rotorOnly;
        this.factory = factory;
        this.type = type;
        this.shapeRadius = shapeRadius;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public double inertia() {
        return inertia;
    }

    @Override
    public Material material() {
        return material;
    }

    @Override
    public Direction.Axis axis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return rotorOnly ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (kind == Kind.SMALL_GEAR || kind == Kind.LARGE_GEAR || kind == Kind.FLYWHEEL) {
            // Диск: полный в плоскости вращения, тонкий вдоль оси
            double half = kind == Kind.FLYWHEEL ? 0.25 : 0.125;
            double lo = 0.5 - half;
            double hi = 0.5 + half;
            return switch (state.getValue(AXIS)) {
                case X -> Shapes.box(lo, 0, 0, hi, 1, 1);
                case Y -> Shapes.box(0, lo, 0, 1, hi, 1);
                case Z -> Shapes.box(0, 0, lo, 1, 1, hi);
            };
        }
        if (shapeRadius >= 0.5) {
            return Shapes.block();
        }
        // Вал — тонкий поперёк и полный вдоль своей оси
        double lo = 0.5 - shapeRadius;
        double hi = 0.5 + shapeRadius;
        return switch (state.getValue(AXIS)) {
            case X -> Shapes.box(0, lo, lo, 1, hi, hi);
            case Y -> Shapes.box(lo, 0, lo, hi, 1, hi);
            case Z -> Shapes.box(lo, lo, 0, hi, hi, 1);
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, be) -> ((KineticBlockEntity) be).serverTick((ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return KineticBlockEntity.report(level, pos, player);
    }
}
