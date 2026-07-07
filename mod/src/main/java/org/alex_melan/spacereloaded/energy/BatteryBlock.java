package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Аккумулятор: 5 визуальных уровней заряда + GUI по ПКМ. */
public class BatteryBlock extends MachineBlock<BatteryBlockEntity> {

    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public BatteryBlock(Properties properties) {
        super(properties, BatteryBlockEntity::new,
                () -> ModBlockEntities.BATTERY, BatteryBlockEntity::serverTick);
        registerDefaultState(getStateDefinition().any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
