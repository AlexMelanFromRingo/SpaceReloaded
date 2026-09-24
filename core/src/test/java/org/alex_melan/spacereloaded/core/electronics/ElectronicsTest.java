package org.alex_melan.spacereloaded.core.electronics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Чистота, выход годных, чистая комната, рост кристалла (006, SC-002…SC-004). */
class ElectronicsTest {

    @Test
    void columnPasses() {
        double once = Purity.afterColumn(Purity.METALLURGICAL, 16);
        assertEquals(8.4, once, 1e-9);
        assertTrue(Purity.afterColumn(once, 16) >= Purity.ELECTRONIC_GRADE);
        assertEquals(Purity.CEILING, Purity.afterColumn(10.5, 16), 1e-9);
        assertEquals(6.0, Purity.afterColumn(Purity.METALLURGICAL, 10), 1e-9);
    }

    @Test
    void diesPerWaferTable() {
        assertEquals(56, DieYield.diesPerWafer(5, 0.25));
        assertEquals(23, DieYield.diesPerWafer(5, 0.5));
        assertEquals(8, DieYield.diesPerWafer(5, 1.0));
    }

    @Test
    void yieldMatchesPoissonEmpirically() {
        double y = DieYield.yield(0.3, 0.5);
        long good = 0;
        int dies = 0;
        for (int i = 0; i < 10_000; i++) {
            good += DieYield.sampleGood(7919L * i + 3, 23, y);
            dies += 23;
        }
        assertEquals(y, good / (double) dies, 0.02);
    }

    @Test
    void cleanPlantScenarios() {
        // Логика (A 0.25, 5 слоёв) в ISO 5 из 9N-кремния ≈ 93 %; процессор из 6N — ноль
        double iso5 = CleanroomAir.ISO5_LIMIT;
        double logic = 5 * DieYield.layerDefects(iso5) + DieYield.impurityDefects(9); // на пределе ISO 5
        assertEquals(0.93, DieYield.yield(logic, 0.25), 0.03);
        double cpuSolar = 8 * DieYield.layerDefects(iso5) + DieYield.impurityDefects(6);
        assertTrue(DieYield.yield(cpuSolar, 0.5) < 0.01);
    }

    @Test
    void cleanroomClasses() {
        // 5×5×3 = 75 м³, 4 модуля по 10 м³/мин
        CleanroomAir.State walking = new CleanroomAir.State(3.5e7, 0, 5e6, 40, 75);
        assertEquals(7, CleanroomAir.isoClass(walking.steadyState()));
        CleanroomAir.State standing = new CleanroomAir.State(3.5e7, 0, 1e5, 40, 75);
        assertEquals(5, CleanroomAir.isoClass(standing.steadyState()));
        CleanroomAir.State suit = new CleanroomAir.State(3.5e7, 0, 1e3, 40, 75);
        assertTrue(CleanroomAir.isoClass(suit.steadyState()) <= 4);
        assertEquals(75 / (40 * CleanroomAir.HEPA), standing.tau(), 1e-9);
        // От воздуха Земли (3.5·10⁷) до ISO 5 — около 11τ (≈ 21 мин)
        double t = 11 * standing.tau();
        assertTrue(CleanroomAir.isoClass(standing.at(t)) <= 5);
        assertEquals(9, CleanroomAir.isoClass(3.5e7));
    }

