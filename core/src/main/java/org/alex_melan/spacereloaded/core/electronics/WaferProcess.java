package org.alex_melan.spacereloaded.core.electronics;

/**
 * Маршрут пластины по фабу (006, FR-423, D65): каждый уровень шаблона — три операции в разных
 * машинах, как в настоящем планарном процессе: окисление (диффузионная печь) → экспонирование
 * через шаблон (станция литографии) → травление окна в SiO₂ плавиковой кислотой (травильная
 * ванна). Последний уровень — металлизация: напыление алюминия вместо окисления и травление
 * алюминия щёлочью. Шаг s ∈ [0, 3M]: уровень s/3, фаза s%3; s = 3M — пластина готова к резке.
 * Каждая операция в чистой комнате добавляет треть дефектов слоя по средней концентрации за
 * операцию — сумма за уровень при постоянном C равна {@link DieYield#layerDefects(double)}.
 */
public final class WaferProcess {

    public enum Operation {
        OXIDIZE, METALLIZE, EXPOSE, ETCH_OXIDE, ETCH_METAL, DONE
    }

    public static final int OPERATIONS_PER_LEVEL = 3;

    private WaferProcess() {
    }

    /** Какая операция нужна пластине на шаге {@code step} при {@code masks} уровнях шаблона. */
    public static Operation next(int step, int masks) {
        if (masks <= 0 || step >= OPERATIONS_PER_LEVEL * masks) {
            return masks <= 0 && step == 0 ? Operation.OXIDIZE : Operation.DONE;
        }
        int level = step / OPERATIONS_PER_LEVEL;
        boolean metal = level == masks - 1;
        return switch (step % OPERATIONS_PER_LEVEL) {
            case 0 -> metal ? Operation.METALLIZE : Operation.OXIDIZE;
            case 1 -> Operation.EXPOSE;
            default -> metal ? Operation.ETCH_METAL : Operation.ETCH_OXIDE;
        };
    }

    public static int totalSteps(int masks) {
        return OPERATIONS_PER_LEVEL * masks;
    }

    /** Дефекты одной операции при средней концентрации частиц за неё. */
    public static double operationDefects(double meanConcentration) {
        return DieYield.layerDefects(meanConcentration) / OPERATIONS_PER_LEVEL;
    }
}
