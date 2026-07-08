package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Data-компоненты предметов (US7): метка и привязка целеуказателя живут
 * НА ПРЕДМЕТЕ — пультов можно держать сколько угодно (по одному на пушку),
 * они переживают перезапуск и не текут между мирами (в отличие от
 * static-карты per-player, от которой ушли по итогам ревью).
 */
public final class ModDataComponents {

    /** Метка цели: измерение + позиция, куда наведёт пушку. */
    public static final DataComponentType<GlobalPos> TARGET_MARK = register("target_mark");

    /** Привязанная пушка: пульт дистанционно перенаводит её и стреляет. */
    public static final DataComponentType<GlobalPos> BOUND_CANNON = register("bound_cannon");

    private static DataComponentType<GlobalPos> register(String name) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name),
                new DataComponentType.Builder<GlobalPos>()
                        .persistent(GlobalPos.CODEC)
                        .networkSynchronized(GlobalPos.STREAM_CODEC)
                        .build());
    }

    public static void init() {
    }

    private ModDataComponents() {
    }
}
