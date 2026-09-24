package org.alex_melan.spacereloaded.lifesupport;

/**
 * Масштаб энергии процессов 006–007: A = 31 E на кВт·ч — честны отношения цен процессов
 * (фитолампа пшеницы в 6.8 раза дороже салатной, насос шлюза, десорбция цеолита), абсолют —
 * общая калибровка мода.
 */
public final class EnergyScale {

    public static final double E_PER_KWH = 31;

    private EnergyScale() {
    }

    public static double fromJoules(double joules) {
        return joules / 3.6e6 * E_PER_KWH;
    }
}
