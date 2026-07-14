package org.alex_melan.spacereloaded.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/**
 * Клиентская половина Jade: рисует то, что положила серверная.
 *
 * <p>Провайдеры создаются внутри {@code registerClient}, а не в статических
 * полях: на выделенном сервере Jade всё равно загрузит этот класс, и трогать
 * клиентские классы в его инициализаторе нельзя.
 */
public class JadeClientPlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                if (!data.contains("sr_energy")) {
                    return;
                }
                tooltip.add(Component.translatable("jade.spacereloaded.energy",
                        data.getLongOr("sr_energy", 0), data.getLongOr("sr_energy_max", 0)));
            }

            @Override
            public Identifier getUid() {
                return JadePlugin.ENERGY;
            }
        }, Block.class);

        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                if (!data.contains("sr_fuel")) {
                    return;
                }
                String fuelType = data.getStringOr("sr_fuel_type", "");
                Component fuel = fuelType.isEmpty()
                        ? Component.translatable("fuel.spacereloaded.empty")
                        : Component.translatable("fuel.spacereloaded."
                                + fuelType.substring(fuelType.indexOf(':') + 1));
                tooltip.add(Component.translatable("jade.spacereloaded.fuel",
                        String.format(Locale.ROOT, "%.0f", data.getDoubleOr("sr_fuel", 0)),
                        String.format(Locale.ROOT, "%.0f", data.getDoubleOr("sr_fuel_max", 0)),
                        fuel));
            }

            @Override
            public Identifier getUid() {
                return JadePlugin.FUEL;
            }
        }, Block.class);

        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                if (!data.contains("sr_rods")) {
                    return;
                }
                tooltip.add(Component.translatable("jade.spacereloaded.rods",
                        data.getIntOr("sr_rods", 0), data.getIntOr("sr_max_rods", 0)));
                String target = data.getStringOr("sr_target", "");
                tooltip.add(target.isEmpty()
                        ? Component.translatable("screen.spacereloaded.cannon.no_target")
                        : Component.translatable("jade.spacereloaded.target", target));
            }

            @Override
            public Identifier getUid() {
                return JadePlugin.CANNON;
            }
        }, Block.class);
    }
}
