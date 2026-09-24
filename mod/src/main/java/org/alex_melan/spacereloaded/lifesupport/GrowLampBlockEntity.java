package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Фитолампа (007, FR-511): светодиоды 1.66 мкмоль/Дж. Лоток под лампой раз в секунду просит
 * мощность своей культуры (пшеница 802 Вт/м², картофель 195) — суточную дозу фотонов в игровых
 * сутках, то есть мощность ×72; энергия — масштаб 006 (31 E/кВт·ч).
 */
public class GrowLampBlockEntity extends MachineBlockEntity {

    public static final double BIO_TIME_SCALE = 72;

    private long litUntil;
    private double debt;

    public GrowLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROW_LAMP, pos, state, 2000, 200, 0);
    }

    /** Отдать свет на секунду при мощности W на м²; false — нет энергии. */
    public boolean power(double watts) {
        debt += EnergyScale.fromJoules(watts * BIO_TIME_SCALE);
        long cost = (long) Math.floor(debt);
        if (energy.amount < cost) {
            debt = Math.min(debt, 1);
            return false;
        }
        energy.amount -= cost;
        debt -= cost;
        if (level != null) {
            litUntil = level.getGameTime() + 40;
        }
        setChanged();
        return true;
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);
        boolean lit = level.getGameTime() < litUntil;
        BlockState state = getBlockState();
        if (state.hasProperty(GrowLampBlock.LIT) && state.getValue(GrowLampBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(GrowLampBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }
}
