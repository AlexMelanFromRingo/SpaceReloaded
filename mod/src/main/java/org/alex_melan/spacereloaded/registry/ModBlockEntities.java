package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.sealing.AtmosphereControllerBlockEntity;

import java.util.Set;

public final class ModBlockEntities {

    public static final BlockEntityType<AtmosphereControllerBlockEntity> ATMOSPHERE_CONTROLLER =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "atmosphere_controller"),
                    new BlockEntityType<>(AtmosphereControllerBlockEntity::new,
                            Set.of(ModBlocks.ATMOSPHERE_CONTROLLER)));

    public static void init() {
    }

    private ModBlockEntities() {
    }
}
