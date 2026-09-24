package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.kinetics.KineticGraph;
import org.alex_melan.spacereloaded.core.kinetics.KineticSolver;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.core.kinetics.ShaftStrength;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Механические сети измерений (005, FR-302…FR-309, D50–D53). Топология пересобирается только по
 * событию изменения кинетического блока (и его соседей по диагонали — сцепления шестерён);
 * сеть в равновесии спит и не тратит главный поток; будят её изменения узлов, машины и моторы.
 * Сеть, у которой рядом выгруженный чанк, заморожена целиком (частичная симуляция — ложь).
 */
public final class KineticNetworks {

    /** Шаг решателя = тик сервера, с. */
    public static final double DT = 0.05;
    /** Тиков без заметного изменения ω до сна. */
    private static final int CALM_TICKS_TO_SLEEP = 20;

    /** Ребро к соседу: ω_соседа = factor·ω_этого, КПД. */
    record Link(BlockPos pos, double factor, double efficiency) {
    }

    /** Сеть. */
    static final class Net {
        final List<BlockPos> nodes = new ArrayList<>();
        final Map<Long, Integer> index = new HashMap<>();
        KineticGraph.Analysis analysis;
        double omega;
        int calm;
        boolean sleeping;
        boolean frozen;
        long lastSync = Long.MIN_VALUE / 2;
        double lastSyncedOmega = Double.NaN;

        String stateKey() {
            if (frozen) {
                return "frozen";
            }
            return switch (analysis.state()) {
                case JAMMED -> "jammed";
                case TOO_LARGE -> "too_large";
                case EMPTY -> "idle";
                case OK -> omega == 0 ? "stopped" : "running";
            };
        }
    }

    private static final class LevelData {
        final Map<Long, Net> byPos = new HashMap<>();
        final Set<Long> dirty = new LinkedHashSet<>();
    }

    private static final Map<ResourceKey<Level>, LevelData> LEVELS = new HashMap<>();

    private KineticNetworks() {
    }

    private static LevelData data(ServerLevel level) {
        return LEVELS.computeIfAbsent(level.dimension(), k -> new LevelData());
    }

    /** Узел просит (пере)собрать свою сеть. */
    public static void request(ServerLevel level, BlockPos pos) {
        data(level).dirty.add(pos.asLong());
    }

    /** Будит сеть узла (изменилась нагрузка/источник). */
    public static void wake(ServerLevel level, BlockPos pos) {
        Net net = data(level).byPos.get(pos.asLong());
        if (net != null) {
            net.sleeping = false;
            net.calm = 0;
        }
    }

