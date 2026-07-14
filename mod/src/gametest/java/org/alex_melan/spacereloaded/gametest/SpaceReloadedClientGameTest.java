package org.alex_melan.spacereloaded.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.cannon.KineticProjectileEntity;
import org.alex_melan.spacereloaded.cannon.OrbitalCannonBlockEntity;
import org.alex_melan.spacereloaded.cannon.TargetingDesignatorItem;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.machine.CrusherBlockEntity;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;
import org.alex_melan.spacereloaded.rocket.RocketEntity;
import org.alex_melan.spacereloaded.rocket.RocketInteractions;
import org.alex_melan.spacereloaded.sealing.SealedZone;
import org.alex_melan.spacereloaded.sealing.ZoneManager;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

/**
 * E2E-стенд (аналогичен rt-sound-mod): реальный клиент + встроенный сервер.
 * <ol>
 *   <li>Герметичность: куб с контроллером — SEALED; диагональная щель в углу —
 *       LEAK (осознанный 26-направленный дизайн, конституция VIII);</li>
 *   <li>Промышленность: дробилка перерабатывает сырьё при наличии энергии;</li>
 *   <li>Ракета: сборка с площадки читает честное топливо из бака.</li>
 * </ol>
 */
public class SpaceReloadedClientGameTest implements FabricClientGameTest {

