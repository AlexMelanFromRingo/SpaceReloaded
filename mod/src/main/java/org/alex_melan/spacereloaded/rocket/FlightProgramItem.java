package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Полётная программа (AR-стиль guidance chip, но данные — предмет-носитель):
 * <ul>
 *   <li>Sneak+ПКМ в воздух — циклить планету назначения (реестр планет);</li>
 *   <li>ПКМ по посадочному маяку — записать точку прибытия;</li>
 *   <li>ПКМ по припаркованной ракете — загрузить программу в борт;</li>
 *   <li>ПКМ в воздух — показать записанный маршрут.</li>
 * </ul>
 */
public class FlightProgramItem extends Item {

    public FlightProgramItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.getBlockState(context.getClickedPos()).is(ModBlocks.LANDING_BEACON)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        GlobalPos pad = GlobalPos.of(level.dimension(), context.getClickedPos().immutable());
        context.getItemInHand().set(ModDataComponents.PROGRAM_PAD, pad);
        // Аутентификация: частота пишется в программу, ТОЛЬКО если у игрока есть
        // ключ связи с каналом этого маяка (секрет — ключ, а не доступ к маяку).
        int beaconFreq = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(level.getServer())
                .beaconFrequency(pad);
        int programFreq = 0;
        if (beaconFreq != 0 && hasKey(player, beaconFreq)) {
            programFreq = beaconFreq;
        }
        context.getItemInHand().set(ModDataComponents.PROGRAM_FREQUENCY, programFreq);
        player.sendSystemMessage(Component.translatable(
                "message.spacereloaded.program.pad_set",
                pad.pos().toShortString(), pad.dimension().identifier().toString()));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            // Цикл планеты назначения по синхронизированному реестру
            List<Identifier> planets = new ArrayList<>();
            for (ModRegistries.PlanetProfile profile
                    : serverLevel.registryAccess().lookupOrThrow(ModRegistries.PLANETS)) {
                planets.add(profile.dimension());
            }
            planets.sort(Identifier::compareTo);
            if (planets.isEmpty()) {
                return InteractionResult.PASS;
            }
            Identifier current = stack.get(ModDataComponents.PROGRAM_DESTINATION);
            int index = current == null ? -1 : planets.indexOf(current);
            Identifier next = planets.get((index + 1) % planets.size());
            stack.set(ModDataComponents.PROGRAM_DESTINATION, next);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.program.destination_set",
                    Component.translatable("planet.spacereloaded." + planetKey(next))));
        } else {
            serverPlayer.sendSystemMessage(describe(stack));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Есть ли у игрока ключ связи с нужным каналом (аутентификация доставки). */
    private static boolean hasKey(ServerPlayer player, int frequency) {
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.is(org.alex_melan.spacereloaded.registry.ModItems.FREQUENCY_KEY)
                    && stack.getOrDefault(ModDataComponents.KEY_FREQUENCY, 0) == frequency) {
                return true;
            }
        }
        return false;
    }

    /** Ключ локализации планеты: earth_orbit/moon/... из id измерения. */
    public static String planetKey(Identifier dimension) {
        return dimension.getPath().equals("overworld") ? "earth" : dimension.getPath();
    }

    public static Component describe(ItemStack stack) {
        Identifier destination = stack.get(ModDataComponents.PROGRAM_DESTINATION);
        GlobalPos pad = stack.get(ModDataComponents.PROGRAM_PAD);
        return Component.translatable("message.spacereloaded.program.describe",
                destination == null ? Component.literal("—")
                        : Component.translatable("planet.spacereloaded." + planetKey(destination)),
                pad == null ? "—" : pad.pos().toShortString() + " @ " + pad.dimension().identifier());
    }
}
