package org.alex_melan.spacereloaded.core.nuclear;

/**
 * Тепло реактора Kilopower (008, FR-623, D83). Стирлинг берёт тепло с горячего конца тепловой трубы
 * через проводимость G = 1.09 Вт/K на двигатель (**оценка**: 735 Вт(т) при 1073 K и холодном
 * конце 400 K — номинал 250 Вт(э) при КПД 34 %, класс двигателей Kilopower); КПД — 0.55 Карно. Сброс — двусторонние
 * панели радиатора 1 м² по Стефану–Больцману, ε = 0.9. Холодный конец T_х находится из баланса
 * радиатора (итерация). Теплоёмкость зоны с отражателем — 30 кДж/K (**оценка** по разогреву KRUSTY).
 */
public final class ReactorThermal {

    public static final double SIGMA = 5.670e-8;
    public static final double EMISSIVITY = 0.9;
    public static final double CONDUCTANCE_W_PER_K = 735.0 / 673.0;
    public static final double HEAT_CAPACITY_J_PER_K = 30e3;
    public static final double DAMAGE_K = 1300;
    public static final double MELT_K = 1420;

    private ReactorThermal() {
    }

    public static double stirlingEfficiency(double hotK, double coldK) {
        return hotK <= coldK ? 0 : 0.55 * (1 - coldK / hotK);
    }

    /** Сброс радиатора, Вт: n панелей по 1 м² с двух сторон. */
    public static double radiatorW(int panels, double tK, double envK) {
        return 2.0 * panels * EMISSIVITY * SIGMA * (Math.pow(tK, 4) - Math.pow(envK, 4));
    }

    /** Отбор тепла Стирлингами при температуре зоны T: {тепло, Вт(т); электричество, Вт(э); T_х, K}. */
    public static double[] extract(double coreK, int stirlings, int panels, double envK) {
        if (stirlings <= 0 || panels <= 0) {
            return new double[] {0, 0, envK};
        }
        double cold = Math.max(envK, 300);
        double heat = 0;
        double eta = 0;
        for (int i = 0; i < 40; i++) {
            heat = Math.max(0, stirlings * CONDUCTANCE_W_PER_K * (coreK - cold));
            eta = stirlingEfficiency(coreK, cold);
            double reject = heat * (1 - eta);
            // холодный конец, при котором радиатор сбрасывает reject
            double next = Math.pow(reject / (2.0 * panels * EMISSIVITY * SIGMA) + Math.pow(envK, 4), 0.25);
            if (Math.abs(next - cold) < 0.01) {
                cold = next;
                break;
            }
            cold = 0.5 * (cold + next);
        }
        return new double[] {heat, heat * eta, cold};
    }

    /** Шаг температуры зоны за dt при тепловыделении P и отборе Q. */
    public static double step(double coreK, double powerW, double removedW, double dt) {
        return coreK + (powerW - removedW) * dt / HEAT_CAPACITY_J_PER_K;
    }
}
