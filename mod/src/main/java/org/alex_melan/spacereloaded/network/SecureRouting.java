package org.alex_melan.spacereloaded.network;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;

/**
 * Защищённая маршрутизация беспилотных рейсов (Phase 12, CTF-элемент).
 * Маяк с частотой (каналом) требует, чтобы полётная программа несла ту же
 * частоту, иначе доставка не проходит (аутентификация). Открытый маяк
 * (частота 0) доставляет любому, но его канал может перехватить чужая
 * тарелка-перехватчик, уведя груз на свою площадку. Отсюда игровая
 * динамика: шифруй свои маяки или рискуешь потерять дефицитный груз.
 */
public final class SecureRouting {

    private SecureRouting() {
    }

    /**
     * @param destination куда реально уйдёт груз (может отличаться от цели при перехвате);
     *                    {@code null} — доставка отклонена (аутентификация не прошла)
     */
    public record Result(GlobalPos destination, boolean intercepted, boolean authFailed) {
    }

    public static Result resolve(MinecraftServer server, GlobalPos targetBeacon, int programFrequency) {
        SpaceNetworkState net = SpaceNetworkState.get(server);
        int beaconFreq = net.beaconFrequency(targetBeacon);
        if (beaconFreq != 0 && programFrequency != beaconFreq) {
            return new Result(null, false, true); // защищённый канал, ключ не подошёл
        }
        if (beaconFreq == 0) {
            // Перехват только живым дишем в ТОМ ЖЕ измерении (заброшенная/выгруженная
            // тарелка не крадёт вечно; межпространственный перехват невозможен)
            for (var entry : net.interceptors().entrySet()) {
                GlobalPos dish = entry.getKey();
                if (entry.getValue() != 0 || !dish.dimension().equals(targetBeacon.dimension())) {
                    continue;
                }
                var level = server.getLevel(dish.dimension());
                if (level != null && level.isLoaded(dish.pos())
                        && level.getBlockEntity(dish.pos()) instanceof InterceptorDishBlockEntity live
                        && live.listenFrequency() == 0) {
                    return new Result(dish, true, false);
                }
            }
        }
        return new Result(targetBeacon, false, false);
    }
}
