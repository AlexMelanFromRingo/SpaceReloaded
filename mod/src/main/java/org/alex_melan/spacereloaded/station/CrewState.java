package org.alex_melan.spacereloaded.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.station.SpinGravity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Декондиционирование экипажа (007, FR-516, D76): сутки в невесомости (g < 0.1 g₀) копятся —
 * атрофия мышц и потеря костной массы 1–1.5 % в месяц (NASA). На теле с g ≥ 0.3 g₀ накопленное
 * даёт «Слабость» (от 1 суток) и замедление (от 7 суток); время при g ≥ 0.3 g₀ (на планете или в
 * кольце) снимает его с той же скоростью, с какой набиралось — восстановление после полёта
 * длится примерно столько же, сколько полёт. Время — игровые сутки (биология ×72).
 */
public final class CrewState extends SavedData {

    public static final double MICRO_G = 0.1 * SpinGravity.G0;
    public static final double HEALTHY_G = 0.3 * SpinGravity.G0;

    private static final Codec<Map<UUID, Double>> MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE);
    public static final Codec<CrewState> CODEC = RecordCodecBuilder.create(i -> i.group(
            MAP.optionalFieldOf("microgravity_days", Map.of()).forGetter(s -> s.days)
    ).apply(i, CrewState::new));
    public static final SavedDataType<CrewState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "crew_state"), CrewState::new, CODEC,
            DataFixTypes.LEVEL);

    private final Map<UUID, Double> days;

    public CrewState() {
        this(Map.of());
    }

    private CrewState(Map<UUID, Double> days) {
        this.days = new HashMap<>(days);
    }

    public static CrewState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public double days(UUID player) {
        return days.getOrDefault(player, 0.0);
    }

    /** Раз в секунду: игрок при гравитации g (м/с²). */
    public void update(ServerPlayer player, double g, double dtDays) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        double d = days(player.getUUID());
        if (g < MICRO_G) {
            d += dtDays;
        } else if (g >= HEALTHY_G && d > 0) {
            if (d >= 1) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, d >= 7 ? 1 : 0, true, true));
            }
            if (d >= 7) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, true));
            }
            d = Math.max(0, d - dtDays);
        }
        days.put(player.getUUID(), d);
        setDirty();
    }
}
