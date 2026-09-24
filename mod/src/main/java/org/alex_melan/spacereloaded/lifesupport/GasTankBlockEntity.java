package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

/**
 * Газовый бак (007, D71): кислород или азот, кг. Род газа задаёт первая заправка; пустой бак
 * принимает любой. Содержимое переживает разборку (компоненты предмета) — газ можно везти.
 */
public class GasTankBlockEntity extends BlockEntity {

    private GasKind kind = GasKind.NONE;
    private double mass;

    public GasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_TANK, pos, state);
    }

    public GasKind kind() {
        return kind;
    }

    public double mass() {
        return mass;
    }

    /** Принять газ; возвращает принятую массу. */
    public double insert(GasKind gas, double kg) {
        if (kg <= 0 || gas == GasKind.NONE || (kind != GasKind.NONE && kind != gas)) {
            return 0;
        }
        double accepted = Math.min(kg, gas.capacityKg() - mass);
        if (accepted <= 0) {
            return 0;
        }
        kind = gas;
        mass += accepted;
        changed();
        return accepted;
    }

    /** Выдать газ; возвращает выданную массу. */
    public double extract(GasKind gas, double kg) {
        if (kind != gas || kg <= 0) {
            return 0;
        }
        double given = Math.min(kg, mass);
        mass -= given;
        if (mass <= 1e-9) {
            mass = 0;
            kind = GasKind.NONE;
        }
        changed();
        return given;
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            int fill = kind == GasKind.NONE ? 0 : (int) Math.ceil(4 * mass / kind.capacityKg());
            BlockState next = state.setValue(GasTankBlock.GAS, kind).setValue(GasTankBlock.FILL, Math.min(4, fill));
            if (next != state) {
                serverLevel.setBlock(getBlockPos(), next, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Положить газ в соседние баки (источники: электролизёр, реактор, разделитель). */
    public static double pushToNeighbors(ServerLevel level, BlockPos pos, GasKind gas, double kg) {
        double left = kg;
        for (Direction d : Direction.values()) {
            if (left <= 0) {
                break;
            }
            if (level.getBlockEntity(pos.relative(d)) instanceof GasTankBlockEntity tank) {
                left -= tank.insert(gas, left);
            }
        }
        return kg - left;
    }

    /** Взять газ из соседних баков. */
    public static double pullFromNeighbors(ServerLevel level, BlockPos pos, GasKind gas, double kg) {
        double left = kg;
        for (Direction d : Direction.values()) {
            if (left <= 0) {
                break;
            }
            if (level.getBlockEntity(pos.relative(d)) instanceof GasTankBlockEntity tank) {
                left -= tank.extract(gas, left);
            }
        }
        return kg - left;
    }

    /** Запас газа в соседних баках, кг. */
    public static double availableAround(ServerLevel level, BlockPos pos, GasKind gas) {
        double total = 0;
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(pos.relative(d)) instanceof GasTankBlockEntity tank && tank.kind == gas) {
                total += tank.mass;
            }
        }
        return total;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (kind != GasKind.NONE && mass > 0) {
            components.set(ModDataComponents.GAS_KIND, kind.ordinal());
            components.set(ModDataComponents.GAS_KG, (float) mass);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer k = components.get(ModDataComponents.GAS_KIND);
        Float m = components.get(ModDataComponents.GAS_KG);
        if (k != null && m != null && k > 0 && k < GasKind.values().length) {
            kind = GasKind.values()[k];
            mass = m;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("gas", kind.getSerializedName());
        output.putDouble("mass", mass);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            kind = GasKind.valueOf(input.getStringOr("gas", "none").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            kind = GasKind.NONE;
        }
        mass = input.getDoubleOr("mass", 0);
    }
}
