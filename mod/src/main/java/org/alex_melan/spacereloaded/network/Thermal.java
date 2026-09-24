package org.alex_melan.spacereloaded.network;

import net.minecraft.server.level.ServerLevel;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModRegistries;

/**
 * Тепловая модель (Phase 14): у каждого тела своя температура с суточным
 * размахом (Луна без атмосферы качается от жара к морозу, у Марса холодно).
 * Экстремальная среда не «ещё один урон игроку» (это было бы дублем удушья),
 * а НАГРУЗКА на климат-контроль базы: контроллер атмосферы на горячем/холодном
 * теле тратит больше энергии, чтобы держать зону, — стимул к хорошей энергетике.
 */
public final class Thermal {

    private Thermal() {
    }

    /** Текущая температура среды измерения, °C (полдень — пик, полночь — минимум). */
    public static double temperature(ServerLevel level) {
        var profile = PlanetManager.profileFor(level);
        double base = profile.map(ModRegistries.PlanetProfile::temperature).orElse(20.0);
        double amplitude = profile.map(ModRegistries.PlanetProfile::temperatureAmplitude).orElse(0.0);
        if (amplitude == 0.0) {
            return base;
        }
        double phase = (level.getGameTime() % 24000L) / 24000.0 * 2.0 * Math.PI;
        return base + amplitude * Math.sin(phase);
    }

    /**
     * Температура среды в точке (004, FR-241): в укрытии (нет неба, над головой ≥ N
     * непрозрачных блоков — лавовая трубка, бункер) — стабильная температура укрытия тела,
     * иначе общая суточная формула измерения.
     */
    public static double temperature(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (isSheltered(level, pos)) {
            return PlanetManager.profileFor(level).map(ModRegistries.PlanetProfile::shelterTemperature)
                    .orElse(temperature(level));
        }
        return temperature(level);
    }

    /** Укрытие по правилу ядра {@link org.alex_melan.spacereloaded.core.worldgen.ShelterRule}. */
    public static boolean isSheltered(ServerLevel level, net.minecraft.core.BlockPos pos) {
        int need = SpaceReloaded.config().shelterMinRockBlocks;
        boolean sky = level.canSeeSky(pos);
        if (sky) {
            return false;
        }
        int opaque = 0;
        net.minecraft.core.BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 64 && opaque < need; i++) {
            cursor.move(net.minecraft.core.Direction.UP);
            if (level.getBlockState(cursor).canOcclude()) {
                opaque++;
            }
        }
        return org.alex_melan.spacereloaded.core.worldgen.ShelterRule.isSheltered(false, opaque, need);
    }

    /** Множитель энергопотребления климат-контроля: 1 в комфорте, растёт с |ΔT|. */
    public static double climateLoadFactor(ServerLevel level) {
        var config = SpaceReloaded.config();
        return 1.0 + Math.abs(temperature(level) - config.thermalComfort)
                / Math.max(1.0, config.thermalLoadScale);
    }

    /** Нагрузка климат-контроля по позиции контроллера (укрытие стабилизирует ΔT). */
    public static double climateLoadFactor(ServerLevel level, net.minecraft.core.BlockPos pos) {
        var config = SpaceReloaded.config();
        return 1.0 + Math.abs(temperature(level, pos) - config.thermalComfort)
                / Math.max(1.0, config.thermalLoadScale);
    }
}
