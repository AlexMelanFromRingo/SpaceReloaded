package org.alex_melan.spacereloaded.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

/**
 * Ключ связи (Phase 12 CTF): прошивается в ЦУПе (получает частоту-канал),
 * затем защищает маяк (ПКМ по маяку записывает частоту) или настраивает
 * перехватчик. Полётная программа при отметке защищённого маяка сама
 * запоминает его частоту, поэтому доставку проходит только «свой» груз.
 */
public class FrequencyKeyItem extends Item {

    public FrequencyKeyItem(Properties properties) {
        super(properties);
    }

    /** Детерминированный ненулевой код канала по позиции ЦУПа. */
    public static int codeFor(BlockPos pos) {
        int code = pos.hashCode() & 0x7FFFFFFF;
        return code == 0 ? 1 : code;
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
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        var state = level.getBlockState(pos);

        if (state.is(ModBlocks.MISSION_CONTROL)) {
            int code = codeFor(pos);
            stack.set(ModDataComponents.KEY_FREQUENCY, code);
            player.sendSystemMessage(Component.translatable("message.spacereloaded.key.programmed", code));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (state.is(ModBlocks.LANDING_BEACON)) {
            int freq = stack.getOrDefault(ModDataComponents.KEY_FREQUENCY, 0);
            SpaceNetworkState.get(level.getServer())
                    .secureBeacon(GlobalPos.of(level.dimension(), pos.immutable()), freq);
            player.sendSystemMessage(Component.translatable(freq == 0
                    ? "message.spacereloaded.key.beacon_open"
                    : "message.spacereloaded.key.beacon_secured", freq));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (level.getBlockEntity(pos) instanceof InterceptorDishBlockEntity dish) {
            int freq = stack.getOrDefault(ModDataComponents.KEY_FREQUENCY, 0);
            dish.setListenFrequency(freq);
            player.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.interceptor.tuned", freq));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
