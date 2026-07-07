package org.alex_melan.spacereloaded.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.sealing.GasFloodFill;
import org.alex_melan.spacereloaded.core.sealing.SealingRequest;
import org.alex_melan.spacereloaded.core.sealing.SealingResult;
import org.alex_melan.spacereloaded.sealing.RegionSnapshot;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

import java.util.concurrent.CompletableFuture;

/**
 * Debug-команды (T027):
 * /spacereloaded debug vacuum on|off — режим вакуума в текущем измерении;
 * /spacereloaded debug zone — диагностика герметичности от позиции игрока
 * (аналог команды из RoomCheckerPlugin, но асинхронный и с точками утечки).
 */
public final class SpaceReloadedCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spacereloaded")
                .then(Commands.literal("debug")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("vacuum")
                                .then(Commands.literal("on").executes(ctx -> setVacuum(ctx.getSource(), true)))
                                .then(Commands.literal("off").executes(ctx -> setVacuum(ctx.getSource(), false))))
                        .then(Commands.literal("zone").executes(ctx -> diagnoseZone(ctx.getSource()))))
                .then(Commands.literal("recipes").executes(ctx -> dumpRecipes(ctx.getSource()))));
    }

    private static int setVacuum(CommandSourceStack source, boolean vacuum) {
        ServerLevel level = source.getLevel();
        ZoneManager.setVacuumWorld(level, vacuum);
        source.sendSuccess(() -> Component.literal("Режим вакуума в " + level.dimension().identifier()
                + ": " + (vacuum ? "ВКЛ" : "ВЫКЛ")), true);
        return 1;
    }

    private static int diagnoseZone(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        int radius = SpaceReloaded.config().sealingMaxRadius;

        // Снимок в главном потоке, полная диагностика в фоне, ответ обратно (принцип IV)
        RegionSnapshot snapshot = RegionSnapshot.capture(level, pos, radius, ZoneManager.isVacuumWorld(level));
        SealingRequest request = SealingRequest.diagnostic(
                PackedPos.pack(pos.getX(), pos.getY(), pos.getZ()), radius);

        CompletableFuture
                .supplyAsync(() -> GasFloodFill.analyze(snapshot, request), ZoneManager.executor())
                .thenAcceptAsync(result -> source.sendSuccess(() -> Component.literal(format(result)), false),
                        level.getServer());
        return 1;
    }

    /** T044: дамп машинных рецептов в чат (пока нет REI-порта для 26.2). */
    private static int dumpRecipes(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var manager = level.recipeAccess();
        int count = 0;
        for (var holder : manager.getRecipes()) {
            String line = switch (holder.value()) {
                case org.alex_melan.spacereloaded.machine.recipe.CrusherRecipe r ->
                        "[дробилка] " + describeIngredient(r.input()) + " -> " + describe(r.resultStack());
                case org.alex_melan.spacereloaded.machine.recipe.ElectricFurnaceRecipe r ->
                        "[электропечь] " + describeIngredient(r.input()) + " -> " + describe(r.resultStack());
                case org.alex_melan.spacereloaded.machine.recipe.AssemblyRecipe r ->
                        "[сборка] " + r.ingredients().stream().map(SpaceReloadedCommands::describeIngredient)
                                .collect(java.util.stream.Collectors.joining(" + ")) + " -> " + describe(r.resultStack());
                default -> null;
            };
            if (line != null) {
                count++;
                String message = line;
                source.sendSuccess(() -> Component.literal(message), false);
            }
        }
        int total = count;
        source.sendSuccess(() -> Component.literal("Всего машинных рецептов: " + total), false);
        return count;
    }

    private static String describeIngredient(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return ingredient.items()
                .map(holder -> holder.value().toString())
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private static String describe(net.minecraft.world.item.ItemStack stack) {
        return stack.getCount() + "x " + stack.getItem();
    }

    private static String format(SealingResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Герметичность: ").append(switch (result.status()) {
            case SEALED -> "ЗАМКНУТО";
            case LEAK -> "УТЕЧКА";
            case UNBOUNDED -> "НЕ ЗАМКНУТО В РАДИУСЕ";
            case INVALID_ORIGIN -> "СТАРТ В СТЕНЕ";
        });
        sb.append(" | объём: ").append(result.volume().size());
        sb.append(" | точек утечки: ").append(result.leakPoints().size());
        sb.append(" | выходов за радиус: ").append(result.escapePoints().size());
        sb.append(" | проверено: ").append(result.blocksVisited());
        sb.append(" | ").append(result.elapsedNanos() / 1_000_000).append(" мс");
        if (!result.leakPoints().isEmpty()) {
            long first = result.leakPoints().iterator().nextLong();
            sb.append(" | первая утечка: ").append(PackedPos.toString(first));
        }
        return sb.toString();
    }

    private SpaceReloadedCommands() {
    }
}
