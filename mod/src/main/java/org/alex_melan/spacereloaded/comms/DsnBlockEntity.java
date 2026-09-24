package org.alex_melan.spacereloaded.comms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.comms.LinkBudget;
import org.alex_melan.spacereloaded.core.comms.SkyGeometry;
import org.alex_melan.spacereloaded.industry.IndustryStructures;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.FormableBlock;
import org.alex_melan.spacereloaded.multiblock.FormedStructure;
import org.alex_melan.spacereloaded.multiblock.HammerTarget;
import org.alex_melan.spacereloaded.multiblock.StatusProvider;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;
import org.alex_melan.spacereloaded.network.SpaceNetworkState;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Антенна дальней связи (008, US6, D87; прототип — DSN 34 м, X-диапазон). Шаблон — приёмник (ключ) и
 * опора; тарелка — связные панели в плоскости над опорой (заливка, ≤ 1000 — Ø 36 м): диаметр по площади
 * D = 2·√(A/π) м. Скорость линии — {@link LinkBudget} до выбранного тела; положение тела на небе и
 * дальность — {@link SkyGeometry} (круговые орбиты, долготы по времени со сжатием ×130 — синодический
 * период Марса выходит 144 000 тиков, как окна перелётов 003). Цель за горизонтом — связи нет. Линия
 * ≥ 1 кбит/с засчитывается как покрытие для беспилотных рейсов к телу и для снимков с его орбиты.
 */
public class DsnBlockEntity extends BlockEntity implements HammerTarget, StatusProvider, IndustryStructures.StructureOwner {

    public static final double TELEMETRY_BPS = 1000;
    public static final int MAX_PANELS = 1000;
    public static final double MOON_DISTANCE_M = 3.844e8;
    private static final double YEAR_TICKS = 365.25 / 130 * 24000;

    /** Тело-цель: радиус орбиты (а. е.; Луна — в системе Земли), период, начальная долгота, передатчик борта. */
    public record Body(Identifier id, double au, double periodTicks, double longitude0, double txW, double txDishM) {
    }

    public static final List<Body> BODIES = List.of(
            new Body(Identifier.withDefaultNamespace("overworld"), 1.0, YEAR_TICKS, 0, 100, 3),
            new Body(Identifier.fromNamespaceAndPath("spacereloaded", "moon"), 1.0, YEAR_TICKS, 0, 20, 1),
            // противостояние Марса — в середине окна перелёта 003 (фаза 12000 тиков)
            new Body(Identifier.fromNamespaceAndPath("spacereloaded", "mars"), 1.524, 686.98 / 130 * 24000,
                    2 * Math.PI * 12000 * (1 / YEAR_TICKS - 1 / (686.98 / 130 * 24000)), 100, 3),
            new Body(Identifier.fromNamespaceAndPath("spacereloaded", "asteroid_belt"), 2.7,
                    Math.pow(2.7, 1.5) * YEAR_TICKS, 1.0, 100, 3));

    private final FormedStructure structure = new FormedStructure();
    private boolean claimed;
    private int target;
    private int[] panels = new int[0];
    private final Set<Long> formedPanels = new HashSet<>();
    private double rateBps;
    private double rangeM;
    public Object clientAnim;

