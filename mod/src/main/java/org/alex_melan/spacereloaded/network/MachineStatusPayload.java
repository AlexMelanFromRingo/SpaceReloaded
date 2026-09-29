package org.alex_melan.spacereloaded.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.List;

/**
 * S2C (008): экран состояния мультиблока — заголовок, строки величин, шкалы (доля 0…1, цвет) и кнопки
 * действий (id, подпись, значение). Экран сам просит обновление раз в секунду.
 */
public record MachineStatusPayload(BlockPos pos, Component title, List<Component> lines, List<Gauge> gauges,
                                   List<Action> actions, boolean refresh) implements CustomPacketPayload {

    /** Открыть экран (ответ на клик). */
    public MachineStatusPayload(BlockPos pos, Component title, List<Component> lines, List<Gauge> gauges,
                                List<Action> actions) {
        this(pos, title, lines, gauges, actions, false);
    }

    /**
     * 011: ответ на запрос обновления — только обновляет открытый экран и никогда не открывает его
     * заново (иначе ответ, пришедший после закрытия, возвращал окно).
     */
    public MachineStatusPayload asRefresh() {
        return new MachineStatusPayload(pos, title, lines, gauges, actions, true);
    }

    public record Gauge(Component label, float fraction, int rgb) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Gauge> CODEC = StreamCodec.composite(
                ComponentSerialization.TRUSTED_STREAM_CODEC, Gauge::label, ByteBufCodecs.FLOAT, Gauge::fraction,
                ByteBufCodecs.INT, Gauge::rgb, Gauge::new);
    }

    public record Action(String id, Component label, double value) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Action::id, ComponentSerialization.TRUSTED_STREAM_CODEC, Action::label,
                ByteBufCodecs.DOUBLE, Action::value, Action::new);
    }

    public static final CustomPacketPayload.Type<MachineStatusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "machine_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineStatusPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MachineStatusPayload::pos,
            ComponentSerialization.TRUSTED_STREAM_CODEC, MachineStatusPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list()), MachineStatusPayload::lines,
            Gauge.CODEC.apply(ByteBufCodecs.list()), MachineStatusPayload::gauges,
            Action.CODEC.apply(ByteBufCodecs.list()), MachineStatusPayload::actions,
            ByteBufCodecs.BOOL, MachineStatusPayload::refresh,
            MachineStatusPayload::new);

    @Override
    public CustomPacketPayload.Type<MachineStatusPayload> type() {
        return TYPE;
    }
}
