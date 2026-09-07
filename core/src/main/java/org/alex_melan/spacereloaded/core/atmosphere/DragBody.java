package org.alex_melan.spacereloaded.core.atmosphere;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Аэродинамическое тело (Полёт 2.0, FR-081/FR-082): квадратичное сопротивление
 * F = −½·ρ·|v|·C_d·A_eff·v̂, общее для ракет, снарядов орудия и метеоритов.
 *
 * <p>Площадь сечения зависит от направления движения: A_eff = A_x·|v̂_x| + A_y·|v̂_y| + A_z·|v̂_z|
 * — проекция коробки габаритов на направление скорости. Упрощение (документировано):
 * C_d постоянный (блочная ракета — тупое тело, 0.5 между конусом 0.3 и кубом 1.05;
 * обтекаемый лом — 0.1), форма и угол атаки не учитываются.
 *
 * @param cd    коэффициент сопротивления (≥ 0; 0 — тело не тормозится)
 * @param areaX площадь проекции на плоскость YZ (движение вдоль X), м²
 * @param areaY площадь проекции на плоскость XZ (движение вдоль Y), м²
 * @param areaZ площадь проекции на плоскость XY (движение вдоль Z), м²
 */
public record DragBody(double cd, double areaX, double areaY, double areaZ) {

    /** Тело без сопротивления (вакуумный расчёт / тесты 001). */
    public static final DragBody NONE = new DragBody(0, 0, 0, 0);

    public DragBody {
        if (cd < 0 || areaX < 0 || areaY < 0 || areaZ < 0) {
            throw new IllegalArgumentException("negative drag parameter");
        }
    }

    /** Сфероподобное тело одной площадью во всех направлениях (снаряд, метеорит). */
    public static DragBody uniform(double cd, double areaM2) {
        return new DragBody(cd, areaM2, areaM2, areaM2);
    }

    /** Эффективная площадь для направления скорости, м². */
    public double effectiveArea(Vec3d velocity) {
        double speed = velocity.length();
        if (speed < 1e-9) {
            return 0;
        }
        return (areaX * Math.abs(velocity.x()) + areaY * Math.abs(velocity.y())
                + areaZ * Math.abs(velocity.z())) / speed;
    }

    /** Сила сопротивления, Н (против скорости); ноль в вакууме и при C_d = 0. */
    public Vec3d force(Vec3d velocity, double density) {
        double speed = velocity.length();
        if (speed < 1e-9 || density <= 0 || cd <= 0) {
            return Vec3d.ZERO;
        }
        double magnitude = 0.5 * density * speed * speed * cd * effectiveArea(velocity);
        return velocity.scale(-magnitude / speed);
    }
}
