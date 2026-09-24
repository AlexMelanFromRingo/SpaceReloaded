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
        double planet = PlanetManager.gravity(level);
        var crew = org.alex_melan.spacereloaded.station.CrewState.get(level.getServer());
        for (ServerPlayer player : List.copyOf(level.players())) {
            AttributeInstance attribute = player.getAttribute(Attributes.GRAVITY);
            if (attribute == null) {
                continue;
            }
            // 007: во вращающемся кольце вес — ω²·r по высоте над ободом, а не гравитация тела
            var ring = org.alex_melan.spacereloaded.station.SpinRings.gravityFor(level, player);
            double gravity = ring.orElse(planet);
            if (ring.isPresent() && gravity >= org.alex_melan.spacereloaded.station.CrewState.HEALTHY_G) {
                org.alex_melan.spacereloaded.industry.IndustryAdvancements.award(player,
                        org.alex_melan.spacereloaded.industry.IndustryAdvancements.CORIOLIS);
            }
            crew.update(player, gravity, 20 / 24000.0);
            double amount = gravity / PlanetManager.EARTH_GRAVITY - 1.0; // ADD_MULTIPLIED_TOTAL
            AttributeModifier current = attribute.getModifier(GRAVITY_MODIFIER_ID);
            boolean needed = Math.abs(amount) > 0.001;
            if (current != null && (!needed || Math.abs(current.amount() - amount) > 1e-4)) {
                attribute.removeModifier(GRAVITY_MODIFIER_ID);
                current = null;
            }
            if (needed && current == null) {
                attribute.addTransientModifier(new AttributeModifier(GRAVITY_MODIFIER_ID,
                        amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }
}
