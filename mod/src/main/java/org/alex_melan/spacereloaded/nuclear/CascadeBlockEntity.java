package org.alex_melan.spacereloaded.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.nuclear.Enrichment;
import org.alex_melan.spacereloaded.core.nuclear.PointKinetics;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.industry.IndustryStructures;
import org.alex_melan.spacereloaded.lifesupport.EnergyScale;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.FormedStructure;
import org.alex_melan.spacereloaded.multiblock.HammerTarget;
import org.alex_melan.spacereloaded.multiblock.StatusProvider;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Каскад газовых центрифуг (008, US3, D84): обогатительная часть симметричного каскада, число
 * центрифуг N = число ступеней — продукт {@link Enrichment#product}(N, x_f, α = 1.3). Работа
 * разделения копится по энергии (50 кВт·ч на ЕРР — современные центрифуги; наработка ускорена:
 * 20 ЕРР на центрифугу в игровые сутки), на каждый килограмм продукта списывается питание по балансу
 * F = P·(x_p − x_w)/(x_f − x_w), остальное — отвал 0.25 %. Уран учитывается в кг U, предметы UF₆ —
 * порциями по 10 кг. Корзина реактора изготавливается здесь же: продукт + цирконий (сплав U-Zr).
 */
public class CascadeBlockEntity extends MachineBlockEntity
        implements HammerTarget, StatusProvider, org.alex_melan.spacereloaded.multiblock.ControllerBlock.ItemAcceptor,
        IndustryStructures.StructureOwner {

    public static final double KG_PER_ITEM = 10;
    public static final double SWU_PER_DAY = 20;
    public static final double KWH_PER_SWU = 50;
    public static final double BASKET_U235_KG = 28;
    /** Цирконий в сплаве U-Zr — 10 % массы урана; предмет циркония — 7.2 кг. */
    public static final double ZR_FRACTION = 0.10;
    public static final double ZR_KG_PER_ITEM = 7.2;

    private final FormedStructure structure = new FormedStructure();
    private boolean claimed;
    private double feedKg;
    private double feedU235;
    private double productKg;
    private double productU235;
    private double tailsKg;
    private double swu;
    private double energyDebt;
    private boolean running;
    public Object clientAnim;

    public CascadeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASCADE, pos, state, 50_000, 5_000, 0);
    }

    private Direction face() {
        return getBlockState().getValue(ControllerBlock.FACING);
    }

    public int centrifuges() {
        return structure.formed() ? structure.repeats() : 0;
    }

    public boolean running() {
        return running;
    }

    public boolean formed() {
        return getBlockState().getValue(ControllerBlock.FORMED);
    }

    public double feedEnrichment() {
        return feedKg > 1e-9 ? feedU235 / feedKg : Enrichment.NATURAL;
    }

    public double productEnrichment() {
        return Enrichment.product(Math.max(0, centrifuges()), feedEnrichment(), Enrichment.ALPHA);
    }

    public double productKg() {
        return productKg;
    }

    public double productStockEnrichment() {
        return productKg > 1e-9 ? productU235 / productKg : 0;
    }

    public double tailsKg() {
        return tailsKg;
    }

    public double feedKg() {
        return feedKg;
    }

    @Override
    public void markStructureDirty() {
        structure.markDirty();
    }

    @Override
    public boolean hammer(ServerLevel level, ServerPlayer player) {
        boolean ok = structure.hammer(level, getBlockPos(), face(), player);
        setFormed(level);
        return ok;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            structure.dismantle(serverLevel, pos, state.getValue(ControllerBlock.FACING), state.getBlock());
        }
        super.preRemoveSideEffects(pos, state);
    }

    public static void serverTick(CascadeBlockEntity be, ServerLevel level) {
        be.tick(level);
    }

    private void tick(ServerLevel level) {
        if (!claimed) {
            claimed = true;
            structure.reclaim(level, getBlockPos(), face());
        }
        if (structure.revalidate(level, getBlockPos(), face())) {
            setChanged();
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);
        setFormed(level);
        int n = centrifuges();
        boolean was = running;
        running = false;
        if (n > 0 && feedKg > 0.5) {
            double xf = feedEnrichment();
            double xp = productEnrichment();
            double dSwu = n * SWU_PER_DAY / 1200.0;
            energyDebt += EnergyScale.fromJoules(dSwu * KWH_PER_SWU * 3.6e6);
            long cost = (long) Math.floor(energyDebt);
            if (energy.amount >= cost) {
                energy.amount -= cost;
                energyDebt -= cost;
                swu += dSwu;
                running = true;
                double perKg = Enrichment.swu(1, xf, xp, Enrichment.TAILS);
                double feedPerKg = 1 / Enrichment.productPerFeed(xf, xp, Enrichment.TAILS);
                while (swu >= perKg && feedKg >= feedPerKg) {
                    swu -= perKg;
                    feedKg -= feedPerKg;
                    feedU235 -= xp + (feedPerKg - 1) * Enrichment.TAILS;
                    productKg += 1;
                    productU235 += xp;
                    tailsKg += feedPerKg - 1;
                }
                feedU235 = Math.max(0, feedU235);
            } else {
                energyDebt = Math.min(energyDebt, cost);
            }
        }
        if (running && level.getGameTime() % 80 == 0) {
            level.playSound(null, getBlockPos(), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.8f);
        }
        if (was != running) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.ACTIVE, running), Block.UPDATE_CLIENTS);
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    private void setFormed(ServerLevel level) {
        boolean formed = structure.formed();
        if (getBlockState().getValue(ControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean accept(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.URANIUM_HEXAFLUORIDE)) {
            return false;
        }
        float x = stack.getOrDefault(ModDataComponents.ENRICHMENT, (float) Enrichment.NATURAL);
        int count = stack.getCount();
        feedKg += count * KG_PER_ITEM;
        feedU235 += count * KG_PER_ITEM * x;
        stack.shrink(count);
        setChanged();
        return true;
    }

    private ItemStack hexafluoride(int items, double enrichment) {
        ItemStack s = new ItemStack(ModItems.URANIUM_HEXAFLUORIDE, items);
        s.set(ModDataComponents.ENRICHMENT, (float) enrichment);
        return s;
    }

    @Override
    public void action(ServerLevel level, ServerPlayer player, String action, double value) {
        switch (action) {
            case "product" -> {
                int items = (int) Math.floor(productKg / KG_PER_ITEM);
                if (items > 0) {
                    double x = productStockEnrichment();
                    give(player, hexafluoride(items, x));
                    productKg -= items * KG_PER_ITEM;
                    productU235 -= items * KG_PER_ITEM * x;
                }
            }
            case "tails" -> {
                int items = (int) Math.floor(tailsKg / KG_PER_ITEM);
                if (items > 0) {
                    give(player, new ItemStack(ModItems.DEPLETED_URANIUM_HEXAFLUORIDE, items));
                    tailsKg -= items * KG_PER_ITEM;
                }
            }
            case "basket" -> makeBasket(player);
            default -> {
            }
        }
        setChanged();
    }

    /** Корзина: продукт на 28 кг U-235 (или весь, если меньше) + цирконий 10 % массы урана. */
    private void makeBasket(ServerPlayer player) {
        double x = productStockEnrichment();
        if (productKg < 1 || x <= 0) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.cascade.no_product"));
            return;
        }
        double uKg = Math.min(productKg, BASKET_U235_KG / x);
        int zrItems = (int) Math.ceil(uKg * ZR_FRACTION / ZR_KG_PER_ITEM);
        var inv = player.getInventory();
        int have = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.ZIRCONIUM)) {
                have += inv.getItem(i).getCount();
            }
        }
        if (have < zrItems) {
            player.sendSystemMessage(Component.translatable("message.spacereloaded.cascade.need_zirconium", zrItems));
            return;
        }
        int left = zrItems;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.ZIRCONIUM)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        double u235 = uKg * x;
        productKg -= uKg;
        productU235 -= u235;
        give(player, ReactorBlockEntity.basket(u235, (float) x));
        player.sendSystemMessage(Component.translatable(u235 > PointKinetics.CRITICAL_MASS_KG
                        ? "message.spacereloaded.cascade.basket" : "message.spacereloaded.cascade.basket_subcritical",
                String.format(Locale.ROOT, "%.1f", u235), String.format(Locale.ROOT, "%.1f", x * 100),
                String.format(Locale.ROOT, "%.2f", PointKinetics.fuelExcess(u235))));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public MachineStatusPayload status(ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        int n = centrifuges();
        if (!structure.formed()) {
            lines.add(Component.translatable("status.spacereloaded.cascade.not_formed").withColor(0xE0B23C));
        }
        double xf = feedEnrichment();
        double xp = productEnrichment();
        lines.add(Component.translatable("status.spacereloaded.cascade.stages", n, pct(xf), pct(xp), pct(Enrichment.TAILS)));
        double perKg = n > 0 ? Enrichment.swu(1, xf, xp, Enrichment.TAILS) : 0;
        double feedPer = n > 0 ? 1 / Enrichment.productPerFeed(xf, xp, Enrichment.TAILS) : 0;
        lines.add(Component.translatable("status.spacereloaded.cascade.balance", f1(feedPer), f1(perKg)));
        double perDay = n > 0 ? n * SWU_PER_DAY / perKg : 0;
        lines.add(Component.translatable("status.spacereloaded.cascade.rate", f2(perDay), f1(n * SWU_PER_DAY)));
        lines.add(Component.translatable("status.spacereloaded.cascade.stock", f1(feedKg), f1(productKg),
                pct(productStockEnrichment()), f1(tailsKg)));
        List<MachineStatusPayload.Gauge> gauges = List.of(
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.enrichment"), (float) xp, 0x57C4C4),
                new MachineStatusPayload.Gauge(Component.translatable("gauge.spacereloaded.swu"),
                        (float) (perKg > 0 ? swu / perKg : 0), 0x8FE0A8));
        List<MachineStatusPayload.Action> actions = List.of(
                new MachineStatusPayload.Action("product", Component.translatable("action.spacereloaded.cascade.product"), 0),
                new MachineStatusPayload.Action("tails", Component.translatable("action.spacereloaded.cascade.tails"), 0),
                new MachineStatusPayload.Action("basket", Component.translatable("action.spacereloaded.cascade.basket"), 0));
        return new MachineStatusPayload(getBlockPos(), Component.translatable("screen.spacereloaded.cascade"), lines, gauges,
                actions);
    }

    private static String pct(double x) {
        return String.format(Locale.ROOT, "%.2f", x * 100);
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Стенд: питание и накопленная работа разделения. */
    public void testFeed(double kgU, double enrichment, double swuUnits) {
        feedKg += kgU;
        feedU235 += kgU * enrichment;
        swu += swuUnits;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        structure.save(output);
        output.putDouble("feed", feedKg);
        output.putDouble("feed235", feedU235);
        output.putDouble("product", productKg);
        output.putDouble("product235", productU235);
        output.putDouble("tails", tailsKg);
        output.putDouble("swu", swu);
        output.putBoolean("running", running);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        structure.load(input);
        claimed = false;
        feedKg = input.getDoubleOr("feed", 0);
        feedU235 = input.getDoubleOr("feed235", 0);
        productKg = input.getDoubleOr("product", 0);
        productU235 = input.getDoubleOr("product235", 0);
        tailsKg = input.getDoubleOr("tails", 0);
        swu = input.getDoubleOr("swu", 0);
        running = input.getBooleanOr("running", false);
    }
}
