package org.alex_melan.spacereloaded.energy;

import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Ректенна: генератор энергии от орбитальных энергоспутников (тикер как у машин). */
public class RectennaBlock extends MachineBlock<RectennaBlockEntity> {

    public RectennaBlock(Properties properties) {
        super(properties, RectennaBlockEntity::new,
                () -> ModBlockEntities.RECTENNA, RectennaBlockEntity::serverTick);
    }
}
