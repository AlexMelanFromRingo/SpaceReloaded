package org.alex_melan.spacereloaded.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * S2C (007, D77): игрок во вращающемся кольце — ось (0 — вне кольца, 1 — X, 2 — Z) и ω, рад/с.
 * В системе кольца небо вращается с −ω вокруг его оси; клиент поворачивает небесную сферу.
 */
public record SpinSkyPayload(int axis, float omega) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpinSkyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "spin_sky"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SpinSkyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpinSkyPayload::axis, ByteBufCodecs.FLOAT, SpinSkyPayload::omega, SpinSkyPayload::new);

    @Override
    public CustomPacketPayload.Type<SpinSkyPayload> type() {
        return TYPE;
    }
}
