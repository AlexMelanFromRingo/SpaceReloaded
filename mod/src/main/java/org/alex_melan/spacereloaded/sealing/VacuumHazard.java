package org.alex_melan.spacereloaded.sealing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModDamageTypes;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.List;

/**
 * Урон вакуума (T025, FR-006): в безатмосферном измерении вне герметичной зоны
 * и без кислородной маски игрок задыхается. Проверка амортизирована интервалом
 * из конфига; принадлежность зоне — O(1) по индексу.
 */
public final class VacuumHazard {

    public static void tick(ServerLevel level) {
        var config = SpaceReloaded.config();
        if (level.getGameTime() % config.vacuumCheckIntervalTicks != 0) {
            return;
        }
        if (!ZoneManager.isVacuumWorld(level)) {
            return;
        }
        // Копия: hurtServer может менять список игроков (смерть/телепорт)
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (ZoneManager.isInsideSealedZone(level, player.blockPosition())) {
                continue;
            }
            if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK)) {
                continue; // TODO US2: расход кислорода из баллонов
            }
            player.hurtServer(level, ModDamageTypes.vacuum(level), config.vacuumDamage);
        }
    }

    private VacuumHazard() {
    }
}
