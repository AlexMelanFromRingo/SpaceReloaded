package org.alex_melan.spacereloaded.client.render.anim;

/**
 * Плавный привод подвижной части мультиблока (008, D81): критически демпфированная пружина к цели
 * с ограничением скорости привода, шаг — по реальному времени кадра. Поэтому стержень реактора,
 * свод печи или тарелка антенны идут к новой цели без рывков при любом тикрейте и частоте кадров,
 * а смена цели на полпути гладко переходит в новое движение. Состояние живёт в клиентском экземпляре
 * блок-сущности; физика на сервере от анимации не зависит.
 */
public final class SmoothDrive {

    private double position;
    private double velocity;
    private long lastNanos;
    private boolean started;

    /**
     * @param target цель (единицы детали: доля хода, градусы…)
     * @param maxSpeed предел скорости привода, единиц/с
     * @param omega собственная частота пружины, 1/с (≈ 4 — мягко, 10 — резко)
     */
    public double update(double target, double maxSpeed, double omega) {
        long now = System.nanoTime();
        if (!started) {
            started = true;
            position = target;
            lastNanos = now;
            return position;
        }
        double dt = Math.min(0.1, (now - lastNanos) * 1e-9);
        lastNanos = now;
        // полу-неявный шаг: устойчив при больших ω·dt
        double accel = omega * omega * (target - position) - 2 * omega * velocity;
        velocity += accel * dt;
        velocity = Math.max(-maxSpeed, Math.min(maxSpeed, velocity));
        position += velocity * dt;
        if (Math.abs(target - position) < 1e-4 && Math.abs(velocity) < 1e-3) {
            position = target;
            velocity = 0;
        }
        return position;
    }

    /** Мгновенно поставить в положение (например, SCRAM рисуется своим законом). */
    public void snap(double value) {
        position = value;
        velocity = 0;
        started = true;
        lastNanos = System.nanoTime();
    }

    public double position() {
        return position;
    }

    /** Фаза вращения по угловой скорости (для роторов), рад: накапливается по времени кадра. */
    public static final class Spinner {
        private double angle;
        private double omega;
        private long lastNanos;

        /** Роторы разгоняются и тормозят плавно (время разгона tau, с). */
        public double update(double targetOmega, double tau) {
            long now = System.nanoTime();
            double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) * 1e-9);
            lastNanos = now;
            omega += (targetOmega - omega) * (1 - Math.exp(-dt / Math.max(1e-3, tau)));
            angle = (angle + omega * dt) % (2 * Math.PI);
            return angle;
        }

        public double omega() {
            return omega;
        }
    }
}
