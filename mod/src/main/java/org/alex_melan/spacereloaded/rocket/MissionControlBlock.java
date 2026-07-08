package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Locale;

/**
 * ЦУП (v1): ПКМ — телеметрия всех бортов в радиусе 64 блока: статус,
 * топливо, высота. Реальные данные честной физики; экраны-GUI — позже.
 */
public class MissionControlBlock extends Block {

    private static final double RANGE = 64.0;

    public MissionControlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        List<RocketEntity> rockets = serverLevel.getEntities(
                EntityTypeTest.forClass(RocketEntity.class),
                new AABB(pos).inflate(RANGE), entity -> true);
        if (rockets.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.spacereloaded.mission_control.empty"));
            return InteractionResult.SUCCESS_SERVER;
        }
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.spacereloaded.mission_control.header", rockets.size()));
        int index = 1;
        for (RocketEntity rocket : rockets) {
            String status = rocket.isParked() ? "▮" : "▲";
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.mission_control.entry",
                    index++, status,
                    String.format(Locale.ROOT, "%.0f", rocket.propellantKg()),
                    String.format(Locale.ROOT, "%.0f", rocket.getY()),
                    rocket.isParked()
                            ? Component.translatable("message.spacereloaded.mission_control.parked")
                            : Component.translatable("message.spacereloaded.mission_control.flight")));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
