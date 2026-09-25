package org.alex_melan.spacereloaded.orbit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.alex_melan.spacereloaded.core.orbit.OrbitalImaging;
import org.alex_melan.spacereloaded.industry.IndustryAdvancements;
import org.alex_melan.spacereloaded.network.SpaceNetworkState;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.vehicle.Soils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Орбитальная съёмка (007, US5, D79, FR-524/525). Оптика — {@link OrbitalImaging}: телескоп
 * D = 10 см на парковочной орбите тела даёт GSD = 1.22·λ·h/D, масштаб карты k не мельче его; ожидание —
 * доля T_cov/N спутников, сжатая k_астро.
 * <p>
 * Снимок видит только поверхность: верхний блок столбца (без подземных руд — оптика не видит
 * сквозь грунт) и рельеф теней по высоте. Загруженные чанки читаются по карте высот в главном
 * потоке; остальная местность — по рельефу генератора (базовая высота, вода, биом или грунт тела
 * из {@code soils}) в фоновом потоке (принцип IV), без построек и фич.
 */
public final class OrbitalImages {

    public static final double APERTURE_M = 0.10;
    private static final int SIZE = 128;
    /** Заказы в проявке — второй клик по тому же снимку ничего не запускает. */
    private static final Set<ImageOrder> DEVELOPING = new HashSet<>();

    private OrbitalImages() {
    }

    /** Оптика над телом: GSD, минимальный масштаб, параметры орбиты. */
    public record Optics(double gsd, int minScale, double radius, double altitude, double mu) {
    }

    /** Тело под орбитой: для орбитальной платформы — ближайшее по Δv тело с посадкой; для тела — само. */
    public static Optional<ResourceKey<Level>> bodyUnder(ServerLevel level) {
        var profile = PlanetManager.profileFor(level);
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        if (!"platform".equals(profile.get().arrival())) {
            return Optional.of(level.dimension());
        }
        Identifier best = null;
        double bestDv = Double.MAX_VALUE;
        for (Identifier target : profile.get().transitionTargets()) {
            var body = PlanetManager.profileById(level, target);
            if (body.isEmpty() || body.get().transfer().bodyRadius() <= 0) {
                continue;
            }
            double dv = profile.get().transfer().deltaVTo(target);
            if (dv < bestDv) {
                bestDv = dv;
                best = body.get().dimension();
            }
        }
        return best == null ? Optional.empty() : Optional.of(ResourceKey.create(Registries.DIMENSION, best));
    }

    public static Optional<Optics> optics(ServerLevel body) {
        var profile = PlanetManager.profileFor(body);
        if (profile.isEmpty() || profile.get().transfer().bodyRadius() <= 0
                || profile.get().transfer().parkingAltitude() <= 0) {
            return Optional.empty();
        }
        double r = profile.get().transfer().bodyRadius();
        double h = profile.get().transfer().parkingAltitude();
        double gsd = OrbitalImaging.gsd(OrbitalImaging.VISIBLE_M, h, APERTURE_M);
        return Optional.of(new Optics(gsd, OrbitalImaging.minScale(gsd), r, h, profile.get().gravity() * r * r));
    }

