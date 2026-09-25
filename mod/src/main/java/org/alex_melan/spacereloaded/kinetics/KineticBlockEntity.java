package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

import java.util.Locale;

/**
 * Узел механической сети (005, D53). Сервер хранит точную ω узла (обновляется решателем) и
 * визуальную фазу для клиента: угол = угол₀ + ω_вид·(t − t₀), где ω_вид ограничена визуальным
 * пределом (выше — стробоскоп; физика не меняется). Клиенту уходят (ω_вид, угол₀, t₀) только
 * при заметном изменении скорости — рендер чистая функция времени, без тиков на клиенте.
 */
public class KineticBlockEntity extends BlockEntity implements org.alex_melan.spacereloaded.multiblock.StatusProvider {

    /** Точная скорость узла, рад/с (сервер). */
    protected double omega;
    /** Последний момент через узел и мощность источника (для отчёта/Jade). */
    protected double transmittedTorque;
    protected double sourcePower;
    /** Состояние сети для отчёта. */
    protected String networkState = "idle";
    /** Визуальная фаза (синхронизируется). */
    private double visualOmega;
    /** Множитель визуальной скорости сети (≤ 1). */
    private double visualScale = 1;
    private double angle0;
    private long tick0;
    private boolean registered;

    public KineticBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.KINETIC, pos, state);
    }

    protected KineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public KineticBlock kineticBlock() {
        return (KineticBlock) getBlockState().getBlock();
    }

    /** Механика узла для решателя (сервер): по умолчанию — только трение и инерция ротора. */
    public NodeLoad load(ServerLevel level) {
        return new NodeLoad(0, 0, SpaceReloaded.config().kineticFrictionTorqueNm, kineticBlock().inertia());
    }

    /** Серверный тик: регистрация в сети при загрузке; машины расширяют. */
    public void serverTick(ServerLevel level) {
        if (!registered) {
            registered = true;
            KineticNetworks.request(level, getBlockPos());
        }
    }

    // --------- решатель → узел ---------

    /** Обновление точного состояния шагом решателя (без синхронизации). */
    public void applyStep(double newOmega, double transmitted, double power, String state) {
        this.omega = newOmega;
        this.transmittedTorque = transmitted;
        this.sourcePower = power;
        this.networkState = state;
    }

    /** Синхронизация визуальной фазы клиенту (непрерывно по углу) с последним масштабом сети. */
    public void syncMotion(long now) {
        syncMotion(now, visualScale);
    }

    /**
     * Синхронизация с масштабом визуальной скорости сети: предел {@code kineticVisualMaxOmega} сеть
     * применяет ОДНИМ множителем ко всем узлам, иначе ограничение ломало бы передаточные отношения и
     * зубья на экране проскальзывали бы.
     */
    public void syncMotion(long now, double scale) {
        angle0 = visualAngle(now);
        tick0 = now;
        visualScale = scale;
        visualOmega = omega * scale;
        sendMotion();
    }

    /**
     * Согласование фазы при сборке сети: угол узла = rᵢ·Θ (Θ — фаза корня), чтобы зубья шестерён,
     * поставленных в разное время, входили во впадины соседей (сдвиг зацепления добавляет рендер).
     */
    public void alignPhase(long now, double angle, double scale) {
        angle0 = angle;
        tick0 = now;
        visualScale = scale;
        visualOmega = omega * scale;
        sendMotion();
    }

    private void sendMotion() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public double visualAngle(double time) {
        return angle0 + visualOmega * (time - tick0) / 20.0;
    }

    public double omega() {
        return omega;
    }

    public double transmittedTorque() {
        return transmittedTorque;
    }

    public double sourcePower() {
        return sourcePower;
    }

    public String networkState() {
        return networkState;
    }

    public static double toRpm(double omega) {
        return omega * 60 / (2 * Math.PI);
    }

    /** ПКМ пустой рукой по узлу: экран (010) — об/мин, момент, мощность, состояние сети. */
    public static InteractionResult report(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity node && player instanceof ServerPlayer sp) {
            org.alex_melan.spacereloaded.network.ModNetworking.openStatus(sp, node.status((ServerLevel) level));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public org.alex_melan.spacereloaded.network.MachineStatusPayload status(ServerLevel level) {
        double power = Math.abs(transmittedTorque * omega);
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("message.spacereloaded.kinetic.report",
                String.format(Locale.ROOT, "%.0f", toRpm(omega)),
                String.format(Locale.ROOT, "%.0f", transmittedTorque),
                String.format(Locale.ROOT, "%.1f", power / 1000.0),
                Component.translatable("message.spacereloaded.kinetic.state." + networkState)));
        extraReport(lines);
        double maxOmega = SpaceReloaded.config().flywheelMaxOmega;
        return new org.alex_melan.spacereloaded.network.MachineStatusPayload(getBlockPos(),
                getBlockState().getBlock().getName(), lines,
                java.util.List.of(new org.alex_melan.spacereloaded.network.MachineStatusPayload.Gauge(
                        Component.translatable("gauge.spacereloaded.kinetic.omega"),
                        (float) Math.min(1, Math.abs(omega) / maxOmega), 0x6FD5E8)),
                java.util.List.of());
    }

    /** Доп. строки отчёта у машин. */
    protected void extraReport(java.util.List<Component> lines) {
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            KineticNetworks.request(serverLevel, getBlockPos());
        }
        super.setRemoved();
    }

    // --------- синхронизация ---------

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("omega", omega);
        output.putDouble("vis_omega", visualOmega);
        output.putDouble("angle0", angle0);
        output.putLong("tick0", tick0);
        output.putString("net_state", networkState);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        omega = input.getDoubleOr("omega", 0);
        visualOmega = input.getDoubleOr("vis_omega", 0);
        angle0 = input.getDoubleOr("angle0", 0);
        tick0 = input.getLongOr("tick0", 0);
        networkState = input.getStringOr("net_state", "idle");
        registered = false;
    }
}
