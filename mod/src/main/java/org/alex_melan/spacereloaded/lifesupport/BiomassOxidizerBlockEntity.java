package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.electronics.ProcessMachineBlockEntity;
import org.alex_melan.spacereloaded.electronics.ProcessMenu;
import org.alex_melan.spacereloaded.registry.ItemMasses;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.registry.ModTags;
import org.alex_melan.spacereloaded.sealing.SealedZone;

/**
 * Окислитель биомассы (007, FR-514, D75): каталитическое «мокрое» сжигание несъедобной биомассы
 * (целлюлоза C₆H₁₀O₅ + 6O₂ → 6CO₂ + 5H₂O: на кг — 1.185 кг O₂ и 1.628 кг CO₂). Замыкает углерод
 * оранжереи: посадки, кормящие человека, связывают втрое больше CO₂, чем он выдыхает (BVAD, BIOS-3).
 * Скорость — 2 кг сухой биомассы в игровые сутки (солома 40 м² пшеницы — 1.2 кг); нужен кислород
 * зоны (pO₂ ≥ 10 кПа). Слот 0 — биомасса (тег {@code spacereloaded:biomass}).
 */
public class BiomassOxidizerBlockEntity extends ProcessMachineBlockEntity {

    public static final double KG_PER_DAY = 2.0;
    public static final double O2_PER_KG = 6 * 31.999 / 162.14;
    public static final double CO2_PER_KG = 6 * 44.01 / 162.14;

    private double burnt;
    private double lastDay = -1;
    private boolean working;

    public BiomassOxidizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOMASS_OXIDIZER, pos, state, ProcessMenu.Layout.BIOMASS_OXIDIZER, 1000, 100);
    }

    @Override
    protected void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        double now = LifeSupportState.days(level);
        double dt = lastDay < 0 ? 0 : now - lastDay;
        lastDay = now;
        SealedZone zone = LifeSupportState.zoneAround(level, getBlockPos());
        LifeSupportState.Gas gas = zone == null || !zone.isSealed() ? null : LifeSupportState.now(level, zone);
        ItemStack fuel = items.get(0);
        working = gas != null && gas.pO2() >= 10 && fuel.is(ModTags.BIOMASS);
        LifeSupportState.contribute(level, getBlockPos(), working
                ? new LifeSupportState.Contribution(-O2_PER_KG * KG_PER_DAY, CO2_PER_KG * KG_PER_DAY, 0, 0)
                : LifeSupportState.Contribution.NONE);
        if (!working) {
            return;
        }
        burnt += KG_PER_DAY * dt;
        double unit = ItemMasses.massOf(level.registryAccess(), fuel);
        while (burnt >= unit && !fuel.isEmpty()) {
            burnt -= unit;
            fuel.shrink(1);
        }
        setChanged();
    }

    @Override
    protected int progress() {
        return working ? 1 : 0;
    }

    @Override
    protected int maxProgress() {
        return 1;
    }

    @Override
    protected MenuType<ProcessMenu> menuType() {
        return ModMenus.BIOMASS_OXIDIZER;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("burnt", burnt);
        output.putDouble("last_day", lastDay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnt = input.getDoubleOr("burnt", 0);
        lastDay = input.getDoubleOr("last_day", -1);
    }
}
