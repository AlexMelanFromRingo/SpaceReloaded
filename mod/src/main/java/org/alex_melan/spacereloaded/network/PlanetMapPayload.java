package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.ArrayList;
import java.util.List;

/**
 * Открытие карты полёта. Почти всё клиент считает сам по синхронизированному
 * реестру планет; с сервера приходит только то, чего клиент знать не может:
 * какие измерения накрыты спутниковой связью, и сидит ли игрок в ракете.
 */
public record PlanetMapPayload(List<Identifier> coveredDimensions, boolean pilotingRocket)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlanetMapPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "planet_map"));

    private static final StreamCodec<ByteBuf, List<Identifier>> IDS =
            ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC, 64)
                    .map(list -> (List<Identifier>) list, ArrayList::new);

    public static final StreamCodec<ByteBuf, PlanetMapPayload> CODEC = StreamCodec.composite(
            IDS, PlanetMapPayload::coveredDimensions,
            ByteBufCodecs.BOOL, PlanetMapPayload::pilotingRocket,
            PlanetMapPayload::new);

    @Override
    public CustomPacketPayload.Type<PlanetMapPayload> type() {
        return TYPE;
    }
}
