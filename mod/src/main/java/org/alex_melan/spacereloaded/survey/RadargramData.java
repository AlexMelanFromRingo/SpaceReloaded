package org.alex_melan.spacereloaded.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * Радарограмма (009, US4, FR-742): трассы по ходу ровера (шаг 0.5 м), в каждой — {@code samples}
 * отсчётов по 3 нс, яркость 0…255. {@code epsilon} — проницаемость верхнего слоя: по ней шкала
 * времени переводится в глубину. Хранится в компоненте предмета {@code radargram}.
 */
public record RadargramData(Identifier dimension, float epsilon, int samples, byte[] data) {

    public static final Codec<RadargramData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(RadargramData::dimension),
            Codec.FLOAT.fieldOf("epsilon").forGetter(RadargramData::epsilon),
            Codec.INT.fieldOf("samples").forGetter(RadargramData::samples),
            Codec.BYTE_BUFFER.xmap(b -> {
                byte[] out = new byte[b.remaining()];
                b.duplicate().get(out);
                return out;
            }, java.nio.ByteBuffer::wrap).fieldOf("data").forGetter(RadargramData::data)
    ).apply(i, RadargramData::new));

    public int traces() {
        return samples <= 0 ? 0 : data.length / samples;
    }

    /** Яркость отсчёта 0…1. */
    public double at(int trace, int sample) {
        return (data[trace * samples + sample] & 0xFF) / 255.0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RadargramData r && r.dimension.equals(dimension) && r.epsilon == epsilon
                && r.samples == samples && java.util.Arrays.equals(r.data, data);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(data) * 31 + dimension.hashCode();
    }
}
