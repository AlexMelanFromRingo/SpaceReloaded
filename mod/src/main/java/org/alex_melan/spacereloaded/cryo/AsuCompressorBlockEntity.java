package org.alex_melan.spacereloaded.cryo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.kinetics.KineticBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Центробежный компрессор воздухоразделительной колонны (008, US4, D85) на валу 005: момент
 * нагрузки ∝ ω (b·ω), поглощаемая мощность b·ω²; при 750 об/мин (номинал мотора) — 150 кВт, половина
 * мотора-генератора. Мощность сжатия задаёт поток воздуха колонны (0.061 кВт·ч/кг).
 */
public class AsuCompressorBlockEntity extends KineticBlockEntity {

    public static final double NOMINAL_OMEGA = 750 * 2 * Math.PI / 60;
    public static final double NOMINAL_W = 150e3;
    public static final double B = NOMINAL_W / (NOMINAL_OMEGA * NOMINAL_OMEGA);

    public AsuCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASU_COMPRESSOR, pos, state);
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        return new NodeLoad(0, B, SpaceReloaded.config().kineticFrictionTorqueNm, kineticBlock().inertia());
    }

    /** Мощность сжатия, Вт. */
    public double absorbedW() {
        return B * omega() * omega();
    }
}