    public DsnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DSN, pos, state);
    }

    private Direction face() {
        return getBlockState().getValue(ControllerBlock.FACING);
    }

    public boolean formed() {
        return getBlockState().getValue(ControllerBlock.FORMED);
    }

    /** Панели тарелки: пары (dx, dz) от центра. */
    public int[] panels() {
        return panels;
    }

    public double diameterM() {
        return 2 * Math.sqrt(panels.length / 2.0 / Math.PI);
    }

    public double rateBps() {
        return rateBps;
    }

    public Body targetBody() {
        return BODIES.get(Math.floorMod(target, BODIES.size()));
    }

    @Override
    public void markStructureDirty() {
        structure.markDirty();
    }

    @Override
    public boolean hammer(ServerLevel level, ServerPlayer player) {
        boolean ok = structure.hammer(level, getBlockPos(), face(), player);
        scanPanels(level);
        setFormed(level);
        return ok;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            structure.dismantle(serverLevel, pos, state.getValue(ControllerBlock.FACING), state.getBlock());
            unformPanels(serverLevel);
            SpaceNetworkState.get(serverLevel.getServer()).setGroundLink(GlobalPos.of(serverLevel.dimension(), pos),
                    targetBody().id(), 0);
        }
        super.preRemoveSideEffects(pos, state);
    }

    public static void serverTick(DsnBlockEntity be, ServerLevel level) {
        be.tick(level);
    }

    private void tick(ServerLevel level) {
        if (!claimed) {
            claimed = true;
            structure.reclaim(level, getBlockPos(), face());
        }
        if (structure.revalidate(level, getBlockPos(), face())) {
            setChanged();
        }
        if (level.getGameTime() % 40 != 0) {
            return;
        }
        setFormed(level);
        scanPanels(level);
        Body own = bodyOf(level.dimension().identifier());
        Body body = targetBody();
        boolean up = structure.formed() && panels.length > 0 && !body.id().equals(level.dimension().identifier())
                && SkyGeometry.aboveHorizon(skyAngle(level.getDefaultClockTime(), level.getGameTime(), own, body));
        rangeM = own == null ? 0 : distance(level.getGameTime(), own, body);
        rateBps = up && own != null ? LinkBudget.rate(body.txW(), body.txDishM(), diameterM(), rangeM) : 0;
        SpaceNetworkState.get(level.getServer()).setGroundLink(GlobalPos.of(level.dimension(), getBlockPos()), body.id(), rateBps);
        boolean active = rateBps > 0;
        if (getBlockState().getValue(ControllerBlock.ACTIVE) != active) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    public static Body bodyOf(Identifier dim) {
        for (Body b : BODIES) {
            if (b.id().equals(dim)) {
                return b;
            }
        }
        return null;
    }

    private static double[] helio(Body b, long gameTime) {
        double l = b.longitude0() + 2 * Math.PI * gameTime / b.periodTicks();
        return new double[] {b.au() * Math.cos(l), b.au() * Math.sin(l)};
    }

    private static boolean earthSystem(Body b) {
        return b.au() == 1.0;
    }

    /** Дальность между телами, м. */
    public static double distance(long gameTime, Body from, Body to) {
        if (earthSystem(from) && earthSystem(to)) {
            return MOON_DISTANCE_M;
        }
        double[] a = helio(from, gameTime);
        double[] b = helio(to, gameTime);
        return SkyGeometry.AU * Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    /** Угол цели на дуге неба наблюдателя (одинаково считается на сервере и клиенте). */
    public static double skyAngle(long clockTime, long gameTime, Body from, Body to) {
        if (from == null) {
            return Math.PI / 2;
        }
        double elong;
        if (earthSystem(from) && earthSystem(to)) {
            if (to.id().getPath().equals("moon")) {
                int phase = (int) Math.floorMod(clockTime / 24000L, 8L);  // 0 — полнолуние
                elong = Math.PI - phase * Math.PI / 4;
            } else {
                return Math.PI / 3;  // Земля в небе Луны неподвижна (приливный захват)
            }
        } else {
            double[] o = helio(from, gameTime);
            double[] t = helio(to, gameTime);
            double tx = t[0] - o[0], ty = t[1] - o[1];
            double sx = -o[0], sy = -o[1];
            elong = Math.atan2(sx * ty - sy * tx, sx * tx + sy * ty);
        }
        return SkyGeometry.skyAngle(clockTime, elong);
    }

    /** Панели тарелки: заливка в плоскости над опорой от центра. */
    private void scanPanels(ServerLevel level) {
        if (!structure.formed()) {
            unformPanels(level);
            panels = new int[0];
            return;
        }
        BlockPos center = getBlockPos().above(2);
        List<int[]> found = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(center);
        seen.add(center.asLong());
        while (!queue.isEmpty() && found.size() < MAX_PANELS) {
            BlockPos p = queue.poll();
            if (!level.isLoaded(p) || !level.getBlockState(p).is(ModBlocks.DISH_PANEL)) {
                continue;
            }
            found.add(new int[] {p.getX() - center.getX(), p.getZ() - center.getZ()});
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n.asLong())) {
                    queue.add(n);
                }
            }
        }
        int[] packed = new int[found.size() * 2];
        Set<Long> now = new HashSet<>();
        for (int i = 0; i < found.size(); i++) {
            packed[2 * i] = found.get(i)[0];
            packed[2 * i + 1] = found.get(i)[1];
            now.add(center.offset(found.get(i)[0], 0, found.get(i)[1]).asLong());
        }
        List<BlockPos> off = new ArrayList<>();
        for (long l : formedPanels) {
            if (!now.contains(l)) {
                off.add(BlockPos.of(l));
            }
        }
        FormableBlock.apply(level, off, false);
        List<BlockPos> on = new ArrayList<>();
        for (long l : now) {
            on.add(BlockPos.of(l));
        }
        FormableBlock.apply(level, on, true);
        formedPanels.clear();
        formedPanels.addAll(now);
        if (!java.util.Arrays.equals(packed, panels)) {
            panels = packed;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void unformPanels(ServerLevel level) {
        List<BlockPos> all = new ArrayList<>();
        for (long l : formedPanels) {
            all.add(BlockPos.of(l));
        }
        FormableBlock.apply(level, all, false);
        formedPanels.clear();
    }

    private void setFormed(ServerLevel level) {
        boolean formed = structure.formed();
        if (getBlockState().getValue(ControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void action(ServerLevel level, ServerPlayer player, String action, double value) {
        if ("target".equals(action)) {
            do {
                target = Math.floorMod(target + 1, BODIES.size());
            } while (targetBody().id().equals(level.dimension().identifier()));
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            setChanged();
        }
    }

    @Override
    public MachineStatusPayload status(ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        if (!structure.formed()) {
            lines.add(Component.translatable("status.spacereloaded.dsn.not_formed").withColor(0xE0B23C));
        }
        Body body = targetBody();
        Body own = bodyOf(level.dimension().identifier());
        lines.add(Component.translatable("status.spacereloaded.dsn.target",
                Component.translatable("planet.spacereloaded." + (body.id().getPath().equals("overworld") ? "earth" : body.id().getPath())),
                String.format(Locale.ROOT, "%.3g", rangeM / 1e9)));
        lines.add(Component.translatable("status.spacereloaded.dsn.dish", panels.length / 2,
                String.format(Locale.ROOT, "%.1f", diameterM()),
                String.format(Locale.ROOT, "%.1f", 10 * Math.log10(Math.max(1, LinkBudget.gain(diameterM()))))));
        double angle = skyAngle(level.getDefaultClockTime(), level.getGameTime(), own, body);
        lines.add(SkyGeometry.aboveHorizon(angle)
                ? Component.translatable("status.spacereloaded.dsn.sky", String.format(Locale.ROOT, "%.0f",
                        Math.toDegrees(Math.min(angle % (2 * Math.PI), Math.PI - angle % (2 * Math.PI)))))
                : Component.translatable("status.spacereloaded.dsn.below"));
        lines.add(Component.translatable("status.spacereloaded.dsn.rate", formatRate(rateBps),
                Component.translatable(rateBps >= TELEMETRY_BPS ? "status.spacereloaded.dsn.link_ok" : "status.spacereloaded.dsn.link_no")));
        List<MachineStatusPayload.Gauge> gauges = List.of(new MachineStatusPayload.Gauge(
                Component.translatable("gauge.spacereloaded.link"),
                (float) Math.max(0, Math.min(1, Math.log10(Math.max(1, rateBps)) / 9)), 0x57C4C4));
        return new MachineStatusPayload(getBlockPos(), Component.translatable("screen.spacereloaded.dsn"), lines, gauges,
                List.of(new MachineStatusPayload.Action("target", Component.translatable("action.spacereloaded.dsn.target"), 0)));
    }

    public static String formatRate(double bps) {
        if (bps >= 1e9) {
            return String.format(Locale.ROOT, "%.1f Гбит/с", bps / 1e9);
        }
        if (bps >= 1e6) {
            return String.format(Locale.ROOT, "%.1f Мбит/с", bps / 1e6);
        }
        if (bps >= 1e3) {
            return String.format(Locale.ROOT, "%.1f кбит/с", bps / 1e3);
        }
        return String.format(Locale.ROOT, "%.0f бит/с", bps);
    }

    /** Стенд: цель по id. */
    public void testTarget(Identifier id) {
        for (int i = 0; i < BODIES.size(); i++) {
            if (BODIES.get(i).id().equals(id)) {
                target = i;
            }
        }
    }

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
        structure.save(output);
        output.putInt("target", target);
        output.putIntArray("panels", panels);
        output.putDouble("rate", rateBps);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        structure.load(input);
        claimed = false;
        target = input.getIntOr("target", 2);
        panels = input.getIntArray("panels").orElse(new int[0]);
        rateBps = input.getDoubleOr("rate", 0);
    }
}
