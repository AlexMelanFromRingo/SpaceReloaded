package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Угловой редуктор (005, FR-301): коническая передача 1:1 на все 6 граней. Узел хранит
 * «окружную» скорость s (вращение, видимое со стороны грани снаружи); сосед по грани d
 * получает осевую ω = s·sgn(d) — так петли через редуктор согласованы.
 */
public class GearboxBlock extends Block implements EntityBlock, KineticBlock {

    public GearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Kind kind() {
        return Kind.GEARBOX;
    }

    @Override
    public double inertia() {
        return 4.0;
    }

    @Override
    public Material material() {
        return Material.NONE;
    }

    @Override
    public Direction.Axis axis(BlockState state) {
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KineticBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.KINETIC) {
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
