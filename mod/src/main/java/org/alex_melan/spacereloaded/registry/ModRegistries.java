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
     * Аэродинамический профиль атмосферы тела (Полёт 2.0, FR-080): плотность у
     * поверхности, высота шкалы (в игровых метрах) и уровень отсчёта. Поля лежат
     * в JSON профиля планеты плоско (MapCodec), без вложенного объекта.
     */
    public record AtmosphereSpec(double density, double scaleHeight, double datumY) {
        public static final com.mojang.serialization.MapCodec<AtmosphereSpec> MAP_CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.doubleRange(0.0, 50.0).optionalFieldOf("atmosphere_density", 0.0)
                                .forGetter(AtmosphereSpec::density),
                        Codec.doubleRange(1.0, 100_000.0).optionalFieldOf("scale_height", 110.0)
                                .forGetter(AtmosphereSpec::scaleHeight),
                        Codec.DOUBLE.optionalFieldOf("datum_y", 63.0).forGetter(AtmosphereSpec::datumY)
                ).apply(instance, AtmosphereSpec::new));

        public static final AtmosphereSpec VACUUM = new AtmosphereSpec(0.0, 110.0, 63.0);

        public org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile toCore() {
            return density <= 0
                    ? org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile.VACUUM
                    : new org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile(
                            density, scaleHeight, datumY);
        }
    }



    /** Тепловая пара профиля (temperature / temperature_amplitude) — плоские поля через MapCodec. */
    public record ThermalSpec(double temperature, double temperatureAmplitude) {
        public static final com.mojang.serialization.MapCodec<ThermalSpec> MAP_CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.DOUBLE.optionalFieldOf("temperature", 20.0).forGetter(ThermalSpec::temperature),
                        Codec.DOUBLE.optionalFieldOf("temperature_amplitude", 0.0)
                                .forGetter(ThermalSpec::temperatureAmplitude)
                ).apply(instance, ThermalSpec::new));
    }

    /**
     * Таблица стоимости перелётов профиля (003, FR-100): «id записи цели → Δv, м/с».
     * Значения мода выведены из уравнений Гомана/патч-коник по реальным орбитам
     * (см. TransferOrbitsTest); отсутствующая запись стоит 0 — совместимость с аддонами.
     * Плоское поле {@code transfer_delta_v} через MapCodec (лимит 16 полей RecordCodecBuilder).
     */
    public record TransferSpec(java.util.Map<Identifier, Double> deltaV) {
        public static final com.mojang.serialization.MapCodec<TransferSpec> MAP_CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.unboundedMap(Identifier.CODEC, Codec.doubleRange(0.0, 1.0e6))
                                .optionalFieldOf("transfer_delta_v", java.util.Map.of())
                                .forGetter(TransferSpec::deltaV)
                ).apply(instance, TransferSpec::new));

        public static final TransferSpec NONE = new TransferSpec(java.util.Map.of());

        /** Δv перелёта к цели, м/с (0, если записи нет). */
        public double deltaVTo(Identifier target) {
            Double value = deltaV.get(target);
            return value == null ? 0.0 : value;
        }
    }

    /**
     * Профиль небесного тела (FR-030, паттерн Ad Astra): физика измерения —
     * данными. transition_target — id ПРОФИЛЯ, куда попадает ракета, набрав
     * transition_altitude; координаты масштабируются отношением coordinate_scale.
     * {@code aero} — плотность/высота шкалы атмосферы (FR-080); без полей — вакуум.
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
            long windowPhaseTicks,
            boolean requiresCoverage,
            double temperature,
            double temperatureAmplitude,
            AtmosphereSpec aero,
            TransferSpec transfer
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
                Codec.LONG.optionalFieldOf("window_phase_ticks", 0L).forGetter(PlanetProfile::windowPhaseTicks),
                Codec.BOOL.optionalFieldOf("requires_coverage", false).forGetter(PlanetProfile::requiresCoverage),
                ThermalSpec.MAP_CODEC.forGetter(p -> new ThermalSpec(p.temperature(), p.temperatureAmplitude())),
                AtmosphereSpec.MAP_CODEC.forGetter(PlanetProfile::aero),
                TransferSpec.MAP_CODEC.forGetter(PlanetProfile::transfer)
        ).apply(instance, PlanetProfile::fromCodec));

        /** Фабрика для кодека: 16 полей группы (лимит RecordCodecBuilder) при 17 компонентах записи. */
        private static PlanetProfile fromCodec(Identifier dimension, double gravity, boolean breathable,
                                               double solarEfficiency, double coordinateScale, int transitionAltitude,
                                               java.util.List<Identifier> transitionTargets, String arrival,
                                               String atmosphere, long synodicPeriodTicks, long windowWidthTicks,
                                               long windowPhaseTicks, boolean requiresCoverage,
                                               ThermalSpec thermal, AtmosphereSpec aero, TransferSpec transfer) {
            return new PlanetProfile(dimension, gravity, breathable, solarEfficiency, coordinateScale,
                    transitionAltitude, transitionTargets, arrival, atmosphere, synodicPeriodTicks,
                    windowWidthTicks, windowPhaseTicks, requiresCoverage,
                    thermal.temperature(), thermal.temperatureAmplitude(), aero, transfer);
        }

        /** Стоимость перелёта из этого тела к цели (id записи), м/с; 0 без записи. */
        public double transferDeltaVTo(Identifier target) {
            return transfer == null ? 0.0 : transfer.deltaVTo(target);
        }
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
