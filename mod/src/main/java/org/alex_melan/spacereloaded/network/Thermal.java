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

    /** Множитель энергопотребления климат-контроля: 1 в комфорте, растёт с |ΔT|. */
    public static double climateLoadFactor(ServerLevel level) {
        var config = SpaceReloaded.config();
        return 1.0 + Math.abs(temperature(level) - config.thermalComfort)
                / Math.max(1.0, config.thermalLoadScale);
    }
}
