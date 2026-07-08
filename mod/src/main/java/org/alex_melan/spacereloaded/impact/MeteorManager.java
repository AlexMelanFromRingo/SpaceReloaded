package org.alex_melan.spacereloaded.impact;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModEntities;

/**
 * Метеоритные дожди (backlog): на безатмосферных телах у загруженных игроков
 * с конфигурируемой частотой падают метеориты. Спавн в горизонтальном
 * отступе от игрока (в его прогруженных чанках — тикеты не нужны), падение
 * строго вниз. Предупреждение: свист над точкой падения при спавне.
 */
public final class MeteorManager {

    private MeteorManager() {
    }

    public static void tick(ServerLevel level) {
        var config = SpaceReloaded.config();
        if (!config.meteorsEnabled || level.getGameTime() % config.meteorCheckIntervalTicks != 0) {
            return;
        }
        // Только безатмосферные ПОВЕРХНОСТИ (Луна): нужен профиль планеты
        // с breathable=false и arrival!=platform. Это отсекает и debug-вакуум
        // в оверворлде (профиля нет / дышит), и орбитальную станцию
        // (arrival=platform, breathable=false — но это не поверхность).
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        if (profile.isEmpty() || profile.get().breathable()
                || "platform".equals(profile.get().arrival())) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || random.nextDouble() >= config.meteorChancePerCheck) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = config.meteorMinHorizontal
                    + random.nextDouble() * (config.meteorHorizontalRange - config.meteorMinHorizontal);
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = player.getY() + config.meteorSpawnAltitude;

            MeteorEntity meteor = new MeteorEntity(ModEntities.METEOR, level);
            meteor.setPos(x, y, z);
            meteor.configure(config.meteorMassKg, new Vec3(0, -config.meteorSpeed, 0));
            level.addFreshEntity(meteor);

            // Свист входа над точкой падения — предупреждение
            level.playSound(null, BlockPos.containing(x, player.getY(), z),
                    SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.WEATHER, 6.0f, 0.4f);
        }
    }
}
