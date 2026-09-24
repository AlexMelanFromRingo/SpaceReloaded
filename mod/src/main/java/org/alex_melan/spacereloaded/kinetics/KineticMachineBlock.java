package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Станок от вала: ПКМ предметом — во вход; Sneak+ПКМ пустой рукой — забрать выход;
 * ПКМ пустой рукой — отчёт (обороты, окно, вход/выход). Воронки: вход сверху/сбоку, выход снизу.
 */
public class KineticMachineBlock extends KineticAxisBlock {

    public KineticMachineBlock(Properties properties, Kind kind, double inertia,
                               BiFunction<BlockPos, BlockState, ? extends KineticBlockEntity> factory,
                               Supplier<BlockEntityType<? extends KineticBlockEntity>> type) {
        super(properties, kind, inertia, Material.STEEL, false, 0.5, factory, type);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof KineticMachineBlockEntity<?> machine && machine.insert(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KineticMachineBlockEntity<?> machine) {
                ItemStack out = machine.takeOutput();
                if (!out.isEmpty() && !player.getInventory().add(out)) {
                    player.drop(out, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
