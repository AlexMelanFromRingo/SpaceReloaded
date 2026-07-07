package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.core.rocketry.PartProperties;
import org.alex_melan.spacereloaded.core.rocketry.PartRole;
import org.alex_melan.spacereloaded.registry.ModRegistries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Мост данных: датапак-реестр {@code spacereloaded:part_properties} →
 * {@code PartProperties} ядра (T043 → T051). Кэш на время жизни уровня;
 * сбрасывается при перезагрузке данных (реестр пересоздаётся).
 */
public final class PartPropertiesResolver {

    private final Map<Block, Optional<PartProperties>> cache = new HashMap<>();
    private final Map<Identifier, ModRegistries.RocketPartEntry> byBlockId = new HashMap<>();

    public PartPropertiesResolver(ServerLevel level) {
        Registry<ModRegistries.RocketPartEntry> registry =
                level.registryAccess().lookupOrThrow(ModRegistries.PART_PROPERTIES);
        for (ModRegistries.RocketPartEntry entry : registry) {
            byBlockId.put(entry.block(), entry);
        }
    }

    /** Свойства детали для BlockState либо empty — блок не является деталью ракеты. */
    public Optional<PartProperties> resolve(BlockState state) {
        return cache.computeIfAbsent(state.getBlock(), block -> {
            Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            ModRegistries.RocketPartEntry entry = byBlockId.get(id);
            if (entry == null) {
                return Optional.empty();
            }
            PartRole role;
            try {
                role = PartRole.valueOf(entry.role().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
            String fuel = entry.fuel().map(Identifier::toString).orElse(PartProperties.NO_FUEL);
            return Optional.of(new PartProperties(entry.massKg(), role,
                    entry.thrustN(), entry.ispSec(), fuel,
                    entry.propellantCapacityKg(), entry.gyroTorqueNm()));
        });
    }
}
