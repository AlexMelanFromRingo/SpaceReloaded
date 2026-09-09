package org.alex_melan.spacereloaded.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.station.WetWorkshopPlanner;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.rocket.RocketData;
import org.alex_melan.spacereloaded.rocket.RocketEntity;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wet workshop — адаптер конверсии (003, FR-121…FR-126, D26): припаркованный
 * борт, у которого стенка бака/корпуса прилегает к передней грани порта,
 * становится модулем станции из блоков по плану {@link WetWorkshopPlanner}.
 * Порядок: проверки (без мутаций мира) → груз наружу → топливо стравлено →
 * блоки → люк → сущность удалена. Работает в любом измерении: на орбите —
 * модуль станции, на Марсе — посадочный модуль как база.
 */
public final class WetWorkshop {

    /** Радиус поиска борта от передней клетки порта, блоки. */
    private static final double CAPTURE_RADIUS = 3.0;

    private WetWorkshop() {
    }

    public record Result(boolean converted, Component message) {
    }

    public static Component convert(ServerLevel level, BlockPos portPos, Direction facing) {
        return tryConvert(level, portPos, facing).message();
    }

    public static Result tryConvert(ServerLevel level, BlockPos portPos, Direction facing) {
        BlockPos front = portPos.relative(facing);
        List<RocketEntity> candidates = level.getEntities(EntityTypeTest.forClass(RocketEntity.class),
                new AABB(front).inflate(CAPTURE_RADIUS),
                rocket -> rocket.isParked() && !rocket.isDebris());
        if (candidates.isEmpty()) {
            return fail("message.spacereloaded.workshop.no_craft");
        }
        RocketEntity craft = null;
        WetWorkshopPlanner.Plan plan = null;
        BlockPos base = null;
        long hatchCell = 0;
        for (RocketEntity candidate : candidates) {
            RocketData data = candidate.rocketDataForDocking();
            WetWorkshopPlanner.Plan candidatePlan = WetWorkshopPlanner.plan(data.toStructure());
            BlockPos candidateBase = baseOf(candidate);
            var hatch = WetWorkshopPlanner.hatchCell(candidatePlan,
                    front.getX() - candidateBase.getX(), front.getY() - candidateBase.getY(),
                    front.getZ() - candidateBase.getZ());
            if (hatch.isPresent()) {
                craft = candidate;
                plan = candidatePlan;
                base = candidateBase;
                hatchCell = hatch.getAsLong();
                break;
            }
        }
        if (craft == null) {
            return fail("message.spacereloaded.workshop.no_wall");
        }
        if (!craft.getPassengers().isEmpty()) {
            return fail("message.spacereloaded.workshop.crew_aboard");
        }
        double capacity = craft.clientFuelCapacityKg();
        double residual = craft.propellantKg();
        double maxResidual = capacity * SpaceReloaded.config().wetWorkshopMaxResidualFraction;
        if (residual > maxResidual + 1e-6) {
            return new Result(false, Component.translatable("message.spacereloaded.workshop.drain_first",
                    fmt(residual), fmt(maxResidual)));
        }
        // Все клетки-цели должны быть свободны (борт — сущность, но под ним может лежать чужой блок)
        Map<Long, WetWorkshopPlanner.Kind> index = WetWorkshopPlanner.index(plan);
        RocketData data = craft.rocketDataForDocking();
        for (RocketData.Entry entry : data.blocks()) {
            BlockPos target = worldPos(base, entry.localPos());
            if (!level.getBlockState(target).isAir()) {
                return new Result(false, Component.translatable("message.spacereloaded.workshop.blocked",
                        target.toShortString()));
            }
        }

        // Груз — наружу у порта (честно, ничего не пропадает)
        int cargoDropped = 0;
        ItemStack stack;
        while (!(stack = craft.unloadCargo()).isEmpty()) {
            cargoDropped += stack.getCount();
            Block.popResource(level, front, stack);
        }
        // Блоки по плану
        int shell = 0;
        int interior = 0;
        int keep = 0;
        for (RocketData.Entry entry : data.blocks()) {
            BlockPos target = worldPos(base, entry.localPos());
            WetWorkshopPlanner.Kind kind = index.getOrDefault(entry.localPos(), WetWorkshopPlanner.Kind.KEEP);
            BlockState placed;
            if (entry.localPos() == hatchCell) {
                placed = ModBlocks.HERMETIC_HATCH.defaultBlockState();
            } else {
                placed = switch (kind) {
                    case SHELL -> ModBlocks.MODULE_HULL.defaultBlockState();
                    case INTERIOR -> net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    case KEEP -> entry.state();
                };
            }
            if (entry.localPos() != hatchCell) {
                switch (kind) {
                    case SHELL -> shell++;
                    case INTERIOR -> interior++;
                    case KEEP -> keep++;
                }
            }
            if (!placed.isAir()) {
                level.setBlock(target, placed, 3);
            }
            ZoneManager.markBlockChanged(level, target);
        }
        craft.ejectPassengers();
        craft.discard();
        level.playSound(null, front, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 0.6f);
        SpaceReloaded.LOGGER.info("Wet workshop у {}: оболочка {}, объём {}, сохранено {}, стравлено {} кг, груз {} предм.",
                portPos.toShortString(), shell, interior, keep, Math.round(residual), cargoDropped);
        return new Result(true, Component.translatable("message.spacereloaded.workshop.converted",
                shell, interior, keep, fmt(residual), cargoDropped));
    }

    private static Result fail(String key) {
        return new Result(false, Component.translatable(key));
    }

    /** Мировая позиция локальной клетки борта (как при разборке в блоки). */
    private static BlockPos baseOf(RocketEntity rocket) {
        return new BlockPos((int) Math.round(rocket.getX() - rocket.halfX()),
                (int) Math.round(rocket.getY()),
                (int) Math.round(rocket.getZ() - rocket.halfZ()));
    }

    private static BlockPos worldPos(BlockPos base, long local) {
        return base.offset(PackedPos.unpackX(local), PackedPos.unpackY(local), PackedPos.unpackZ(local));
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
