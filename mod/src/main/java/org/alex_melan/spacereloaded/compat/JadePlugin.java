package org.alex_melan.spacereloaded.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.cannon.OrbitalCannonBlockEntity;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

/**
 * Серверная половина интеграции с Jade: кладёт цифры блока в NBT, который
 * Jade сам доставит клиенту. Рисование строк — в клиентской половине
 * {@code JadeClientPlugin}: {@code ITooltip} тянет клиентские классы, которых
 * в общем source set нет.
 *
 * <p>Мягкая зависимость: класс упомянут в {@code fabric.mod.json} под ключом
 * точки входа {@code "jade"}. Без Jade этот entrypoint никто не вызывает,
 * класс не грузится, ссылки на {@code snownee.jade.*} не резолвятся.
 */
public class JadePlugin implements IWailaPlugin {

    public static final Identifier ENERGY = id("energy");
    public static final Identifier FUEL = id("fuel_tank");
    public static final Identifier CANNON = id("orbital_cannon");

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, path);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ENERGY_DATA, MachineBlockEntity.class);
        registration.registerBlockDataProvider(FUEL_DATA, FuelTankBlockEntity.class);
        registration.registerBlockDataProvider(CANNON_DATA, OrbitalCannonBlockEntity.class);
    }

    private static final IServerDataProvider<BlockAccessor> ENERGY_DATA =
            new IServerDataProvider<>() {
                @Override
                public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                    if (accessor.getBlockEntity() instanceof MachineBlockEntity machine) {
                        data.putLong("sr_energy", machine.energyStorage().getAmount());
                        data.putLong("sr_energy_max", machine.energyStorage().getCapacity());
                    }
                }

                @Override
                public Identifier getUid() {
                    return ENERGY;
                }
            };

    private static final IServerDataProvider<BlockAccessor> FUEL_DATA =
            new IServerDataProvider<>() {
                @Override
                public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                    if (accessor.getBlockEntity() instanceof FuelTankBlockEntity tank) {
                        data.putDouble("sr_fuel", tank.propellantKg());
                        data.putDouble("sr_fuel_max", tank.capacityKg());
                        data.putString("sr_fuel_type", tank.fuelType());
                    }
                }

                @Override
                public Identifier getUid() {
                    return FUEL;
                }
            };

    private static final IServerDataProvider<BlockAccessor> CANNON_DATA =
            new IServerDataProvider<>() {
                @Override
                public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                    if (accessor.getBlockEntity() instanceof OrbitalCannonBlockEntity cannon) {
                        data.putInt("sr_rods", cannon.rods());
                        data.putInt("sr_max_rods", SpaceReloaded.config().cannonMaxRods);
                        var target = cannon.target();
                        data.putString("sr_target", target == null ? ""
                                : target.pos().toShortString() + " @ "
                                        + target.dimension().identifier().getPath());
                    }
                }

                @Override
                public Identifier getUid() {
                    return CANNON;
                }
            };
}