    @Test
    void meanIsExactIntegral() {
        CleanroomAir.State s = new CleanroomAir.State(1e6, 0, 1e5, 40, 75);
        int n = 100_000;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += s.at((i + 0.5) * 3.0 / n);
        }
        assertEquals(sum / n, s.mean(3.0), sum / n * 1e-6);
        CleanroomAir.State next = s.evolve(3.0, 5e6, 40, 75);
        assertEquals(s.at(3.0), next.c0(), 1e-6);
    }

    @Test
    void crystalGrowth() {
        assertTrue(CrystalGrowth.monocrystalline(0.03));
        assertFalse(CrystalGrowth.monocrystalline(0.08));
        assertEquals(0.46, CrystalGrowth.usableFraction(0.35, 1.5), 0.01);
    }

    @Test
    void blendConservesImpurityMass() {
        assertEquals(9.0, Purity.blend(9, 5, 9, 3), 1e-9);
        // 1 кг технического кремния в 9 кг электронного: доля примесей ≈ 0.1·10⁻² → ~3N
        double mix = Purity.blend(2, 1, 9, 9);
        assertEquals(3.0, mix, 0.01);
        assertEquals(Purity.impurityFraction(mix) * 10,
                Purity.impurityFraction(2) + 9 * Purity.impurityFraction(9), 1e-12);
    }

    @Test
    void integralAcrossEventsIsExact() {
        CleanroomAir.State a = new CleanroomAir.State(1e6, 0, 1e5, 40, 75);
        CleanroomAir.State b = a.evolve(2.0, 5e6, 40, 75); // игрок пошёл
        double exact = a.integral(2.0) + b.integral(5.0);
        int n = 200_000;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double t = (i + 0.5) * 5.0 / n;
            sum += t < 2.0 ? a.at(t) : b.at(t);
        }
        assertEquals(sum * 5.0 / n, exact, exact * 1e-5);
        // без фильтров рост линеен
        CleanroomAir.State dirty = new CleanroomAir.State(100, 0, 750, 0, 75);
        assertEquals((100 + 100 + 750 * 4 / 75.0) / 2 * 4, dirty.integral(4), 1e-9);
    }

    @Test
    void scheilSplitConservesDopant() {
        double g = CrystalGrowth.usableFraction(CrystalGrowth.K_PHOSPHORUS, CrystalGrowth.RESISTIVITY_TOLERANCE);
        double[] split = CrystalGrowth.segregate(CrystalGrowth.K_BORON, g);
        assertEquals(1.0, g * split[0] + (1 - g) * split[1], 1e-12);
        assertTrue(split[0] < 1 && split[1] > 1); // слиток чище расплава, хвост грязнее
        // алюминий (k = 0.002) — плохая легирующая примесь для Cz: годна лишь треть слитка
        assertEquals(0.334, CrystalGrowth.usableFraction(CrystalGrowth.K_ALUMINIUM, 1.5), 0.001);
    }

    @Test
    void waferRoute() {
        assertEquals(WaferProcess.Operation.OXIDIZE, WaferProcess.next(0, 5));
        assertEquals(WaferProcess.Operation.EXPOSE, WaferProcess.next(1, 5));
        assertEquals(WaferProcess.Operation.ETCH_OXIDE, WaferProcess.next(2, 5));
        assertEquals(WaferProcess.Operation.METALLIZE, WaferProcess.next(12, 5));
        assertEquals(WaferProcess.Operation.ETCH_METAL, WaferProcess.next(14, 5));
        assertEquals(WaferProcess.Operation.DONE, WaferProcess.next(15, 5));
        double level = 0;
        for (int i = 0; i < WaferProcess.OPERATIONS_PER_LEVEL; i++) {
            level += WaferProcess.operationDefects(CleanroomAir.ISO5_LIMIT);
        }
        assertEquals(DieYield.layerDefects(CleanroomAir.ISO5_LIMIT), level, 1e-12);
    }

    @Test
    void openHatchInfiltration() {
        // ISO 5-комната (стоит один человек), люк 1 м² открыт на 30 с при воздухе Земли снаружи
        CleanroomAir.State closed = new CleanroomAir.State(2500, 0, 1e5, 40, 75);
        CleanroomAir.State open = closed.evolve(0, 1e5, 40, 75, 30, 3.5e7);
        double spike = open.at(0.5);
        assertTrue(CleanroomAir.isoClass(spike) >= 8); // комната «выбита» в ISO 8+
        CleanroomAir.State shut = open.evolve(0.5, 1e5, 40, 75, 0, 0);
        assertEquals(closed.steadyState(), shut.steadyState(), 1e-9);
        // возврат: ~ln(spike/Css)·τ
        double back = Math.log(spike / closed.steadyState()) * shut.tau();
        assertTrue(CleanroomAir.isoClass(shut.at(0.5 + back * 1.3)) <= 5);
        // при q > 0 стационар ограничен смесью: (G + q·C_out)/(Qη + q)
        assertEquals((1e5 + 30 * 3.5e7) / (40 * CleanroomAir.HEPA + 30), open.steadyState(), 1e-6);
    }
}
