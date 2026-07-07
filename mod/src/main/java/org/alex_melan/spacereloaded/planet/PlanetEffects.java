package org.alex_melan.spacereloaded.planet;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.List;

/**
 * Планетарные эффекты для игроков: пониженная гравитация через транзиентный
 * модификатор атрибута GRAVITY (множитель g/9.81; не сохраняется в NBT —
 * утечки модификаторов между измерениями невозможны).
 */
public final class PlanetEffects {

    private static final Identifier GRAVITY_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "planet_gravity");

    private PlanetEffects() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        double gravity = PlanetManager.gravity(level);
        boolean lowGravity = gravity < PlanetManager.EARTH_GRAVITY - 0.01;
        double amount = gravity / PlanetManager.EARTH_GRAVITY - 1.0; // ADD_MULTIPLIED_TOTAL

        for (ServerPlayer player : List.copyOf(level.players())) {
            AttributeInstance attribute = player.getAttribute(Attributes.GRAVITY);
            if (attribute == null) {
                continue;
            }
            boolean has = attribute.hasModifier(GRAVITY_MODIFIER_ID);
            if (lowGravity && !has) {
                attribute.addTransientModifier(new AttributeModifier(GRAVITY_MODIFIER_ID,
                        amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!lowGravity && has) {
                attribute.removeModifier(GRAVITY_MODIFIER_ID);
            }
        }
    }
}
