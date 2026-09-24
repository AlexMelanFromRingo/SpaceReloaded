package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

public class AtmosphereControllerBlock extends Block implements EntityBlock,
        org.alex_melan.spacereloaded.registry.CosmeticState {

    public AtmosphereControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE);
    }

    @Override
    public boolean onlyCosmetic(BlockState before, BlockState after) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmosphereControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.ATMOSPHERE_CONTROLLER) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            AtmosphereControllerBlockEntity controller = (AtmosphereControllerBlockEntity) blockEntity;
            controller.serverTick((ServerLevel) tickLevel);
            // Питание есть — вентилятор гонит воздух, лампа горит
            BlockState st = controller.getBlockState();
            boolean on = controller.powered();
            if (st.getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE) != on) {
                tickLevel.setBlock(pos, st.setValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE, on),
                        Block.UPDATE_CLIENTS);
            }
        };
    }
}
