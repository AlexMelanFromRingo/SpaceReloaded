package org.alex_melan.spacereloaded.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.core.station.RotatingAssembly;
import org.alex_melan.spacereloaded.core.station.SpinGravity;
import org.alex_melan.spacereloaded.registry.ItemMasses;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ступица вращающегося кольца (007, FR-517…FR-520, D77). Сборка — связные (6-связность) блоки от
 * ступицы, кроме оси (мотор противовращения, стальной вал подшипника по оси); она обязана быть
 * изолирована от мира — иначе вращать нечего (предел {@link #MAX_BLOCKS}). По массам таблицы
 * {@code item_mass} — момент инерции I = Σm·ρ² и дисбаланс |Σm·ρ⃗|. Своя динамика с сохранением
 * момента импульса: I·dω/dt = τ_двигателей + τ_мотора − τ_трения. Мир рисуется во вращающейся
 * системе экипажа: «вниз» — от оси, то есть ниже ступицы по −Y; обитаем обод под ступицей.
 */
public class SpinHubBlockEntity extends BlockEntity {

    public static final int MAX_BLOCKS = 4096;
    /** Допустимая радиальная нагрузка подшипника ступицы, Н (**оценка**: крупный опорно-поворотный подшипник). */
    public static final double BEARING_LIMIT_N = 5e5;
    /** Трение подшипника: выбег за ~10 реальных суток. */
    public static final double FRICTION_TAU_S = 10 * 86400;

    private double omega;
    private boolean dirty = true;
    private boolean isolated;
    private final Set<Long> blocks = new HashSet<>();
    private final List<BlockPos> thrusters = new ArrayList<>();
    private final List<BlockPos> tanks = new ArrayList<>();
    private RotatingAssembly assembly;
    private AABB bounds;
    private double torqueBudget;

    public SpinHubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIN_HUB, pos, state);
    }

    public Direction.Axis axis() {
        return getBlockState().getValue(SpinHubBlock.AXIS);
    }

    public double omega() {
        return omega;
    }

    public boolean isolated() {
        return isolated;
    }

    public RotatingAssembly assembly() {
        return assembly;
    }

    public AABB bounds() {
        return bounds;
    }

    public boolean contains(BlockPos pos) {
        return blocks.contains(pos.asLong());
    }

    public void markDirty() {
        dirty = true;
    }

    /** Добавить момент на этот тик (мотор противовращения), Н·м·с — импульс момента. */
    public void applyImpulse(double angularImpulse) {
        torqueBudget += angularImpulse;
    }

    /** Ось как направление: +X или +Z. */
    private double[] axisUnit() {
        return axis() == Direction.Axis.X ? new double[] {1, 0, 0} : new double[] {0, 0, 1};
    }

    private void rescan(ServerLevel level) {
        dirty = false;
        blocks.clear();
        thrusters.clear();
        tanks.clear();
        Direction.Axis axis = axis();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        blocks.add(worldPosition.asLong());
        for (Direction d : Direction.values()) {
            if (d.getAxis() != axis) {
                queue.add(worldPosition.relative(d));
            }
        }
        List<RotatingAssembly.Mass> masses = new ArrayList<>();
        masses.add(new RotatingAssembly.Mass(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, mass(level, worldPosition)));
        isolated = true;
        int minX = worldPosition.getX(), minY = worldPosition.getY(), minZ = worldPosition.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (blocks.contains(p.asLong())) {
                continue;
            }
            BlockState state = level.getBlockState(p);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.is(ModBlocks.DESPIN_MOTOR)
                    || (onAxis(p) && state.is(ModBlocks.STEEL_SHAFT))) {
                continue;
            }
            if (blocks.size() >= MAX_BLOCKS) {
                isolated = false; // сборка связана с миром — вращать нечего
                break;
            }
            blocks.add(p.asLong());
            masses.add(new RotatingAssembly.Mass(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, mass(level, p)));
            if (state.is(ModBlocks.RIM_THRUSTER)) {
                thrusters.add(p.immutable());
            }
            if (level.getBlockEntity(p) instanceof org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity) {
                tanks.add(p.immutable());
            }
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
            for (Direction d : Direction.values()) {
                queue.add(p.relative(d));
            }
        }
        bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        RotatingAssembly.Axis a = axis == Direction.Axis.X ? RotatingAssembly.Axis.X : RotatingAssembly.Axis.Z;
        assembly = new RotatingAssembly(masses, a, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5);
        if (!isolated) {
            omega = 0;
        }
        setChanged();
    }

    private boolean onAxis(BlockPos p) {
        return axis() == Direction.Axis.X ? p.getY() == worldPosition.getY() && p.getZ() == worldPosition.getZ()
                : p.getY() == worldPosition.getY() && p.getX() == worldPosition.getX();
    }

    private static double mass(ServerLevel level, BlockPos p) {
        return ItemMasses.massOf(level.registryAccess(), new ItemStack(level.getBlockState(p).getBlock().asItem()));
    }

    public static void serverTick(ServerLevel level, BlockPos pos, SpinHubBlockEntity hub) {
        SpinRings.register(level, pos);
        if (level.getGameTime() % 20 == 0) {
            hub.second(level);
        }
    }

    private void second(ServerLevel level) {
        if (dirty) {
            rescan(level);
        }
        if (!isolated || assembly == null || assembly.inertia() <= 0) {
            torqueBudget = 0;
            return;
        }
        double dt = 1.0;
        double impulse = torqueBudget;
        torqueBudget = 0;
        impulse += thrusterImpulse(level, dt);
        impulse -= omega * assembly.inertia() / FRICTION_TAU_S * dt;
        omega += impulse / assembly.inertia();
        if (assembly.bearingForce(omega) > BEARING_LIMIT_N) {
            // дисбаланс сорвал подшипник: ступица разрушена, сборка встаёт
            level.destroyBlock(worldPosition, true);
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    2.0f, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            return;
        }
        setChanged();
    }

    /** Двигатели обода: импульс момента (r × F)·ось за dt, топливо из баков сборки. */
    private double thrusterImpulse(ServerLevel level, double dt) {
        double total = 0;
        double[] u = axisUnit();
        for (BlockPos p : thrusters) {
            BlockState state = level.getBlockState(p);
            if (!state.is(ModBlocks.RIM_THRUSTER) || !level.hasNeighborSignal(p)) {
                continue;
            }
            double needKg = RimThrusterBlock.THRUST_N / (RimThrusterBlock.ISP_S * SpinGravity.G0) * dt;
            if (!drain(level, needKg)) {
                continue;
            }
            Direction exhaust = state.getValue(RimThrusterBlock.FACING);
            double[] f = {-exhaust.getStepX() * RimThrusterBlock.THRUST_N, -exhaust.getStepY() * RimThrusterBlock.THRUST_N,
                    -exhaust.getStepZ() * RimThrusterBlock.THRUST_N};
            double[] r = {p.getX() - worldPosition.getX(), p.getY() - worldPosition.getY(), p.getZ() - worldPosition.getZ()};
            double[] torque = {r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0]};
            total += (torque[0] * u[0] + torque[1] * u[1] + torque[2] * u[2]) * dt;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX() + 0.5 + exhaust.getStepX() * 0.7,
                    p.getY() + 0.5 + exhaust.getStepY() * 0.7, p.getZ() + 0.5 + exhaust.getStepZ() * 0.7, 3, 0.05, 0.05, 0.05, 0.05);
        }
        return total;
    }

    private boolean drain(ServerLevel level, double kg) {
        for (BlockPos p : tanks) {
            if (level.getBlockEntity(p) instanceof org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity tank
                    && tank.propellantKg() >= kg) {
                tank.drain(kg);
                return true;
            }
        }
        return false;
    }

    /** Гравитация в точке кольца (ниже ступицы), м/с². */
    public double gravityAt(double y) {
        double r = worldPosition.getY() + 0.5 - y;
        return r > 0 ? SpinGravity.gravity(Math.abs(omega), r) : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("omega", omega);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        omega = input.getDoubleOr("omega", 0);
        dirty = true;
    }
}
