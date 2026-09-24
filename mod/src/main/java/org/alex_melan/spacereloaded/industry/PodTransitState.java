package org.alex_melan.spacereloaded.industry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.industry.CatcherOdds;
import org.alex_melan.spacereloaded.network.SpaceNetworkState;
import org.alex_melan.spacereloaded.planet.ModTickets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Капсулы в пути (004, FR-210/FR-224, D35; принцип V): очередь выстрелов катапульт в
 * SavedData оверворлда — переживает перезапуск. За 40 тиков до прибытия чанк ловушки
 * удерживается авто-протухающим билетом; в момент прибытия считается смещение по зерну
 * выстрела и σ покрытия, затем приём в ловушку или промах. Запись удаляется только после
 * обработки: груз не теряется и не дублируется из-за перезапуска.
 */
public class PodTransitState extends SavedData {

    /** Капсула в пути. */
    public record PodTransit(UUID id, ItemStack pod, List<ItemStack> cargo, ResourceKey<Level> targetDimension,
                             BlockPos targetPos, long arrivalTick, long seed, Optional<UUID> shooter,
                             boolean ticketed) {
        public static final Codec<PodTransit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(PodTransit::id),
                ItemStack.OPTIONAL_CODEC.fieldOf("pod").forGetter(PodTransit::pod),
                ItemStack.CODEC.listOf().fieldOf("cargo").forGetter(PodTransit::cargo),
                Level.RESOURCE_KEY_CODEC.fieldOf("target_dim").forGetter(PodTransit::targetDimension),
                BlockPos.CODEC.fieldOf("target_pos").forGetter(PodTransit::targetPos),
                Codec.LONG.fieldOf("arrival").forGetter(PodTransit::arrivalTick),
                Codec.LONG.fieldOf("seed").forGetter(PodTransit::seed),
                UUIDUtil.CODEC.optionalFieldOf("shooter").forGetter(PodTransit::shooter),
                Codec.BOOL.optionalFieldOf("ticketed", false).forGetter(PodTransit::ticketed)
        ).apply(instance, PodTransit::new));

        PodTransit withTicketed() {
            return new PodTransit(id, pod, cargo, targetDimension, targetPos, arrivalTick, seed, shooter, true);
        }

        PodTransit postponed(long tick) {
            return new PodTransit(id, pod, cargo, targetDimension, targetPos, tick, seed, shooter, ticketed);
        }

        /** Все предметы капсулы (груз + сама капсула) — для приёма. */
        public List<ItemStack> allItems() {
            List<ItemStack> all = new ArrayList<>(cargo.size() + 1);
            cargo.forEach(stack -> all.add(stack.copy()));
            if (!pod.isEmpty()) {
                all.add(pod.copy());
            }
            return all;
        }
    }

    public static final Codec<PodTransitState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PodTransit.CODEC.listOf().optionalFieldOf("pods", List.of()).forGetter(s -> s.pods)
    ).apply(instance, PodTransitState::new));

    public static final SavedDataType<PodTransitState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "pod_transit"),
            PodTransitState::new, CODEC, DataFixTypes.LEVEL);

    /** Сколько раз откладывать прибытие, если чанк ловушки ещё не прогрузился. */
    private static final long MAX_POSTPONE_TICKS = 600;

    private final List<PodTransit> pods;

    public PodTransitState() {
        this(List.of());
    }

    private PodTransitState(List<PodTransit> pods) {
        this.pods = new ArrayList<>(pods);
    }

    public static PodTransitState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void enqueue(PodTransit transit) {
        pods.add(transit);
        setDirty();
    }

    /** Только для стенда: убрать капсулы, выпущенные сценарием. */
    public void removeIf(java.util.function.Predicate<PodTransit> filter) {
        if (pods.removeIf(filter)) {
            setDirty();
        }
    }

    public List<PodTransit> pods() {
        return List.copyOf(pods);
    }

    /** Серверный тик (END_SERVER_TICK), работа раз в 20 тиков. */
    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 20 != 0) {
            return;
        }
        PodTransitState state = get(server);
        if (state.pods.isEmpty()) {
            return;
        }
        List<PodTransit> keep = new ArrayList<>();
        boolean changed = false;
        for (PodTransit transit : state.pods) {
            ServerLevel target = server.getLevel(transit.targetDimension());
            if (target == null) {
                resolveLost(server, transit, "not_found", 0, 0);
                changed = true;
                continue;
            }
            PodTransit current = transit;
            if (!current.ticketed() && now >= current.arrivalTick() - 40) {
                ModTickets.holdAround(target, current.targetPos(), 1);
                current = current.withTicketed();
                changed = true;
            }
            if (now < current.arrivalTick()) {
                keep.add(current);
                continue;
            }
            if (!target.isLoaded(current.targetPos())) {
                ModTickets.holdAround(target, current.targetPos(), 1);
                if (now - current.arrivalTick() < MAX_POSTPONE_TICKS) {
                    keep.add(current);
                    continue;
                }
            }
            resolve(server, target, current);
            changed = true;
        }
        if (changed) {
            state.pods.clear();
            state.pods.addAll(keep);
            state.setDirty();
        }
    }

    /** Прибытие: приём или промах. Публично для стенда (детерминировано зерном). */
    public static boolean resolve(MinecraftServer server, ServerLevel target, PodTransit transit) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        if (!(target.getBlockEntity(transit.targetPos()) instanceof MassCatcherBlockEntity catcher)
                || !catcher.operational(target)) {
            resolveLost(server, transit, "not_found", 0, 0);
            return false;
        }
        boolean covered = SpaceNetworkState.get(server).hasCoverage(target.dimension());
        double sigma = covered ? config.podSigmaCovered : config.podSigmaUncovered;
        double[] offset = CatcherOdds.sampleOffset(transit.seed(), sigma);
        double miss = Math.hypot(offset[0], offset[1]);
        double radius = catcher.captureRadius();
        if (miss <= radius) {
            catcher.accept(target, transit.allItems());
            ServerPlayer shooter = transit.shooter().map(id -> server.getPlayerList().getPlayer(id)).orElse(null);
            if (shooter != null) {
                IndustryAdvancements.award(shooter, IndustryAdvancements.MASS_CATCH);
            }
            IndustryAdvancements.awardNearby(target, transit.targetPos(), 16, IndustryAdvancements.MASS_CATCH);
            return true;
        }
        catcher.recordLoss(miss, radius);
        resolveLost(server, transit, covered ? "miss" : "miss_uncovered", miss, radius);
        return false;
    }

    private static void resolveLost(MinecraftServer server, PodTransit transit, String kind, double miss,
                                    double radius) {
        transit.shooter().map(id -> server.getPlayerList().getPlayer(id)).ifPresent(player ->
                player.sendSystemMessage(Component.translatable("message.spacereloaded.mass_catcher." + kind,
                        String.format(Locale.ROOT, "%.1f", miss), String.format(Locale.ROOT, "%.1f", radius),
                        transit.targetPos().toShortString())));
        SpaceReloaded.LOGGER.info("Капсула {} потеряна ({}): промах {} при радиусе {}", transit.id(), kind,
                miss, radius);
    }
}
