package org.alex_melan.spacereloaded.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.SpaceReloaded;

public final class ModCreativeTab {

    public static final CreativeModeTab TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "main")),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spacereloaded"))
                    .icon(() -> new ItemStack(ModBlocks.ATMOSPHERE_CONTROLLER))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.HULL_PLATING);
                        output.accept(ModBlocks.HERMETIC_GLASS);
                        output.accept(ModBlocks.HERMETIC_HATCH);
                        output.accept(ModBlocks.ATMOSPHERE_CONTROLLER);
                        output.accept(ModBlocks.SOLAR_PANEL);
                        output.accept(ModBlocks.RTG);
                        output.accept(ModBlocks.BATTERY);
                        output.accept(ModBlocks.ENERGY_CABLE);
                        output.accept(ModItems.OXYGEN_MASK);
                    })
                    .build());

    public static void init() {
    }

    private ModCreativeTab() {
    }
}
