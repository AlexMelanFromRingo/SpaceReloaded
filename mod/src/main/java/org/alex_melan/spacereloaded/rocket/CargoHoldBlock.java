package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Грузовой отсек: контейнер без GUI — хопперы и погрузчик (автоматизация честная). */
public class CargoHoldBlock extends Block implements EntityBlock {

    public CargoHoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoHoldBlockEntity(pos, state);
    }
}
