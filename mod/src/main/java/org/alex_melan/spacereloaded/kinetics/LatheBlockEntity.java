package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.machine.recipe.MachiningRecipe;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModRecipes;
import org.alex_melan.spacereloaded.registry.ModSounds;

/**
 * Токарный станок (005, FR-312, D54): резание — постоянный момент τ = P_ном/ω_ном, пока идёт
 * операция; работа резания τ·ω·dt копится до энергии рецепта (удельная энергия резания ×
 * снимаемый объём). Вне окна оборотов шпинделя станок стоит.
 */
public class LatheBlockEntity extends KineticMachineBlockEntity<MachiningRecipe.Lathe> {

    private double workJ;
    private boolean cutting;

    public LatheBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LATHE, pos, state, ModRecipes.MACHINING);
    }

    private static double rpmToOmega(double rpm) {
        return rpm * 2 * Math.PI / 60;
    }

    @Override
    protected double nominalOmega() {
        return rpmToOmega(SpaceReloaded.config().latheNominalRpm);
    }

    @Override
    protected double minOmega() {
        return rpmToOmega(SpaceReloaded.config().latheMinRpm);
    }

    @Override
    protected double maxOmega() {
        return rpmToOmega(SpaceReloaded.config().latheMaxRpm);
    }

    @Override
    protected double machineDeltaUm() {
        return SpaceReloaded.config().toleranceLatheUm;
    }

    private double cuttingTorque() {
        return SpaceReloaded.config().latheNominalPowerW / nominalOmega();
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        return new NodeLoad(0, 0, config.kineticFrictionTorqueNm + (cutting ? cuttingTorque() : 0), 5.0);
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        var recipe = currentRecipe(level);
        boolean ready = recipe.isPresent() && inWindow() && outputFits(recipe.get().value().resultStack());
        boolean wasCutting = cutting;
        cutting = ready;
        if (ready) {
            workJ += cuttingTorque() * Math.abs(omega) * KineticNetworks.DT;
            sampleDeviation();
            if (level.getGameTime() % 30 == 0) {
                level.playSound(null, getBlockPos(), ModSounds.LATHE_CUT, SoundSource.BLOCKS, 0.6f, 1.0f);
            }
            double need = Math.max(1.0, recipe.get().value().data().energyJ());
            if (workJ >= need) {
                workJ = 0;
                finishOperation(level, recipe.get().value());
            }
        } else if (!recipe.isPresent()) {
            workJ = 0;
        }
        if (cutting != wasCutting) {
            KineticNetworks.wake(level, getBlockPos());
        }
    }

    public double progress(ServerLevel level) {
        return currentRecipe(level).map(r -> workJ / Math.max(1.0, r.value().data().energyJ())).orElse(0.0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("work", workJ);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workJ = input.getDoubleOr("work", 0);
    }
}
