package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.industry.CatcherOdds;
import org.alex_melan.spacereloaded.network.SpaceNetworkState;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModSounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ловушка масс (004, US2, D35): инвентарь 27, сетка-уловитель — связная по граням группа
 * секций в плоскости ловушки. Радиус захвата r = min(r₀ + k·√n, r_max). Работает только
 * на орбитальной платформе (arrival = platform) — там, куда катапульта целится по таблице
 * перелётов; флаг конфига стенда снимает ограничение.
 */
public class MassCatcherBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, IndustryStructures.StructureOwner {

    public static final int SLOTS = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int netBlocks;
    private boolean dirty = true;
    private int caught;
    private int lost;
    private double lastMiss = -1;

    public MassCatcherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MASS_CATCHER, pos, state);
    }

    @Override
    public void markStructureDirty() {
        dirty = true;
    }

    public static void serverTick(MassCatcherBlockEntity catcher, ServerLevel level) {
        if (catcher.dirty) {
            catcher.dirty = false;
            catcher.rescan(level);
        }
    }

    private void rescan(ServerLevel level) {
        int limit = SpaceReloaded.config().catcherNetMaxBlocks;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> net = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = getBlockPos().relative(d);
            if (level.getBlockState(n).is(ModBlocks.CATCHER_NET) && seen.add(n)) {
                queue.add(n);
            }
        }
        while (!queue.isEmpty() && net.size() < limit) {
            BlockPos cur = queue.poll();
            net.add(cur);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = cur.relative(d);
                if (!n.equals(getBlockPos()) && level.getBlockState(n).is(ModBlocks.CATCHER_NET) && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        netBlocks = net.size();
        Set<BlockPos> claimed = new HashSet<>();
        claimed.add(getBlockPos());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            claimed.add(getBlockPos().relative(d));
        }
        for (BlockPos cell : net) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                claimed.add(cell.relative(d));
            }
            claimed.add(cell);
        }
        IndustryStructures.claim(level, getBlockPos(), claimed);
        setChanged();
    }

    /** Ловушка валидна как цель: орбитальная платформа (или флаг стенда). */
    public boolean operational(ServerLevel level) {
        if (SpaceReloaded.config().massCatcherAnyDimension) {
            return true;
        }
        return PlanetManager.profileFor(level).map(p -> "platform".equals(p.arrival())).orElse(false);
    }

    public int netBlocks() {
        return netBlocks;
    }

    public double captureRadius() {
        SpaceReloadedConfig config = SpaceReloaded.config();
        return CatcherOdds.captureRadius(netBlocks, config.catcherBaseRadius, config.catcherRadiusPerSqrtNet,
                config.catcherMaxRadius);
    }

    /** Приём: всё в инвентарь, излишек — рядом; визуал и звук. */
    public void accept(ServerLevel level, List<ItemStack> incoming) {
        for (ItemStack stack : incoming) {
            ItemStack rest = insert(stack);
            if (!rest.isEmpty()) {
                Block.popResource(level, getBlockPos().above(), rest);
            }
        }
        caught++;
        lastMiss = 0;
        setChanged();
        CargoPodEntity.arrive(level, Vec3.atCenterOf(getBlockPos().above()));
        level.playSound(null, getBlockPos(), ModSounds.CATCHER_CATCH, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    private ItemStack insert(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < SLOTS && !remaining.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                items.set(slot, remaining);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int move = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (move > 0) {
                    existing.grow(move);
                    remaining.shrink(move);
                }
            }
        }
        return remaining;
    }

    public void recordLoss(double miss, double radius) {
        lost++;
        lastMiss = miss;
        setChanged();
    }

    public int caught() {
        return caught;
    }

    public int lost() {
        return lost;
    }

    public List<Component> report(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        boolean covered = SpaceNetworkState.get(level.getServer()).hasCoverage(level.dimension());
        double sigma = covered ? config.podSigmaCovered : config.podSigmaUncovered;
        List<Component> lines = new ArrayList<>();
        if (!operational(level)) {
            lines.add(Component.translatable("message.spacereloaded.mass_catcher.orbit_only"));
        }
        lines.add(Component.translatable("message.spacereloaded.mass_catcher.status",
                String.format(Locale.ROOT, "%.1f", captureRadius()), netBlocks,
                Component.translatable(covered ? "message.spacereloaded.mass_catcher.covered"
                        : "message.spacereloaded.mass_catcher.uncovered"),
                String.format(Locale.ROOT, "%.0f", 100 * CatcherOdds.captureProbability(captureRadius(), sigma))));
        lines.add(Component.translatable("message.spacereloaded.mass_catcher.counters", caught, lost,
                lastMiss < 0 ? "—" : String.format(Locale.ROOT, "%.1f", lastMiss)));
        return lines;
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            IndustryStructures.release(serverLevel, getBlockPos());
        }
        super.setRemoved();
    }

    // ---------------- контейнер ----------------

    @Override
    protected net.minecraft.network.chat.Component getDefaultName() {
        return Component.translatable("block.spacereloaded.mass_catcher");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    private static final int[] ALL = java.util.stream.IntStream.range(0, SLOTS).toArray();

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("caught", caught);
        output.putInt("lost", lost);
        output.putDouble("last_miss", lastMiss);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        caught = input.getIntOr("caught", 0);
        lost = input.getIntOr("lost", 0);
        lastMiss = input.getDoubleOr("last_miss", -1);
        dirty = true;
    }
}
