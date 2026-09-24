package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.alex_melan.spacereloaded.core.lifesupport.Metabolism;
import org.alex_melan.spacereloaded.sealing.SealedZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Отчёт о воздухе зоны (007, FR-508): давление и парциальные давления, запас газа в баках у
 * владельца зоны и аналитический прогноз — через сколько (игровые часы) pCO₂ дойдёт до предела МКС
 * и кислород до порога гипоксии без дозаправки.
 */
public final class LifeSupportReport {

    private LifeSupportReport() {
    }

    public static List<Component> lines(ServerLevel level, SealedZone zone) {
        List<Component> lines = new ArrayList<>();
        LifeSupportState.Gas gas = LifeSupportState.now(level, zone);
        if (gas == null) {
            return lines;
        }
        int co2 = Metabolism.co2Level(gas.pCo2());
        int hyp = Metabolism.hypoxiaLevel(gas.pO2());
        ChatFormatting color = gas.pressure() < CrewHazard.VACUUM_KPA ? ChatFormatting.RED
                : co2 >= 2 || hyp >= 2 ? ChatFormatting.RED : co2 >= 1 || hyp >= 1 ? ChatFormatting.YELLOW
                : ChatFormatting.GREEN;
        lines.add(Component.translatable("message.spacereloaded.life.gas", fmt(gas.pressure(), 1), fmt(gas.pO2(), 1),
                fmt(gas.pCo2(), 2), fmt(gas.mO2() + gas.mN2() + gas.mCo2(), 1)).withStyle(color));
        double o2Store = GasTankBlockEntity.availableAround(level, zone.controllerPos(), GasKind.OXYGEN);
        double n2Store = GasTankBlockEntity.availableAround(level, zone.controllerPos(), GasKind.NITROGEN);
        lines.add(Component.translatable("message.spacereloaded.life.store", fmt(o2Store, 0), fmt(n2Store, 0))
                .withStyle(ChatFormatting.GRAY));
        double co2Days = gas.daysToCo2(Metabolism.CO2_LEVELS[0]);
        double o2Days = gas.daysToO2(Metabolism.O2_LEVELS[0]);
        lines.add(Component.translatable("message.spacereloaded.life.forecast", hours(co2Days), hours(o2Days))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static String fmt(double v, int digits) {
        return String.format(Locale.ROOT, "%." + digits + "f", v);
    }

    /** Игровые часы (сутки × 24) или «—». */
    private static String hours(double days) {
        if (Double.isInfinite(days) || days > 1e6) {
            return "—";
        }
        return String.format(Locale.ROOT, "%.1f", days * 24);
    }
}
