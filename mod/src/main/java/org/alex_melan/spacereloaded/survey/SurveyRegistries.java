package org.alex_melan.spacereloaded.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.List;

/**
 * Датапак-реестры разведки 009: {@code spacereloaded:spectral} — минералы с диагностическими полосами
 * ближнего ИК (блок → минерал, цвет карты, полоса), {@code spacereloaded:dielectric} — среды георадара
 * (блок → ε, tan δ). Блоки без записи: спектр без полос (фон карты); для радара — твёрдый блок по
 * умолчанию (ε 6, tan δ 0.02), воздух (1, 0).
 */
public final class SurveyRegistries {

    public record Signature(List<String> blocks, String mineral, int mapColorId, double bandUm) {
        public static final Codec<Signature> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("blocks").forGetter(Signature::blocks),
                Codec.STRING.fieldOf("mineral").forGetter(Signature::mineral),
                Codec.intRange(1, 63).fieldOf("map_color_id").forGetter(Signature::mapColorId),
                Codec.DOUBLE.optionalFieldOf("band_um", 0.0).forGetter(Signature::bandUm)
        ).apply(i, Signature::new));

        public MapColor mapColor() {
            return MapColor.byId(mapColorId);
        }
    }

    public record Dielectric(List<String> blocks, double epsilon, double lossTangent) {
        public static final Codec<Dielectric> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("blocks").forGetter(Dielectric::blocks),
                Codec.doubleRange(1, 100).fieldOf("epsilon").forGetter(Dielectric::epsilon),
                Codec.doubleRange(0, 10).fieldOf("loss_tangent").forGetter(Dielectric::lossTangent)
        ).apply(i, Dielectric::new));
    }

    public static final ResourceKey<Registry<Signature>> SPECTRAL = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "spectral"));
    public static final ResourceKey<Registry<Dielectric>> DIELECTRIC = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "dielectric"));

    public static final Dielectric SOLID = new Dielectric(List.of(), 6.0, 0.02);
    public static final Dielectric AIR = new Dielectric(List.of(), 1.0, 0.0);

    private static final BlockTable<Signature> SIGNATURES = new BlockTable<>(Signature::blocks);
    private static final BlockTable<Dielectric> MEDIA = new BlockTable<>(Dielectric::blocks);

    private SurveyRegistries() {
    }

    public static void init() {
        DynamicRegistries.register(SPECTRAL, Signature.CODEC);
        DynamicRegistries.register(DIELECTRIC, Dielectric.CODEC);
    }

    /** Минерал верхнего блока (null — спектр без диагностических полос). */
    public static Signature signature(RegistryAccess access, BlockState state) {
        SIGNATURES.refresh(access.lookupOrThrow(SPECTRAL));
        return SIGNATURES.get(state);
    }

    /** Среда блока для георадара. */
    public static Dielectric dielectric(RegistryAccess access, BlockState state) {
        if (state.isAir()) {
            return AIR;
        }
        MEDIA.refresh(access.lookupOrThrow(DIELECTRIC));
        Dielectric d = MEDIA.get(state);
        return d != null ? d : SOLID;
    }
}
