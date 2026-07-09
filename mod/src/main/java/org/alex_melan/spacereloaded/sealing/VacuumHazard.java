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

    /** Последнее отправленное клиенту состояние «в открытом вакууме» (звук). */
    private static final java.util.Map<java.util.UUID, Boolean> SYNCED_EXPOSURE =
            new java.util.HashMap<>();

    public static void clearAll() {
        SYNCED_EXPOSURE.clear();
    }

    /** Синхронизация приглушения звука: вакуумное измерение и вне зоны. */
    private static void syncExposure(net.minecraft.server.level.ServerPlayer player, boolean exposed) {
        Boolean previous = SYNCED_EXPOSURE.put(player.getUUID(), exposed);
        if (previous == null || previous != exposed) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                    new org.alex_melan.spacereloaded.network.VacuumStatePayload(exposed));
        }
    }

    public static void tick(ServerLevel level) {
        var config = SpaceReloaded.config();
        if (level.getGameTime() % config.vacuumCheckIntervalTicks != 0) {
            return;
        }
        if (!ZoneManager.isVacuumWorld(level)) {
            // Вернулись в атмосферу — вернуть звук
            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                if (Boolean.TRUE.equals(SYNCED_EXPOSURE.get(player.getUUID()))) {
                    syncExposure(player, false);
                }
            }
            return;
        }
        // Копия: hurtServer может менять реестр сущностей (смерть/дроп)
        List<LivingEntity> victims = new ArrayList<>();
        level.getEntities(LIVING, entity -> true, victims);
        for (LivingEntity entity : victims) {
            boolean insideZone = ZoneManager.isInsideSealedZone(level, entity.blockPosition());
            // Звук глушится в открытом вакууме независимо от режима игры — физика
            if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                syncExposure(serverPlayer, !insideZone);
            }
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (insideZone) {
                continue;
            }
            if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK)) {
                if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
                    continue; // мобы в маске дышат бесплатно (нет инвентаря)
                }
                if (consumeOxygen(player)) {
                    // Дышим; но без полного скафандра среда грызёт (холод/радиация)
                    if (!hasFullSuit(player)) {
                        entity.hurtServer(level, ModDamageTypes.exposure(level),
                                config.exposureDamage);
                    }
                    continue;
                }
            }
            entity.hurtServer(level, ModDamageTypes.vacuum(level), config.vacuumDamage);
        }
    }

    /** Полный сет EVA: грудь+ноги+ботинки из тега space_suit. */
    public static boolean hasFullSuit(net.minecraft.world.entity.player.Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST)
                        .is(org.alex_melan.spacereloaded.registry.ModTags.SPACE_SUIT)
                && player.getItemBySlot(EquipmentSlot.LEGS)
                        .is(org.alex_melan.spacereloaded.registry.ModTags.SPACE_SUIT)
                && player.getItemBySlot(EquipmentSlot.FEET)
                        .is(org.alex_melan.spacereloaded.registry.ModTags.SPACE_SUIT);
    }

    /** Баллоны (US6): маска дышит из первого заряженного баллона в инвентаре. */
    private static boolean consumeOxygen(net.minecraft.world.entity.player.Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ModItems.OXYGEN_CANISTER) && stack.getDamageValue() < stack.getMaxDamage()) {
                stack.setDamageValue(stack.getDamageValue() + 1);
                return true;
            }
        }
        return false;
    }

    private VacuumHazard() {
    }
}
