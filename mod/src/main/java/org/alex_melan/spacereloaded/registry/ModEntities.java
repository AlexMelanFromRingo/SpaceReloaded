package org.alex_melan.spacereloaded.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

public final class ModEntities {

    /** В 26.2 нет ванильного сериализатора NBT для synched data — регистрируем свой. */
    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG_SERIALIZER =
            EntityDataSerializer.forValueType(ByteBufCodecs.TRUSTED_COMPOUND_TAG);

    public static final ResourceKey<EntityType<?>> ROCKET_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "rocket"));

    public static final EntityType<RocketEntity> ROCKET = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, ROCKET_KEY,
            EntityType.Builder.of(RocketEntity::new, MobCategory.MISC)
                    .sized(1.5f, 2.5f) // реальный AABB строится из структуры (makeBoundingBox)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .fireImmune()
                    .build(ROCKET_KEY));

    public static final ResourceKey<EntityType<?>> KINETIC_PROJECTILE_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "kinetic_projectile"));

    /** Вольфрамовый лом в полёте (US7): большой tracking range — виден издалека. */
    public static final EntityType<org.alex_melan.spacereloaded.cannon.KineticProjectileEntity> KINETIC_PROJECTILE =
            Registry.register(BuiltInRegistries.ENTITY_TYPE, KINETIC_PROJECTILE_KEY,
                    EntityType.Builder.of(org.alex_melan.spacereloaded.cannon.KineticProjectileEntity::new,
                                    MobCategory.MISC)
                            .sized(0.4f, 2.6f)
                            .clientTrackingRange(24)
                            .updateInterval(1)
                            .fireImmune()
                            .build(KINETIC_PROJECTILE_KEY));

    public static final ResourceKey<EntityType<?>> METEOR_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "meteor"));

    /** Метеорит: падающая огненная сущность (US: метеориты). */
    public static final EntityType<org.alex_melan.spacereloaded.impact.MeteorEntity> METEOR =
            Registry.register(BuiltInRegistries.ENTITY_TYPE, METEOR_KEY,
                    EntityType.Builder.of(org.alex_melan.spacereloaded.impact.MeteorEntity::new,
                                    MobCategory.MISC)
                            .sized(1.2f, 1.2f)
                            .clientTrackingRange(24)
                            .updateInterval(1)
                            .fireImmune()
                            .build(METEOR_KEY));

    public static void init() {
        FabricEntityDataRegistry.register(
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "compound_tag"),
                COMPOUND_TAG_SERIALIZER);
    }

    private ModEntities() {
    }
}
