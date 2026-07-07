package org.alex_melan.spacereloaded.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.energy.CableNetworkManager;
import org.alex_melan.spacereloaded.sealing.ZoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Полное покрытие инвалидации (T024): sendBlockUpdated проходит для ЛЮБОГО
 * серверного изменения блока — игрок, команды (/setblock, /fill), взрывы,
 * поршни. События Fabric ловят только действия игрока; этот хук закрывает
 * остальное (пробел нашёл автоматический стенд: setblock не рушил зону).
 * Дёшево: O(1) добавление в отложенное множество конца тика.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void spacereloaded$onBlockUpdated(BlockPos pos, BlockState oldState,
                                              BlockState newState, int flags, CallbackInfo ci) {
        if (oldState != newState) {
            ServerLevel level = (ServerLevel) (Object) this;
            ZoneManager.markBlockChanged(level, pos);
            CableNetworkManager.markBlockChanged(level, pos);
        }
    }
}
