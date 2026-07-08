package org.alex_melan.spacereloaded.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * S2C: игрок в ОТКРЫТОМ вакууме (вакуумное измерение, вне герметичной зоны).
 * Клиент по нему глушит внешние звуки — вакуум звук не проводит.
 */
public record VacuumStatePayload(boolean exposed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VacuumStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    SpaceReloaded.MOD_ID, "vacuum_state"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, VacuumStatePayload> CODEC =
            ByteBufCodecs.BOOL.map(VacuumStatePayload::new, VacuumStatePayload::exposed);

    @Override
    public CustomPacketPayload.Type<VacuumStatePayload> type() {
        return TYPE;
    }
}
