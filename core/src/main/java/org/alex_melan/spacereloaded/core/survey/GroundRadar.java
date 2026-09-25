package org.alex_melan.spacereloaded.core.survey;

import java.util.List;

/**
 * Георадар (009, US4, FR-740/741; прототип — RIMFAX на Perseverance, 150–1200 МГц): импульс уходит
 * в грунт, отражается от каждой границы сред и возвращается через время двойного пробега
 * t = Σ 2·dᵢ·√εᵢ / c. Амплитуда эха (дБ) = коэффициент отражения Френеля 20·lg|r|,
 * r = (√ε₁ − √ε₂)/(√ε₁ + √ε₂), минус потери пропускания на верхних границах 20·lg(1 − r²),
 * затухание 2·α·d, α = 8.686·π·f·√ε·tan δ / c (дБ/м, среда с малыми потерями), и сферическое
 * расхождение 20·lg(2d / 1 м). Эхо слабее динамического диапазона приёмника (**оценка** 60 дБ)
 * не видно — отсюда глубина: десятки метров в сухом реголите, метр во влажной почве.
 */
public final class GroundRadar {

    public static final double C = 299_792_458;
    /** Шаг отсчёта трассы, нс, и число отсчётов (3 × 128 = 384 нс — 33 м при ε = 3). */
    public static final double SAMPLE_NS = 3;
    public static final int SAMPLES = 128;

    /** Слой среды под антенной: толщина (м), диэлектрическая проницаемость, тангенс потерь. */
    public record Layer(double thicknessM, double epsilon, double lossTangent) {
    }

    private GroundRadar() {
    }

    /** Затухание волны, дБ/м (одна сторона). */
    public static double attenuationDbPerM(double freqHz, double epsilon, double lossTangent) {
        return 8.686 * Math.PI * freqHz * Math.sqrt(epsilon) * lossTangent / C;
    }

    /** Время двойного пробега до глубины d в однородной среде, нс. */
    public static double twoWayNs(double depthM, double epsilon) {
        return 2 * depthM * Math.sqrt(epsilon) / C * 1e9;
    }

    /** Коэффициент отражения по амплитуде на границе сред. */
    public static double reflection(double eps1, double eps2) {
        double a = Math.sqrt(eps1), b = Math.sqrt(eps2);
        return (a - b) / (a + b);
    }

    /**
     * Трасса: яркость отсчётов 0…1 (0 — шум, 1 — эхо на уровне 0 дБ). Отсчёт 0 — поверхность
     * (прямое отражение от грунта не пишется: антенна прижата к нему).
     */
    public static double[] trace(List<Layer> layers, double freqHz, double rangeDb) {
        double[] out = new double[SAMPLES];
        double t = 0, att = 0, trans = 0, depth = 0;
        for (int i = 0; i + 1 < layers.size(); i++) {
            Layer a = layers.get(i), b = layers.get(i + 1);
            t += twoWayNs(a.thicknessM(), a.epsilon());
            att += 2 * attenuationDbPerM(freqHz, a.epsilon(), a.lossTangent()) * a.thicknessM();
            depth += a.thicknessM();
            double r = reflection(a.epsilon(), b.epsilon());
            if (Math.abs(r) > 1e-6) {
                double db = 20 * Math.log10(Math.abs(r)) + trans - att - 20 * Math.log10(Math.max(1, 2 * depth));
                int k = (int) Math.round(t / SAMPLE_NS);
                if (db > -rangeDb && k < SAMPLES) {
                    out[k] = Math.max(out[k], (db + rangeDb) / rangeDb);
                }
                trans += 2 * 10 * Math.log10(Math.max(1e-12, 1 - r * r));   // туда и обратно
            }
            if (t / SAMPLE_NS >= SAMPLES) {
                break;
            }
        }
        return out;
    }

    /** Глубина отсчёта k в среде ε, м (шкала радарограммы). */
    public static double depthAt(int sample, double epsilon) {
        return sample * SAMPLE_NS * 1e-9 * C / (2 * Math.sqrt(epsilon));
    }
}
