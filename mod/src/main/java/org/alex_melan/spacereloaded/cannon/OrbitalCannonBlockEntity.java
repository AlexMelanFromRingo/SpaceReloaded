package org.alex_melan.spacereloaded.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.ballistics.BallisticIntegrator;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.planet.ModTickets;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModEntities;

import org.jetbrains.annotations.Nullable;

/**
 * Орбитальная кинетическая пушка (US7, T080–T083): работает только на орбите
 * (профиль планеты arrival=platform), заряжается вольфрамовыми ломами,
 * питается от кабельной сети. Выстрел спавнит {@link KineticProjectileEntity}
 * в ЦЕЛЕВОМ измерении на входе в атмосферу — честный межпространственный
 * удар с упреждающей загрузкой чанков цели (FR-043, принцип V).
 */
public class OrbitalCannonBlockEntity extends MachineBlockEntity {

    private int rods;
    @Nullable
    private GlobalPos target;
    // НЕ Long.MIN_VALUE: getGameTime() - MIN_VALUE переполняется в отрицательное
    // и свежепоставленная пушка вечно «перезаряжается»
    private long lastFireGameTime = -1_000_000L;

    public OrbitalCannonBlockEntity(BlockPos pos, BlockState state) {
        super(org.alex_melan.spacereloaded.registry.ModBlockEntities.ORBITAL_CANNON, pos, state,
                SpaceReloaded.config().cannonEnergyCapacity, 1024, 0);
    }

    public int rods() {
        return rods;
    }

    /** Зарядить один лом из руки; true, если принят. */
    public boolean loadRod() {
        if (rods >= SpaceReloaded.config().cannonMaxRods) {
            return false;
        }
        rods++;
        setChanged();
        return true;
    }

    /** Навести по метке целеуказателя. */
    public void setTarget(GlobalPos target) {
        this.target = target;
        setChanged();
    }

    @Nullable
    public GlobalPos target() {
        return target;
    }

    /**
     * Выстрел. Проверки честные: орбита, цель, лом, энергия. Снаряд входит
     * в атмосферу над целью с дульной скоростью вниз — дальше только физика.
     *
     * @return сообщение игроку (статус или причина отказа)
     */
    public Component tryFire(ServerLevel level) {
        var config = SpaceReloaded.config();
        boolean inOrbit = PlanetManager.profileFor(level)
                .map(profile -> "platform".equals(profile.arrival()))
                .orElse(false);
        if (!inOrbit) {
            return Component.translatable("message.spacereloaded.cannon.not_in_orbit");
        }
        if (target == null) {
            return Component.translatable("message.spacereloaded.cannon.no_target");
        }
        if (rods <= 0) {
            return Component.translatable("message.spacereloaded.cannon.no_rods");
        }
        if (energy.amount < config.cannonEnergyPerShot) {
            return Component.translatable("message.spacereloaded.cannon.no_energy",
                    energy.amount, config.cannonEnergyPerShot);
        }
        if (level.getGameTime() - lastFireGameTime < config.cannonCooldownTicks) {
            return Component.translatable("message.spacereloaded.cannon.cooldown");
        }
        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) {
            return Component.translatable("message.spacereloaded.cannon.no_target");
        }

        rods--;
        energy.amount -= config.cannonEnergyPerShot;
        lastFireGameTime = level.getGameTime();
        setChanged();

        BlockPos aim = target.pos();
        // Упреждающая загрузка чанков цели ДО спавна снаряда (FR-043):
        // persist + keep-dimension-active — измерение без игроков тикает
        ModTickets.holdStrike(targetLevel, aim, 2);

        double spawnY = aim.getY() + config.cannonDropAltitude;
        KineticProjectileEntity projectile = new KineticProjectileEntity(
                ModEntities.KINETIC_PROJECTILE, targetLevel);
        projectile.setPos(aim.getX() + 0.5, spawnY, aim.getZ() + 0.5);
        projectile.configure(config.cannonRodMassKg, config.cannonDragCoeff,
                new Vec3(0, -config.cannonMuzzleSpeed, 0), aim);
        targetLevel.addFreshEntity(projectile);

        // Предупреждение внизу: гром за секунды до удара (FR-044)
        targetLevel.playSound(null, aim, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 8.0f, 0.6f);
        level.playSound(null, getBlockPos(), SoundEvents.WITHER_SHOOT,
                SoundSource.BLOCKS, 3.0f, 0.4f);

        double eta = BallisticIntegrator.etaToAltitude(spawnY, -config.cannonMuzzleSpeed,
                aim.getY(), PlanetManager.gravity(targetLevel));
        String etaText = Double.isNaN(eta) ? "?" : String.valueOf(Math.round(eta));
        SpaceReloaded.LOGGER.info("Пушка {}: выстрел по {} в {}, подлёт ~{} с",
                getBlockPos(), aim, target.dimension().identifier(), etaText);
        return Component.translatable("message.spacereloaded.cannon.fired",
                aim.getX(), aim.getY(), aim.getZ(), etaText);
    }

    /** Статус для ПКМ без предметов, когда стрелять нельзя/нечем. */
    public Component status() {
        var config = SpaceReloaded.config();
        String targetText = target == null ? "—"
                : target.pos().toShortString() + " @ " + target.dimension().identifier();
        return Component.translatable("message.spacereloaded.cannon.status",
                rods, config.cannonMaxRods, energy.amount, config.cannonEnergyCapacity, targetText);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("rods", rods);
        if (target != null) {
            output.putString("target_dim", target.dimension().identifier().toString());
            output.putLong("target_pos", target.pos().asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rods = input.getIntOr("rods", 0);
        String dim = input.getStringOr("target_dim", "");
        if (!dim.isEmpty()) {
            target = GlobalPos.of(
                    ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim)),
                    BlockPos.of(input.getLongOr("target_pos", 0L)));
        }
    }

    /** Снос блока: заряженные ломы — честные предметы, выбрасываем (не сжигаем). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && rods > 0) {
            net.minecraft.world.level.block.Block.popResource(level, pos,
                    new net.minecraft.world.item.ItemStack(
                            org.alex_melan.spacereloaded.registry.ModItems.TUNGSTEN_ROD, rods));
            rods = 0;
        }
    }

    /** Тикер: как у станков — присоединяемся к кабельной сети рядом. */
    public static void serverTick(OrbitalCannonBlockEntity cannon, ServerLevel level) {
        if (cannon.level != null && cannon.level.getGameTime() % 40 == 0) {
            cannon.ensureAdjacentCableNetworks(level);
        }
    }
}
