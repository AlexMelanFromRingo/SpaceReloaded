package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import org.alex_melan.spacereloaded.core.lifesupport.Metabolism;
import org.alex_melan.spacereloaded.registry.ModDamageTypes;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.sealing.SealedZone;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

/**
 * Состав воздуха зоны и самочувствие экипажа (007, FR-504, D72): раз в секунду игрок в зоне
 * получает эффекты по pCO₂ (гиперкапния: усталость → медлительность → тошнота → потеря сознания)
 * и по pO₂ (гипоксия: медлительность → тошнота и слабость → потеря сознания). Маска с заряженным
 * баллоном — открытый контур: дыхание из баллона, выдох наружу, эффектов нет, баллон тратится.
 */
public final class CrewHazard {

    private static final int DURATION = 60;

    private CrewHazard() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            SealedZone zone = ZoneManager.zoneContaining(level, player.blockPosition());
            if (zone == null || !zone.isSealed()) {
                continue;
            }
            LifeSupportState.Gas gas = LifeSupportState.now(level, zone);
            if (gas == null || gas.pressure() < VACUUM_KPA) {
                continue; // «вакуум» зоны — забота VacuumHazard
            }
            if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK)
                    && org.alex_melan.spacereloaded.sealing.VacuumHazard.consumeOxygen(player)
                    && org.alex_melan.spacereloaded.sealing.VacuumHazard.consumeOxygen(player)) {
                continue;
            }
            apply(level, player, Metabolism.co2Level(gas.pCo2()), Metabolism.hypoxiaLevel(gas.pO2()));
        }
    }

    /** Армстронгова граница: ниже 6.3 кПа кровь кипит — это вакуум, а не «плохой воздух». */
    public static final double VACUUM_KPA = 6.3;

    private static void apply(ServerLevel level, ServerPlayer player, int co2, int hypoxia) {
        if (co2 >= 1) {
            effect(player, MobEffects.MINING_FATIGUE, co2 >= 2 ? 1 : 0);
        }
        if (co2 >= 2 || hypoxia >= 1) {
            effect(player, MobEffects.SLOWNESS, 0);
        }
        if (co2 >= 3 || hypoxia >= 2) {
            effect(player, MobEffects.NAUSEA, 0);
        }
        if (hypoxia >= 2) {
            effect(player, MobEffects.WEAKNESS, 0);
        }
        if (co2 >= 4 || hypoxia >= 3) {
            player.hurtServer(level, ModDamageTypes.asphyxia(level), 2.0f);
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, DURATION, amplifier, true, true));
    }
}
