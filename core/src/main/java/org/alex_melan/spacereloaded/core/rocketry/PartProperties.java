package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Физические свойства детали ракеты. В моде заполняются из датапак-реестра
 * {@code spacereloaded:part_properties} (FR-013); в ядре — просто данные.
 *
 * <p>Единицы СИ: масса — кг, тяга — Н, удельный импульс — секунды,
 * ёмкость бака — кг топлива, момент гиродина — Н·м.
 *
 * @param massKg                 сухая масса детали
 * @param role                   роль в структуре
 * @param thrustN                тяга двигателя (0 для не-двигателей)
 * @param ispSec                 удельный импульс (0 для не-двигателей)
 * @param fuelType               тип топлива: у ENGINE — что потребляет, у TANK — что хранит
 * @param propellantCapacityKg   ёмкость бака по массе топлива (0 для не-баков)
 * @param gyroTorqueNm           компенсирующий момент гиродина (0 для не-гиродинов)
 */
public record PartProperties(
        double massKg,
        PartRole role,
        double thrustN,
        double ispSec,
        String fuelType,
        double propellantCapacityKg,
        double gyroTorqueNm
) {
    public static final String NO_FUEL = "";

    public PartProperties {
        if (massKg < 0) {
            throw new IllegalArgumentException("massKg < 0");
        }
        if (thrustN < 0 || ispSec < 0 || propellantCapacityKg < 0 || gyroTorqueNm < 0) {
            throw new IllegalArgumentException("negative physical property");
        }
        if (role == PartRole.ENGINE && (thrustN <= 0 || ispSec <= 0 || fuelType.isEmpty())) {
            throw new IllegalArgumentException("engine requires thrust, isp and fuel type");
        }
        if (role == PartRole.TANK && (propellantCapacityKg <= 0 || fuelType.isEmpty())) {
            throw new IllegalArgumentException("tank requires capacity and fuel type");
        }
    }

    public static PartProperties hull(double massKg) {
        return new PartProperties(massKg, PartRole.HULL, 0, 0, NO_FUEL, 0, 0);
    }

    public static PartProperties command(double massKg) {
        return new PartProperties(massKg, PartRole.COMMAND, 0, 0, NO_FUEL, 0, 0);
    }

    public static PartProperties seat(double massKg) {
        return new PartProperties(massKg, PartRole.SEAT, 0, 0, NO_FUEL, 0, 0);
    }

    public static PartProperties engine(double massKg, double thrustN, double ispSec, String fuelType) {
        return new PartProperties(massKg, PartRole.ENGINE, thrustN, ispSec, fuelType, 0, 0);
    }

    public static PartProperties tank(double massKg, double capacityKg, String fuelType) {
        return new PartProperties(massKg, PartRole.TANK, 0, 0, fuelType, capacityKg, 0);
    }

    public static PartProperties gyro(double massKg, double torqueNm) {
        return new PartProperties(massKg, PartRole.GYRO, 0, 0, NO_FUEL, 0, torqueNm);
    }
}
