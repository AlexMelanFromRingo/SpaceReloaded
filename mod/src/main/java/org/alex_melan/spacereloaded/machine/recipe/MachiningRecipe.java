package org.alex_melan.spacereloaded.machine.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModRecipes;

import java.util.List;
import java.util.function.Function;

/**
 * Операция станка от вала (005, FR-311/FR-312, D54/D56): пресс ({@code spacereloaded:pressing})
 * или токарный станок ({@code spacereloaded:machining}). Вход — предмет и шаг полуфабриката
 * ({@code step}, 0 — исходная заготовка); результат — следующий полуфабрикат (шаг + 1,
 * накопленная Σδ²) или готовая деталь (качество). {@code delta_um} — погрешность станка на
 * операцию (−1 — из конфига), {@code energy_j} — энергия резания (токарный),
 * {@code balance} — операция балансировки (Σδ² / 4).
 */
public abstract class MachiningRecipe implements Recipe<SingleRecipeInput> {

    /** Поля рецепта. */
    public record Data(Ingredient ingredient, int step, ItemStackTemplate result, double deltaUm, double energyJ,
                       boolean balance) {
        public static final MapCodec<Data> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Data::ingredient),
                com.mojang.serialization.Codec.INT.optionalFieldOf("step", 0).forGetter(Data::step),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(Data::result),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("delta_um", -1.0).forGetter(Data::deltaUm),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("energy_j", 0.0).forGetter(Data::energyJ),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("balance", false).forGetter(Data::balance)
        ).apply(instance, Data::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.of(
                (buf, d) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, d.ingredient);
                    ByteBufCodecs.VAR_INT.encode(buf, d.step);
                    ItemStackTemplate.STREAM_CODEC.encode(buf, d.result);
                    ByteBufCodecs.DOUBLE.encode(buf, d.deltaUm);
                    ByteBufCodecs.DOUBLE.encode(buf, d.energyJ);
                    ByteBufCodecs.BOOL.encode(buf, d.balance);
                },
                buf -> new Data(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ItemStackTemplate.STREAM_CODEC.decode(buf), ByteBufCodecs.DOUBLE.decode(buf),
                        ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.BOOL.decode(buf)));
    }

    protected final Data data;
    private PlacementInfo placementInfo;

    protected MachiningRecipe(Data data) {
        this.data = data;
    }

    public Data data() {
        return data;
    }

    /** Текущий шаг предмета (0 — нет компонента). */
    public static int stepOf(ItemStack stack) {
        Integer step = stack.get(ModDataComponents.MACHINING_STEP);
        return step == null ? 0 : step;
    }

    public static float deltaSqOf(ItemStack stack) {
        Float sum = stack.get(ModDataComponents.MACHINING_DELTA_SQ);
        return sum == null ? 0f : sum;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.item();
        return data.ingredient.test(stack) && stepOf(stack) == data.step;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return data.result.create();
    }

    public ItemStack resultStack() {
        return data.result.create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(List.of(data.ingredient));
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    static <R extends MachiningRecipe> MapCodec<R> codec(Function<Data, R> factory) {
        return Data.MAP_CODEC.xmap(factory, MachiningRecipe::data);
    }

    static <R extends MachiningRecipe> StreamCodec<RegistryFriendlyByteBuf, R> streamCodec(Function<Data, R> factory) {
        return Data.STREAM_CODEC.map(factory, MachiningRecipe::data);
    }

    /** Пресс. */
    public static final class Pressing extends MachiningRecipe {
        public static final MapCodec<Pressing> MAP_CODEC = codec(Pressing::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, Pressing> STREAM_CODEC = streamCodec(Pressing::new);

        public Pressing(Data data) {
            super(data);
        }

        @Override
        public RecipeSerializer<Pressing> getSerializer() {
            return ModRecipes.PRESSING_SERIALIZER;
        }

        @Override
        public RecipeType<Pressing> getType() {
            return ModRecipes.PRESSING;
        }
    }

    /** Токарный станок. */
    public static final class Lathe extends MachiningRecipe {
        public static final MapCodec<Lathe> MAP_CODEC = codec(Lathe::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, Lathe> STREAM_CODEC = streamCodec(Lathe::new);

        public Lathe(Data data) {
            super(data);
        }

        @Override
        public RecipeSerializer<Lathe> getSerializer() {
            return ModRecipes.MACHINING_SERIALIZER;
        }

        @Override
        public RecipeType<Lathe> getType() {
            return ModRecipes.MACHINING;
        }
    }
}
