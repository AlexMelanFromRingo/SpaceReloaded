package org.alex_melan.spacereloaded.core.electronics;

/**
 * Воздух чистой комнаты (006, FR-421, D64): зона объёмом V с фильтровентиляционными модулями
 * (расход Q, эффективность HEPA η), источниками частиц G и подсосом наружного воздуха q с
 * концентрацией C_out (открытый люк, пробоина; у герметичной зоны q = 0). Баланс частиц:
 * V·dC/dt = G + q·C_out − (Q·η + q)·C, стационар C_ss = (G + q·C_out)/(Q·η + q), релаксация
 * C(t) = C_ss + (C₀ − C_ss)·e^(−t/τ), τ = V/(Q·η + q). Всё — замкнутые формулы, без тикеров.
 * Классы — ISO 14644-1 по частицам ≥ 0.5 мкм: C_N = 10^N·(0.1/0.5)^2.08.
 */
public final class CleanroomAir {

    /** Эффективность HEPA-фильтра. */
    public static final double HEPA = 0.9997;
    /** Предел ISO 5 для ≥ 0.5 мкм, частиц/м³. */
    public static final double ISO5_LIMIT = 3520;
    private static final double SIZE_FACTOR = Math.pow(0.1 / 0.5, 2.08);

    /**
     * Состояние: концентрация в момент t0 и параметры, действующие с этого момента.
     *
     * @param c0     концентрация в t0, 1/м³
     * @param t0     момент, мин
     * @param source генерация G, частиц/мин
     * @param flow   расход через фильтры Q, м³/мин
     * @param volume объём зоны V, м³
     * @param infiltration подсос наружного воздуха q, м³/мин
     * @param outside концентрация снаружи C_out, 1/м³
     */
    public record State(double c0, double t0, double source, double flow, double volume,
                        double infiltration, double outside) {

        /** Герметичная зона (q = 0). */
        public State(double c0, double t0, double source, double flow, double volume) {
            this(c0, t0, source, flow, volume, 0, 0);
        }

        /** Скорость очистки Q·η + q, м³/мин. */
        private double removal() {
            return flow * HEPA + infiltration;
        }

        public double steadyState() {
            double removal = removal();
            return removal <= 0 ? Double.POSITIVE_INFINITY : (source + infiltration * outside) / removal;
        }

        public double tau() {
            double removal = removal();
            return removal <= 0 ? Double.POSITIVE_INFINITY : volume / removal;
        }

        /** Концентрация в момент t. */
        public double at(double t) {
            if (removal() <= 0) {
                return c0 + source * Math.max(0, t - t0) / Math.max(1e-9, volume); // без фильтров копится
            }
            double css = steadyState();
            return css + (c0 - css) * Math.exp(-Math.max(0, t - t0) / tau());
        }

        /**
         * Интеграл ∫C dt на [t0, t1] (частиц·мин/м³) — замкнутая форма; вместе с накопленным
         * интегралом прошлых отрезков даёт точную среднюю за любой шаг фаба, сколько бы событий
         * (вход игрока, новый модуль) ни случилось внутри шага.
         */
        public double integral(double t1) {
            double dt = Math.max(0, t1 - t0);
            if (dt == 0) {
                return 0;
            }
            if (removal() <= 0) {
                return (c0 + at(t1)) / 2 * dt; // без фильтров рост линеен — трапеция точна
            }
            double css = steadyState();
            double tau = tau();
            return css * dt + (c0 - css) * tau * (1 - Math.exp(-dt / tau));
        }

        /** Средняя концентрация на [t0, t1] — точный интеграл экспоненты. */
        public double mean(double t1) {
            if (t1 <= t0) {
                return c0;
            }
            return integral(t1) / (t1 - t0);
        }

        /** Новый отрезок: текущая концентрация сохраняется, параметры меняются (событие). */
        public State evolve(double t, double newSource, double newFlow, double newVolume) {
            return new State(at(t), t, newSource, newFlow, newVolume, infiltration, outside);
        }

        /** Новый отрезок с другим подсосом (люк открыт/закрыт). */
        public State evolve(double t, double newSource, double newFlow, double newVolume,
                            double newInfiltration, double newOutside) {
            return new State(at(t), t, newSource, newFlow, newVolume, newInfiltration, newOutside);
        }
    }

    private CleanroomAir() {
    }

    /** Класс ISO 14644-1 (наименьший N, для которого C ≤ C_N), 1…9. */
    public static int isoClass(double concentration) {
        for (int n = 1; n <= 9; n++) {
            if (concentration <= Math.pow(10, n) * SIZE_FACTOR) {
                return n;
            }
        }
        return 9;
    }
}
