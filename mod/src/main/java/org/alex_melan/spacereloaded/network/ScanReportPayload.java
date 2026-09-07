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
 *
 * <p>Полёт 2.0: {@code stages} — ЛТХ ступеней снизу вверх (FR-063); {@code ascent*}
 * и {@code maxQ*} — результат численного моделирования подъёма (FR-084);
 * {@code requiredDeltaV} = Δv, потраченный до высоты перехода в этой модели.
 */
public record ScanReportPayload(int blocks, double massKg, double dryMassKg, double thrustN,
                                double twr, double deltaV, double requiredDeltaV,
                                List<String> warnings, String error,
                                List<StageLine> stages, boolean ascentReached,
                                double ascentDeltaV, double ascentApexM,
                                double maxQPa, boolean maxQExceeded) implements CustomPacketPayload {

    /** Строка ступени: номер снизу, Δv, TWR на зажигании, есть ли двигатели. */
    public record StageLine(int index, double deltaV, double twr, boolean hasEngines) {
    }

    public static final CustomPacketPayload.Type<ScanReportPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "scan_report"));

    /** Отчёт об ошибке скана: ракета не прочиталась. */
    public static ScanReportPayload error(String key) {
        return new ScanReportPayload(0, 0, 0, 0, 0, 0, 0, List.of(), key,
                List.of(), false, 0, 0, 0, false);
    }

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
                ByteBufCodecs.VAR_INT.encode(buf, payload.stages().size());
                for (StageLine stage : payload.stages()) {
                    ByteBufCodecs.VAR_INT.encode(buf, stage.index());
                    ByteBufCodecs.DOUBLE.encode(buf, stage.deltaV());
                    ByteBufCodecs.DOUBLE.encode(buf, stage.twr());
                    ByteBufCodecs.BOOL.encode(buf, stage.hasEngines());
                }
                ByteBufCodecs.BOOL.encode(buf, payload.ascentReached());
                ByteBufCodecs.DOUBLE.encode(buf, payload.ascentDeltaV());
                ByteBufCodecs.DOUBLE.encode(buf, payload.ascentApexM());
                ByteBufCodecs.DOUBLE.encode(buf, payload.maxQPa());
                ByteBufCodecs.BOOL.encode(buf, payload.maxQExceeded());
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
                String error = ByteBufCodecs.STRING_UTF8.decode(buf);
                int stageCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<StageLine> stages = new ArrayList<>(stageCount);
                for (int i = 0; i < stageCount; i++) {
                    stages.add(new StageLine(ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.DOUBLE.decode(buf),
                            ByteBufCodecs.BOOL.decode(buf)));
                }
                boolean reached = ByteBufCodecs.BOOL.decode(buf);
                double ascentDeltaV = ByteBufCodecs.DOUBLE.decode(buf);
                double apex = ByteBufCodecs.DOUBLE.decode(buf);
                double maxQ = ByteBufCodecs.DOUBLE.decode(buf);
                boolean maxQExceeded = ByteBufCodecs.BOOL.decode(buf);
                return new ScanReportPayload(blocks, mass, dry, thrust, twr, deltaV, required,
                        List.copyOf(warnings), error, List.copyOf(stages), reached,
                        ascentDeltaV, apex, maxQ, maxQExceeded);
            });

    @Override
    public CustomPacketPayload.Type<ScanReportPayload> type() {
        return TYPE;
    }
}
