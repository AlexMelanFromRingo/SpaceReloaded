package org.alex_melan.spacereloaded.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
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
import org.alex_melan.spacereloaded.registry.ModDataComponents;
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
        context.waitTicks(60); // кулдаун пушки
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
}
