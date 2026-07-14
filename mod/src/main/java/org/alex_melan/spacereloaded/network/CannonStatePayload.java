package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Состояние орудия для терминала (замена простыни в чате): сервер шлёт снимок,
 * клиент открывает или обновляет экран.
 *
 * @param targetDim пустой идентификатор пути = цель не назначена
 */
public record CannonStatePayload(BlockPos cannonPos, Identifier cannonDim,
                                 int rods, int maxRods, long energy, long energyCapacity,
                                 long energyPerShot, int cooldownTicks,
                                 BlockPos target, Identifier targetDim,
                                 boolean hasTarget) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CannonStatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "cannon_state"));

    public static final StreamCodec<ByteBuf, CannonStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.cannonPos());
                Identifier.STREAM_CODEC.encode(buf, payload.cannonDim());
                ByteBufCodecs.VAR_INT.encode(buf, payload.rods());
                ByteBufCodecs.VAR_INT.encode(buf, payload.maxRods());
                ByteBufCodecs.VAR_LONG.encode(buf, payload.energy());
                ByteBufCodecs.VAR_LONG.encode(buf, payload.energyCapacity());
                ByteBufCodecs.VAR_LONG.encode(buf, payload.energyPerShot());
                ByteBufCodecs.VAR_INT.encode(buf, payload.cooldownTicks());
                BlockPos.STREAM_CODEC.encode(buf, payload.target());
                Identifier.STREAM_CODEC.encode(buf, payload.targetDim());
                ByteBufCodecs.BOOL.encode(buf, payload.hasTarget());
            },
            buf -> new CannonStatePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    Identifier.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    BlockPos.STREAM_CODEC.decode(buf),
                    Identifier.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    @Override
    public CustomPacketPayload.Type<CannonStatePayload> type() {
        return TYPE;
    }
}
