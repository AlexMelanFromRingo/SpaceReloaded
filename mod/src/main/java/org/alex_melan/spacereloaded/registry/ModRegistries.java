package org.alex_melan.spacereloaded.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.Optional;

/**
 * Датапак-реестры (T043, FR-013): свойства деталей ракет и топлива задаются
 * данными и синхронизируются на клиент. Файлы:
 * data/&lt;ns&gt;/spacereloaded/part_properties/*.json и .../fuels/*.json.
 * В Phase 6 из part_properties строится RocketStructure ядра.
 */
public final class ModRegistries {

    /** Физические свойства детали ракеты (СИ; см. core PartProperties). */
    public record RocketPartEntry(
            Identifier block,
            double massKg,
            String role,
            double thrustN,
            double ispSec,
            Optional<Identifier> fuel,
            double propellantCapacityKg,
            double gyroTorqueNm
    ) {
        public static final Codec<RocketPartEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("block").forGetter(RocketPartEntry::block),
                Codec.DOUBLE.fieldOf("mass_kg").forGetter(RocketPartEntry::massKg),
                Codec.STRING.fieldOf("role").forGetter(RocketPartEntry::role),
                Codec.DOUBLE.optionalFieldOf("thrust_n", 0.0).forGetter(RocketPartEntry::thrustN),
                Codec.DOUBLE.optionalFieldOf("isp_sec", 0.0).forGetter(RocketPartEntry::ispSec),
                Identifier.CODEC.optionalFieldOf("fuel").forGetter(RocketPartEntry::fuel),
                Codec.DOUBLE.optionalFieldOf("propellant_capacity_kg", 0.0)
                        .forGetter(RocketPartEntry::propellantCapacityKg),
                Codec.DOUBLE.optionalFieldOf("gyro_torque_nm", 0.0).forGetter(RocketPartEntry::gyroTorqueNm)
        ).apply(instance, RocketPartEntry::new));
    }

    /** Топливо (пока только относительная эффективность; расширяется данными). */
    public record FuelEntry(double efficiency) {
        public static final Codec<FuelEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("efficiency", 1.0).forGetter(FuelEntry::efficiency)
        ).apply(instance, FuelEntry::new));
    }

    /**
     * Профиль небесного тела (FR-030, паттерн Ad Astra): физика измерения —
     * данными. transition_target — id ПРОФИЛЯ, куда попадает ракета, набрав
     * transition_altitude; координаты масштабируются отношением coordinate_scale.
     */
    public record PlanetProfile(
            Identifier dimension,
            double gravity,
            boolean breathable,
            double solarEfficiency,
            double coordinateScale,
            int transitionAltitude,
            java.util.List<Identifier> transitionTargets,
            String arrival,
            String atmosphere,
            long synodicPeriodTicks,
            long windowWidthTicks,
            long windowPhaseTicks
    ) {
        public static final Codec<PlanetProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(PlanetProfile::dimension),
                Codec.DOUBLE.fieldOf("gravity").forGetter(PlanetProfile::gravity),
                Codec.BOOL.optionalFieldOf("breathable", false).forGetter(PlanetProfile::breathable),
                Codec.DOUBLE.optionalFieldOf("solar_efficiency", 1.0).forGetter(PlanetProfile::solarEfficiency),
                Codec.DOUBLE.optionalFieldOf("coordinate_scale", 1.0).forGetter(PlanetProfile::coordinateScale),
                Codec.INT.optionalFieldOf("transition_altitude", 100_000).forGetter(PlanetProfile::transitionAltitude),
                Identifier.CODEC.listOf().optionalFieldOf("transition_targets", java.util.List.of())
                        .forGetter(PlanetProfile::transitionTargets),
                Codec.STRING.optionalFieldOf("arrival", "descend").forGetter(PlanetProfile::arrival),
                Codec.STRING.optionalFieldOf("atmosphere", "none").forGetter(PlanetProfile::atmosphere),
                Codec.LONG.optionalFieldOf("synodic_period_ticks", 0L).forGetter(PlanetProfile::synodicPeriodTicks),
                Codec.LONG.optionalFieldOf("window_width_ticks", 0L).forGetter(PlanetProfile::windowWidthTicks),
                Codec.LONG.optionalFieldOf("window_phase_ticks", 0L).forGetter(PlanetProfile::windowPhaseTicks)
        ).apply(instance, PlanetProfile::new));
    }

    public static final ResourceKey<Registry<RocketPartEntry>> PART_PROPERTIES =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "part_properties"));

    public static final ResourceKey<Registry<FuelEntry>> FUELS =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "fuels"));

    public static final ResourceKey<Registry<PlanetProfile>> PLANETS =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "planets"));

    public static void init() {
        DynamicRegistries.registerSynced(PART_PROPERTIES, RocketPartEntry.CODEC);
        DynamicRegistries.registerSynced(FUELS, FuelEntry.CODEC);
        DynamicRegistries.registerSynced(PLANETS, PlanetProfile.CODEC);
    }

    private ModRegistries() {
    }
}
