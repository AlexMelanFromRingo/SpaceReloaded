package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** Кнопка терминала орудия: {@link #FIRE} стреляет, {@link #REFRESH} просит снимок. */
public record CannonActionPayload(BlockPos cannonPos, Identifier cannonDim, int action)
        implements CustomPacketPayload {

    public static final int FIRE = 0;
    public static final int REFRESH = 1;

    public static final CustomPacketPayload.Type<CannonActionPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "cannon_action"));

    public static final StreamCodec<ByteBuf, CannonActionPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CannonActionPayload::cannonPos,
            Identifier.STREAM_CODEC, CannonActionPayload::cannonDim,
            ByteBufCodecs.VAR_INT, CannonActionPayload::action,
            CannonActionPayload::new);

    @Override
    public CustomPacketPayload.Type<CannonActionPayload> type() {
        return TYPE;
    }
}
