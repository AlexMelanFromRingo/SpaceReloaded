package org.alex_melan.spacereloaded.survey;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Радарограмма (009, US4): ПКМ открывает экран «путь × глубина». Данные — синхронизированный
 * компонент, поэтому экран открывается на клиенте без пакета: клиентский инициализатор ставит
 * {@link #CLIENT_OPEN}.
 */
public class RadargramItem extends Item {

    /** Открыть экран радарограммы (ставит клиент). */
    public static Consumer<RadargramData> CLIENT_OPEN = data -> { };

    public RadargramItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        RadargramData data = player.getItemInHand(hand).get(ModDataComponents.RADARGRAM);
        if (data == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            CLIENT_OPEN.accept(data);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        RadargramData data = stack.get(ModDataComponents.RADARGRAM);
        if (data != null) {
            tooltip.accept(Component.translatable("item.spacereloaded.radargram.info", data.traces(),
                    String.format(Locale.ROOT, "%.1f", data.traces() * GroundRadarService.STEP_M),
                    String.format(Locale.ROOT, "%.0f", org.alex_melan.spacereloaded.core.survey.GroundRadar.depthAt(
                            data.samples() - 1, data.epsilon()))));
        }
    }
}
