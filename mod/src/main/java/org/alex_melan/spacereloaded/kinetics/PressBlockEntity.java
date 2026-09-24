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
 * Механический пресс (005, FR-311, D54): кривошип бьёт раз в цикл; удар — работа W за t_удара,
 * нагрузка на сеть τ = W/(t·ω) на время удара (пиковая мощность 250 кВт при средней 25 кВт —
 * без маховика обороты проседают). Шток анимируется от синхронизированного тика удара.
 */
public class PressBlockEntity extends KineticMachineBlockEntity<MachiningRecipe.Pressing> {

    private int cycleTick;
    private boolean striking;
    private long strokeStartTick = Long.MIN_VALUE / 2;

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESS, pos, state, ModRecipes.PRESSING);
    }

    private static double rpmToOmega(double rpm) {
        return rpm * 2 * Math.PI / 60;
    }

    @Override
    protected double nominalOmega() {
        return rpmToOmega(SpaceReloaded.config().pressNominalRpm);
    }

    @Override
    protected double minOmega() {
        return rpmToOmega(SpaceReloaded.config().pressMinRpm);
    }

    @Override
    protected double maxOmega() {
        return rpmToOmega(SpaceReloaded.config().pressMaxRpm);
    }

    @Override
    protected double machineDeltaUm() {
        return SpaceReloaded.config().tolerancePressUm;
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        double strike = 0;
        if (striking) {
            double seconds = config.pressStrokeTicks * KineticNetworks.DT;
            strike = config.pressStrokeJ / (seconds * Math.max(Math.abs(omega), minOmega()));
        }
        return new NodeLoad(0, 0, config.kineticFrictionTorqueNm + strike, 20.0);
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        SpaceReloadedConfig config = SpaceReloaded.config();
        var recipe = currentRecipe(level);
        boolean ready = recipe.isPresent() && inWindow() && outputFits(recipe.get().value().resultStack());
        boolean wasStriking = striking;
        if (ready) {
            cycleTick++;
            sampleDeviation();
            striking = cycleTick > config.pressCycleTicks - config.pressStrokeTicks;
            if (striking && !wasStriking) {
                strokeStartTick = level.getGameTime();
                level.playSound(null, getBlockPos(), ModSounds.PRESS_STAMP, SoundSource.BLOCKS, 1.0f, 1.0f);
                syncMotion(level.getGameTime());
            }
            if (cycleTick >= config.pressCycleTicks) {
                cycleTick = 0;
                striking = false;
                finishOperation(level, recipe.get().value());
            }
        } else {
            cycleTick = 0;
            striking = false;
        }
        if (striking != wasStriking) {
            KineticNetworks.wake(level, getBlockPos());
        }
    }

    public long strokeStartTick() {
        return strokeStartTick;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("cycle", cycleTick);
        output.putLong("stroke", strokeStartTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cycleTick = input.getIntOr("cycle", 0);
        strokeStartTick = input.getLongOr("stroke", Long.MIN_VALUE / 2);
    }
}
