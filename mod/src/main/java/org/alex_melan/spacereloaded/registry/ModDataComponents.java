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

    /** Полётная программа, привязанная к ловушке масс (004, FR-209): цель катапульты. */
    public static final DataComponentType<GlobalPos> PROGRAM_CATCHER = register("program_catcher");

    /** Полуфабрикат детали (005): номер пройденного шага цепочки операций. */
    public static final DataComponentType<Integer> MACHINING_STEP = registerInt("machining_step");

    /** Полуфабрикат детали (005): накопленная Σδ² погрешностей операций, мкм². */
    public static final DataComponentType<Float> MACHINING_DELTA_SQ = registerFloat("machining_delta_sq");

    /** Готовая деталь двигателя (005): качество q ∈ [0, 1]. */
    public static final DataComponentType<Float> PART_QUALITY = registerFloat("part_quality");

    /** Чистота кремния, «девятки» N9 (006). */
    public static final DataComponentType<Float> PURITY = registerFloat("purity");
    /** Пластина: пройдено операций маршрута (3 на уровень шаблона), дефекты (1/см²), тип шаблона. */
    public static final DataComponentType<Integer> WAFER_STEP = registerInt("wafer_step");
    public static final DataComponentType<Float> WAFER_DEFECTS = registerFloat("wafer_defects");
    public static final DataComponentType<Integer> WAFER_KIND = registerInt("wafer_kind");
    /** Остаток длины слитка после пилы, мм (слиток, снятый с оправки недорезанным). */
    public static final DataComponentType<Float> INGOT_LENGTH = registerFloat("ingot_length");
    /** Тир наведения полётной программы: 1 — прошивная память, 2 — бортовой компьютер (006). */
    public static final DataComponentType<Integer> GUIDANCE_TIER = registerInt("guidance_tier");
    /** Турбонасос с колесом из жаропрочного никелевого сплава (006): 1. */
    public static final DataComponentType<Integer> TURBINE_SUPERALLOY = registerInt("turbine_superalloy");

    /** Заряд батареи ровера, E (007). */
    public static final DataComponentType<Float> ROVER_CHARGE = registerFloat("rover_charge");
    /** Газовый бак (007): род газа (ordinal GasKind) и масса, кг. */
    public static final DataComponentType<Integer> GAS_KIND = registerInt("gas_kind");
    public static final DataComponentType<Float> GAS_KG = registerFloat("gas_kg");

    /** Частота (канал) ключа связи — прошивается в ЦУПе. */
    public static final DataComponentType<Integer> KEY_FREQUENCY = registerInt("key_frequency");

    /** Частота, записанная в полётную программу при отметке защищённого маяка. */
    public static final DataComponentType<Integer> PROGRAM_FREQUENCY = registerInt("program_frequency");

    private static DataComponentType<Float> registerFloat(String name) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name),
                new DataComponentType.Builder<Float>()
                        .persistent(com.mojang.serialization.Codec.FLOAT)
                        .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT)
                        .build());
    }

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
