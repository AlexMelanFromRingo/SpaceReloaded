package org.alex_melan.spacereloaded.orbit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * Заказ орбитального снимка (007, US5): тело, точка ЦУПа (центр карты), масштаб k и игровой тик
 * готовности. Хранится в компоненте предмета {@code orbital_image} — у сервера нет очереди заказов,
 * снимок проявляется, когда игрок достанет его после срока.
 */
public record ImageOrder(Identifier dimension, int x, int z, int scale, long readyTick) {

    public static final Codec<ImageOrder> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(ImageOrder::dimension),
            Codec.INT.fieldOf("x").forGetter(ImageOrder::x),
            Codec.INT.fieldOf("z").forGetter(ImageOrder::z),
            Codec.INT.fieldOf("scale").forGetter(ImageOrder::scale),
            Codec.LONG.fieldOf("ready").forGetter(ImageOrder::readyTick)
    ).apply(i, ImageOrder::new));
}
