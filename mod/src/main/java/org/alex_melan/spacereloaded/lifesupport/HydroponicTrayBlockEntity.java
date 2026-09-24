package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere;
import org.alex_melan.spacereloaded.core.lifesupport.CropModel;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ItemMasses;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.sealing.SealedZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Гидропонный лоток 1 м² (007, FR-510…FR-515, D75). Культура — профиль датапака (NASA BVAD).
 * Раз в секунду: свет (фитолампа над лотком или небо через гермостекло днём), pCO₂ зоны
 * (фотосинтез насыщается выше 0.12 кПа, ниже — линеен), удобрение и вода — эффективная доля
 * номинала r; накопленный рост += r·Δt (игровые сутки). Вклад в газ зоны сообщается как приток
 * (насыщение) или объёмное удаление (линейный режим) — согласованно с ростом.
 * <p>
 * Урожай в момент сбора: съедобная биомасса = P·∫r dt (честная суточная продуктивность BPC),
 * солома = несъедобная доля. Первый сбор — через цикл/10 (цикл культуры сжат, суточная норма нет);
 * культура остаётся посаженной — непрерывный поток, как в ступенчатых посевах BPC.
 */
public class HydroponicTrayBlockEntity extends BlockEntity {

    /** Сжатие цикла созревания (суточная продуктивность не меняется). */
    public static final double CYCLE_COMPRESSION = 10;
    /** Солнце на Земле, ясный летний день, моль/м²·сут (**оценка**) и пропускание гермостекла. */
    public static final double EARTH_DLI = 45;
    public static final double GLASS = 0.9;
    /** Удобрения: 1 костная мука = 100 м²·сут (≈ 6.5 г солей на м² в сутки, BVAD; >50 % — из рецикла). */
    public static final double FERTILIZER_DAYS = 100;
    public static final double BUCKET_KG = 1000;

    private Identifier cropId;
    private double growth;
    private double fertilizer;
    private double water;
    private double lastDay = -1;
    private double rate;

