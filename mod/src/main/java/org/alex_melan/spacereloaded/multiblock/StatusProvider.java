package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;

/**
 * Контроллер с экраном состояния (008, FR-603): строки физических величин, шкалы и кнопки действий.
 * Экран у всех мультиблоков 008 общий ({@code MachineStatusScreen}); данные и действия — здесь.
 */
public interface StatusProvider {

    /** Снимок для экрана (раз в секунду, пока экран открыт). */
    MachineStatusPayload status(ServerLevel level);

    /** Кнопка экрана: id действия и его значение (например, «шаг стержня +0.05»). */
    default void action(ServerLevel level, ServerPlayer player, String action, double value) {
    }
}
