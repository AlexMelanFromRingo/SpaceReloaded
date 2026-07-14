package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** Выбор цели перелёта с карты: id записи планеты, а не измерения. */
public record SetDestinationPayload(Identifier target) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetDestinationPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "set_destination"));

    public static final StreamCodec<ByteBuf, SetDestinationPayload> CODEC =
            Identifier.STREAM_CODEC.map(SetDestinationPayload::new, SetDestinationPayload::target);

    @Override
    public CustomPacketPayload.Type<SetDestinationPayload> type() {
        return TYPE;
    }
}
