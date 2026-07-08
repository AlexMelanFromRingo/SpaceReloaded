package org.alex_melan.spacereloaded.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.alex_melan.spacereloaded.client.VacuumAmbience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Вакуум не проводит звук (Dead-Space-стиль): в открытом вакууме внешние
 * источники глушатся до 10%. Исключения: музыка и интерфейс — как есть;
 * звуки вплотную к игроку (свои шаги, удары рукой, урон) передаются через
 * скафандр приглушённо (60%).
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void spacereloaded$muffleInVacuum(SoundInstance sound,
                                              CallbackInfoReturnable<Float> cir) {
        if (!VacuumAmbience.isExposed()) {
            return;
        }
        SoundSource source = sound.getSource();
        if (source == SoundSource.MASTER || source == SoundSource.MUSIC
                || source == SoundSource.RECORDS || source == SoundSource.UI
                || source == SoundSource.VOICE) {
            return; // музыка/интерфейс/радио — не через среду
        }
        LocalPlayer player = Minecraft.getInstance().player;
        float muffled;
        if (sound.isRelative() || player == null) {
            muffled = cir.getReturnValueF() * 0.6f; // «у уха» — через скафандр
        } else {
            double dx = sound.getX() - player.getX();
            double dy = sound.getY() - player.getY();
            double dz = sound.getZ() - player.getZ();
            boolean own = dx * dx + dy * dy + dz * dz < 4.0;
            muffled = cir.getReturnValueF() * (own ? 0.6f : 0.1f);
        }
        cir.setReturnValue(muffled);
    }
}
