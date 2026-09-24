package org.alex_melan.spacereloaded.core.kinetics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Граф механической трансмиссии (005, FR-301/FR-302, D50). Узел — вращающийся блок, ребро —
 * жёсткая связь «ω_b = f·ω_a» с КПД η (вал по оси: f = +1; сцепление малых шестерён: f = −1;
 * малая → большая: f = −½). Жёсткое звено имеет одну степень свободы: каждому узлу сопоставляется
 * знаковое отношение rᵢ к опорному (узел 0), ωᵢ = rᵢ·ω, и КПД пути eᵢ от опорного узла.
 *
 * <p>Упрощение: КПД пути берётся по остовному дереву обхода в ширину (в цикле реальный поток
 * мощности делится между ветвями — не моделируется). Противоречивый цикл (зубья обязаны
 * вращаться с двумя скоростями сразу) — заклинивание: сеть не вращается.
 */
public final class KineticGraph {

    /** Состояние анализа сети. */
    public enum State {
        OK,
        JAMMED,
        TOO_LARGE,
        EMPTY
    }

    /**
     * Результат анализа.
     *
     * @param state  состояние
     * @param ratio  rᵢ (ωᵢ = rᵢ·ω опорного)
     * @param eff    eᵢ — КПД пути от опорного узла
     * @param parent родитель в дереве обхода (−1 у корня)
     * @param order  порядок обхода в ширину (корень первым)
     */
    public record Analysis(State state, double[] ratio, double[] eff, int[] parent, int[] order) {
        public int size() {
            return ratio.length;
        }
    }

    private record Edge(int to, double factor, double efficiency) {
    }

    private final List<List<Edge>> adjacency = new ArrayList<>();

    /** Добавляет узел, возвращает его индекс. */
    public int addNode() {
        adjacency.add(new ArrayList<>(4));
        return adjacency.size() - 1;
    }

    public int size() {
        return adjacency.size();
    }

    /** Связь ω_b = factor·ω_a с КПД efficiency (обе стороны). */
    public void addEdge(int a, int b, double factor, double efficiency) {
        if (factor == 0) {
            throw new IllegalArgumentException("factor = 0");
        }
        adjacency.get(a).add(new Edge(b, factor, efficiency));
        adjacency.get(b).add(new Edge(a, 1.0 / factor, efficiency));
    }

    /** Анализ от узла 0; сеть больше maxNodes — TOO_LARGE. */
    public Analysis analyze(int maxNodes) {
        int n = adjacency.size();
        if (n == 0) {
            return new Analysis(State.EMPTY, new double[0], new double[0], new int[0], new int[0]);
        }
        double[] ratio = new double[n];
        double[] eff = new double[n];
        int[] parent = new int[n];
        int[] order = new int[n];
        boolean[] seen = new boolean[n];
        Arrays.fill(parent, -1);
        if (n > maxNodes) {
            return new Analysis(State.TOO_LARGE, ratio, eff, parent, new int[0]);
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        ratio[0] = 1.0;
        eff[0] = 1.0;
        seen[0] = true;
        queue.add(0);
        int count = 0;
        State state = State.OK;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order[count++] = u;
            for (Edge edge : adjacency.get(u)) {
                double expected = edge.factor * ratio[u];
                if (!seen[edge.to]) {
                    seen[edge.to] = true;
                    ratio[edge.to] = expected;
                    eff[edge.to] = eff[u] * edge.efficiency;
                    parent[edge.to] = u;
                    queue.add(edge.to);
                } else if (Math.abs(ratio[edge.to] - expected)
                        > 1e-6 * Math.max(Math.abs(expected), Math.abs(ratio[edge.to]))) {
                    state = State.JAMMED;
                }
            }
        }
        return new Analysis(state, ratio, eff, parent, Arrays.copyOf(order, count));
    }
}
