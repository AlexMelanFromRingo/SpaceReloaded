package org.alex_melan.spacereloaded.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Небо во вращающемся кольце (007, D77): в системе кольца небесная сфера вращается с −ω вокруг оси
 * ступицы. Угол копится по реальному времени кадра, ω приходит с сервера при входе и при изменении.
 */
public final class SpinSky {

    private static int axis;
    private static double omega;
    private static double angle;
    private static long lastNanos;
    private static ClientLevel level;

    private SpinSky() {
    }

    public static void set(int newAxis, double newOmega) {
        if (newAxis != axis) {
            angle = 0;
        }
        axis = newAxis;
        omega = newOmega;
        level = Minecraft.getInstance().level;
    }

    /** 0 — вне кольца, 1 — ось X, 2 — ось Z. */
    public static int axis() {
        if (level != Minecraft.getInstance().level) {
            axis = 0; // сменился мир: пакета выхода могло не быть
        }
        return axis;
    }

    /** Угол поворота неба, радианы (−ω·t). */
    public static float angle() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.25, (now - lastNanos) * 1e-9);
        lastNanos = now;
        angle = (angle - omega * dt) % (2 * Math.PI);
        return (float) angle;
    }
}
