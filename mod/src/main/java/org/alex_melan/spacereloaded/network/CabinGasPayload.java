package org.alex_melan.spacereloaded.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * S2C (007, FR-516): газ зоны, в которой стоит игрок, — давление, pO₂, pCO₂ (кПа), раз в секунду
 * со сверкой источников. Давление < 0 — игрок вне зоны (HUD гаснет).
 */
public record CabinGasPayload(float pressure, float pO2, float pCo2) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CabinGasPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "cabin_gas"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, CabinGasPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CabinGasPayload::pressure, ByteBufCodecs.FLOAT, CabinGasPayload::pO2,
            ByteBufCodecs.FLOAT, CabinGasPayload::pCo2, CabinGasPayload::new);

    @Override
    public CustomPacketPayload.Type<CabinGasPayload> type() {
        return TYPE;
    }
}
