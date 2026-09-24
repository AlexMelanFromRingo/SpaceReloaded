package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Собственные звуки мода (004, FR-254, D40): синтезированы {@code tools/gen_sounds.py}
 * (sox), лежат в {@code assets/spacereloaded/sounds/}, заменяются ресурс-паком.
 * Приглушение в вакууме — общий SoundEngineMixin.
 */
public final class ModSounds {

    public static final SoundEvent MASS_DRIVER_CHARGE = register("mass_driver.charge");
    public static final SoundEvent MASS_DRIVER_FIRE = register("mass_driver.fire");
    public static final SoundEvent MASS_DRIVER_SLED = register("mass_driver.sled");
    public static final SoundEvent CATCHER_CATCH = register("mass_catcher.catch");
    public static final SoundEvent REGOLITH_REACTOR_HUM = register("regolith_reactor.hum");
    public static final SoundEvent ORBITAL_CANNON_FIRE = register("orbital_cannon.fire");
    public static final SoundEvent PRESS_STAMP = register("mechanical_press.stamp");
    public static final SoundEvent LATHE_CUT = register("lathe.cut");
    public static final SoundEvent MOTOR_HUM = register("motor.hum");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }

    private ModSounds() {
    }
}
