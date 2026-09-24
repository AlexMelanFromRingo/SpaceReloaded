package org.alex_melan.spacereloaded.lifesupport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.lifesupport.CropModel;

/**
 * Культуры гидропоники — датапак {@code data/<ns>/spacereloaded/crops/<id>.json} (007, контракт
 * datapack.md): семя, урожай, солома, свет, цикл, продуктивность и газообмен по NASA BVAD,
 * {@code fresh_factor} — сырая масса урожая на сухую.
 */
public final class CropProfiles {

    public record Profile(Identifier seed, Identifier harvest, Identifier byproduct, double dli, double cycleDays,
                          double edible, double harvestIndex, double o2, double co2, double water, double freshFactor) {
        public static final Codec<Profile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("seed").forGetter(Profile::seed),
                Identifier.CODEC.fieldOf("harvest").forGetter(Profile::harvest),
                Identifier.CODEC.fieldOf("byproduct").forGetter(Profile::byproduct),
                Codec.DOUBLE.fieldOf("dli").forGetter(Profile::dli),
                Codec.DOUBLE.fieldOf("cycle_days").forGetter(Profile::cycleDays),
                Codec.DOUBLE.fieldOf("edible_g_m2_day").forGetter(Profile::edible),
                Codec.DOUBLE.fieldOf("harvest_index").forGetter(Profile::harvestIndex),
                Codec.DOUBLE.fieldOf("o2_g_m2_day").forGetter(Profile::o2),
                Codec.DOUBLE.fieldOf("co2_g_m2_day").forGetter(Profile::co2),
                Codec.DOUBLE.fieldOf("water_kg_m2_day").forGetter(Profile::water),
                Codec.DOUBLE.optionalFieldOf("fresh_factor", 1.0).forGetter(Profile::freshFactor)
        ).apply(i, Profile::new));

        public CropModel.Crop crop() {
            return new CropModel.Crop(seed.getPath(), dli, cycleDays, edible, harvestIndex, o2, co2, water);
        }

        public Item harvestItem() {
            return BuiltInRegistries.ITEM.getValue(harvest);
        }

        public Item byproductItem() {
            return BuiltInRegistries.ITEM.getValue(byproduct);
        }
    }

    public static final ResourceKey<Registry<Profile>> CROPS = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "crops"));

    private CropProfiles() {
    }

    /** Профиль по семени (или null). */
    public static Profile bySeed(RegistryAccess access, ItemStack seed) {
        Identifier id = BuiltInRegistries.ITEM.getKey(seed.getItem());
        for (Profile p : access.lookupOrThrow(CROPS)) {
            if (p.seed().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Профиль по идентификатору записи. */
    public static Profile byId(RegistryAccess access, Identifier id) {
        return access.lookupOrThrow(CROPS).getValue(id);
    }

    public static Identifier idOf(RegistryAccess access, Profile profile) {
        return access.lookupOrThrow(CROPS).getKey(profile);
    }
}
