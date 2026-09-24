package org.alex_melan.spacereloaded.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.vehicle.Terramechanics;

/**
 * Грунт тела для механики Беккера (007, контракт datapack.md) — датапак
 * {@code data/<ns>/spacereloaded/soils/<id>.json}: измерение и параметры n, k_c, k_φ, c, φ, K
 * (единицы Беккера: Н и см); необязательный {@code surface} — верхний блок грунта (цвет орбитального
 * снимка несгенерированной местности, 007 US5). Нет записи — твёрдая поверхность (сопротивление качению 0.015,
 * сцепление 0.7).
 */
public final class Soils {

    public record Entry(Identifier dimension, double n, double kc, double kphi, double cohesion, double phiDeg,
                        double shearK, java.util.Optional<Identifier> surface) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Entry::dimension),
                Codec.DOUBLE.fieldOf("n").forGetter(Entry::n),
                Codec.DOUBLE.fieldOf("kc").forGetter(Entry::kc),
                Codec.DOUBLE.fieldOf("kphi").forGetter(Entry::kphi),
                Codec.DOUBLE.fieldOf("c").forGetter(Entry::cohesion),
                Codec.DOUBLE.fieldOf("phi_deg").forGetter(Entry::phiDeg),
                Codec.DOUBLE.fieldOf("k_cm").forGetter(Entry::shearK),
                Identifier.CODEC.optionalFieldOf("surface").forGetter(Entry::surface)
        ).apply(i, Entry::new));

        public Terramechanics.Soil soil() {
            return new Terramechanics.Soil(n, kc, kphi, cohesion, phiDeg, shearK);
        }
    }

    public static final ResourceKey<Registry<Entry>> SOILS = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "soils"));

    private Soils() {
    }

    /** Рыхлый грунт под колесом: песок, гравий, земля, реголит (тег {@code spacereloaded:loose_soil}). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> LOOSE = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "loose_soil"));

    /** Грунт под колесом: рыхлый блок — грунт тела, иначе null (твёрдая поверхность: камень, плиты). */
    public static Terramechanics.Soil under(ServerLevel level, net.minecraft.core.BlockPos ground) {
        return level.getBlockState(ground).is(LOOSE) ? of(level) : null;
    }

    /** Запись грунта измерения, если есть. */
    public static java.util.Optional<Entry> entry(ServerLevel level) {
        Identifier dim = level.dimension().identifier();
        for (Entry e : level.registryAccess().lookupOrThrow(SOILS)) {
            if (e.dimension().equals(dim)) {
                return java.util.Optional.of(e);
            }
        }
        return java.util.Optional.empty();
    }

    /** Грунт измерения (или null — твёрдая поверхность). */
    public static Terramechanics.Soil of(ServerLevel level) {
        Identifier dim = level.dimension().identifier();
        for (Entry e : level.registryAccess().lookupOrThrow(SOILS)) {
            if (e.dimension().equals(dim)) {
                return e.soil();
            }
        }
        return null;
    }
}