    /** Sneak+ПКМ по ЦУПу пустой картой: следующий доступный масштаб и его цена во времени. */
    public static void cycleScale(ServerLevel level, ServerPlayer player, ItemStack map) {
        var optics = optics(level);
        if (optics.isEmpty() || optics.get().minScale() < 0) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.no_orbit"));
            return;
        }
        Optics o = optics.get();
        int current = map.getOrDefault(ModDataComponents.IMAGE_SCALE, o.minScale());
        int next = current >= OrbitalImaging.MAX_SCALE ? o.minScale() : Math.max(o.minScale(), current + 1);
        map.set(ModDataComponents.IMAGE_SCALE, next);
        int sats = SpaceNetworkState.get(level.getServer()).imagingSats(level.dimension());
        long worst = OrbitalImaging.waitTicks(o.radius(), o.altitude(), o.mu(), next, Math.max(1, sats), 1.0);
        player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.scale", next, 1 << next,
                String.format(Locale.ROOT, "%.2f", o.gsd()),
                String.format(Locale.ROOT, "%.0f", OrbitalImaging.LINE_PIXELS * (1 << next) / 1000.0),
                minutes(worst)));
    }

    /**
     * ПКМ по ЦУПу пустой картой: заказ снимка вокруг ЦУПа. Нет спутника-камеры над своим телом — снимок
     * удалённого тела через антенну дальней связи (линия ≥ порога телеметрии к телу со спутником-камерой):
     * центр — точка ЦУПа в координатах того тела, к ожиданию пролёта добавляется передача сырых данных
     * полосы ((128·2^k м)² / GSD² пикселей по 12 бит) на скорости линии.
     */
    public static void order(ServerLevel level, BlockPos at, ServerPlayer player, ItemStack map) {
        var network = SpaceNetworkState.get(level.getServer());
        ServerLevel imaged = level;
        int sats = network.imagingSats(level.dimension());
        double linkBps = 0;
        if (sats <= 0) {
            for (var body : org.alex_melan.spacereloaded.comms.DsnBlockEntity.BODIES) {
                var key = ResourceKey.create(Registries.DIMENSION, body.id());
                double rate = network.groundLinkRate(body.id());
                ServerLevel other = level.getServer().getLevel(key);
                if (other != null && !key.equals(level.dimension()) && rate >= org.alex_melan.spacereloaded.comms.DsnBlockEntity.telemetryBps()
                        && network.imagingSats(key) > 0 && rate > linkBps) {
                    imaged = other;
                    linkBps = rate;
                }
            }
            if (linkBps <= 0) {
                player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.no_satellite"));
                return;
            }
            sats = network.imagingSats(imaged.dimension());
        }
        var optics = optics(imaged);
        if (optics.isEmpty() || optics.get().minScale() < 0) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.no_orbit"));
            return;
        }
        Optics o = optics.get();
        int scale = map.getOrDefault(ModDataComponents.IMAGE_SCALE, o.minScale());
        if (scale < o.minScale()) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.diffraction",
                    String.format(Locale.ROOT, "%.2f", o.gsd()), o.minScale()));
            return;
        }
        long now = level.getGameTime();
        long seed = at.asLong() * 0x9E3779B97F4A7C15L ^ now * 0xC2B2AE3D27D4EB4FL ^ player.getUUID().getLeastSignificantBits();
        seed = (seed ^ (seed >>> 31)) * 0xBF58476D1CE4E5B9L;
        double u = ((seed ^ (seed >>> 29)) >>> 11) * 0x1.0p-53;
        long wait = OrbitalImaging.waitTicks(o.radius(), o.altitude(), o.mu(), scale, sats, u);
        int cx = at.getX();
        int cz = at.getZ();
        long downlink = 0;
        if (imaged != level) {
            double from = PlanetManager.profileFor(level).map(p -> p.coordinateScale()).orElse(1.0);
            double to = PlanetManager.profileFor(imaged).map(p -> p.coordinateScale()).orElse(1.0);
            cx = (int) Math.round(at.getX() * from / to);
            cz = (int) Math.round(at.getZ() * from / to);
            double side = 128.0 * (1 << scale);
            double bits = side * side / (o.gsd() * o.gsd()) * 12;
            downlink = Math.round(bits / linkBps * 20);
            wait += downlink;
        }
        ImageOrder order = new ImageOrder(imaged.dimension().identifier(), cx, cz, scale, now + wait);
        map.shrink(1);
        ItemStack image = new ItemStack(ModItems.ORBITAL_IMAGE);
        image.set(ModDataComponents.IMAGE_ORDER, order);
        if (!player.getInventory().add(image)) {
            player.drop(image, false);
        }
        player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.ordered", scale, 1 << scale,
                String.format(Locale.ROOT, "%.0f", OrbitalImaging.LINE_PIXELS * (1 << scale) / 1000.0), sats,
                minutes(wait)));
        if (imaged != level) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.remote",
                    Component.translatable("planet.spacereloaded." + imaged.dimension().identifier().getPath()),
                    org.alex_melan.spacereloaded.comms.DsnBlockEntity.formatRate(linkBps), minutes(downlink)));
        }
    }

    /** Проявка: по готовности заказ превращается в запертую карту. */
    public static void develop(ServerPlayer player, ItemStack image, ImageOrder order) {
        var server = player.level().getServer();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, order.dimension()));
        if (level == null) {
            return;
        }
        long left = order.readyTick() - level.getGameTime();
        if (left > 0) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.waiting", minutes(left)));
            return;
        }
        if (!DEVELOPING.add(order)) {
            return;
        }
        ItemStack map = MapItem.create(level, order.x(), order.z(), (byte) order.scale(), false, false);
        var fresh = MapItem.getSavedData(map, level);
        var id = map.get(DataComponents.MAP_ID);
        int step = 1 << order.scale();
        int x0 = fresh.centerX - SIZE / 2 * step + step / 2;
        int z0 = fresh.centerZ - SIZE / 2 * step + step / 2;
        int[] height = new int[SIZE * SIZE];
        MapColor[] color = new MapColor[SIZE * SIZE];
        readLoaded(level, x0, z0, step, height, color);
        MapColor surface = Soils.entry(level).flatMap(Soils.Entry::surface)
                .map(b -> BuiltInRegistries.BLOCK.getValue(b).defaultMapColor()).orElse(null);
        CompletableFuture.runAsync(() -> readGenerator(level, x0, z0, step, height, color, surface), Util.backgroundExecutor())
                .thenRunAsync(() -> {
                    DEVELOPING.remove(order);
                    var locked = fresh.locked();
                    for (int i = 0; i < SIZE * SIZE; i++) {
                        int north = i >= SIZE ? height[i - SIZE] : height[i];
                        MapColor.Brightness b = color[i] == MapColor.WATER || height[i] == north ? MapColor.Brightness.NORMAL
                                : height[i] > north ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
                        locked.colors[i] = color[i].getPackedId(b);
                    }
                    locked.setDirty();
                    level.setMapData(id, locked);
                    replace(player, order, map);
                    IndustryAdvancements.award(player, IndustryAdvancements.VIEW_FROM_ABOVE);
                    player.sendSystemMessage(Component.translatable("message.spacereloaded.imaging.developed",
                            order.scale(), 1 << order.scale()));
                }, server);
    }

    /** Верхний блок столбца загруженных чанков: карта высот WORLD_SURFACE и цвет, как у ванильной карты. */
    private static void readLoaded(ServerLevel level, int x0, int z0, int step, int[] height, MapColor[] color) {
        var cursor = new BlockPos.MutableBlockPos();
        for (int pz = 0; pz < SIZE; pz++) {
            for (int px = 0; px < SIZE; px++) {
                int x = x0 + px * step;
                int z = z0 + pz * step;
                var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                MapColor c;
                do {
                    c = chunk.getBlockState(cursor.set(x, y, z)).getMapColor(level, cursor);
                } while (c == MapColor.NONE && --y > level.getMinY());
                height[pz * SIZE + px] = y;
                color[pz * SIZE + px] = c;
            }
        }
    }

    /** Остальная местность — рельеф генератора: вода над дном, иначе грунт тела или цвет биома. */
    private static void readGenerator(ServerLevel level, int x0, int z0, int step, int[] height, MapColor[] color,
                                      MapColor surface) {
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        for (int pz = 0; pz < SIZE; pz++) {
            for (int px = 0; px < SIZE; px++) {
                int i = pz * SIZE + px;
                if (color[i] != null) {
                    continue;
                }
                int x = x0 + px * step;
                int z = z0 + pz * step;
                int top = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                height[i] = top - 1;
                if (top > floor) {
                    color[i] = MapColor.WATER;
                } else if (surface != null) {
                    color[i] = surface;
                } else {
                    color[i] = biomeColor(generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                            QuartPos.fromBlock(top), QuartPos.fromBlock(z), random.sampler()));
                }
            }
        }
    }

    private static MapColor biomeColor(net.minecraft.core.Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return MapColor.COLOR_ORANGE;
        }
        if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BEACH)) {
            return MapColor.SAND;
        }
        if (biome.value().getBaseTemperature() < 0.15f) {
            return MapColor.SNOW;
        }
        return MapColor.GRASS;
    }

    private static void replace(ServerPlayer player, ImageOrder order, ItemStack map) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack s = inventory.getItem(slot);
            if (s.is(ModItems.ORBITAL_IMAGE) && order.equals(s.get(ModDataComponents.IMAGE_ORDER))) {
                inventory.setItem(slot, map);
                return;
            }
        }
        if (!inventory.add(map)) {
            player.drop(map, false);
        }
    }

    private static String minutes(long ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 1200.0);
    }

    /** Пустая ли это карта для заказа (ванильная {@code map}). */
    public static boolean isBlankMap(ItemStack stack) {
        return stack.is(Items.MAP);
    }
}
