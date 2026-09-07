package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Клавиша отделения ступени (Полёт 2.0, FR-065): полей нет — действие относится
 * к транспорту отправителя, сервер сам проверяет, что он пилот своего борта.
 */
public record StageSeparatePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StageSeparatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "stage_separate"));

    public static final StreamCodec<ByteBuf, StageSeparatePayload> CODEC =
            StreamCodec.unit(new StageSeparatePayload());

    @Override
    public CustomPacketPayload.Type<StageSeparatePayload> type() {
        return TYPE;
    }
}
