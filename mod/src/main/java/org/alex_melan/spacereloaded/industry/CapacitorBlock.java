package org.alex_melan.spacereloaded.industry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.alex_melan.spacereloaded.energy.MachineBlock;
import org.alex_melan.spacereloaded.registry.CosmeticState;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Конденсатор катапульты с видимой шкалой заряда (0…4), как у батареи. */
public class CapacitorBlock extends MachineBlock<CapacitorBlockEntity> implements CosmeticState {

    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public CapacitorBlock(Properties properties) {
        super(properties, CapacitorBlockEntity::new, () -> ModBlockEntities.CAPACITOR, CapacitorBlockEntity::serverTick);
        registerDefaultState(getStateDefinition().any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    public boolean onlyCosmetic(BlockState before, BlockState after) {
        return true;
    }
}
