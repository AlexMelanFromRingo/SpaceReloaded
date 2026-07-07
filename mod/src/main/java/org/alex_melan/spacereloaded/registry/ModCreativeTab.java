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
                        output.accept(ModBlocks.COAL_GENERATOR);
                        output.accept(ModBlocks.BATTERY);
                        output.accept(ModBlocks.ENERGY_CABLE);
                        output.accept(ModBlocks.CREATIVE_POWER);
                        output.accept(ModItems.OXYGEN_MASK);
                        output.accept(ModItems.OXYGEN_CANISTER);
                        output.accept(ModBlocks.ELECTROLYZER);
                        output.accept(ModBlocks.REFINERY);
                        output.accept(ModBlocks.OIL_SHALE);
                        output.accept(ModItems.FUELING_HOSE);
                        output.accept(ModBlocks.MOON_REGOLITH);
                        output.accept(ModBlocks.MOON_STONE);
                        output.accept(ModBlocks.MOON_ICE);
                        output.accept(ModBlocks.CRUSHER);
                        output.accept(ModBlocks.ELECTRIC_FURNACE);
                        output.accept(ModBlocks.ASSEMBLY_TABLE);
                        output.accept(ModBlocks.TITANIUM_ORE);
                        output.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE);
                        output.accept(ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
                        output.accept(ModItems.RAW_TITANIUM);
                        output.accept(ModItems.RAW_TUNGSTEN);
                        output.accept(ModItems.TITANIUM_DUST);
                        output.accept(ModItems.TUNGSTEN_DUST);
                        output.accept(ModItems.IRON_DUST);
                        output.accept(ModItems.COAL_DUST);
                        output.accept(ModItems.STEEL_BLEND);
                        output.accept(ModItems.TITANIUM_INGOT);
                        output.accept(ModItems.TUNGSTEN_INGOT);
                        output.accept(ModItems.STEEL_INGOT);
                        output.accept(ModItems.TITANIUM_ALLOY_INGOT);
                        output.accept(ModItems.CARBON_FIBER);
                        output.accept(ModBlocks.ROCKET_HULL);
                        output.accept(ModBlocks.FUEL_TANK);
                        output.accept(ModBlocks.ROCKET_ENGINE);
                        output.accept(ModBlocks.COMMAND_MODULE);
                        output.accept(ModBlocks.GYROSCOPE);
                        output.accept(ModBlocks.HYDROLOX_ENGINE);
                        output.accept(ModBlocks.FUELING_PUMP);
                        output.accept(ModBlocks.ROCKET_SEAT);
                        output.accept(ModBlocks.LAUNCH_PAD);
                        output.accept(ModBlocks.ASSEMBLY_PYLON);
                    })
                    .build());

    public static void init() {
    }

    private ModCreativeTab() {
    }
}
