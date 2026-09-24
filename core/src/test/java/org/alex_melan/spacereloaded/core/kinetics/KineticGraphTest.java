package org.alex_melan.spacereloaded.core.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KineticGraphTest {

    @Test
    void shaftChainAndGearRatios() {
        KineticGraph g = new KineticGraph();
        int motor = g.addNode();
        int shaft = g.addNode();
        int small = g.addNode();
        int large = g.addNode();
        g.addEdge(motor, shaft, 1, 1);
        g.addEdge(shaft, small, 1, 1);
        g.addEdge(small, large, -0.5, 0.98);
        KineticGraph.Analysis a = g.analyze(512);
        assertEquals(KineticGraph.State.OK, a.state());
        assertEquals(1.0, a.ratio()[shaft], 1e-12);
        assertEquals(-0.5, a.ratio()[large], 1e-12);
        assertEquals(0.98, a.eff()[large], 1e-12);
    }

    @Test
    void triangleOfSmallGearsJams() {
        // Три малые шестерни, попарно сцепленные: 0 и 1 вращаются в разные стороны, 1 и 2 тоже,
        // значит 0 и 2 — в одну, но они сцеплены между собой — противоречие
        KineticGraph g = new KineticGraph();
        int a = g.addNode();
        int b = g.addNode();
        int c = g.addNode();
        g.addEdge(a, b, -1, 0.98);
        g.addEdge(b, c, -1, 0.98);
        g.addEdge(a, c, -1, 0.98);
        assertEquals(KineticGraph.State.JAMMED, g.analyze(512).state());
    }

    @Test
    void consistentLoopDoesNotJam() {
        // Четыре малые шестерни квадратом: знаки чередуются — цикл согласован
        KineticGraph g = new KineticGraph();
        int[] n = {g.addNode(), g.addNode(), g.addNode(), g.addNode()};
        for (int i = 0; i < 4; i++) {
            g.addEdge(n[i], n[(i + 1) % 4], -1, 0.98);
        }
        assertEquals(KineticGraph.State.OK, g.analyze(512).state());
    }

    @Test
    void sizeLimit() {
        KineticGraph g = new KineticGraph();
        for (int i = 0; i < 10; i++) {
            g.addNode();
        }
        assertEquals(KineticGraph.State.TOO_LARGE, g.analyze(8).state());
        assertEquals(KineticGraph.State.EMPTY, new KineticGraph().analyze(8).state());
    }
}
