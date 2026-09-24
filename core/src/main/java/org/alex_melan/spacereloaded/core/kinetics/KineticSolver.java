package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Решатель жёсткой трансмиссии (005, FR-303, D51): вся сеть — одно уравнение
 * J·dω/dt = A − B·ω − C·sgn(ω) в системе опорного узла, где
 * J = Σ Iᵢ·rᵢ², A = Σ rᵢ·eᵢ·aᵢ, B = Σ rᵢ²·eᵢ·bᵢ, C = Σ |rᵢ|·cᵢ/eᵢ
 * (мощность сохраняется через передачу: τ_оп·ω = τᵢ·ωᵢ → τ_оп = rᵢ·τᵢ; потери — множитель
 * КПД у источника и делитель у нагрузки). Шаг неявный по линейной части (жёсткий мотор при
 * малой инерции валов устойчив), кулоновское трение — с покоем: при ω = 0 и |A| ≤ C сеть стоит.
 *
 * <p>Моменты через узлы — сумма «чистых» приведённых моментов поддерева обхода (включая
 * инерционный −I·r²·α), делённая на |rᵢ|: это момент, который вал узла передаёт к корню, —
 * по нему проверяется прочность валов и муфт.
 */
public final class KineticSolver {

    /**
     * Итог шага.
     *
     * @param omega       ω опорного узла после шага, рад/с
     * @param alpha       угловое ускорение шага, рад/с²
     * @param resting     сеть в покое (статическое трение держит)
     * @param transmitted |момент| через узел к родителю, Н·м (в системе узла)
     * @param power       мощность источника узла τ·ω_loc, Вт (отрицательная — узел поглощает, мотор генерирует)
     */
    public record Step(double omega, double alpha, boolean resting, double[] transmitted, double[] power) {
    }

    /** Нижний предел приведённой инерции, кг·м²: сеть без маховика не бывает безынерционной. */
    public static final double MIN_INERTIA = 1e-3;

    private KineticSolver() {
    }

    /** Равновесие без трения-покоя: ω* = (A − C·sgn)/B (для проверок и отчётов). */
    public static double equilibrium(KineticGraph.Analysis analysis, NodeLoad[] loads) {
        double[] sums = sums(analysis, loads);
        double a = sums[0];
        double b = sums[1];
        double c = sums[2];
        if (Math.abs(a) <= c || b <= 0) {
            return 0.0;
        }
        return (a - Math.signum(a) * c) / b;
    }

    /** {A, B, C, J}. */
    static double[] sums(KineticGraph.Analysis analysis, NodeLoad[] loads) {
        double a = 0;
        double b = 0;
        double c = 0;
        double j = 0;
        for (int i = 0; i < analysis.size(); i++) {
            NodeLoad load = loads[i];
            double r = analysis.ratio()[i];
            double e = analysis.eff()[i];
            a += r * e * load.a();
            b += r * r * e * load.b();
            c += Math.abs(r) * load.c() / e;
            j += load.inertia() * r * r;
        }
        return new double[] {a, b, c, j};
    }

    /** Один шаг длительности dt, с. */
    public static Step step(KineticGraph.Analysis analysis, NodeLoad[] loads, double omega, double dt) {
        int n = analysis.size();
        double[] s = sums(analysis, loads);
        double a = s[0];
        double b = s[1];
        double c = s[2];
        double j = Math.max(MIN_INERTIA, s[3]);
        boolean resting = false;
        double next;
        double frictionScale = 1.0;
        double sgn;
        if (omega == 0.0 && Math.abs(a) <= c) {
            resting = true;
            next = 0.0;
            sgn = Math.signum(a);
            frictionScale = c > 0 ? Math.abs(a) / c : 0.0;
        } else {
            sgn = omega != 0.0 ? Math.signum(omega) : Math.signum(a);
            next = (j * omega + dt * (a - c * sgn)) / (j + dt * b);
            if (omega != 0.0 && Math.signum(next) != Math.signum(omega)) {
                next = 0.0; // трение не разгоняет в обратную сторону — остановка
            }
        }
        double alpha = (next - omega) / dt;

        double[] net = new double[n];
        double[] power = new double[n];
        for (int i = 0; i < n; i++) {
            NodeLoad load = loads[i];
            double r = analysis.ratio()[i];
            double e = analysis.eff()[i];
            double omegaLocal = r * next;
            double source = load.sourceTorque(omegaLocal);
            power[i] = source * omegaLocal;
            net[i] = r * e * source - Math.abs(r) * load.c() / e * sgn * frictionScale
                    - load.inertia() * r * r * alpha;
        }
        double[] subtree = net.clone();
        int[] order = analysis.order();
        for (int k = order.length - 1; k > 0; k--) {
            int i = order[k];
            subtree[analysis.parent()[i]] += subtree[i];
        }
        double[] transmitted = new double[n];
        for (int k = 1; k < order.length; k++) {
            int i = order[k];
            transmitted[i] = Math.abs(subtree[i]) / Math.abs(analysis.ratio()[i]);
        }
        return new Step(next, alpha, resting, transmitted, power);
    }
}
