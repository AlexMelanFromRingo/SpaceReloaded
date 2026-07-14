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
 * Скан-отчёт стартового комплекса: раньше был простынёй в чате, теперь экран.
 * Пустой {@code error} означает, что ракета вообще прочиталась.
 */
public record ScanReportPayload(int blocks, double massKg, double dryMassKg, double thrustN,
                                double twr, double deltaV, double requiredDeltaV,
                                List<String> warnings, String error) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ScanReportPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "scan_report"));

    public static final StreamCodec<ByteBuf, ScanReportPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.VAR_INT.encode(buf, payload.blocks());
                ByteBufCodecs.DOUBLE.encode(buf, payload.massKg());
                ByteBufCodecs.DOUBLE.encode(buf, payload.dryMassKg());
                ByteBufCodecs.DOUBLE.encode(buf, payload.thrustN());
                ByteBufCodecs.DOUBLE.encode(buf, payload.twr());
                ByteBufCodecs.DOUBLE.encode(buf, payload.deltaV());
                ByteBufCodecs.DOUBLE.encode(buf, payload.requiredDeltaV());
                ByteBufCodecs.VAR_INT.encode(buf, payload.warnings().size());
                for (String warning : payload.warnings()) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, warning);
                }
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.error());
            },
            buf -> {
                int blocks = ByteBufCodecs.VAR_INT.decode(buf);
                double mass = ByteBufCodecs.DOUBLE.decode(buf);
                double dry = ByteBufCodecs.DOUBLE.decode(buf);
                double thrust = ByteBufCodecs.DOUBLE.decode(buf);
                double twr = ByteBufCodecs.DOUBLE.decode(buf);
                double deltaV = ByteBufCodecs.DOUBLE.decode(buf);
                double required = ByteBufCodecs.DOUBLE.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                List<String> warnings = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    warnings.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                return new ScanReportPayload(blocks, mass, dry, thrust, twr, deltaV, required,
                        List.copyOf(warnings), ByteBufCodecs.STRING_UTF8.decode(buf));
            });

    @Override
    public CustomPacketPayload.Type<ScanReportPayload> type() {
        return TYPE;
    }
}
