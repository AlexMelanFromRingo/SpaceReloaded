package org.alex_melan.spacereloaded.sealing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModDamageTypes;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Урон вакуума (T025, FR-006): в безатмосферном измерении вне герметичной зоны
 * задыхаются ВСЕ живые существа, не только игроки. Кислородная маска на голове
 * спасает и мобов (надетая через диспенсер/команду — честно так честно).
 */
public final class VacuumHazard {

    private static final EntityTypeTest<net.minecraft.world.entity.Entity, LivingEntity> LIVING =
            EntityTypeTest.forClass(LivingEntity.class);

    public static void tick(ServerLevel level) {
        var config = SpaceReloaded.config();
        if (level.getGameTime() % config.vacuumCheckIntervalTicks != 0) {
            return;
        }
        if (!ZoneManager.isVacuumWorld(level)) {
            return;
        }
        // Копия: hurtServer может менять реестр сущностей (смерть/дроп)
        List<LivingEntity> victims = new ArrayList<>();
        level.getEntities(LIVING, entity -> true, victims);
        for (LivingEntity entity : victims) {
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (ZoneManager.isInsideSealedZone(level, entity.blockPosition())) {
                continue;
            }
            if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK)) {
                continue; // TODO US2+: расход кислорода из баллонов
            }
            entity.hurtServer(level, ModDamageTypes.vacuum(level), config.vacuumDamage);
        }
    }

    private VacuumHazard() {
    }
}
