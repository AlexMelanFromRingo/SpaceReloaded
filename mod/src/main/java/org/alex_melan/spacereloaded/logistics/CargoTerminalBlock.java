package org.alex_melan.spacereloaded.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

import java.util.Locale;

/**
 * Грузовой терминал: ПКМ — статус линии в чат; Sneak+ПКМ — AUTO/HOLD;
 * ПКМ полётной программой — загрузить программу (обрабатывает предмет).
 */
public class CargoTerminalBlock extends Block implements EntityBlock {

    public CargoTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoTerminalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.CARGO_TERMINAL) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                CargoTerminalBlockEntity.serverTick((CargoTerminalBlockEntity) blockEntity,
                        (ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CargoTerminalBlockEntity terminal)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            CargoTerminalBlockEntity.Mode mode = terminal.toggleMode();
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.spacereloaded.terminal.mode_set",
                    Component.translatable("message.spacereloaded.terminal.mode."
                            + mode.name().toLowerCase(Locale.ROOT))));
            return InteractionResult.SUCCESS_SERVER;
        }
        serverPlayer.sendSystemMessage(Component.translatable("message.spacereloaded.terminal.status.header",
                pos.toShortString()));
        for (Component line : terminal.statusLines()) {
            serverPlayer.sendSystemMessage(line);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
