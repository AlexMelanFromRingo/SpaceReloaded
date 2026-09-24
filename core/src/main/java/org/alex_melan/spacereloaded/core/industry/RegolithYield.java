package org.alex_melan.spacereloaded.core.industry;

import java.util.random.RandomGenerator;

/**
 * Выход цикла реголитового реактора — электролиз расплава реголита (MRE, 004, FR-232, D36).
 * Реголит на ~40–45 % по массе — связанный кислород; MRE при ~1600 °C извлекает 20–30 %
 * массы как O₂ и оставляет на катоде металлы (Fe, Si, Ti, Al), остальное — шлак.
 * Масштаб единиц — от электролизёра мода (лёд → 300 ед. O₂): реголит беднее и дороже
 * (research-physics §4). Металлы упрощены до гарантированной железной пыли и шанса титана
 * (ильменит FeTiO₃ морских районов).
 *
 * @param oxygen         единицы O₂ баллона за цикл
 * @param ironDust       железная пыль за цикл (гарантированно)
 * @param titaniumChance шанс одной титановой пыли за цикл
 * @param slag           шлак за цикл
 */
public record RegolithYield(int oxygen, int ironDust, double titaniumChance, int slag) {

    /** Итог одного цикла. */
    public record Output(int oxygen, int ironDust, int titaniumDust, int slag) {
    }

    public RegolithYield {
        if (titaniumChance < 0 || titaniumChance > 1) {
            throw new IllegalArgumentException("titaniumChance вне [0, 1]: " + titaniumChance);
        }
    }

    public Output roll(RandomGenerator random) {
        int titanium = random.nextDouble() < titaniumChance ? 1 : 0;
        return new Output(oxygen, ironDust, titanium, slag);
    }
}
