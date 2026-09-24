package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * Масса животного для закона Клейбера (007, FR-503): взрослые особи по справочникам; прочие —
 * объём габарита × 0.3 (заполнение) × 1000 кг/м³ (плотность тела ≈ воды).
 */
final class AnimalMass {

    private static final Map<String, Double> KG = Map.ofEntries(
            Map.entry("cow", 600.0), Map.entry("mooshroom", 600.0), Map.entry("horse", 500.0),
            Map.entry("donkey", 250.0), Map.entry("mule", 350.0), Map.entry("llama", 150.0),
            Map.entry("pig", 100.0), Map.entry("sheep", 60.0), Map.entry("goat", 50.0),
            Map.entry("villager", 70.0), Map.entry("wandering_trader", 70.0), Map.entry("wolf", 35.0),
            Map.entry("cat", 4.0), Map.entry("ocelot", 12.0), Map.entry("chicken", 2.0),
            Map.entry("rabbit", 2.0), Map.entry("fox", 6.0), Map.entry("bee", 0.0001));

    private AnimalMass() {
    }

    static double of(LivingEntity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        Double kg = KG.get(id);
        if (kg != null) {
            return entity.isBaby() ? kg * 0.2 : kg;
        }
        var box = entity.getBoundingBox();
        return box.getXsize() * box.getYsize() * box.getZsize() * 0.3 * 1000;
    }
}