    /** Хук изменения блока (ServerLevelMixin): только если затронут кинетический блок. */
    public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!(oldState.getBlock() instanceof KineticBlock) && !(newState.getBlock() instanceof KineticBlock)) {
            return;
        }
        LevelData data = data(level);
        data.dirty.add(pos.asLong());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx | dy | dz) != 0) {
                        BlockPos n = pos.offset(dx, dy, dz);
                        if (data.byPos.containsKey(n.asLong())) {
                            data.dirty.add(n.asLong());
                        } else if (level.isLoaded(n) && level.getBlockState(n).getBlock() instanceof KineticBlock) {
                            data.dirty.add(n.asLong());
                        }
                    }
                }
            }
        }
    }

    public static void clearAll() {
        LEVELS.clear();
    }

    // ------------------------------------------------------------------ связность

    private static double sign(Direction d) {
        return d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
    }

    private static Direction[] alongAxis(Direction.Axis axis) {
        return new Direction[] {Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE),
                Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)};
    }

    /** Связи узла с соседями (D50). {@code unloaded[0]} — встретился невыгруженный сосед. */
    static List<Link> links(ServerLevel level, BlockPos pos, BlockState state, KineticBlock block,
                            boolean[] unloaded) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        List<Link> out = new ArrayList<>(6);
        if (block.kind() == KineticBlock.Kind.GEARBOX) {
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                if (!level.isLoaded(n)) {
                    unloaded[0] = true;
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (!(ns.getBlock() instanceof KineticBlock nb)) {
                    continue;
                }
                if (nb.kind() == KineticBlock.Kind.GEARBOX) {
                    out.add(new Link(n, -1, config.bevelEfficiency));
                } else if (nb.axis(ns) == d.getAxis() && nb.transmitsAxially(ns)) {
                    out.add(new Link(n, sign(d), config.bevelEfficiency));
                }
            }
            return out;
        }
        Direction.Axis axis = block.axis(state);
        if (block.transmitsAxially(state)) {
            for (Direction d : alongAxis(axis)) {
                BlockPos n = pos.relative(d);
                if (!level.isLoaded(n)) {
                    unloaded[0] = true;
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (!(ns.getBlock() instanceof KineticBlock nb)) {
                    continue;
                }
                if (nb.kind() == KineticBlock.Kind.GEARBOX) {
                    out.add(new Link(n, sign(d.getOpposite()), config.bevelEfficiency));
                } else if (nb.axis(ns) == axis && nb.transmitsAxially(ns)) {
                    out.add(new Link(n, 1, 1));
                }
            }
        }
        boolean small = block.kind() == KineticBlock.Kind.SMALL_GEAR;
        boolean large = block.kind() == KineticBlock.Kind.LARGE_GEAR;
        if (small || large) {
            List<Direction> plane = new ArrayList<>(4);
            for (Direction d : Direction.values()) {
                if (d.getAxis() != axis) {
                    plane.add(d);
                }
            }
            if (small) {
                for (Direction d : plane) {
                    addMesh(level, pos.relative(d), axis, KineticBlock.Kind.SMALL_GEAR, -1, config, out, unloaded);
                }
            }
            for (int i = 0; i < plane.size(); i++) {
                for (int j = i + 1; j < plane.size(); j++) {
                    Direction a = plane.get(i);
                    Direction b = plane.get(j);
                    if (a.getAxis() == b.getAxis()) {
                        continue;
                    }
                    BlockPos diag = pos.relative(a).relative(b);
                    if (small) {
                        // ω_большой·1.0 = ω_малой·0.5 по модулю, направления встречные
                        addMesh(level, diag, axis, KineticBlock.Kind.LARGE_GEAR, -0.5, config, out, unloaded);
                    } else {
                        addMesh(level, diag, axis, KineticBlock.Kind.SMALL_GEAR, -2, config, out, unloaded);
                    }
                }
            }
        }
        return out;
    }

    private static void addMesh(ServerLevel level, BlockPos n, Direction.Axis axis, KineticBlock.Kind wanted,
                                double factor, SpaceReloadedConfig config, List<Link> out, boolean[] unloaded) {
        if (!level.isLoaded(n)) {
            unloaded[0] = true;
            return;
        }
        BlockState ns = level.getBlockState(n);
        if (ns.getBlock() instanceof KineticBlock nb && nb.kind() == wanted && nb.axis(ns) == axis) {
            out.add(new Link(n, factor, config.gearMeshEfficiency));
        }
    }

    // ------------------------------------------------------------------ сборка

    private static Net build(ServerLevel level, BlockPos start) {
        int max = SpaceReloaded.config().kineticMaxBlocks;
        Net net = new Net();
        KineticGraph graph = new KineticGraph();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> processed = new HashSet<>();
        net.index.put(start.asLong(), graph.addNode());
        net.nodes.add(start);
        queue.add(start);
        boolean[] unloaded = {false};
        boolean tooLarge = false;
        while (!queue.isEmpty()) {
            BlockPos u = queue.poll();
            BlockState us = level.getBlockState(u);
            if (!(us.getBlock() instanceof KineticBlock ub)) {
                continue;
            }
            int iu = net.index.get(u.asLong());
            for (Link link : links(level, u, us, ub, unloaded)) {
                long key = link.pos().asLong();
                if (processed.contains(key)) {
                    continue;
                }
                Integer iv = net.index.get(key);
                if (iv == null) {
                    if (net.nodes.size() >= max) {
                        tooLarge = true;
                        continue;
                    }
                    iv = graph.addNode();
                    net.index.put(key, iv);
                    net.nodes.add(link.pos().immutable());
                    queue.add(link.pos().immutable());
                }
                graph.addEdge(iu, iv, link.factor(), link.efficiency());
            }
            processed.add(u.asLong());
        }
        net.analysis = tooLarge
                ? new KineticGraph.Analysis(KineticGraph.State.TOO_LARGE, new double[net.nodes.size()],
                        new double[net.nodes.size()], new int[net.nodes.size()], new int[0])
                : graph.analyze(max);
        net.frozen = unloaded[0];
        return net;
    }

    private static void rebuild(ServerLevel level, LevelData data) {
        Set<Long> seeds = new LinkedHashSet<>(data.dirty);
        data.dirty.clear();
        ArrayDeque<Long> expand = new ArrayDeque<>(seeds);
        while (!expand.isEmpty()) {
            Net old = data.byPos.get(expand.poll());
            if (old == null) {
                continue;
            }
            for (BlockPos p : old.nodes) {
                if (data.byPos.get(p.asLong()) == old) {
                    data.byPos.remove(p.asLong());
                }
                if (seeds.add(p.asLong())) {
                    expand.add(p.asLong());
                }
            }
        }
        long now = level.getGameTime();
        for (long seed : seeds) {
            if (data.byPos.containsKey(seed)) {
                continue;
            }
            BlockPos pos = BlockPos.of(seed);
            if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof KineticBlock)) {
                continue;
            }
            Net net = build(level, pos);
            for (BlockPos p : net.nodes) {
                data.byPos.put(p.asLong(), net);
            }
            KineticGraph.State state = net.analysis.state();
            if (state == KineticGraph.State.OK && level.getBlockEntity(pos) instanceof KineticBlockEntity root) {
                net.omega = root.omega();
            } else {
                net.omega = 0;
            }
            // Мгновенно согласовать узлы и показать состояние (заклинено, слишком велика…); фаза
            // каждого узла — rᵢ·Θ от корня, чтобы зубья соседних шестерён были в зацеплении
            int rootIndex = Math.max(0, net.nodes.indexOf(pos));
            double rootRatio = state == KineticGraph.State.OK ? net.analysis.ratio()[rootIndex] : 0;
            double theta = rootRatio != 0 && level.getBlockEntity(pos) instanceof KineticBlockEntity rootBe
                    ? rootBe.visualAngle(now) / rootRatio : 0;
            double scale = visualScale(net, state == KineticGraph.State.OK ? net.analysis.ratio() : null, net.omega);
            for (int i = 0; i < net.nodes.size(); i++) {
                if (level.getBlockEntity(net.nodes.get(i)) instanceof KineticBlockEntity be) {
                    double r = state == KineticGraph.State.OK ? net.analysis.ratio()[i] : 0;
                    be.applyStep(r * net.omega, 0, 0, net.stateKey());
                    be.alignPhase(now, r * theta, scale);
                }
            }
            net.lastSync = now;
            net.lastSyncedOmega = net.omega;
            if (state == KineticGraph.State.OK && hasRatio(net)) {
                org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, pos, 16,
                        org.alex_melan.spacereloaded.industry.IndustryAdvancements.GEAR_RATIO);
            }
        }
    }

    private static boolean hasRatio(Net net) {
        for (double r : net.analysis.ratio()) {
            if (Math.abs(Math.abs(r) - 1) > 1e-6) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ тик

    /** Конец тика уровня: пересборка грязных сетей и шаги неспящих. */
    public static void tick(ServerLevel level) {
        LevelData data = LEVELS.get(level.dimension());
        if (data == null) {
            return;
        }
        if (!data.dirty.isEmpty()) {
            rebuild(level, data);
        }
        Set<Net> nets = new HashSet<>(data.byPos.values());
        for (Net net : nets) {
            if (net.analysis.state() != KineticGraph.State.OK || net.sleeping) {
                continue;
            }
            if (net.frozen || !allLoaded(level, net)) {
                continue;
            }
            step(level, net);
        }
    }

    private static boolean allLoaded(ServerLevel level, Net net) {
        for (BlockPos p : net.nodes) {
            if (!level.isLoaded(p)) {
                return false;
            }
        }
        return true;
    }

    private static void step(ServerLevel level, Net net) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        int n = net.nodes.size();
        NodeLoad[] loads = new NodeLoad[n];
        KineticBlockEntity[] nodes = new KineticBlockEntity[n];
        for (int i = 0; i < n; i++) {
            if (level.getBlockEntity(net.nodes.get(i)) instanceof KineticBlockEntity be) {
                nodes[i] = be;
                loads[i] = be.load(level);
            } else {
                loads[i] = NodeLoad.NONE;
            }
        }
        double previous = net.omega;
        KineticSolver.Step s = KineticSolver.step(net.analysis, loads, net.omega, DT);
        net.omega = s.omega();
        List<BlockPos> breaks = new ArrayList<>();
        List<BlockPos> trips = new ArrayList<>();
        List<Integer> bursts = new ArrayList<>();
        double woodLimit = ShaftStrength.maxTorque(config.shaftDiameterM, config.woodShaftShearPa);
        double steelLimit = ShaftStrength.maxTorque(config.shaftDiameterM, config.steelShaftShearPa);
        for (int i = 0; i < n; i++) {
            KineticBlockEntity be = nodes[i];
            if (be == null) {
                continue;
            }
            double local = net.analysis.ratio()[i] * net.omega;
            be.applyStep(local, s.transmitted()[i], s.power()[i], net.stateKey());
            KineticBlock kb = be.kineticBlock();
            if (kb.kind() == KineticBlock.Kind.SHAFT) {
                double limit = kb.material() == KineticBlock.Material.WOOD ? woodLimit : steelLimit;
                if (s.transmitted()[i] > limit) {
                    breaks.add(net.nodes.get(i));
                }
            } else if (kb.kind() == KineticBlock.Kind.CLUTCH && s.transmitted()[i] > config.clutchSlipTorqueNm) {
                trips.add(net.nodes.get(i));
            } else if (kb.kind() == KineticBlock.Kind.FLYWHEEL && Math.abs(local) > config.flywheelMaxOmega) {
                bursts.add(i);
            }
        }
        long now = level.getGameTime();
        boolean settled = Math.abs(net.omega - previous) < 1e-4 * Math.max(1, Math.abs(net.omega));
        net.calm = settled ? net.calm + 1 : 0;
        boolean sleepNow = net.calm >= CALM_TICKS_TO_SLEEP;
        double change = Math.abs(net.omega - net.lastSyncedOmega) / Math.max(1, Math.abs(net.omega));
        if (sleepNow || Double.isNaN(net.lastSyncedOmega)
                || (change > config.kineticSyncThreshold && now - net.lastSync >= config.kineticSyncMinTicks)) {
            double scale = visualScale(net, net.analysis.state() == KineticGraph.State.OK ? net.analysis.ratio() : null,
                    net.omega);
            for (KineticBlockEntity be : nodes) {
                if (be != null) {
                    be.syncMotion(now, scale);
                }
            }
            net.lastSync = now;
            net.lastSyncedOmega = net.omega;
        }
        if (sleepNow) {
            net.sleeping = true;
        }
        for (BlockPos p : breaks) {
            SpaceReloaded.LOGGER.debug("Вал {} срезан моментом", p);
            level.destroyBlock(p, true);
        }
        for (BlockPos p : trips) {
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof ClutchBlock) {
                level.setBlock(p, st.setValue(ClutchBlock.TRIPPED, true), Block.UPDATE_ALL);
            }
        }
        for (int i : bursts) {
            if (nodes[i] instanceof FlywheelBlockEntity flywheel) {
                flywheel.burst(level);
            }
        }
    }

    /** Для стенда: ω сети узла (NaN — узла нет в сети). */
    public static double networkOmega(ServerLevel level, BlockPos pos) {
        LevelData data = LEVELS.get(level.dimension());
        Net net = data == null ? null : data.byPos.get(pos.asLong());
        return net == null ? Double.NaN : net.omega;
    }

    /** Для стенда и отчёта: состояние сети узла. */
    public static String networkState(ServerLevel level, BlockPos pos) {
        LevelData data = LEVELS.get(level.dimension());
        Net net = data == null ? null : data.byPos.get(pos.asLong());
        return net == null ? "none" : net.stateKey();
    }

    /** Общий множитель визуальной скорости: самый быстрый узел не быстрее kineticVisualMaxOmega. */
    private static double visualScale(Net net, double[] ratio, double omega) {
        double cap = SpaceReloaded.config().kineticVisualMaxOmega;
        double fastest = 0;
        if (ratio != null) {
            for (double r : ratio) {
                fastest = Math.max(fastest, Math.abs(r * omega));
            }
        }
        return fastest > cap ? cap / fastest : 1;
    }
}
