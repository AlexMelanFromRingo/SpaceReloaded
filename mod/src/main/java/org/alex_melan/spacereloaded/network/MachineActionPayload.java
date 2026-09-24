package org.alex_melan.spacereloaded.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/** C2S (008): кнопка экрана мультиблока; {@link #REFRESH} — просьба обновить экран. */
public record MachineActionPayload(BlockPos pos, String action, double value) implements CustomPacketPayload {

    public static final String REFRESH = "refresh";

    public static final CustomPacketPayload.Type<MachineActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "machine_action"));

    public static final StreamCodec<ByteBuf, MachineActionPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MachineActionPayload::pos, ByteBufCodecs.STRING_UTF8, MachineActionPayload::action,
            ByteBufCodecs.DOUBLE, MachineActionPayload::value, MachineActionPayload::new);

    @Override
    public CustomPacketPayload.Type<MachineActionPayload> type() {
        return TYPE;
    }
}
