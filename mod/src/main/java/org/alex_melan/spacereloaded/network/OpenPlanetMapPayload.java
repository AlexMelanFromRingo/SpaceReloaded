package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** Клиент просит карту полёта (клавиша или блок ЦУП). */
public record OpenPlanetMapPayload(boolean fromRocket) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenPlanetMapPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "open_planet_map"));

    public static final StreamCodec<ByteBuf, OpenPlanetMapPayload> CODEC =
            ByteBufCodecs.BOOL.map(OpenPlanetMapPayload::new, OpenPlanetMapPayload::fromRocket);

    @Override
    public CustomPacketPayload.Type<OpenPlanetMapPayload> type() {
        return TYPE;
    }
}
