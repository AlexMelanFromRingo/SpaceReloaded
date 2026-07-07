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

    private ModDamageTypes() {
    }
}
