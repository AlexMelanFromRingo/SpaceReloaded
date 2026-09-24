package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * Масса кислорода в баллоне (007, D72): 1200 единиц прочности = 0.42 кг O₂. Маска тратит единицу
 * за 10 тиков, то есть 0.84 кг за игровые сутки — та же норма BVAD, что у дыхания в зоне.
 */
public final class OxygenCanisters {

    public static final double KG_PER_UNIT = 0.42 / 1200;

    private OxygenCanisters() {
    }

    /** Зарядить баллон массой kg; возвращает принятую массу. */
    public static double charge(ItemStack canister, double kg) {
        if (!canister.is(ModItems.OXYGEN_CANISTER) || kg <= 0 || canister.getDamageValue() <= 0) {
            return 0;
        }
        int units = (int) Math.min(canister.getDamageValue(), Math.floor(kg / KG_PER_UNIT));
        canister.setDamageValue(canister.getDamageValue() - units);
        return units * KG_PER_UNIT;
    }

    /** Свободное место в баллоне, кг. */
    public static double room(ItemStack canister) {
        return canister.is(ModItems.OXYGEN_CANISTER) ? canister.getDamageValue() * KG_PER_UNIT : 0;
    }
}
