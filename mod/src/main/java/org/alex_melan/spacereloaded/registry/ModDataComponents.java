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

    /** Полётная программа: планета назначения. */
    public static final DataComponentType<Identifier> PROGRAM_DESTINATION =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "program_destination"),
                    new DataComponentType.Builder<Identifier>()
                            .persistent(Identifier.CODEC)
                            .networkSynchronized(Identifier.STREAM_CODEC)
                            .build());

    /** Полётная программа: посадочный маяк (точка прибытия). */
    public static final DataComponentType<GlobalPos> PROGRAM_PAD = register("program_pad");

    /** Частота (канал) ключа связи — прошивается в ЦУПе. */
    public static final DataComponentType<Integer> KEY_FREQUENCY = registerInt("key_frequency");

    /** Частота, записанная в полётную программу при отметке защищённого маяка. */
    public static final DataComponentType<Integer> PROGRAM_FREQUENCY = registerInt("program_frequency");

    private static DataComponentType<Integer> registerInt(String name) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name),
                new DataComponentType.Builder<Integer>()
                        .persistent(com.mojang.serialization.Codec.INT)
                        .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
                        .build());
    }

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
