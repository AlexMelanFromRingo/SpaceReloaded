package org.alex_melan.spacereloaded.multiblock;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** Сервер → клиент: открыть экран руководства инженера. */
public record OpenManualPayload() implements CustomPacketPayload {

    public static final Type<OpenManualPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "open_manual"));

    public static final StreamCodec<ByteBuf, OpenManualPayload> CODEC = StreamCodec.unit(new OpenManualPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
