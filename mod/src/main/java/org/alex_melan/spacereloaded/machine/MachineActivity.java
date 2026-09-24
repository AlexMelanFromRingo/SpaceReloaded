package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Видимое рабочее состояние станков (визуальный проход 005): свойство {@code lit} с задержкой
 * выключения (без мерцания между циклами), свет из свойства, частицы и звук по виду машины —
 * каждый процесс выдаёт себя тем, что в нём происходит физически: дробилка пылит, электропечь
 * дымит, генератор горит, электролизёр пузырится, куб кипит, Сабатье парит (реакция экзотермична).
 */
public final class MachineActivity {

    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    /** Тиков удержания «работает» после последнего признака работы. */
    public static final int HOLD_TICKS = 20;

    /** Источник признака работы. */
    public interface Source {
        boolean isWorking();

        int activeHold();

        void setActiveHold(int hold);
    }

    private MachineActivity() {
    }

    /** После тика машины: обновить свойство и эффекты. */
    public static void update(ServerLevel level, BlockPos pos, BlockState state, Source source) {
        if (!state.hasProperty(ACTIVE)) {
            return;
        }
        int hold = source.isWorking() ? HOLD_TICKS : Math.max(0, source.activeHold() - 1);
        source.setActiveHold(hold);
        boolean active = hold > 0;
        if (state.getValue(ACTIVE) != active || (active && level.getGameTime() % 20 == 0)) {
            // чистая комната (006): пуск/останов меняет генерацию частиц; раз в секунду —
            // перерегистрация после перезапуска мира
            org.alex_melan.spacereloaded.electronics.CleanroomTracker.machine(level, pos,
                    active ? particleRate(state) : 0);
        }
        if (state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        if (active && level.getGameTime() % 10 == Math.floorMod(pos.asLong(), 10)) {
            effects(level, pos, state);
        }
    }

    /** Генерация частиц работающей машиной, 1/мин: дробилка пылит, остальные почти нет. */
    public static double particleRate(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return switch (id) {
            case "crusher", "coal_generator" -> org.alex_melan.spacereloaded.electronics.CleanroomTracker.DUSTY_MACHINE;
            default -> org.alex_melan.spacereloaded.electronics.CleanroomTracker.MACHINE;
        };
    }

    private static void particles(ServerLevel level, ParticleOptions type, double x, double y, double z, int count,
                                  double spread, double speed) {
        level.sendParticles(type, x, y, z, count, spread, spread * 0.5, spread, speed);
    }

    private static void sound(ServerLevel level, BlockPos pos, SoundEvent event, float volume, float pitch) {
        if (level.getRandom().nextFloat() < 0.35f) {
            level.playSound(null, pos, event, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private static void effects(ServerLevel level, BlockPos pos, BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        double x = pos.getX() + 0.5;
        double top = pos.getY() + 1.05;
        double z = pos.getZ() + 0.5;
        Direction face = state.hasProperty(ProcessingMachineBlock.FACING)
                ? state.getValue(ProcessingMachineBlock.FACING) : Direction.NORTH;
        double fx = x + face.getStepX() * 0.55;
        double fz = z + face.getStepZ() * 0.55;
        switch (id) {
            case "crusher" -> {
                particles(level, ParticleTypes.ASH, fx, pos.getY() + 0.4, fz, 6, 0.2, 0.02);
                sound(level, pos, SoundEvents.GRINDSTONE_USE, 0.5f, 0.7f);
            }
            case "electric_furnace" -> {
                particles(level, ParticleTypes.SMOKE, x, top, z, 2, 0.15, 0.01);
                sound(level, pos, SoundEvents.FURNACE_FIRE_CRACKLE, 0.4f, 1.3f);
            }
            case "coal_generator" -> {
                particles(level, ParticleTypes.LARGE_SMOKE, x, top, z, 2, 0.1, 0.02);
                particles(level, ParticleTypes.FLAME, fx, pos.getY() + 0.3, fz, 2, 0.12, 0.0);
                sound(level, pos, SoundEvents.FURNACE_FIRE_CRACKLE, 0.6f, 1.0f);
            }
            case "electrolyzer" -> {
                particles(level, ParticleTypes.BUBBLE_POP, x, top, z, 4, 0.25, 0.01);
                sound(level, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.3f, 1.4f);
            }
            case "refinery" -> {
                particles(level, ParticleTypes.CLOUD, x, top, z, 2, 0.15, 0.01);
                sound(level, pos, SoundEvents.LAVA_POP, 0.25f, 0.6f);
            }
            case "assembly_table" -> {
                particles(level, ParticleTypes.ELECTRIC_SPARK, x, top, z, 5, 0.3, 0.05);
                sound(level, pos, SoundEvents.SMITHING_TABLE_USE, 0.3f, 1.2f);
            }
            case "atmospheric_collector" -> particles(level, ParticleTypes.CLOUD, fx, pos.getY() + 0.5, fz, 2, 0.1, 0.0);
            case "sabatier_reactor" -> {
                particles(level, ParticleTypes.WHITE_SMOKE, x, top, z, 3, 0.15, 0.02);
                sound(level, pos, SoundEvents.FIRE_AMBIENT, 0.3f, 0.8f);
            }
            default -> {
            }
        }
    }
}