    public HydroponicTrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDROPONIC_TRAY, pos, state);
    }

    public CropProfiles.Profile profile() {
        return cropId == null || level == null ? null : CropProfiles.byId(level.registryAccess(), cropId);
    }

    public boolean plant(ItemStack seed) {
        if (cropId != null || level == null) {
            return false;
        }
        CropProfiles.Profile p = CropProfiles.bySeed(level.registryAccess(), seed);
        if (p == null) {
            return false;
        }
        cropId = CropProfiles.idOf(level.registryAccess(), p);
        growth = 0;
        lastDay = -1;
        seed.shrink(1);
        changed();
        return true;
    }

    public void fertilize(ItemStack boneMeal) {
        boneMeal.shrink(1);
        fertilizer += FERTILIZER_DAYS;
        setChanged();
    }

    public void water() {
        water += BUCKET_KG;
        setChanged();
    }

    public double rate() {
        return rate;
    }

    public double growth() {
        return growth;
    }

    public double fertilizer() {
        return fertilizer;
    }

    public double waterKg() {
        return water;
    }

    /** Стенд и витрина: рост до доли цикла (0…1). */
    public void testGrow(double fraction) {
        CropProfiles.Profile p = profile();
        if (p != null) {
            growth = fraction * p.cycleDays() / CYCLE_COMPRESSION;
            changed();
        }
    }

    public boolean mature() {
        CropProfiles.Profile p = profile();
        return p != null && growth >= p.cycleDays() / CYCLE_COMPRESSION;
    }

    /** Свет лотка сейчас (доля номинала, мгновенная): лампа — номинал, небо днём — 2× среднесуточного. */
    private double light(ServerLevel level, CropModel.Crop crop) {
        if (level.getBlockEntity(worldPosition.above()) instanceof GrowLampBlockEntity lamp && lamp.power(crop.lampWattsPerM2())) {
            return 1;
        }
        if (!level.canSeeSky(worldPosition.above())) {
            return 0;
        }
        double solar = PlanetManager.profileFor(level).map(p -> p.solarEfficiency()).orElse(1.0);
        double day = level.getDefaultClockTime() % 24000L < 12000 ? 2 : 0; // половину суток светло
        return day * crop.lightFactor(EARTH_DLI * solar * GLASS);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, HydroponicTrayBlockEntity tray) {
        if (level.getGameTime() % 20 == 0) {
            tray.tick(level);
        }
    }

    private void tick(ServerLevel level) {
        CropProfiles.Profile p = profile();
        double now = LifeSupportState.days(level);
        double dt = lastDay < 0 ? 0 : Math.max(0, now - lastDay);
        lastDay = now;
        if (p == null) {
            rate = 0;
            LifeSupportState.contribute(level, worldPosition, LifeSupportState.Contribution.NONE);
            return;
        }
        CropModel.Crop crop = p.crop();
        SealedZone zone = LifeSupportState.zoneAround(level, worldPosition);
        LifeSupportState.Gas gas = zone == null || !zone.isSealed() ? null : LifeSupportState.now(level, zone);
        double pCo2 = gas == null ? (LifeSupportState.ambientKpa(level) > 0 ? 0.04 : 0) : gas.pCo2();
        boolean air = gas != null ? gas.pressure() >= CrewHazard.VACUUM_KPA : LifeSupportState.ambientKpa(level) > 0;
        double light = air && fertilizer > 0 && water > 0 ? light(level, crop) : 0;
        double co2Factor = CropModel.co2Factor(pCo2);
        rate = light * co2Factor;
        growth += rate * dt;
        if (rate > 0) {
            fertilizer = Math.max(0, fertilizer - dt);
            water = Math.max(0, water - crop.edible() / 1000 * p.freshFactor() * rate * dt);
        }
        // газообмен зоны согласован с ростом
        LifeSupportState.Contribution c = LifeSupportState.Contribution.NONE;
        if (zone != null && light > 0) {
            double uptake = crop.co2() / 1000 * light;
            double oxygen = crop.o2() / 1000 * light;
            if (pCo2 >= CropModel.CO2_SATURATION_KPA) {
                c = new LifeSupportState.Contribution(oxygen, -uptake, 0, 0);
            } else {
                double removal = uptake / CabinAtmosphere.co2Density(CropModel.CO2_SATURATION_KPA);
                c = new LifeSupportState.Contribution(0, 0, removal, oxygen / uptake * removal);
            }
        }
        LifeSupportState.contribute(level, worldPosition, c);
        changed();
    }

    /** Собрать урожай (если созрел): съедобное и солома по интегралу роста. */
    public List<ItemStack> harvest(ServerLevel level) {
        List<ItemStack> out = new ArrayList<>();
        CropProfiles.Profile p = profile();
        if (p == null || !mature()) {
            return out;
        }
        CropModel.Crop crop = p.crop();
        double fresh = crop.edible() * growth / 1000 * p.freshFactor();
        add(out, level, p.harvestItem(), fresh);
        add(out, level, p.byproductItem(), crop.inedible() * growth / 1000);
        growth = 0;
        changed();
        return out;
    }

    private static void add(List<ItemStack> out, ServerLevel level, Item item, double kg) {
        double unit = ItemMasses.massOf(level.registryAccess(), new ItemStack(item));
        int count = (int) Math.floor(kg / unit + 1e-9);
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            out.add(new ItemStack(item, n));
            count -= n;
        }
    }

    public ItemStack uproot() {
        CropProfiles.Profile p = profile();
        if (p == null) {
            return ItemStack.EMPTY;
        }
        ItemStack seed = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(p.seed()));
        cropId = null;
        growth = 0;
        changed();
        return seed;
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            CropProfiles.Profile p = profile();
            BlockState state = getBlockState();
            int stage = p == null ? 0 : (int) Math.min(3, Math.floor(4 * growth / (p.cycleDays() / CYCLE_COMPRESSION)));
            HydroponicTrayBlock.Crop crop = HydroponicTrayBlock.Crop.of(cropId);
            BlockState next = state.setValue(HydroponicTrayBlock.CROP, crop).setValue(HydroponicTrayBlock.STAGE, stage);
            if (next != state) {
                serverLevel.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            LifeSupportState.contribute(serverLevel, worldPosition, LifeSupportState.Contribution.NONE);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (cropId != null) {
            output.putString("crop", cropId.toString());
        }
        output.putDouble("growth", growth);
        output.putDouble("fertilizer", fertilizer);
        output.putDouble("water", water);
        output.putDouble("last_day", lastDay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cropId = input.getString("crop").map(Identifier::tryParse).orElse(null);
        growth = input.getDoubleOr("growth", 0);
        fertilizer = input.getDoubleOr("fertilizer", 0);
        water = input.getDoubleOr("water", 0);
        lastDay = input.getDoubleOr("last_day", -1);
    }
}
