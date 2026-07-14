package org.alex_melan.spacereloaded.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.List;
import java.util.Map;

/**
 * Топливо как жидкость (запрос с плейтеста): вёдра, лужи и труба любого
 * соседнего мода через Fabric Transfer API.
 *
 * <p><b>Плотность.</b> Внутри мода топливо всегда меряется массой в килограммах:
 * от неё зависит и тяга, и Циолковский. Transfer API меряет объём в каплях.
 * Мост между ними — {@code kgPerBucket}: у гидролокса ведро легче, у керолокса
 * тяжелее, ровно как у настоящих пар. Числа игровые, соотношение честное.
 */
public final class ModFluids {

    /** Всё ракетное топливо: одним тегом удобно проверять «это вообще топливо?». */
    public static final TagKey<Fluid> PROPELLANT = TagKey.create(Registries.FLUID,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "propellant"));

    /**
     * Комплект одного топлива.
     *
     * @param fuelId     строковый id, как он хранится в баках
     * @param kgPerBucket сколько килограммов в одном ведре
     */
    public record Propellant(String fuelId, Fluid source, Fluid flowing,
                             Block block, Item bucket, double kgPerBucket) {
    }

    public static final Propellant KEROLOX = create("kerolox", 24.0);
    public static final Propellant HYDROLOX = create("hydrolox", 12.0);
    public static final Propellant METHALOX = create("methalox", 20.0);

    private static final List<Propellant> ALL = List.of(KEROLOX, HYDROLOX, METHALOX);
    private static final Map<String, Propellant> BY_FUEL_ID = Map.of(
            KEROLOX.fuelId(), KEROLOX,
            HYDROLOX.fuelId(), HYDROLOX,
            METHALOX.fuelId(), METHALOX);

    public static List<Propellant> all() {
        return ALL;
    }

    /** Комплект по строковому id топлива из бака; null, если топливо чужое. */
    public static Propellant byFuelId(String fuelId) {
        return BY_FUEL_ID.get(fuelId);
    }

    /** Комплект по ведру; null, если ведро чужое. */
    public static Propellant byBucket(Item bucket) {
        for (Propellant propellant : ALL) {
            if (propellant.bucket() == bucket) {
                return propellant;
            }
        }
        return null;
    }

    /** Комплект по жидкости (источник или поток); null, если жидкость не наша. */
    public static Propellant byFluid(Fluid fluid) {
        for (Propellant propellant : ALL) {
            if (propellant.source() == fluid || propellant.flowing() == fluid) {
                return propellant;
            }
        }
        return null;
    }

    /** Капли -> килограммы. */
    public static double dropletsToKg(long droplets, double kgPerBucket) {
        return droplets * kgPerBucket / FluidConstants.BUCKET;
    }

    /** Килограммы -> капли, с усечением вниз: лишнюю каплю не выдумываем. */
    public static long kgToDroplets(double kg, double kgPerBucket) {
        return (long) Math.floor(kg / kgPerBucket * FluidConstants.BUCKET);
    }

    /**
     * Изменяемая ячейка на время сборки комплекта: ссылки проставляются по мере
     * появления объектов. Конструктор {@code LiquidBlock} читает source/flowing
     * прямо в момент создания блока, поэтому «сначала всё, потом record» нельзя.
     */
    static final class Cell {
        String fuelId;
        PropellantFluid source;
        PropellantFluid flowing;
        Block block;
        Item bucket;
    }

    private static Propellant create(String name, double kgPerBucket) {
        Cell cell = new Cell();
        cell.fuelId = SpaceReloaded.MOD_ID + ":" + name;
        PropellantFluid source = registerFluid(name, new PropellantFluid.Source(cell));
        PropellantFluid flowing = registerFluid("flowing_" + name, new PropellantFluid.Flowing(cell));
        cell.source = source;
        cell.flowing = flowing;

        Identifier blockId = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, blockId);
        Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
                new PropellantLiquidBlock(source, BlockBehaviour.Properties.of()
                        .liquid()
                        .replaceable()
                        .noCollision()
                        .strength(100.0f)
                        .pushReaction(PushReaction.DESTROY)
                        .noLootTable()
                        .sound(SoundType.EMPTY)
                        .setId(blockKey)));
        cell.block = block;
        // BlockItem у жидкости не бывает: в руке она только в ведре

        Identifier bucketId = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name + "_bucket");
        ResourceKey<Item> bucketKey = ResourceKey.create(Registries.ITEM, bucketId);
        Item bucket = Registry.register(BuiltInRegistries.ITEM, bucketKey,
                new BucketItem(source, new Item.Properties()
                        .setId(bucketKey)
                        .craftRemainder(Items.BUCKET)
                        .stacksTo(1)));
        cell.bucket = bucket;

        return new Propellant(cell.fuelId, source, flowing, block, bucket, kgPerBucket);
    }

    private static PropellantFluid registerFluid(String name, PropellantFluid fluid) {
        ResourceKey<Fluid> key = ResourceKey.create(Registries.FLUID,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
        return Registry.register(BuiltInRegistries.FLUID, key, fluid);
    }

    public static void init() {
        // Класслоадинг триггерит статические регистрации
    }

    private ModFluids() {
    }

    /** Блок жидкости: только проброс protected-конструктора. */
    public static class PropellantLiquidBlock extends LiquidBlock {
        public PropellantLiquidBlock(PropellantFluid fluid, Properties properties) {
            super(fluid, properties);
        }
    }
}