    /** База для построек — в воздухе над спавном, чтобы не зависеть от рельефа. */
    private static final int BX = 100;
    private static final int BY = 150;
    private static final int BZ = 100;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder().create()) {
            sp.getClientLevel().waitForChunksRender();

            testSealing(context, sp);
            testCrusher(context, sp);
            testRocketAssembly(context, sp);
            testOrbitalCannon(context, sp);
            testDocking(context, sp);
            testScanAndProgram(context, sp);
            testBatteryBalancing(context, sp);
            testCargoLoop(context, sp);
            testMeteor(context, sp);
            testRedstoneAirlock(context, sp);
            testTelemetryScreen(context, sp);
            testMarsChemistry(context, sp);
            testOrbitalNetwork(context, sp);
            testDeepSpace(context, sp);
            testNavigation(context, sp);
            testPlanetTerrain(context, sp);
            testPropellantFluids(context, sp);
            testBodyExclusiveOres(context, sp);
        }
    }

    // ---------- 1. Герметичность ----------

    /**
     * Телепорт игрока к площадке на обсидиановый пятачок. Чанки вокруг сайта
     * форсируются и ДОжидаются: после дальнего tp область грузится лениво,
     * и команды /fill//setblock молча отказывают («That position is not
     * loaded») — источник флейков, пойманный этим стендом.
     */
    private void moveTo(ClientGameTestContext context, TestSingleplayerContext sp, int x, int z) {
        sp.getServer().runCommand(String.format("forceload add %d %d %d %d",
                x - 16, z - 16, x + 24, z + 24));
        boolean loaded = false;
        for (int waited = 0; waited < 400 && !loaded; waited += 10) {
            loaded = sp.getServer().computeOnServer(server ->
                    server.overworld().isLoaded(new BlockPos(x, BY, z)));
            if (!loaded) {
                context.waitTicks(10);
            }
        }
        assertThat(loaded, "Чанки площадки должны загрузиться после forceload");
        sp.getServer().runCommand(fill(x - 1, BY - 1, z - 1, x + 1, BY - 1, z + 1, "minecraft:obsidian"));
        sp.getServer().runCommand(String.format("tp @p %d %d %d", x, BY, z));
        context.waitTicks(10);
    }

    private void testSealing(ClientGameTestContext context, TestSingleplayerContext sp) {
        moveTo(context, sp, BX - 4, BZ + 2);
        // Куб 5×5×5 из обшивки, полость 3×3×3, контроллер + креатив-источник внутри
        sp.getServer().runCommand(fill(BX, BY, BZ, BX + 4, BY + 4, BZ + 4, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(BX + 1, BY + 1, BZ + 1, BX + 3, BY + 3, BZ + 3, "minecraft:air"));
        sp.getServer().runCommand(set(BX + 2, BY + 1, BZ + 2, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(BX + 1, BY + 1, BZ + 2, "spacereloaded:creative_power"));
        sp.getServer().runCommand("spacereloaded debug vacuum on");

        SealingStatus sealed = waitForStatus(context, sp, 200, SealingStatus.SEALED);
        assertThat(sealed == SealingStatus.SEALED,
                "Куб должен быть герметичен, получено: " + sealed);
        log("герметичный куб → SEALED ✓");

        // Диагональная щель: вынимаем угловой блок — 26 направлений обязаны поймать
        sp.getServer().runCommand(set(BX + 4, BY + 4, BZ + 4, "minecraft:air"));
        SealingStatus leaked = waitForStatus(context, sp, 200,
                SealingStatus.LEAK, SealingStatus.UNBOUNDED);
        assertThat(leaked == SealingStatus.LEAK || leaked == SealingStatus.UNBOUNDED,
                "Диагональная щель должна давать утечку, получено: " + leaked);
        log("диагональная щель → " + leaked + " ✓");
        int leakPts = sp.getServer().computeOnServer(server -> {
            var zone = ZoneManager.zoneAt(server.overworld(), new BlockPos(BX + 2, BY + 1, BZ + 2));
            return zone == null ? 0 : zone.leakPoints().size();
        });
        assertThat(leakPts > 0, "Сканер: у утечки должны быть точки пробоя, получено: " + leakPts);
        log("точки пробоя для сканера: " + leakPts + " ✓");

        // Починка — снова герметичен
        sp.getServer().runCommand(set(BX + 4, BY + 4, BZ + 4, "spacereloaded:hull_plating"));
        SealingStatus repaired = waitForStatus(context, sp, 200, SealingStatus.SEALED);
        assertThat(repaired == SealingStatus.SEALED,
                "После починки угла зона должна восстановиться, получено: " + repaired);
        log("починка угла → SEALED ✓");

        // Вентрешётка: цельный на вид блок, но газ проходит (тег passes_gas)
        sp.getServer().runCommand(set(BX + 2, BY + 4, BZ + 2, "spacereloaded:vent_grate"));
        SealingStatus vented = waitForStatus(context, sp, 200,
                SealingStatus.LEAK, SealingStatus.UNBOUNDED);
        assertThat(vented == SealingStatus.LEAK || vented == SealingStatus.UNBOUNDED,
                "Вентрешётка в стене должна давать утечку (тег passes_gas), получено: " + vented);
        sp.getServer().runCommand(set(BX + 2, BY + 4, BZ + 2, "spacereloaded:hull_plating"));
        log("вентрешётка → " + vented + " (тег passes_gas) ✓");
    }

    /** Поллинг статуса зоны до ожидаемого (пересчёты асинхронные). */
    private SealingStatus waitForStatus(ClientGameTestContext context, TestSingleplayerContext sp,
                                        int maxTicks, SealingStatus... expected) {
        SealingStatus status = zoneStatus(sp);
        for (int waited = 0; waited < maxTicks; waited += 10) {
            for (SealingStatus want : expected) {
                if (status == want) {
                    return status;
                }
            }
            context.waitTicks(10);
            status = zoneStatus(sp);
        }
        return status;
    }

    private SealingStatus zoneStatus(TestSingleplayerContext sp) {
        return sp.getServer().computeOnServer(server -> {
            SealedZone zone = ZoneManager.zoneAt(server.overworld(),
                    new BlockPos(BX + 2, BY + 1, BZ + 2));
            return zone == null ? SealingStatus.INVALID_ORIGIN : zone.status();
        });
    }

    // ---------- 2. Дробилка ----------

    private void testCrusher(ClientGameTestContext context, TestSingleplayerContext sp) {
        int mx = BX + 40;
        moveTo(context, sp, mx - 3, BZ);
        sp.getServer().runCommand(set(mx, BY, BZ, "spacereloaded:crusher"));
        sp.getServer().runCommand(set(mx + 1, BY, BZ, "spacereloaded:creative_power"));
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(mx, BY, BZ))
                    instanceof CrusherBlockEntity crusher) {
                crusher.setItem(0, new ItemStack(ModItems.RAW_TITANIUM, 2));
            }
        });
        context.waitTicks(130); // 100 тиков операция + запас

        ItemStack output = sp.getServer().computeOnServer(server ->
                server.overworld().getBlockEntity(new BlockPos(mx, BY, BZ))
                        instanceof CrusherBlockEntity crusher ? crusher.getItem(1) : ItemStack.EMPTY);
        assertThat(output.is(ModItems.TITANIUM_DUST) && output.getCount() >= 2,
                "Дробилка должна выдать ≥2 титановой пыли, получено: " + output);
        log("дробилка: сырьё → " + output.getCount() + " пыли ✓");
    }

    // ---------- 3. Сборка ракеты с честным топливом ----------

    private void testRocketAssembly(ClientGameTestContext context, TestSingleplayerContext sp) {
        int rx = BX + 80;
        moveTo(context, sp, rx - 5, BZ);
        // Площадка 3×3 + пилон высотой 5
        sp.getServer().runCommand(fill(rx - 1, BY, BZ - 1, rx + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(rx - 2, BY + 1, BZ, rx - 2, BY + 5, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(rx - 2, BY, BZ, "spacereloaded:launch_pad"));
        // Ракета: двигатель, бак, командный модуль
        sp.getServer().runCommand(set(rx, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(rx, BY + 2, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(rx, BY + 3, BZ, "spacereloaded:command_module"));
        context.waitTick();

        // Честное топливо: 1000 кг керолокса в бак
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(rx, BY + 2, BZ))
                    instanceof FuelTankBlockEntity tank) {
                tank.setPropellant(1000.0, "spacereloaded:kerolox");
            }
        });

        // Сборка с пилона от лица игрока
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(rx - 2, BY + 3, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);

        List<RocketEntity> rockets = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        new AABB(rx - 8, BY - 2, BZ - 8, rx + 8, BY + 12, BZ + 8),
                        RocketEntity::isParked));
        assertThat(!rockets.isEmpty(), "Ракета должна собраться в сущность");

        double propellant = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        new AABB(rx - 8, BY - 2, BZ - 8, rx + 8, BY + 12, BZ + 8),
                        RocketEntity::isParked).get(0).propellantKg());
        assertThat(Math.abs(propellant - 1000.0) < 1.0,
                "Топливо ракеты должно совпасть с баком (1000 кг), получено: " + propellant);
        log("сборка ракеты: сущность + честные " + propellant + " кг топлива ✓");

        // Блоки поднялись в сущность
        boolean blocksGone = sp.getServer().computeOnServer(server ->
                server.overworld().getBlockState(new BlockPos(rx, BY + 2, BZ)).isAir());
        assertThat(blocksGone, "Блоки ракеты должны подняться в сущность");
        log("блоки структуры изъяты из мира ✓");
    }

    // ---------- 4. Орбитальная пушка: межпространственный выстрел ----------

    /**
     * Пушка стоит НА ОРБИТЕ (spacereloaded:earth_orbit), цель — каменная
     * платформа в оверворлде. Честная проверка US7: снаряд спавнится в целевом
     * измерении, падает по баллистике ядра и выносит кратер по E = ½mv².
     */
    private void testOrbitalCannon(ClientGameTestContext context, TestSingleplayerContext sp) {
        int tx = BX + 120;
        moveTo(context, sp, tx - 10, BZ);
        // Мишень: каменная платформа 7×7 (moveTo уже форсировал чанки области)
        sp.getServer().runCommand(fill(tx - 3, BY, BZ - 3, tx + 3, BY, BZ + 3, "minecraft:stone"));
        context.waitTick();
        boolean platformSolid = sp.getServer().computeOnServer(server ->
                !server.overworld().getBlockState(new BlockPos(tx, BY, BZ)).isAir());
        assertThat(platformSolid, "Мишень должна быть камнем ДО выстрела (иначе тест ложный)");
        // Кратер r≈13 и взрыв силой 10: игрока уводим за радиус поражения
        sp.getServer().runCommand(String.format("tp @p %d %d %d", tx - 30, BY, BZ));

        // Самообстрел запрещён: свежая пушка, цель в её собственном измерении
        String selfShot = sp.getServer().computeOnServer(server -> {
            ServerLevel orbit = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")));
            if (orbit == null) {
                return "нет измерения орбиты";
            }
            BlockPos cannonPos = new BlockPos(50, 120, 50);
            orbit.setBlock(cannonPos, ModBlocks.ORBITAL_CANNON.defaultBlockState(), 3);
            if (!(orbit.getBlockEntity(cannonPos) instanceof OrbitalCannonBlockEntity cannon)) {
                return "нет BE пушки";
            }
            cannon.loadRod();
            ((SimpleEnergyStorage) cannon.energyStorage()).amount =
                    SpaceReloaded.config().cannonEnergyCapacity;
            cannon.setTarget(GlobalPos.of(orbit.dimension(), new BlockPos(55, 100, 55)));
            return cannon.tryFire(orbit).getString();
        });
        assertThat(selfShot.contains("own dimension"),
                "Пушка не должна бить по своему измерению, получено: " + selfShot);
        log("пушка: самообстрел запрещён ✓");

        // Пушка на орбите: заряжаем лом, энергию и наводим напрямую через BE
        String fired = sp.getServer().computeOnServer(server -> {
            ServerLevel orbit = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")));
            if (orbit == null) {
                return "нет измерения орбиты";
            }
            BlockPos cannonPos = new BlockPos(50, 120, 50);
            orbit.setBlock(cannonPos, ModBlocks.ORBITAL_CANNON.defaultBlockState(), 3);
            if (!(orbit.getBlockEntity(cannonPos) instanceof OrbitalCannonBlockEntity cannon)) {
                return "нет BE пушки";
            }
            cannon.loadRod();
            ((SimpleEnergyStorage) cannon.energyStorage()).amount =
                    SpaceReloaded.config().cannonEnergyCapacity;
            cannon.setTarget(GlobalPos.of(Level.OVERWORLD, new BlockPos(tx, BY, BZ)));
            return cannon.tryFire(orbit).getString();
        });
        log("пушка: " + fired);
        assertThat(fired.contains("impact"),
                "Выстрел должен состояться (сообщение об успехе), получено: " + fired);

        // Подлёт ~3.6 с; ждём кратер поллингом, следя за высотой снаряда
        boolean crater = false;
        for (int waited = 0; waited < 400 && !crater; waited += 20) {
            context.waitTicks(20);
            crater = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(tx, BY, BZ)).isAir());
            double projY = sp.getServer().computeOnServer(server ->
                    server.overworld().getEntities(
                            EntityTypeTest.forClass(KineticProjectileEntity.class),
                            new AABB(tx - 16, BY - 16, BZ - 16, tx + 16, BY + 600, BZ + 16),
                            e -> true).stream()
                            .mapToDouble(e -> e.getY()).findFirst().orElse(Double.NaN));
            log(String.format("t=%d тиков: снаряд y=%.1f, кратер=%s", waited + 20, projY, crater));
        }
        assertThat(crater, "Кинетический удар должен вынести кратер в платформе");
        log("межпространственный выстрел: кратер в оверворлде ✓");

        // --- 4b. Пульт: привязка к пушке + дистанционный выстрел из другого измерения
        sp.getServer().runCommand(fill(tx - 3, BY, BZ - 3, tx + 3, BY, BZ + 3, "minecraft:stone"));
        context.waitTicks(220); // кулдаун пушки (200 тиков)
        String remote = sp.getServer().computeOnServer(server -> {
            ServerLevel orbit = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")));
            BlockPos cannonPos = new BlockPos(50, 120, 50);
            if (orbit == null
                    || !(orbit.getBlockEntity(cannonPos) instanceof OrbitalCannonBlockEntity cannon)) {
                return "нет пушки";
            }
            cannon.loadRod();
            // Пульт: привязка + метка как data-компоненты предмета
            ItemStack designator = new ItemStack(ModItems.TARGETING_DESIGNATOR);
            designator.set(ModDataComponents.BOUND_CANNON,
                    GlobalPos.of(orbit.dimension(), cannonPos));
            designator.set(ModDataComponents.TARGET_MARK,
                    GlobalPos.of(Level.OVERWORLD, new BlockPos(tx, BY, BZ)));
            TargetingDesignatorItem.remoteRetarget(server, designator);
            return TargetingDesignatorItem.remoteFire(server, designator).getString();
        });
        log("пульт: " + remote);
        assertThat(remote.contains("impact"),
                "Дистанционный выстрел с пульта должен состояться, получено: " + remote);
        boolean remoteCrater = false;
        for (int waited = 0; waited < 400 && !remoteCrater; waited += 20) {
            context.waitTicks(20);
            remoteCrater = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(tx, BY, BZ)).isAir());
        }
        assertThat(remoteCrater, "Дистанционный выстрел должен вынести кратер");
        log("пульт: привязка + дистанционный выстрел ✓");
    }

    // ---------- 5. Стыковка: расстыковка/стыковка по узлу (US6) ----------

    private void testDocking(ClientGameTestContext context, TestSingleplayerContext sp) {
        int dx = BX + 160;
        moveTo(context, sp, dx - 5, BZ);
        // Двухступенчатая: [двигатель|бак|кресло] + узел + [бак|модуль]
        sp.getServer().runCommand(fill(dx - 1, BY, BZ - 1, dx + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(dx - 2, BY + 1, BZ, dx - 2, BY + 7, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(dx - 2, BY, BZ, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(set(dx, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(dx, BY + 2, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(dx, BY + 3, BZ, "spacereloaded:rocket_seat"));
        sp.getServer().runCommand(set(dx, BY + 4, BZ, "spacereloaded:docking_clamp"));
        sp.getServer().runCommand(set(dx, BY + 5, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(dx, BY + 6, BZ, "spacereloaded:command_module"));
        context.waitTick();
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(dx, BY + 2, BZ))
                    instanceof FuelTankBlockEntity tank) {
                tank.setPropellant(1000.0, "spacereloaded:kerolox");
            }
            if (server.overworld().getBlockEntity(new BlockPos(dx, BY + 5, BZ))
                    instanceof FuelTankBlockEntity tank) {
                tank.setPropellant(1000.0, "spacereloaded:kerolox");
            }
        });
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(dx - 2, BY + 4, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);

        AABB area = new AABB(dx - 10, BY - 2, BZ - 10, dx + 10, BY + 16, BZ + 10);
        // Расстыковка по узлу
        String undockResult = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked);
            if (rockets.size() != 1) {
                return "ожидалась 1 ракета, найдено " + rockets.size();
            }
            RocketEntity rocket = rockets.get(0);
            int clampY = org.alex_melan.spacereloaded.rocket.DockingSystem
                    .clampLocalY(rocket.rocketDataForDocking()).orElse(-1);
            if (clampY < 0) {
                return "узел не найден в структуре";
            }
            return org.alex_melan.spacereloaded.rocket.DockingSystem
                    .undock(server.overworld(), rocket, clampY).getString();
        });
        context.waitTicks(5);
        List<RocketEntity> split = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        area, RocketEntity::isParked));
        assertThat(split.size() == 2, "После расстыковки должно быть 2 аппарата ("
                + undockResult + "), найдено: " + split.size());
        log("расстыковка: " + undockResult + " ✓");

        // Стыковка обратно: у носителя узел — нижний ряд (ниже ничего)
        String dockResult = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked);
            for (RocketEntity candidate : rockets) {
                var clamp = org.alex_melan.spacereloaded.rocket.DockingSystem
                        .clampLocalY(candidate.rocketDataForDocking());
                if (clamp.isPresent()) {
                    return org.alex_melan.spacereloaded.rocket.DockingSystem
                            .dock(server.overworld(), candidate, clamp.getAsInt()).getString();
                }
            }
            return "носитель с узлом не найден";
        });
        context.waitTicks(5);
        List<RocketEntity> merged = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        area, RocketEntity::isParked));
        assertThat(merged.size() == 1, "После стыковки должен остаться 1 аппарат ("
                + dockResult + "), найдено: " + merged.size());
        double fuel = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        area, RocketEntity::isParked).get(0).propellantKg());
        assertThat(Math.abs(fuel - 2000.0) < 1.0,
                "Топливо после стыковки должно суммироваться (2000 кг), получено: " + fuel);
        log("стыковка: " + dockResult + " · топливо " + fuel + " кг ✓");
        // Убрать аппарат, чтобы не мешал будущим сценариям
        sp.getServer().runOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        area, e -> true).forEach(Entity::discard));
    }

    // ---------- 6. Скан-отчёт и полётная программа ----------

    private void testScanAndProgram(ClientGameTestContext context, TestSingleplayerContext sp) {
        int px = BX + 200;
        moveTo(context, sp, px - 5, BZ);
        sp.getServer().runCommand(fill(px - 1, BY, BZ - 1, px + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(px - 2, BY + 1, BZ, px - 2, BY + 4, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(px - 2, BY, BZ, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(set(px, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(px, BY + 2, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(px, BY + 3, BZ, "spacereloaded:command_module"));
        context.waitTick();

        // Скан без сборки: отчёт с TWR, блоки остаются в мире
        String scan = sp.getServer().computeOnServer(server ->
                RocketInteractions.scanFromPylon(server.overworld(),
                        new BlockPos(px - 2, BY + 2, BZ),
                        server.getPlayerList().getPlayers().get(0)).getString());
        assertThat(scan.contains("TWR"), "Скан должен вернуть сводку с TWR, получено: " + scan);
        boolean stillThere = sp.getServer().computeOnServer(server ->
                !server.overworld().getBlockState(new BlockPos(px, BY + 2, BZ)).isAir());
        assertThat(stillThere, "Скан не должен собирать ракету (блоки остаются)");
        log("скан-отчёт: " + scan + " · блоки на месте ✓");

        // Сборка + полётная программа (цель: орбита, маяк на орбите)
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(px - 2, BY + 2, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        String installed = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class),
                    new AABB(px - 8, BY - 2, BZ - 8, px + 8, BY + 12, BZ + 8),
                    RocketEntity::isParked);
            if (rockets.isEmpty()) {
                return "ракета не собралась";
            }
            ItemStack program = new ItemStack(ModItems.FLIGHT_PROGRAM);
            program.set(ModDataComponents.PROGRAM_DESTINATION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit"));
            program.set(ModDataComponents.PROGRAM_PAD, GlobalPos.of(
                    ResourceKey.create(Registries.DIMENSION,
                            Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")),
                    new BlockPos(60, 101, 60)));
            return rockets.get(0).installProgram(server.overworld(), program).getString();
        });
        assertThat(!installed.contains("unreachable") && !installed.contains("недостижима")
                        && !installed.contains("не собралась"),
                "Программа должна загрузиться, получено: " + installed);
        log("полётная программа: " + installed + " ✓");
        sp.getServer().runOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        new AABB(px - 8, BY - 2, BZ - 8, px + 8, BY + 12, BZ + 8),
                        e -> true).forEach(Entity::discard));
    }

    // ---------- 7. Батареи: выравнивание заряда в сети ----------

    private void testBatteryBalancing(ClientGameTestContext context, TestSingleplayerContext sp) {
        int bx2 = BX + 240;
        moveTo(context, sp, bx2 - 3, BZ);
        // Полная батарея — кабель — пустая батарея
        sp.getServer().runCommand(set(bx2, BY, BZ, "spacereloaded:battery"));
        sp.getServer().runCommand(set(bx2 + 1, BY, BZ, "spacereloaded:energy_cable"));
        sp.getServer().runCommand(set(bx2 + 2, BY, BZ, "spacereloaded:battery"));
        context.waitTick();
        long capacity = sp.getServer().computeOnServer(server -> {
            var be = server.overworld().getBlockEntity(new BlockPos(bx2, BY, BZ));
            if (be instanceof org.alex_melan.spacereloaded.energy.BatteryBlockEntity battery) {
                var storage = (SimpleEnergyStorage) battery.energyStorage();
                storage.amount = storage.getCapacity();
                return storage.getCapacity();
            }
            return 0L;
        });
        assertThat(capacity > 0, "Батарея должна найтись и зарядиться");

        // Ждём выравнивания (пропускная способность сети ограничена)
        long[] amounts = {0, 0};
        for (int waited = 0; waited < 2400; waited += 100) {
            context.waitTicks(100);
            long[] current = sp.getServer().computeOnServer(server -> {
                long a = 0;
                long b = 0;
                if (server.overworld().getBlockEntity(new BlockPos(bx2, BY, BZ))
                        instanceof org.alex_melan.spacereloaded.energy.BatteryBlockEntity left) {
                    a = left.energyStorage().getAmount();
                }
                if (server.overworld().getBlockEntity(new BlockPos(bx2 + 2, BY, BZ))
                        instanceof org.alex_melan.spacereloaded.energy.BatteryBlockEntity right) {
                    b = right.energyStorage().getAmount();
                }
                return new long[]{a, b};
            });
            amounts = current;
            if (Math.abs(amounts[0] - amounts[1]) <= capacity * 6 / 100) {
                break;
            }
        }
        long total = amounts[0] + amounts[1];
        assertThat(Math.abs(total - capacity) <= capacity / 100,
                "Энергия должна сохраниться (" + capacity + "), получено: " + total);
        assertThat(Math.abs(amounts[0] - amounts[1]) <= capacity * 6 / 100,
                "Батареи должны выровняться (гистерезис 5%), получено: "
                        + amounts[0] + " и " + amounts[1]);
        log("батареи выровнялись: " + amounts[0] + " / " + amounts[1] + " ✓");

        // Покой после выравнивания: заряды больше не меняются (нет карусели)
        long[] settled = amounts;
        context.waitTicks(60);
        long[] later = sp.getServer().computeOnServer(server -> {
            long a = 0;
            long b = 0;
            if (server.overworld().getBlockEntity(new BlockPos(bx2, BY, BZ))
                    instanceof org.alex_melan.spacereloaded.energy.BatteryBlockEntity left) {
                a = left.energyStorage().getAmount();
            }
            if (server.overworld().getBlockEntity(new BlockPos(bx2 + 2, BY, BZ))
                    instanceof org.alex_melan.spacereloaded.energy.BatteryBlockEntity right) {
                b = right.energyStorage().getAmount();
            }
            return new long[]{a, b};
        });
        assertThat(later[0] == settled[0] && later[1] == settled[1],
                "После выравнивания заряды должны стоять на месте, было "
                        + settled[0] + "/" + settled[1] + ", стало " + later[0] + "/" + later[1]);
        log("карусели нет: заряды стабильны ✓");
    }

    // ---------- 8. Грузовой контур: сундук → погрузчик → борт → обратно ----------

    private void testCargoLoop(ClientGameTestContext context, TestSingleplayerContext sp) {
        int cx = BX + 280;
        moveTo(context, sp, cx - 5, BZ);
        // Мини-ракета с грузовым отсеком + погрузчик с сундуком
        sp.getServer().runCommand(fill(cx - 1, BY, BZ - 1, cx + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(cx - 2, BY + 1, BZ, cx - 2, BY + 5, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(cx - 2, BY, BZ, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(set(cx, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(cx, BY + 2, BZ, "spacereloaded:cargo_hold"));
        sp.getServer().runCommand(set(cx, BY + 3, BZ, "spacereloaded:command_module"));
        sp.getServer().runCommand(set(cx + 3, BY, BZ, "spacereloaded:cargo_loader"));
        sp.getServer().runCommand(set(cx + 4, BY, BZ, "minecraft:chest"));
        context.waitTick();
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(cx + 4, BY, BZ))
                    instanceof net.minecraft.world.Container chest) {
                chest.setItem(0, new ItemStack(ModItems.TITANIUM_INGOT, 10));
            }
        });
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(cx - 2, BY + 3, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(30); // погрузчик: раз в 10 тиков по стеку

        AABB area = new AABB(cx - 8, BY - 2, BZ - 8, cx + 8, BY + 12, BZ + 8);
        int loaded = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                                area, RocketEntity::isParked).stream()
                        .mapToInt(RocketEntity::cargoCount).sum());
        assertThat(loaded == 10, "Погрузчик должен загрузить 10 слитков в борт, загружено: " + loaded);
        log("погрузка: 10 слитков в борту ✓");

        // Разгрузка обратно в сундук
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(cx + 3, BY, BZ))
                    instanceof org.alex_melan.spacereloaded.rocket.CargoLoaderBlockEntity loader) {
                loader.cycleMode(); // LOAD -> UNLOAD
            }
        });
        context.waitTicks(30);
        int chestCount = sp.getServer().computeOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(cx + 4, BY, BZ))
                    instanceof net.minecraft.world.Container chest) {
                int total = 0;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    total += chest.getItem(slot).getCount();
                }
                return total;
            }
            return 0;
        });
        assertThat(chestCount == 10, "Разгрузка должна вернуть 10 слитков в сундук, там: " + chestCount);
        log("разгрузка: 10 слитков вернулись в сундук ✓");
        sp.getServer().runOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        area, e -> true).forEach(Entity::discard));
    }

    // ---------- 9. Метеорит: падение, кратер, метеоритное железо ----------

    private void testMeteor(ClientGameTestContext context, TestSingleplayerContext sp) {
        int mx = BX + 320;
        moveTo(context, sp, mx - 5, BZ);
        sp.getServer().runCommand(fill(mx - 3, BY, BZ - 3, mx + 3, BY, BZ + 3, "minecraft:stone"));
        context.waitTick();
        boolean solid = sp.getServer().computeOnServer(server ->
                !server.overworld().getBlockState(new BlockPos(mx, BY, BZ)).isAir());
        assertThat(solid, "Мишень должна быть камнем до метеорита");

        // Спавним метеорит прямо над платформой (логика удара не зависит от измерения)
        sp.getServer().runOnServer(server -> {
            org.alex_melan.spacereloaded.impact.MeteorEntity meteor =
                    new org.alex_melan.spacereloaded.impact.MeteorEntity(
                            org.alex_melan.spacereloaded.registry.ModEntities.METEOR, server.overworld());
            meteor.setPos(mx + 0.5, BY + 60, BZ + 0.5);
            meteor.configure(800, new net.minecraft.world.phys.Vec3(0, -55, 0));
            server.overworld().addFreshEntity(meteor);
        });
        boolean crater = false;
        for (int waited = 0; waited < 300 && !crater; waited += 20) {
            context.waitTicks(20);
            crater = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(mx, BY, BZ)).isAir());
        }
        assertThat(crater, "Метеорит должен вынести кратер в платформе");

        context.waitTicks(20); // предметы оседают
        int iron = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(
                        EntityTypeTest.forClass(net.minecraft.world.entity.item.ItemEntity.class),
                        new AABB(mx - 12, BY - 12, BZ - 12, mx + 12, BY + 12, BZ + 12),
                        e -> e.getItem().is(ModItems.METEORIC_IRON)).stream()
                        .mapToInt(e -> e.getItem().getCount()).sum());
        assertThat(iron >= 2, "Метеорит должен оставить метеоритное железо, найдено: " + iron);
        log("метеорит: кратер + " + iron + " метеоритного железа ✓");
        sp.getServer().runOnServer(server ->
                server.overworld().getEntities(
                        EntityTypeTest.forClass(net.minecraft.world.entity.item.ItemEntity.class),
                        new AABB(mx - 12, BY - 12, BZ - 12, mx + 12, BY + 12, BZ + 12),
                        e -> true).forEach(net.minecraft.world.entity.Entity::discard));
    }

    // ---------- 10. Шлюз по редстоуну ----------

    private void testRedstoneAirlock(ClientGameTestContext context, TestSingleplayerContext sp) {
        int ax = BX + 360;
        moveTo(context, sp, ax - 5, BZ);
        sp.getServer().runCommand(set(ax, BY, BZ, "spacereloaded:hermetic_hatch"));
        context.waitTick();
        // Подать сигнал — редстоун-блок вплотную
        sp.getServer().runCommand(set(ax + 1, BY, BZ, "minecraft:redstone_block"));
        boolean opened = false;
        for (int waited = 0; waited < 120 && !opened; waited += 10) {
            context.waitTicks(10);
            opened = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(ax, BY, BZ))
                            .getValue(org.alex_melan.spacereloaded.sealing.HermeticHatchBlock.OPEN));
        }
        assertThat(opened, "Люк должен открыться по редстоун-сигналу");
        log("шлюз: редстоун открыл люк ✓");
        // Снять сигнал — мгновенное закрытие
        sp.getServer().runCommand(set(ax + 1, BY, BZ, "minecraft:air"));
        context.waitTicks(10);
        boolean closed = sp.getServer().computeOnServer(server ->
                !server.overworld().getBlockState(new BlockPos(ax, BY, BZ))
                        .getValue(org.alex_melan.spacereloaded.sealing.HermeticHatchBlock.OPEN));
        assertThat(closed, "Люк должен закрыться при снятии сигнала");
        log("шлюз: снятие сигнала закрыло люк ✓");

        // Дверь 2×2 открывается РУКОЙ целиком: регрессия на неверный neighborChanged,
        // из-за которого setBlock по соседу группы читался как «сигнал снят»
        int gx = ax + 12;
        sp.getServer().runCommand(fill(gx, BY, BZ, gx + 1, BY + 1, BZ, "spacereloaded:hermetic_hatch"));
        context.waitTick();
        sp.getServer().runOnServer(server -> {
            ServerLevel overworld = server.overworld();
            BlockPos hit = new BlockPos(gx, BY, BZ);
            overworld.getBlockState(hit).useWithoutItem(overworld,
                    server.getPlayerList().getPlayers().getFirst(),
                    new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(hit),
                            net.minecraft.core.Direction.NORTH, hit, false));
        });
        int openCount = 0;
        for (int waited = 0; waited < 160 && openCount < 4; waited += 10) {
            context.waitTicks(10);
            openCount = sp.getServer().computeOnServer(server -> {
                int open = 0;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        if (server.overworld().getBlockState(new BlockPos(gx + dx, BY + dy, BZ))
                                .getValue(org.alex_melan.spacereloaded.sealing.HermeticHatchBlock.OPEN)) {
                            open++;
                        }
                    }
                }
                return open;
            });
        }
        assertThat(openCount == 4,
                "Дверь 2×2 должна открыться целиком по ПКМ, открыто блоков: " + openCount);
        log("шлюз: дверь 2×2 открылась рукой целиком ✓");
    }

    // ---------- 11. Экран телеметрии: статус зоны ----------

    private void testTelemetryScreen(ClientGameTestContext context, TestSingleplayerContext sp) {
        int tsx = BX - 30;
        moveTo(context, sp, tsx - 4, BZ);
        // Герметичный куб + экран рядом
        sp.getServer().runCommand(fill(tsx, BY, BZ, tsx + 4, BY + 4, BZ + 4, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(tsx + 1, BY + 1, BZ + 1, tsx + 3, BY + 3, BZ + 3, "minecraft:air"));
        sp.getServer().runCommand(set(tsx + 2, BY + 1, BZ + 2, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(tsx + 1, BY + 1, BZ + 2, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(tsx + 6, BY + 1, BZ + 2, "spacereloaded:telemetry_screen"));
        // Ждём: зона SEALED, экран показывает status=1
        int sealedStatus = 0;
        for (int waited = 0; waited < 300 && sealedStatus != 1; waited += 10) {
            context.waitTicks(10);
            sealedStatus = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(tsx + 6, BY + 1, BZ + 2))
                            .getValue(org.alex_melan.spacereloaded.sealing.TelemetryScreenBlock.STATUS));
        }
        assertThat(sealedStatus == 1, "Экран должен показать ЗАМКНУТО (1), получено: " + sealedStatus);
        log("экран телеметрии: ЗАМКНУТО ✓");
        // Пробить куб — экран должен переключиться на УТЕЧКУ (2)
        sp.getServer().runCommand(set(tsx + 2, BY + 4, BZ + 2, "minecraft:air"));
        int leakStatus = 0;
        for (int waited = 0; waited < 300 && leakStatus != 2; waited += 10) {
            context.waitTicks(10);
            leakStatus = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(tsx + 6, BY + 1, BZ + 2))
                            .getValue(org.alex_melan.spacereloaded.sealing.TelemetryScreenBlock.STATUS));
        }
        assertThat(leakStatus == 2, "Экран должен показать УТЕЧКУ (2), получено: " + leakStatus);
        log("экран телеметрии: УТЕЧКА ✓");
    }

    // ---------- 12. Марс: реактор Сабатье + окна Гомана ----------

    private void testMarsChemistry(ClientGameTestContext context, TestSingleplayerContext sp) {
        int sx = BX - 60;
        moveTo(context, sp, sx - 4, BZ);
        sp.getServer().runCommand(set(sx, BY, BZ, "spacereloaded:sabatier_reactor"));
        sp.getServer().runCommand(set(sx + 1, BY, BZ, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(sx, BY, BZ + 1, "spacereloaded:fuel_tank"));
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(sx, BY, BZ))
                    instanceof org.alex_melan.spacereloaded.machine.SabatierReactorBlockEntity r) {
                r.setItem(0, new ItemStack(ModItems.CARBON_DIOXIDE, 4));
                r.setItem(1, new ItemStack(net.minecraft.world.item.Items.ICE, 4));
            }
        });
        double methalox = 0;
        for (int waited = 0; waited < 400 && methalox <= 0; waited += 20) {
            context.waitTicks(20);
            methalox = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockEntity(new BlockPos(sx, BY, BZ + 1))
                            instanceof FuelTankBlockEntity t
                            && "spacereloaded:methalox".equals(t.fuelType()) ? t.propellantKg() : 0.0);
        }
        assertThat(methalox > 0, "Сабатье должен произвести метанокс в бак, получено: " + methalox);
        log("реактор Сабатье: метанокс в баке " + methalox + " кг ✓");

        // Окна Гомана: у Марса синод 144000, окно 24000, фаза 0
        String windowCheck = sp.getServer().computeOnServer(server -> {
            var mars = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(server.overworld(),
                    Identifier.fromNamespaceAndPath("spacereloaded", "mars"));
            if (mars.isEmpty()) {
                return "нет профиля Марса";
            }
            boolean openNow = org.alex_melan.spacereloaded.planet.TransferWindows.isOpen(0L, mars.get());
            boolean closedMid = !org.alex_melan.spacereloaded.planet.TransferWindows.isOpen(50000L, mars.get());
            return (openNow && closedMid) ? "ok" : ("open0=" + openNow + " closed50k=" + closedMid);
        });
        assertThat(windowCheck.equals("ok"), "Окно Марса: открыто в фазе 0, закрыто в середине; получено: " + windowCheck);
        log("окно Гомана к Марсу: открыто/закрыто по фазе ✓");
    }

    // ---------- 13. Орбитальная сеть: покрытие, маршрутизация, бури ----------

    private void testOrbitalNetwork(ClientGameTestContext context, TestSingleplayerContext sp) {
        int nx = BX - 90;
        moveTo(context, sp, nx - 4, BZ);
        // Живая тарелка-перехватчик в загруженном чанке (resolve проверяет живой блок)
        sp.getServer().runCommand(set(nx, BY, BZ, "spacereloaded:interceptor_dish"));
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(nx, BY, BZ))
                    instanceof org.alex_melan.spacereloaded.network.InterceptorDishBlockEntity dish) {
                dish.setListenFrequency(0); // регистрируется как перехватчик открытых каналов
            }
        });
        context.waitTick();

        String result = sp.getServer().computeOnServer(server -> {
            var net = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server);
            var orbit = ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit"));
            var moon = ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "moon"));
            var mars = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(server.overworld(),
                    Identifier.fromNamespaceAndPath("spacereloaded", "mars"));
            if (mars.isEmpty()) {
                return "нет профиля Марса";
            }
            // Покрытие + гейт логистики
            net.addCoverage(orbit);
            boolean cov = net.hasCoverage(orbit) && !net.hasCoverage(moon);
            boolean gateOpen = org.alex_melan.spacereloaded.network.Logistics
                    .coverageSatisfied(server, orbit, mars.get(), true);
            boolean gateBlocked = !org.alex_melan.spacereloaded.network.Logistics
                    .coverageSatisfied(server, moon, mars.get(), true);
            boolean mannedExempt = org.alex_melan.spacereloaded.network.Logistics
                    .coverageSatisfied(server, moon, mars.get(), false);
            // Аутентификация: защищённый маяк
            GlobalPos beacon = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10));
            net.secureBeacon(beacon, 777);
            var authOk = org.alex_melan.spacereloaded.network.SecureRouting.resolve(server, beacon, 777);
            var authFail = org.alex_melan.spacereloaded.network.SecureRouting.resolve(server, beacon, 111);
            boolean auth = beacon.equals(authOk.destination()) && !authOk.authFailed()
                    && authFail.destination() == null && authFail.authFailed();
            // Перехват открытого канала ЖИВЫМ дишем в том же измерении
            GlobalPos openBeacon = GlobalPos.of(Level.OVERWORLD, new BlockPos(nx + 5, BY, BZ));
            GlobalPos dishPos = GlobalPos.of(Level.OVERWORLD, new BlockPos(nx, BY, BZ));
            var hijack = org.alex_melan.spacereloaded.network.SecureRouting.resolve(server, openBeacon, 0);
            boolean intercepted = dishPos.equals(hijack.destination()) && hijack.intercepted();
            // Межпространственный перехват невозможен (диш в оверворлде, маяк на Луне)
            GlobalPos moonBeacon = GlobalPos.of(moon, new BlockPos(60, 101, 60));
            var noHijack = org.alex_melan.spacereloaded.network.SecureRouting.resolve(server, moonBeacon, 0);
            boolean dimSafe = moonBeacon.equals(noHijack.destination()) && !noHijack.intercepted();
            // Буря активна в окне, гаснет после
            long t = server.overworld().getGameTime();
            net.startStorm(Level.OVERWORLD, t + 100);
            boolean storm = net.stormActive(Level.OVERWORLD, t) && !net.stormActive(Level.OVERWORLD, t + 200);
            // КРИТИЧНО: стейт сериализуется в NBT (GlobalPos-ключи ломали сохранение)
            var enc = org.alex_melan.spacereloaded.network.SpaceNetworkState.CODEC
                    .encodeStart(NbtOps.INSTANCE, net).result();
            boolean persist = enc.isPresent();
            if (persist) {
                var dec = org.alex_melan.spacereloaded.network.SpaceNetworkState.CODEC
                        .parse(NbtOps.INSTANCE, enc.get()).result();
                persist = dec.isPresent() && dec.get().hasCoverage(orbit)
                        && dec.get().beaconFrequency(beacon) == 777;
            }

            return (cov && gateOpen && gateBlocked && mannedExempt && auth && intercepted
                    && dimSafe && storm && persist)
                    ? "ok"
                    : ("cov=" + cov + " gate=" + gateOpen + "/" + gateBlocked + " manned=" + mannedExempt
                       + " auth=" + auth + " intercept=" + intercepted + " dimSafe=" + dimSafe
                       + " storm=" + storm + " persist=" + persist);
        });
        assertThat(result.equals("ok"), "Орбитальная сеть; получено: " + result);
        log("орбитальная сеть: покрытие, гейт, защита+живой перехват, буря, персист NBT ✓");
    }

    // ---------- 14. Тепловая модель, энергоспутники, пояс астероидов ----------

    private void testDeepSpace(ClientGameTestContext context, TestSingleplayerContext sp) {
        int rx = BX - 120;
        moveTo(context, sp, rx - 4, BZ);
        // Ректенна: без энергоспутников молчит, с ними — генерирует
        sp.getServer().runCommand(set(rx, BY, BZ, "spacereloaded:rectenna"));
        context.waitTicks(30);
        long before = sp.getServer().computeOnServer(server ->
                server.overworld().getBlockEntity(new BlockPos(rx, BY, BZ))
                        instanceof org.alex_melan.spacereloaded.energy.RectennaBlockEntity r
                        ? r.energyStorage().getAmount() : -1L);
        assertThat(before == 0, "Ректенна без энергоспутников не должна генерировать, получено: " + before);

        var orbitKey = "spacereloaded:earth_orbit";
        sp.getServer().runOnServer(server -> {
            var net = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server);
            var orbit = ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit"));
            net.addPowerSat(orbit);
            net.addPowerSat(orbit);
        });
        long after = 0;
        for (int waited = 0; waited < 100 && after <= 0; waited += 20) {
            context.waitTicks(20);
            after = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockEntity(new BlockPos(rx, BY, BZ))
                            instanceof org.alex_melan.spacereloaded.energy.RectennaBlockEntity r
                            ? r.energyStorage().getAmount() : 0L);
        }
        assertThat(after > 0, "Ректенна с 2 энергоспутниками должна генерировать, получено: " + after);
        log("энергоспутники: ректенна " + after + " E при 2 спутниках ✓");

        // Тепловая модель + профиль пояса астероидов (даёт worldgen загрузиться корректно)
        String checks = sp.getServer().computeOnServer(server -> {
            var moon = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(server.overworld(),
                    Identifier.fromNamespaceAndPath("spacereloaded", "moon"));
            var belt = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(server.overworld(),
                    Identifier.fromNamespaceAndPath("spacereloaded", "asteroid_belt"));
            boolean thermal = moon.isPresent() && Math.abs(moon.get().temperature() + 20) < 0.01
                    && Math.abs(moon.get().temperatureAmplitude() - 130) < 0.01;
            double load = org.alex_melan.spacereloaded.network.Thermal.climateLoadFactor(server.overworld());
            boolean loadOk = load >= 1.0 && Double.isFinite(load);
            boolean beltOk = belt.isPresent() && Math.abs(belt.get().gravity() - 0.49) < 0.01;
            return (thermal && loadOk && beltOk) ? "ok"
                    : ("thermal=" + thermal + " load=" + load + " belt=" + beltOk);
        });
        assertThat(checks.equals("ok"), "Тепло/пояс астероидов; получено: " + checks);
        log("тепловая модель + профиль пояса астероидов ✓");
    }

    // ---------- 15. Навигация: цель где угодно, маршрут по хопам ----------

    private void testNavigation(ClientGameTestContext context, TestSingleplayerContext sp) {
        String result = sp.getServer().computeOnServer(server -> {
            var access = server.overworld().registryAccess();
            var ids = org.alex_melan.spacereloaded.planet.Navigation.planetIds(access);
            Identifier earth = Identifier.fromNamespaceAndPath("spacereloaded", "earth");
            Identifier orbit = Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit");
            Identifier mars = Identifier.fromNamespaceAndPath("spacereloaded", "mars");
            boolean listOk = ids.contains(earth) && ids.contains(mars) && ids.size() >= 4;
            // Земля → Марс: первый хоп орбита
            var hop1 = org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, earth, mars);
            // Орбита → Марс: сразу Марс
            var hop2 = org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, orbit, mars);
            // Уже на месте — маршрута нет
            var hop3 = org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, mars, mars);
            boolean routeOk = orbit.equals(hop1) && mars.equals(hop2) && hop3 == null;
            // Обратно: Марс → Земля через орбиту
            var back = org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, mars, earth);
            boolean backOk = orbit.equals(back);
            return (listOk && routeOk && backOk) ? "ok"
                    : ("list=" + listOk + " hop1=" + hop1 + " hop2=" + hop2 + " hop3=" + hop3 + " back=" + back);
        });
        assertThat(result.equals("ok"), "Навигация по хопам; получено: " + result);
        log("навигация: Земля → орбита → Марс и обратно ✓");
    }

    // ---------- Утилиты ----------

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        return String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block);
    }

    private static String set(int x, int y, int z, String block) {
        return String.format("setblock %d %d %d %s", x, y, z, block);
    }

    private static void assertThat(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void log(String message) {
        System.out.println("[SpaceReloaded Test] " + message);
    }

    // ---------- 18. Рельеф планет: не плоскость ----------

    private void testPlanetTerrain(ClientGameTestContext context, TestSingleplayerContext sp) {
        int[] moonRelief = sp.getServer().computeOnServer(server -> reliefSpan(server, "moon"));
        assertThat(moonRelief[1] - moonRelief[0] >= 10,
                "Луна должна иметь рельеф, перепад высот всего "
                        + (moonRelief[1] - moonRelief[0]) + " блоков");
        log("рельеф Луны: перепад " + (moonRelief[1] - moonRelief[0]) + " блоков ✓");

        int[] marsRelief = sp.getServer().computeOnServer(server -> reliefSpan(server, "mars"));
        assertThat(marsRelief[1] - marsRelief[0] >= 10,
                "Марс должен иметь рельеф, перепад высот всего "
                        + (marsRelief[1] - marsRelief[0]) + " блоков");
        log("рельеф Марса: перепад " + (marsRelief[1] - marsRelief[0]) + " блоков ✓");

        // Марсианская поверхность — не лунный реголит
        String marsTop = sp.getServer().computeOnServer(server -> {
            ServerLevel mars = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "mars")));
            int y = mars.getChunk(0, 0).getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 0, 0);
            return mars.getBlockState(new BlockPos(0, y, 0)).getBlock().getName().getString();
        });
        assertThat(!marsTop.contains("еголит") && !marsTop.toLowerCase().contains("regolith"),
                "На Марсе не должно быть лунного реголита, найдено: " + marsTop);
        log("поверхность Марса: " + marsTop + " ✓");
    }

    /** Мин/макс высоты поверхности по сетке 65×65 вокруг начала координат. */
    private static int[] reliefSpan(net.minecraft.server.MinecraftServer server, String planet) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("spacereloaded", planet)));
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = -32; x <= 32; x += 8) {
            for (int z = -32; z <= 32; z += 8) {
                // ВАЖНО: Level.getHeight не грузит чанк и молча отдаёт minY —
                // высоту спрашиваем у сгенерированного чанка, иначе тест ложно зелёный
                int height = level.getChunk(x >> 4, z >> 4).getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        x & 15, z & 15);
                min = Math.min(min, height);
                max = Math.max(max, height);
            }
        }
        return new int[]{min, max};
    }


    // ---------- 19. Топливо как жидкость: трубы, вёдра, транзакции ----------

    private void testPropellantFluids(ClientGameTestContext context, TestSingleplayerContext sp) {
        int fx = BX + 400;
        moveTo(context, sp, fx - 5, BZ);
        sp.getServer().runCommand(set(fx, BY, BZ, "spacereloaded:fuel_tank"));
        context.waitTick();

        var kerolox = org.alex_melan.spacereloaded.fluid.ModFluids.KEROLOX;
        long bucket = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;

        // Через lookup, как это сделает труба соседнего мода
        double afterInsert = sp.getServer().computeOnServer(server -> {
            var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(
                    server.overworld(), new BlockPos(fx, BY, BZ), null);
            if (storage == null) {
                return -1.0;
            }
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                storage.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(kerolox.source()),
                        bucket, transaction);
                transaction.commit();
            }
            return ((FuelTankBlockEntity) server.overworld()
                    .getBlockEntity(new BlockPos(fx, BY, BZ))).propellantKg();
        });
        assertThat(Math.abs(afterInsert - kerolox.kgPerBucket()) < 0.01,
                "Ведро керолокса = " + kerolox.kgPerBucket() + " кг, получено: " + afterInsert);
        log("жидкость: ведро керолокса залилось как " + afterInsert + " кг ✓");

        // Чужое топливо в занятый бак не лезет
        long mixed = sp.getServer().computeOnServer(server -> {
            var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(
                    server.overworld(), new BlockPos(fx, BY, BZ), null);
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long accepted = storage.insert(
                        net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(
                                org.alex_melan.spacereloaded.fluid.ModFluids.HYDROLOX.source()),
                        bucket, transaction);
                transaction.abort();
                return accepted;
            }
        });
        assertThat(mixed == 0, "Смешивание топлив запрещено, принято капель: " + mixed);
        log("жидкость: смешивание топлив отвергнуто ✓");

        // Откат транзакции обязан вернуть бак в прежнее состояние
        double afterAbort = sp.getServer().computeOnServer(server -> {
            var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(
                    server.overworld(), new BlockPos(fx, BY, BZ), null);
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                storage.extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(kerolox.source()),
                        bucket, transaction);
                transaction.abort();
            }
            return ((FuelTankBlockEntity) server.overworld()
                    .getBlockEntity(new BlockPos(fx, BY, BZ))).propellantKg();
        });
        assertThat(Math.abs(afterAbort - kerolox.kgPerBucket()) < 0.01,
                "Откат транзакции должен вернуть топливо, осталось: " + afterAbort);
        log("жидкость: откат транзакции не потерял топливо ✓");

        // И слив до нуля через commit
        double afterExtract = sp.getServer().computeOnServer(server -> {
            var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(
                    server.overworld(), new BlockPos(fx, BY, BZ), null);
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                storage.extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(kerolox.source()),
                        bucket, transaction);
                transaction.commit();
            }
            return ((FuelTankBlockEntity) server.overworld()
                    .getBlockEntity(new BlockPos(fx, BY, BZ))).propellantKg();
        });
        assertThat(afterExtract < 0.01, "Бак должен опустеть, осталось: " + afterExtract);
        log("жидкость: слив в трубу опустошил бак ✓");

        // Регрессия: неровный (не кратный капле) остаток не должен «залипать»
        // и запирать бак на своём типе после полного слива трубой
        String afterUneven = sp.getServer().computeOnServer(server -> {
            var tank = (FuelTankBlockEntity) server.overworld().getBlockEntity(new BlockPos(fx, BY, BZ));
            tank.setPropellant(999.99977, kerolox.fuelId()); // заведомо не кратно капле
            var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(
                    server.overworld(), new BlockPos(fx, BY, BZ), null);
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                storage.extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(kerolox.source()),
                        Long.MAX_VALUE, transaction);
                transaction.commit();
            }
            return tank.fuelType() + "|" + tank.propellantKg();
        });
        assertThat(afterUneven.startsWith("|"),
                "После полного слива бак должен освободить тип, получено: " + afterUneven);
        log("жидкость: неровный остаток не запер бак (" + afterUneven + ") ✓");

        // И после освобождения бак принимает ДРУГОЕ топливо
        double reused = sp.getServer().computeOnServer(server -> {
            var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(
                    server.overworld(), new BlockPos(fx, BY, BZ), null);
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long accepted = storage.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(
                        org.alex_melan.spacereloaded.fluid.ModFluids.HYDROLOX.source()), bucket, transaction);
                transaction.abort();
                return accepted;
            }
        });
        assertThat(reused > 0, "Освободившийся бак должен принять другое топливо, принято: " + reused);
        log("жидкость: освободившийся бак сменил тип ✓");
        sp.getServer().runCommand(set(fx, BY, BZ, "minecraft:air"));
    }

    // ---------- 20. Прогрессия: у каждого тела свой металл ----------

    private void testBodyExclusiveOres(ClientGameTestContext context, TestSingleplayerContext sp) {
        // Лут пояса грузится: один "minecraft:air" в entries валил ВСЮ таблицу,
        // и астероидный камень молча не давал ничего
        boolean beltLoot = sp.getServer().computeOnServer(server -> !server.reloadableRegistries()
                .getLootTable(ModBlocks.ASTEROID_STONE.getLootTable().orElseThrow())
                .equals(net.minecraft.world.level.storage.loot.LootTable.EMPTY));
        assertThat(beltLoot, "Лут-таблица астероидного камня должна парситься");
        log("прогрессия: лут пояса загружен ✓");

        int moonTitanium = sp.getServer().computeOnServer(server ->
                countOre(server, "moon", ModBlocks.MOON_TITANIUM_ORE));
        assertThat(moonTitanium > 0, "На Луне должен генерироваться титан, найдено: " + moonTitanium);
        log("прогрессия: лунный титан, жил " + moonTitanium + " блоков ✓");

        int marsTungsten = sp.getServer().computeOnServer(server ->
                countOre(server, "mars", ModBlocks.MARS_TUNGSTEN_ORE));
        assertThat(marsTungsten > 0, "На Марсе должен генерироваться вольфрам, найдено: " + marsTungsten);
        log("прогрессия: марсианский вольфрам, жил " + marsTungsten + " блоков ✓");
    }

    /** Сколько блоков руды в 3x3 чанках вокруг начала координат тела. */
    private static int countOre(net.minecraft.server.MinecraftServer server, String planet,
                                net.minecraft.world.level.block.Block ore) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("spacereloaded", planet)));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int chunkX = -1; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) {
                level.getChunk(chunkX, chunkZ); // форсируем генерацию, не полагаемся на кэш
            }
        }
        for (int x = -24; x < 24; x++) {
            for (int z = -24; z < 24; z++) {
                for (int y = 2; y < 90; y++) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(ore)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

}
