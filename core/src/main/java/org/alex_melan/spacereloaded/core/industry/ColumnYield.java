package org.alex_melan.spacereloaded.core.industry;

/**
 * Ректификационная колонна (005, FR-323, D57). По уравнению Фенске минимальное число ступеней
 * разделения N_min = ln[(x_D/(1−x_D))·((1−x_B)/x_B)] / ln α; для керосиновой фракции сланцевого
 * масла (α ≈ 2, чистота 0.95/0.05) N_min ≈ 8.5 — чем больше тарелок, тем больше целевой фракции
 * отбирается без загрязнения. Игровая форма насыщения: Y(K) = Y₄·(1 − e^(−K/4))/(1 − e^(−1)):
 * 4 тарелки — наследные 100 кг, 8 — 137, 12 — 150, 16 — 155 (дальше выигрыш мал — как в жизни).
 */
public final class ColumnYield {

    public static final int REFERENCE_TRAYS = 4;

    private ColumnYield() {
    }

    public static double yield(int trays, double referenceYield) {
        if (trays < REFERENCE_TRAYS) {
            return referenceYield;
        }
        return referenceYield * (1 - Math.exp(-trays / 4.0)) / (1 - Math.exp(-1.0));
    }
}
