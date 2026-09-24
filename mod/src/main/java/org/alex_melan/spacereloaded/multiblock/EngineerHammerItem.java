package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity;
import org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity;
import org.alex_melan.spacereloaded.machine.ElectrolyzerBlockEntity;
import org.alex_melan.spacereloaded.machine.RefineryBlockEntity;

/**
 * Инженерный молот (005, FR-320, в духе Immersive Engineering): ПКМ по ключевому блоку
 * мультиблока — проверка шаблона и формирование; ошибка — сообщение с клеткой и маркер.
 * Для автоматических структур 004 (катапульта, реголитовый реактор) — их отчёт.
 */
public class EngineerHammerItem extends Item {

    public EngineerHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        var be = level.getBlockEntity(pos);
        if (be instanceof ElectrolyzerBlockEntity electrolyzer) {
            electrolyzer.hammer(level, player);
        } else if (be instanceof RefineryBlockEntity refinery) {
            refinery.hammer(level, player);
        } else if (be instanceof RegolithReactorBlockEntity reactor) {
            reactor.report().forEach(player::sendSystemMessage);
        } else if (be instanceof MassDriverBreechBlockEntity breech) {
            breech.report(level).forEach(player::sendSystemMessage);
        } else {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.hammer.not_key"));
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
