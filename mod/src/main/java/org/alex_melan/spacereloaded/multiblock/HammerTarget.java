package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ключевой блок мультиблока, который формируется инженерным молотом (008, D80): молот вызывает
 * {@link #hammer} у любой блок-сущности с этим интерфейсом — новым контроллерам не нужна своя ветка
 * в {@link EngineerHammerItem}.
 */
public interface HammerTarget {

    /** Проверка шаблона и формирование; сообщения игроку — внутри. */
    boolean hammer(ServerLevel level, ServerPlayer player);
}
