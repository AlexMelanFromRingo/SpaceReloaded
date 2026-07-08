package org.alex_melan.spacereloaded.planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Chunk ticket межпространственных операций (D6, принцип V): тип с
 * автопротуханием — утечка невозможна по построению (таймаут = страховка,
 * даже если явное снятие не произошло).
 */
public final class ModTickets {

    /** 30 секунд: с запасом покрывает телепорт + генерацию платформы. */
    public static final TicketType TRANSITION = Registry.register(
            BuiltInRegistries.TICKET_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "transition"),
            new TicketType(600L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    /**
     * Орбитальный удар (US7): в отличие от TRANSITION — FLAG_PERSIST (снаряд
     * в полёте переживает перезапуск сервера: ванильный TicketStorage
     * сохраняет только persist-тикеты) и FLAG_KEEP_DIMENSION_ACTIVE (без него
     * измерение без игроков перестаёт тикать сущности через 300 тиков —
     * снаряд замер бы в воздухе, пока стрелок сидит на орбите).
     */
    public static final TicketType STRIKE = Registry.register(
            BuiltInRegistries.TICKET_TYPE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "strike"),
            new TicketType(600L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
                    | TicketType.FLAG_PERSIST | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    public static void init() {
    }

    /** Удержать чанки вокруг точки на время перехода (auto-expire). */
    public static void holdAround(ServerLevel level, BlockPos pos, int radius) {
        level.getChunkSource().addTicketWithRadius(TRANSITION, ChunkPos.containing(pos), radius);
    }

    /** Удержать чанки орбитального удара (persist + активное измерение). */
    public static void holdStrike(ServerLevel level, BlockPos pos, int radius) {
        level.getChunkSource().addTicketWithRadius(STRIKE, ChunkPos.containing(pos), radius);
    }

    private ModTickets() {
    }
}
