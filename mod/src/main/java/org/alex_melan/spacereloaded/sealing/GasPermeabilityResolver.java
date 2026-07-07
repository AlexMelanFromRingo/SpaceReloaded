package org.alex_melan.spacereloaded.sealing;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.alex_melan.spacereloaded.core.voxel.GasPermeability;
import org.alex_melan.spacereloaded.registry.ModTags;

/**
 * BlockState → проницаемость для газа (FR-002).
 *
 * <p>Потокобезопасность: BlockState иммутабелен, membership в тегах неизменен
 * между перезагрузками данных — читать из фонового потока безопасно; при
 * перезагрузке датапаков все зоны инвалидируются (ZoneManager).
 */
public final class GasPermeabilityResolver {

    /**
     * @param aboveSurface ячейка выше поверхности мира (по heightmap) — «видит небо»
     * @param vacuumWorld  измерение без атмосферы (или включён debug-режим вакуума)
     */
    public static GasPermeability resolve(BlockState state, boolean aboveSurface, boolean vacuumWorld) {
        if (state.isAir()) {
            return (vacuumWorld && aboveSurface) ? GasPermeability.VACUUM : GasPermeability.OPEN;
        }
        if (state.is(ModTags.PASSES_GAS)) {
            return GasPermeability.OPEN;
        }
        // Открытая дверь/люк/калитка пропускает газ независимо от тегов
        if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
            return GasPermeability.OPEN;
        }
        if (state.is(ModTags.AIRTIGHT)) {
            return GasPermeability.BLOCKED;
        }
        // Автоправило: полный непрозрачный куб герметичен по умолчанию.
        // Частичные блоки (заборы, плиты, закрытые НЕгерметичные двери) газ пропускают —
        // деревянная дверь не держит вакуум, это осознанный хардкор.
        return state.canOcclude() ? GasPermeability.BLOCKED : GasPermeability.OPEN;
    }

    private GasPermeabilityResolver() {
    }
}
