package org.alex_melan.spacereloaded.industry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Достижения событий (004, FR-253, D41): JSON с триггером {@code minecraft:impossible},
 * выдаются кодом из серверных событий (выстрел, приём, O₂, укрытие) — это не получение предмета.
 */
public final class IndustryAdvancements {

    public static final String METHALOX = "methalox";
    public static final String SATELLITE = "satellite";
    public static final String CANNON_FIRE = "cannon_fire";
    public static final String MASS_DRIVER = "mass_driver";
    public static final String MASS_CATCH = "mass_catch";
    public static final String LUNAR_AIR = "lunar_air";
    public static final String UNDERGROUND = "underground";
    public static final String GEAR_RATIO = "gear_ratio";
    public static final String FLYWHEEL = "flywheel_energy";
    public static final String STACK = "electrolysis_stack";
    public static final String TOLERANCE = "tolerance";
    public static final String THINKING_SAND = "thinking_sand";
    public static final String ISO5 = "iso5";
    public static final String NINE_NINES = "nine_nines";
    public static final String LIGHT_TANK = "light_tank";
    public static final String CORIOLIS = "coriolis";
    public static final String GREEN_AIR = "green_air";
    public static final String FIRST_TRACK = "first_track";
    public static final String VIEW_FROM_ABOVE = "view_from_above";
    public static final String CLOSED_LOOP = "closed_loop";
    public static final String CRITICALITY = "criticality";
    public static final String ARGON = "argon";
    public static final String FIRST_MELT = "first_melt";
    public static final String VOICE_FROM_MARS = "voice_from_mars";
    // 009: навигация и разведка
    public static final String CELESTIAL_MECHANICS = "celestial_mechanics";
    public static final String SPECTRUM = "spectrum";
    public static final String BELOW_GROUND = "below_ground";

    private IndustryAdvancements() {
    }

    public static void award(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
        if (holder != null) {
            player.getAdvancements().award(holder, "done");
        }
    }

    public static void awardNearby(ServerLevel level, BlockPos pos, double radius, String name) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, radius)) {
                award(player, name);
            }
        }
    }

    /** Тик уровня: «Под поверхностью» — игрок на Луне в укрытии под толщей породы (раз в 40 тиков). */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 40 != 0
                || !level.dimension().identifier().equals(
                        Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "moon"))) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            BlockPos head = player.blockPosition().above();
            if (org.alex_melan.spacereloaded.network.Thermal.isSheltered(level, head)) {
                award(player, UNDERGROUND);
            }
        }
    }
}
