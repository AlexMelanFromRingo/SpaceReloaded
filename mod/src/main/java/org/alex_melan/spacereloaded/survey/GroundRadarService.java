package org.alex_melan.spacereloaded.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.survey.GroundRadar;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Георадар ровера (009, US4, D97): каждые 0.5 м пути — трасса по столбцу ≤ 48 блоков под колёсами
 * (1 блок = 1 м; соседние блоки одной среды сливаются в слой), среды — датапак
 * {@code spacereloaded:dielectric}, физика — {@link GroundRadar}. Буфер ровера — до 256 трасс;
 * бумага печатает его в предмет {@code radargram}.
 */
public final class GroundRadarService {

    public static final double STEP_M = 0.5;
    public static final int MAX_TRACES = 256;
    public static final int COLUMN = 48;
    /** Потребление радара, Вт (RIMFAX 5–10 Вт). */
    public static final double POWER_W = 10;

    private GroundRadarService() {
    }

    /** Столбец сред от блока {@code top} вниз. */
    public static List<GroundRadar.Layer> column(ServerLevel level, BlockPos top) {
        var access = level.registryAccess();
        List<GroundRadar.Layer> layers = new ArrayList<>();
        SurveyRegistries.Dielectric current = null;
        int thickness = 0;
        var cursor = new BlockPos.MutableBlockPos();
        int bottom = Math.max(level.getMinY(), top.getY() - COLUMN + 1);
        for (int y = top.getY(); y >= bottom; y--) {
            var d = SurveyRegistries.dielectric(access, level.getBlockState(cursor.set(top.getX(), y, top.getZ())));
            if (current != null && d.epsilon() == current.epsilon() && d.lossTangent() == current.lossTangent()) {
                thickness++;
                continue;
            }
            if (current != null) {
                layers.add(new GroundRadar.Layer(thickness, current.epsilon(), current.lossTangent()));
            }
            current = d;
            thickness = 1;
        }
        if (current != null) {
            layers.add(new GroundRadar.Layer(thickness, current.epsilon(), current.lossTangent()));
        }
        return layers;
    }

    /** Трасса в байтах яркости (0…255). */
    public static byte[] trace(ServerLevel level, BlockPos top) {
        var config = SpaceReloaded.config();
        double[] t = GroundRadar.trace(column(level, top), config.radarFrequencyMhz * 1e6, config.radarDynamicRangeDb);
        byte[] out = new byte[t.length];
        for (int i = 0; i < t.length; i++) {
            out[i] = (byte) Math.round(Math.max(0, Math.min(1, t[i])) * 255);
        }
        return out;
    }

    /** Буфер трасс ровера. */
    public static final class Recorder {
        private byte[] data = new byte[0];
        private double since;
        private float epsilon = 3;

        public int traces() {
            return data.length / GroundRadar.SAMPLES;
        }

        /** Пройдено {@code moved} м: трасса на каждые 0.5 м, пока буфер не полон. */
        public void advance(ServerLevel level, BlockPos top, double moved) {
            since += moved;
            while (since >= STEP_M && traces() < MAX_TRACES) {
                since -= STEP_M;
                byte[] tr = trace(level, top);
                if (traces() == 0) {
                    epsilon = (float) SurveyRegistries.dielectric(level.registryAccess(), level.getBlockState(top)).epsilon();
                }
                data = Arrays.copyOf(data, data.length + tr.length);
                System.arraycopy(tr, 0, data, data.length - tr.length, tr.length);
            }
            if (traces() >= MAX_TRACES) {
                since = 0;
            }
        }

        /** Печать радарограммы и очистка буфера. */
        public ItemStack print(ServerLevel level) {
            ItemStack out = new ItemStack(ModItems.RADARGRAM);
            out.set(ModDataComponents.RADARGRAM, new RadargramData(level.dimension().identifier(), epsilon,
                    GroundRadar.SAMPLES, data));
            data = new byte[0];
            since = 0;
            return out;
        }

        /** Есть ли отражение глубже {@code depthM} (достижение «Под грунтом»). */
        public boolean echoDeeperThan(double depthM) {
            for (int i = 0; i < data.length; i++) {
                int k = i % GroundRadar.SAMPLES;
                if ((data[i] & 0xFF) > 8 && GroundRadar.depthAt(k, epsilon) > depthM) {
                    return true;
                }
            }
            return false;
        }

        public byte[] raw() {
            return data;
        }

        public float epsilon() {
            return epsilon;
        }

        public void load(byte[] raw, float eps) {
            data = raw.length % GroundRadar.SAMPLES == 0 ? raw : new byte[0];
            epsilon = eps;
        }
    }
}
