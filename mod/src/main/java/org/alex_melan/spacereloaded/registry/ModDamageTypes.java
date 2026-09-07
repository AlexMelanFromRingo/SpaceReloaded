package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.alex_melan.spacereloaded.SpaceReloaded;

public final class ModDamageTypes {

    /** Урон вакуума (FR-006); сам тип определён датапаком: data/spacereloaded/damage_type/vacuum.json */
    public static final ResourceKey<DamageType> VACUUM = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "vacuum"));

    public static DamageSource vacuum(ServerLevel level) {
        // DamageSources.source(...) в 26.2 приватен — собираем DamageSource из Holder'а сами
        return new DamageSource(level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(VACUUM));
    }

    /** Экстремальная среда (US: EVA): маска дышит, но без полного скафандра — холод/радиация. */
    public static final ResourceKey<DamageType> EXPOSURE = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "exposure"));

    public static DamageSource exposure(ServerLevel level) {
        return new DamageSource(level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(EXPOSURE));
    }

    /**
     * Входной нагрев (Полёт 2.0, FR-083): плазма при входе в атмосферу без теплозащиты.
     * Броня не спасает (тег bypasses_armor) — спасает только возвратная капсула в стеке.
     */
    public static final ResourceKey<DamageType> REENTRY_HEAT = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "reentry_heat"));

    public static DamageSource reentryHeat(ServerLevel level) {
        return new DamageSource(level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(REENTRY_HEAT));
    }

    private ModDamageTypes() {
    }
}
