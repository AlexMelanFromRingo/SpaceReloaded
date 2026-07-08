package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.EnergyUtil;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * Атмосферный сборщик (Phase 11): потребляет энергию и сжимает CO2 из
 * атмосферы измерения в выходной слот. На co2-мире (Марс) быстро, на
 * воздушном (Земля) во много раз медленнее (в воздухе Земли CO2 это след),
 * в вакууме не работает вовсе. Слот [0] — выход, забирается хоппером.
 */
public class AtmosphericCollectorBlockEntity extends ChemMachineBlockEntity {

    public AtmosphericCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERIC_COLLECTOR, pos, state, 1);
    }

    public String atmosphere(ServerLevel level) {
        return PlanetManager.atmosphere(level);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
        }
        var config = SpaceReloaded.config();
        String atmosphere = atmosphere(level);
        if (atmosphere.equals("none")) {
            return; // вакуум: собирать нечего
        }
        int ticks = config.collectorTicks * (atmosphere.equals("co2") ? 1 : config.collectorAirPenalty);
        ItemStack out = items.get(0);
        boolean slotOk = out.isEmpty() || (out.is(ModItems.CARBON_DIOXIDE)
                && out.getCount() < out.getMaxStackSize());
        if (slotOk && energy.amount >= config.chemMachineEnergyPerTick) {
            energy.amount -= config.chemMachineEnergyPerTick;
            progress++;
            if (progress >= ticks) {
                progress = 0;
                if (out.isEmpty()) {
                    items.set(0, new ItemStack(ModItems.CARBON_DIOXIDE));
                } else {
                    out.grow(1);
                }
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }
    }

    @Override
    public Component status(ServerLevel level) {
        String atm = atmosphere(level);
        if (atm.equals("none")) {
            return Component.translatable("message.spacereloaded.collector.vacuum");
        }
        return Component.translatable("message.spacereloaded.collector.status",
                Component.translatable("message.spacereloaded.atmosphere." + atm),
                items.get(0).getCount(), energy.amount);
    }

    // Выход: забор с любой стороны, вставки нет
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
}
