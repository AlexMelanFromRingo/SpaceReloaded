package org.alex_melan.spacereloaded.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.planet.ModTickets;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.jetbrains.annotations.Nullable;

/**
 * Целеуказатель-пульт (US7). Метка и привязка хранятся в data-компонентах
 * самого предмета — по пульту на пушку, персистентно, без утечек между мирами.
 *
 * <ul>
 *   <li>ПКМ по блоку — метка; если пульт привязан, пушка перенаводится
 *       дистанционно (из любого измерения);</li>
 *   <li>Sneak+ПКМ по пушке — привязать пульт к ней;</li>
 *   <li>ПКМ по пушке (без sneak) — навести её по метке (обрабатывает блок);</li>
 *   <li>ПКМ в воздух — дистанционный выстрел привязанной пушки;
 *       Sneak+ПКМ в воздух — её статус.</li>
 * </ul>
 */
public class TargetingDesignatorItem extends Item {

    public TargetingDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockState(pos).is(ModBlocks.ORBITAL_CANNON)) {
            if (player.isSecondaryUseActive()) {
                // Привязка пульта к этой пушке
                stack.set(ModDataComponents.BOUND_CANNON,
                        GlobalPos.of(level.dimension(), pos.immutable()));
                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.designator.bound",
                        pos.toShortString(), level.dimension().identifier().toString()));
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS; // наведение по клику делает блок пушки
        }

        // Метка цели — в предмет
        GlobalPos mark = GlobalPos.of(level.dimension(), pos.immutable());
        stack.set(ModDataComponents.TARGET_MARK, mark);
        player.sendSystemMessage(Component.translatable(
                "message.spacereloaded.designator.marked",
                pos.toShortString(), level.dimension().identifier().toString()));
        // Привязанный пульт сразу перенаводит свою пушку
        if (stack.get(ModDataComponents.BOUND_CANNON) != null) {
            player.sendSystemMessage(remoteRetarget(level.getServer(), stack));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.get(ModDataComponents.BOUND_CANNON) == null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.spacereloaded.designator.no_bound"));
            return InteractionResult.SUCCESS_SERVER;
        }
        // Sneak+ПКМ в воздух — статус, ПКМ — дистанционный выстрел
        serverPlayer.sendSystemMessage(player.isSecondaryUseActive()
                ? remoteStatus(level.getServer(), stack)
                : remoteFire(level.getServer(), stack));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Метка пульта (читает блок пушки при клике по ней). */
    @Nullable
    public static GlobalPos mark(ItemStack stack) {
        return stack.get(ModDataComponents.TARGET_MARK);
    }

    // ---------- Дистанционные операции (используются и стендом) ----------

    /** Перенавести привязанную пушку на метку пульта. */
    public static Component remoteRetarget(MinecraftServer server, ItemStack stack) {
        GlobalPos mark = stack.get(ModDataComponents.TARGET_MARK);
        if (mark == null) {
            return Component.translatable("message.spacereloaded.cannon.no_mark");
        }
        OrbitalCannonBlockEntity cannon = boundCannon(server, stack);
        if (cannon == null) {
            return missingMessage(stack);
        }
        cannon.setTarget(mark);
        return Component.translatable("message.spacereloaded.cannon.target_set",
                mark.pos().toShortString(), mark.dimension().identifier().toString());
    }

    /** Дистанционный выстрел привязанной пушки. */
    public static Component remoteFire(MinecraftServer server, ItemStack stack) {
        OrbitalCannonBlockEntity cannon = boundCannon(server, stack);
        if (cannon == null) {
            return missingMessage(stack);
        }
        GlobalPos bound = stack.get(ModDataComponents.BOUND_CANNON);
        return cannon.tryFire(server.getLevel(bound.dimension()));
    }

    /** Статус привязанной пушки. */
    public static Component remoteStatus(MinecraftServer server, ItemStack stack) {
        OrbitalCannonBlockEntity cannon = boundCannon(server, stack);
        return cannon == null ? missingMessage(stack) : cannon.status();
    }

    /**
     * BE привязанной пушки; её чанк при необходимости подгружается честно —
     * ticket с авто-протуханием + синхронный getChunk (пульт работает
     * из любого измерения).
     */
    @Nullable
    private static OrbitalCannonBlockEntity boundCannon(MinecraftServer server, ItemStack stack) {
        GlobalPos bound = stack.get(ModDataComponents.BOUND_CANNON);
        if (bound == null) {
            return null;
        }
        ServerLevel level = server.getLevel(bound.dimension());
        if (level == null) {
            return null;
        }
        ModTickets.holdAround(level, bound.pos(), 1);
        level.getChunk(bound.pos().getX() >> 4, bound.pos().getZ() >> 4);
        return level.getBlockEntity(bound.pos()) instanceof OrbitalCannonBlockEntity cannon
                ? cannon : null;
    }

    private static Component missingMessage(ItemStack stack) {
        GlobalPos bound = stack.get(ModDataComponents.BOUND_CANNON);
        return Component.translatable("message.spacereloaded.designator.cannon_missing",
                bound == null ? "—" : bound.pos().toShortString(),
                bound == null ? "—" : bound.dimension().identifier().toString());
    }
}
