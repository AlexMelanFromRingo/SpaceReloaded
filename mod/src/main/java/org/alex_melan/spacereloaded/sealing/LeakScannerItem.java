package org.alex_melan.spacereloaded.sealing;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Сканер утечек (обвязка герметичности): ПКМ по контроллеру атмосферы —
 * статус его зоны; ПКМ по любому блоку/в воздух — ближайшая зона в радиусе.
 * При УТЕЧКЕ указывает на ближайшую точку пробоя лучом частиц и словами.
 */
public class LeakScannerItem extends Item {

    private static final int SEARCH_RADIUS = 48;

    public LeakScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (context.getPlayer() instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            BlockPos clicked = context.getClickedPos();
            SealedZone zone = level.getBlockState(clicked).is(ModBlocks.ATMOSPHERE_CONTROLLER)
                    ? ZoneManager.zoneAt(serverLevel, clicked)
                    : ZoneManager.nearestZone(serverLevel, clicked, SEARCH_RADIUS);
            report(serverLevel, player, zone);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            report(serverLevel, serverPlayer,
                    ZoneManager.nearestZone(serverLevel, serverPlayer.blockPosition(), SEARCH_RADIUS));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private void report(ServerLevel level, ServerPlayer player, SealedZone zone) {
        if (zone == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.scanner.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        switch (zone.status()) {
            case SEALED -> {
                player.sendSystemMessage(Component.translatable(
                        "message.spacereloaded.scanner.sealed", zone.volume().size())
                        .withStyle(ChatFormatting.GREEN));
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.PLAYERS, 0.7f, 1.6f);
            }
            case LEAK, UNBOUNDED -> {
                BlockPos leak = nearestLeak(zone, player.blockPosition());
                if (leak != null) {
                    beam(level, player.getEyePosition(), leak);
                    double dist = Math.sqrt(leak.distSqr(player.blockPosition()));
                    player.sendSystemMessage(Component.translatable(
                            "message.spacereloaded.scanner.leak",
                            leak.getX(), leak.getY(), leak.getZ(), (int) Math.round(dist))
                            .withStyle(ChatFormatting.RED));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            "message.spacereloaded.scanner.leak_unknown").withStyle(ChatFormatting.RED));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                        SoundSource.PLAYERS, 0.8f, 0.6f);
            }
            default -> player.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.scanner.none").withStyle(ChatFormatting.GRAY));
        }
    }

    private BlockPos nearestLeak(SealedZone zone, BlockPos from) {
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        var it = zone.leakPoints().iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            BlockPos p = new BlockPos(PackedPos.unpackX(packed), PackedPos.unpackY(packed),
                    PackedPos.unpackZ(packed));
            double distSq = p.distSqr(from);
            if (distSq < bestSq) {
                bestSq = distSq;
                best = p;
            }
        }
        return best;
    }

    /** Луч частиц от глаз игрока к пробоине — визуальный указатель. */
    private void beam(ServerLevel level, Vec3 from, BlockPos leak) {
        Vec3 to = Vec3.atCenterOf(leak);
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(60, Math.max(4, delta.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
