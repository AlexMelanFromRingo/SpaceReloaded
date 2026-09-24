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

            // SR_ONLY=testA,testB — только выбранные сценарии (testSealing включает вакуум — идёт всегда)
            String only = System.getenv("SR_ONLY");
            java.util.Set<String> selected = only == null || only.isBlank() ? null
                    : new java.util.HashSet<>(java.util.Arrays.asList(only.split(",")));
            java.util.LinkedHashMap<String, Runnable> scenarios = new java.util.LinkedHashMap<>();
            scenarios.put("testSealing", () -> testSealing(context, sp));
            scenarios.put("testCrusher", () -> testCrusher(context, sp));
            scenarios.put("testRocketAssembly", () -> testRocketAssembly(context, sp));
            scenarios.put("testOrbitalCannon", () -> testOrbitalCannon(context, sp));
            scenarios.put("testDocking", () -> testDocking(context, sp));
            scenarios.put("testScanAndProgram", () -> testScanAndProgram(context, sp));
            scenarios.put("testBatteryBalancing", () -> testBatteryBalancing(context, sp));
            scenarios.put("testCargoLoop", () -> testCargoLoop(context, sp));
            scenarios.put("testMeteor", () -> testMeteor(context, sp));
            scenarios.put("testRedstoneAirlock", () -> testRedstoneAirlock(context, sp));
            scenarios.put("testTelemetryScreen", () -> testTelemetryScreen(context, sp));
            scenarios.put("testMarsChemistry", () -> testMarsChemistry(context, sp));
            scenarios.put("testOrbitalNetwork", () -> testOrbitalNetwork(context, sp));
            scenarios.put("testDeepSpace", () -> testDeepSpace(context, sp));
            scenarios.put("testNavigation", () -> testNavigation(context, sp));
            scenarios.put("testPlanetTerrain", () -> testPlanetTerrain(context, sp));
            scenarios.put("testPropellantFluids", () -> testPropellantFluids(context, sp));
            scenarios.put("testBodyExclusiveOres", () -> testBodyExclusiveOres(context, sp));
            scenarios.put("testStaging", () -> testStaging(context, sp));
            scenarios.put("testAttitude", () -> testAttitude(context, sp));
            scenarios.put("testAtmosphere", () -> testAtmosphere(context, sp));
            scenarios.put("testStrikeGuidance", () -> testStrikeGuidance(context, sp));
            scenarios.put("testCargoLine", () -> testCargoLine(context, sp));
            scenarios.put("testWetWorkshop", () -> testWetWorkshop(context, sp));
            scenarios.put("testMassDriver", () -> testMassDriver(context, sp));
            scenarios.put("testMassCatcher", () -> testMassCatcher(context, sp));
            scenarios.put("testRegolithReactor", () -> testRegolithReactor(context, sp));
            scenarios.put("testTransmission", () -> testTransmission(context, sp));
            scenarios.put("testShaftShear", () -> testShaftShear(context, sp));
            scenarios.put("testPressFlywheel", () -> testPressFlywheel(context, sp));
            scenarios.put("testStackColumn", () -> testStackColumn(context, sp));
            scenarios.put("testEngineQuality", () -> testEngineQuality(context, sp));
            scenarios.put("testExplosionSealing", () -> testExplosionSealing(context, sp));
            scenarios.put("testChemistryChain", () -> testChemistryChain(context, sp));
            scenarios.put("testCrystalAndSaw", () -> testCrystalAndSaw(context, sp));
            scenarios.put("testCleanroomFab", () -> testCleanroomFab(context, sp));
            scenarios.put("testSuperalloyEngine", () -> testSuperalloyEngine(context, sp));
            scenarios.put("testCabinAir", () -> testCabinAir(context, sp));
            scenarios.put("testGreenhouse", () -> testGreenhouse(context, sp));
            scenarios.put("testSpinRing", () -> testSpinRing(context, sp));
            scenarios.put("testRover", () -> testRover(context, sp));
            scenarios.put("testOrbitalImaging", () -> testOrbitalImaging(context, sp));
            scenarios.put("testVisualShowcase", () -> testVisualShowcase(context, sp));
            scenarios.forEach((name, scenario) -> {
                if (selected == null || selected.contains(name) || name.equals("testSealing")) {
                    scenario.run();
                }
            });
        }
    }

    // ---------- 25. Точность орбитального удара (Полёт 2.0, US4) ----------

    /**
     * Без спутникового покрытия цели лом рассеивается в круге из конфига, с покрытием
     * ложится в метку; терминал показывает режим и прогноз скорости удара.
     */
    private void testStrikeGuidance(ClientGameTestContext context, TestSingleplayerContext sp) {
        int gx = BX + 680;
        moveTo(context, sp, gx - 10, BZ);
        // Мишень 41×41 из камня — хватает на кратер r≈13 со смещением до 10; игрок в стороне
        sp.getServer().runCommand(fill(gx - 20, BY, BZ - 20, gx + 20, BY, BZ + 20, "minecraft:stone"));
        context.waitTick();
        sp.getServer().runCommand(String.format("tp @p %d %d %d", gx - 45, BY, BZ));
        BlockPos cannonPos = new BlockPos(70, 120, 70);
        double unguidedSpread = SpaceReloaded.config().cannonUnguidedSpreadBlocks;

        // Без покрытия: терминал честно предупреждает, лом уходит с рассеиванием
        String unguided = sp.getServer().computeOnServer(server -> fireAt(server, cannonPos, gx, 0));
        assertThat(unguided.startsWith("ok"), "Ненаводимый выстрел: " + unguided);
        assertThat(unguided.contains("guided=false"), "Снимок терминала: без покрытия режим ненаводимый: " + unguided);
        assertThat(unguided.contains("impact"), "Выстрел должен состояться: " + unguided);
        double[] hit = waitForCrater(context, sp, gx);
        assertThat(hit[0] >= 0, "Ненаводимый лом должен оставить кратер в мишени");
        assertThat(hit[0] <= unguidedSpread + 1.5,
                "Смещение ненаводимого удара в пределах разброса " + unguidedSpread + ", получено " + hit[0]);
        log(String.format("без покрытия: смещение удара %.1f бл. (разброс до %.0f) ✓", hit[0], unguidedSpread));

        // С покрытием: восстановить мишень, дождаться перезарядки, выстрел ложится в метку
        sp.getServer().runCommand(fill(gx - 20, BY, BZ - 20, gx + 20, BY, BZ + 20, "minecraft:stone"));
        context.waitTicks(220);
        String guided = sp.getServer().computeOnServer(server -> fireAt(server, cannonPos, gx, 1));
        assertThat(guided.startsWith("ok"), "Наводимый выстрел: " + guided);
        assertThat(guided.contains("guided=true"), "Снимок терминала: с покрытием режим спутниковый: " + guided);
        double[] guidedHit = waitForCrater(context, sp, gx);
        assertThat(guidedHit[0] >= 0 && guidedHit[0] <= 1.5,
                "Наводимый удар ложится в метку (≤ 1.5 бл.), получено " + guidedHit[0]);
        log(String.format("по спутнику: смещение удара %.1f бл. ✓", guidedHit[0]));

        sp.getServer().runOnServer(server -> org.alex_melan.spacereloaded.network.SpaceNetworkState
                .get(server).setCoverage(Level.OVERWORLD, 0));
    }

    /** Пушка на орбите с полным зарядом бьёт по (gx, BY, BZ) при заданном покрытии оверворлда. */
    private static String fireAt(net.minecraft.server.MinecraftServer server, BlockPos cannonPos,
                                 int gx, int coverage) {
        ServerLevel orbit = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")));
        if (orbit == null) {
            return "нет измерения орбиты";
        }
        org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server).setCoverage(Level.OVERWORLD, coverage);
        orbit.setBlock(cannonPos, ModBlocks.ORBITAL_CANNON.defaultBlockState(), 3);
        if (!(orbit.getBlockEntity(cannonPos) instanceof OrbitalCannonBlockEntity cannon)) {
            return "нет BE пушки";
        }
        cannon.loadRod();
        ((SimpleEnergyStorage) cannon.energyStorage()).amount = SpaceReloaded.config().cannonEnergyCapacity;
        cannon.setTarget(GlobalPos.of(Level.OVERWORLD, new BlockPos(gx, BY, BZ)));
        var snapshot = cannon.snapshot(orbit);
        String fired = cannon.tryFire(orbit).getString();
        return "ok guided=" + snapshot.guided() + " spread=" + snapshot.spreadBlocks()
                + " impact=" + Math.round(snapshot.impactSpeedMs()) + " | " + fired;
    }

    /**
     * Ждёт кратер в мишени и возвращает {смещение центра кратера от метки, число блоков воздуха}.
     * Центр — центроид блоков воздуха в плоскости мишени.
     */
    private double[] waitForCrater(ClientGameTestContext context, TestSingleplayerContext sp, int gx) {
        for (int waited = 0; waited < 400; waited += 20) {
            context.waitTicks(20);
            double[] result = sp.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                double sumX = 0;
                double sumZ = 0;
                int air = 0;
                for (int x = gx - 20; x <= gx + 20; x++) {
                    for (int z = BZ - 20; z <= BZ + 20; z++) {
                        cursor.set(x, BY, z);
                        if (level.getBlockState(cursor).isAir()) {
                            sumX += x;
                            sumZ += z;
                            air++;
                        }
                    }
                }
                if (air == 0) {
                    return new double[] {-1, 0};
                }
                return new double[] {Math.hypot(sumX / air - gx, sumZ / air - BZ), air};
            });
            if (result[0] >= 0) {
                log(String.format("кратер: %d блоков воздуха, центр в %.1f бл. от метки", (int) result[1], result[0]));
                return result;
            }
        }
        return new double[] {-1, 0};
    }

    // ---------- 24. Атмосфера (Полёт 2.0, US3) ----------

    /**
     * Скан на Земле моделирует подъём (эталон достигает 450 м, напор > 0);
     * отвесный вход на 300 м/с без капсулы зажигает флаг нагрева на Земле
     * и не зажигает на Луне (вакуум — та же ракета, та же скорость).
     */
    private void testAtmosphere(ClientGameTestContext context, TestSingleplayerContext sp) {
        int hx = BX + 600;
        moveTo(context, sp, hx - 5, BZ);
        buildSingleStack(context, sp, hx, false);

        RocketInteractions.ScanResult scan = sp.getServer().computeOnServer(server ->
                RocketInteractions.scanResultAtPylon(server.overworld(), new BlockPos(hx - 2, BY + 3, BZ),
                        server.getPlayerList().getPlayers().get(0)));
        assertThat(scan != null, "Скан эталонной ракеты должен прочитаться");
        assertThat(scan.payload().ascentReached(),
                "Моделирование подъёма: эталон достигает высоты перехода, апогей " + scan.payload().ascentApexM());
        assertThat(scan.payload().maxQPa() > 0, "В атмосфере Земли скоростной напор > 0");
        assertThat(scan.payload().ascentDeltaV() > 0, "Стоимость подъёма > 0");
        log(String.format("скан: подъём до %.0f м за Δv %.0f м/с, макс. напор %.1f кПа ✓",
                scan.payload().ascentApexM(), scan.payload().ascentDeltaV(), scan.payload().maxQPa() / 1000));

        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(hx - 2, BY + 3, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        AABB area = new AABB(hx - 24, BY - 200, BZ - 24, hx + 24, BY + 400, BZ + 24);

        // Земля: аппарат на 260 м, отвесно вниз на 300 м/с → нагрев
        String earth = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked);
            if (rockets.size() != 1) {
                return "ожидалась 1 ракета, найдено " + rockets.size();
            }
            RocketEntity rocket = rockets.get(0);
            rocket.setPos(hx + 0.5, 260, BZ + 0.5);
            if (!rocket.ignite(server.overworld())) {
                return "ignite failed";
            }
            rocket.setFlightVelocity(new net.minecraft.world.phys.Vec3(0, -300, 0));
            return "ok";
        });
        assertThat(earth.equals("ok"), "Подготовка входа: " + earth);
        context.waitTicks(2);
        boolean heatingEarth = sp.getServer().computeOnServer(server ->
                server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        area, e -> true).stream().anyMatch(RocketEntity::clientHeating));
        assertThat(heatingEarth, "Вход на 300 м/с в атмосфере Земли должен зажечь флаг нагрева");
        log("нагрев на Земле ✓");
        sp.getServer().runOnServer(server -> server.overworld().getEntities(
                EntityTypeTest.forClass(RocketEntity.class), area, e -> true).forEach(Entity::discard));

        // Луна: та же модель, но профиль тела без атмосферы — плотность 0 на любой высоте,
        // индекс нагрева 0 (харнесс клиентского стенда не переносит игрока между измерениями,
        // а безлюдное измерение не отслеживает свежие сущности — проверяем данные профиля
        // и ту же формулу, что использует борт; сама формула покрыта AerothermalTest)
        String moonCheck = sp.getServer().computeOnServer(server -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("spacereloaded", "moon")));
            if (level == null) {
                return "нет измерения Луны";
            }
            var env = org.alex_melan.spacereloaded.planet.PlanetManager.environment(level);
            var earthEnv = org.alex_melan.spacereloaded.planet.PlanetManager.environment(server.overworld());
            double moonHeat = org.alex_melan.spacereloaded.core.rocketry.Aerothermal
                    .heatIndex(env.density(200), 300);
            double earthHeat = org.alex_melan.spacereloaded.core.rocketry.Aerothermal
                    .heatIndex(earthEnv.density(200), 300);
            return "moonVacuum=" + env.atmosphere().isVacuum() + " moonDensity=" + env.density(100)
                    + " moonHeat=" + moonHeat + " earthHeat=" + Math.round(earthHeat);
        });
        assertThat(moonCheck.contains("moonVacuum=true") && moonCheck.contains("moonHeat=0.0"),
                "Профиль Луны — вакуум без нагрева: " + moonCheck);
        assertThat(!moonCheck.contains("earthHeat=0"), "На Земле тот же вход греет: " + moonCheck);
        log("на Луне нагрева нет, вакуум (" + moonCheck + ") ✓");
        sp.getServer().runCommand(String.format("tp @p %d %d %d", hx - 5, BY, BZ));
        context.waitTicks(5);
    }

    // ---------- 23. Ориентация (Полёт 2.0, US2) ----------

    /**
     * Пилот держит «вперёд» на тяге: с гироскопом ракета наклоняется по взгляду
     * и набирает горизонтальную скорость; без гироскопа команда не действует.
     */
    private void testAttitude(ClientGameTestContext context, TestSingleplayerContext sp) {
        int ax = BX + 520;
        moveTo(context, sp, ax - 5, BZ);
        buildSingleStack(context, sp, ax, true);
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(ax - 2, BY + 3, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        AABB area = new AABB(ax - 200, BY - 200, BZ - 200, ax + 200, BY + 400, BZ + 200);

        String started = sp.getServer().computeOnServer(server -> flyHoldingForward(server, area));
        assertThat(started.equals("ok"), "Старт с пилотом должен пройти, получено: " + started);
        int piloted = holdForwardFor(context, sp, area, 100);
        log("ориентация: пилот на борту тиков: " + piloted + " из 100");
        double[] withGyro = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, e -> true);
            if (rockets.isEmpty()) {
                return new double[] {-1, -1, -1};
            }
            RocketEntity rocket = rockets.get(0);
            double horizontal = Math.hypot(rocket.getDeltaMovement().x, rocket.getDeltaMovement().z) / 0.05;
            double commanded = Math.hypot(rocket.clientCmdPitchDeg(), rocket.clientCmdRollDeg());
            double actual = Math.hypot(rocket.pitchDeg(), rocket.rollDeg());
            return new double[] {horizontal, commanded, actual};
        });
        assertThat(withGyro[1] > 20, "Командуемый наклон должен вырасти, получено: " + withGyro[1]);
        assertThat(withGyro[2] > 10, "С гироскопом фактический наклон следует за командой, получено: " + withGyro[2]);
        assertThat(withGyro[0] >= 5.0,
                "Наклон на тяге даёт горизонтальную скорость >= 5 м/с, получено: " + withGyro[0]);
        log(String.format("ориентация: команда %.0f°, факт %.0f°, горизонтальная %.1f м/с ✓",
                withGyro[1], withGyro[2], withGyro[0]));
        cleanupFlight(sp, area);

        // Без гироскопа: команда есть, наклона нет
        int bx = ax + 40;
        moveTo(context, sp, bx - 5, BZ);
        buildSingleStack(context, sp, bx, false);
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(bx - 2, BY + 3, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        AABB areaB = new AABB(bx - 200, BY - 200, BZ - 200, bx + 200, BY + 400, BZ + 200);
        String startedB = sp.getServer().computeOnServer(server -> flyHoldingForward(server, areaB));
        assertThat(startedB.equals("ok"), "Старт без гироскопа должен пройти, получено: " + startedB);
        holdForwardFor(context, sp, areaB, 100);
        double[] noGyro = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), areaB, e -> true);
            if (rockets.isEmpty()) {
                return new double[] {-1, -1};
            }
            RocketEntity rocket = rockets.get(0);
            return new double[] {Math.hypot(rocket.pitchDeg(), rocket.rollDeg()), rocket.clientHasGyro() ? 1 : 0};
        });
        assertThat(noGyro[1] == 0, "У стека без гироскопа флаг управления должен быть снят");
        assertThat(noGyro[0] >= 0 && noGyro[0] < 1.0,
                "Без гироскопа наклон не меняется (< 1°), получено: " + noGyro[0]);
        log("без гироскопа команда игнорируется ✓");
        cleanupFlight(sp, areaB);
        sp.getServer().runCommand(String.format("tp @p %d %d %d", bx - 5, BY, BZ));
        context.waitTicks(5);
    }

    /** Одноступенчатый стек: двигатель, бак, командный модуль (+ гироскоп сбоку). */
    private void buildSingleStack(ClientGameTestContext context, TestSingleplayerContext sp, int x, boolean gyro) {
        sp.getServer().runCommand(fill(x - 1, BY, BZ - 1, x + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(x - 2, BY + 1, BZ, x - 2, BY + 5, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(x - 2, BY, BZ, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(set(x, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(x, BY + 2, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(x, BY + 3, BZ, "spacereloaded:command_module"));
        if (gyro) {
            sp.getServer().runCommand(set(x + 1, BY + 3, BZ, "spacereloaded:gyroscope"));
        }
        context.waitTick();
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(x, BY + 2, BZ))
                    instanceof FuelTankBlockEntity tank) {
                tank.setPropellant(2000.0, "spacereloaded:kerolox");
            }
        });
    }

    /** Пилот садится, смотрит на юг, держит «вперёд» + тягу, борт зажигается. */
    private static String flyHoldingForward(net.minecraft.server.MinecraftServer server, AABB area) {
        List<RocketEntity> rockets = server.overworld().getEntities(
                EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked);
        if (rockets.size() != 1) {
            return "ожидалась 1 ракета, найдено " + rockets.size();
        }
        RocketEntity rocket = rockets.get(0);
        var player = server.getPlayerList().getPlayers().get(0);
        player.setShiftKeyDown(false); // sneak на сервере = «слезть» (Player.rideTick)
        if (!player.startRiding(rocket, true, true)) {
            return "startRiding failed";
        }
        player.setYHeadRot(0.0f); // взгляд на юг (+Z): «вперёд» наклоняет к +Z
        player.setLastClientInput(new net.minecraft.world.entity.player.Input(
                true, false, false, false, true, false, false));
        if (!rocket.ignite(server.overworld())) {
            return "ignite failed";
        }
        rocket.setFlightVelocity(new net.minecraft.world.phys.Vec3(0, 5, 0));
        return "ok";
    }

    /**
     * Держит «вперёд + тяга» {@code ticks} тиков. Харнесс клиентского стенда ссаживает
     * серверно посаженного игрока между тиками (см. api-notes), поэтому каждый тик пилот
     * сажается заново тем же вызовом, что и в игре, и ввод подаётся штатной записью Input.
     *
     * @return сколько тиков борт видел пилота на борту
     */
    private static int holdForwardFor(ClientGameTestContext context, TestSingleplayerContext sp,
                                      AABB area, int ticks) {
        int piloted = 0;
        for (int i = 0; i < ticks; i++) {
            context.waitTick();
            piloted += sp.getServer().computeOnServer(server -> {
                List<RocketEntity> rockets = server.overworld().getEntities(
                        EntityTypeTest.forClass(RocketEntity.class), area, r -> !r.isParked());
                if (rockets.isEmpty()) {
                    return 0;
                }
                RocketEntity rocket = rockets.get(0);
                var player = server.getPlayerList().getPlayers().get(0);
                int seen = rocket.getFirstPassenger() == player ? 1 : 0;
                if (player.getVehicle() != rocket) {
                    player.setShiftKeyDown(false);
                    player.startRiding(rocket, true, true);
                }
                player.setYHeadRot(0.0f);
                player.setLastClientInput(new net.minecraft.world.entity.player.Input(
                        true, false, false, false, true, false, false));
                return seen;
            });
        }
        return piloted;
    }

    /** Пилот наземь, аппараты в области — прочь. */
    private static void cleanupFlight(TestSingleplayerContext sp, AABB area) {
        sp.getServer().runOnServer(server -> {
            server.getPlayerList().getPlayers().get(0).stopRiding();
            server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                    area, e -> true).forEach(Entity::discard);
        });
    }

    // ---------- 22. Ступени (Полёт 2.0, US1) ----------

    /**
     * Двухступенчатый стек: скан считает Δv по ступеням, беспилотный старт
     * сам отбрасывает выгоревшую первую ступень (обломок падает и исчезает),
     * ручное отделение доступно только пилоту в полёте.
     */
    private void testStaging(ClientGameTestContext context, TestSingleplayerContext sp) {
        int sx = BX + 440;
        moveTo(context, sp, sx - 5, BZ);
        buildTwoStageStack(context, sp, sx, 150.0);

        // Скан: две ступени, суммарный Δv = сумма ступеней, вторая ступень летит
        RocketInteractions.ScanResult scan = sp.getServer().computeOnServer(server ->
                RocketInteractions.scanResultAtPylon(server.overworld(), new BlockPos(sx - 2, BY + 4, BZ),
                        server.getPlayerList().getPlayers().get(0)));
        assertThat(scan != null, "Скан двухступенчатого стека должен прочитаться");
        assertThat(scan.staged().stageCount() == 2,
                "Ожидались 2 ступени, получено: " + scan.staged().stageCount());
        double dv1 = scan.staged().stages().get(0).deltaV();
        double dv2 = scan.staged().stages().get(1).deltaV();
        assertThat(dv1 > 0 && dv2 > 0
                        && Math.abs(scan.staged().totalDeltaV() - (dv1 + dv2)) < 1.0,
                "Δv стека = сумма ступеней: " + dv1 + " + " + dv2 + " = " + scan.staged().totalDeltaV());
        assertThat(scan.payload().stages().size() == 2, "В пакете скана должны быть 2 строки ступеней");
        log(String.format("ступени: Δv₁=%.0f, Δv₂=%.0f, сумма %.0f м/с ✓", dv1, dv2, scan.staged().totalDeltaV()));

        // Сборка: топливо по ступеням честное (150 кг внизу, 2000 кг вверху)
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(sx - 2, BY + 4, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        AABB area = new AABB(sx - 24, BY - 200, BZ - 24, sx + 24, BY + 400, BZ + 24);
        double[] fuel = sp.getServer().computeOnServer(server -> {
            List<RocketEntity> rockets = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked);
            if (rockets.size() != 1) {
                return new double[] {-1, -1, rockets.size()};
            }
            RocketEntity rocket = rockets.get(0);
            return new double[] {rocket.stagePropellantKg(0), rocket.stagePropellantKg(1), rocket.stageCount()};
        });
        assertThat(fuel[2] == 2, "Собранная ракета должна иметь 2 ступени, получено: " + fuel[2]);
        assertThat(Math.abs(fuel[0] - 150.0) < 1.0 && Math.abs(fuel[1] - 2000.0) < 1.0,
                "Топливо по ступеням: ожидалось 150/2000, получено " + fuel[0] + "/" + fuel[1]);
        log("сборка: топливо по ступеням 150/2000 кг ✓");

        // Отделение на стоянке — отказ (валидация сервера)
        String parked = sp.getServer().computeOnServer(server -> {
            RocketEntity rocket = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked).get(0);
            var player = server.getPlayerList().getPlayers().get(0);
            player.startRiding(rocket, true, true);
            String answer = rocket.requestStageSeparation(player).getString();
            player.stopRiding();
            return answer;
        });
        assertThat(parked.contains("only in flight"),
                "На стоянке отделение должно отклоняться, получено: " + parked);
        log("отделение на стоянке отклонено ✓");

        // Беспилотный старт: первая ступень (150 кг) выгорает за секунды — автопилот отделяет её сам
        String launch = sp.getServer().computeOnServer(server -> {
            RocketEntity rocket = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked).get(0);
            return rocket.launchUnmanned(server.overworld()).getString();
        });
        assertThat(launch.contains("Autopilot"), "Беспилотный старт должен состояться, получено: " + launch);
        boolean separated = false;
        for (int waited = 0; waited < 400 && !separated; waited += 10) {
            context.waitTicks(10);
            separated = sp.getServer().computeOnServer(server -> {
                List<RocketEntity> rockets = server.overworld().getEntities(
                        EntityTypeTest.forClass(RocketEntity.class), area, e -> true);
                boolean debris = rockets.stream().anyMatch(RocketEntity::isDebris);
                boolean upper = rockets.stream().anyMatch(r -> !r.isDebris() && r.stageCount() == 1);
                return debris && upper;
            });
        }
        assertThat(separated, "Автопилот должен отделить выгоревшую ступень: обломок + одноступенчатый остаток");
        log("автопилот: ступень отделена, обломок в полёте ✓");

        // Обломок падает и исчезает (крушение) либо паркуется — вечных объектов нет
        boolean debrisGone = false;
        for (int waited = 0; waited < 1200 && !debrisGone; waited += 20) {
            context.waitTicks(20);
            debrisGone = sp.getServer().computeOnServer(server ->
                    server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                            area, RocketEntity::isDebris).isEmpty());
        }
        assertThat(debrisGone, "Обломок ступени должен разрушиться или припарковаться за 60 с");
        log("обломок утилизирован честно ✓");

        // Ручное отделение пилотом в полёте
        int mx = sx + 40;
        moveTo(context, sp, mx - 5, BZ);
        buildTwoStageStack(context, sp, mx, 2000.0);
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(mx - 2, BY + 4, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        AABB manualArea = new AABB(mx - 24, BY - 200, BZ - 24, mx + 24, BY + 400, BZ + 24);
        String manual = sp.getServer().computeOnServer(server -> {
            RocketEntity rocket = server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), manualArea, RocketEntity::isParked).get(0);
            var player = server.getPlayerList().getPlayers().get(0);
            // Sneak на сервере = «хочу слезть» (Player.rideTick): снимаем флаг явно
            player.setShiftKeyDown(false);
            if (!player.startRiding(rocket, true, true)) {
                return "startRiding failed";
            }
            // Пилот держит тягу (штатный ввод) — и на всякий случай борт уже идёт вверх
            player.setLastClientInput(new net.minecraft.world.entity.player.Input(
                    false, false, false, false, true, false, false));
            if (!rocket.ignite(server.overworld())) {
                return "ignite failed";
            }
            rocket.setFlightVelocity(new net.minecraft.world.phys.Vec3(0, 20, 0));
            return "ok id=" + rocket.getId() + " riding=" + (player.getVehicle() == rocket)
                    + " mode=" + player.gameMode.getGameModeForPlayer() + " spectator=" + player.isSpectator();
        });
        assertThat(manual.startsWith("ok"), "Зажигание с пилотом должно пройти, получено: " + manual);
        log("ручной старт: " + manual);
        context.waitTicks(20);
        String answer = sp.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().get(0);
            // Стенд: если харнесс ссадил игрока между тиками, сажаем обратно тем же вызовом —
            // проверяем серверную валидацию и само отделение с пилотом на борту
            if (!(player.getVehicle() instanceof RocketEntity)) {
                List<RocketEntity> rockets = server.overworld().getEntities(
                        EntityTypeTest.forClass(RocketEntity.class), manualArea, r -> !r.isParked());
                if (!rockets.isEmpty()) {
                    player.setShiftKeyDown(false);
                    player.startRiding(rockets.get(0), true, true);
                }
            }
            if (!(player.getVehicle() instanceof RocketEntity rocket)) {
                List<RocketEntity> rockets = server.overworld().getEntities(
                        EntityTypeTest.forClass(RocketEntity.class), manualArea, e -> true);
                StringBuilder info = new StringBuilder("пилот не в ракете; sneak=" + player.isShiftKeyDown()
                        + " игрок y=" + Math.round(player.getY()) + " аппаратов=" + rockets.size());
                for (RocketEntity r : rockets) {
                    info.append(" [y=").append(Math.round(r.getY())).append(" launched=").append(!r.isParked())
                            .append(" debris=").append(r.isDebris()).append(" пассажиров=")
                            .append(r.getPassengers().size()).append(']');
                }
                return info.toString();
            }
            return rocket.requestStageSeparation(player).getString();
        });
        assertThat(answer.contains("separated"), "Пилот должен отделить ступень в полёте, получено: " + answer);
        context.waitTicks(2);
        int count = sp.getServer().computeOnServer(server -> server.overworld().getEntities(
                EntityTypeTest.forClass(RocketEntity.class), manualArea, e -> true).size());
        assertThat(count == 2, "После ручного отделения должно быть 2 аппарата, найдено: " + count);
        log("ручное отделение пилотом: " + answer + " ✓");

        // Уборка: игрок наземь, аппараты прочь
        sp.getServer().runOnServer(server -> {
            server.getPlayerList().getPlayers().get(0).stopRiding();
            server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                    manualArea, e -> true).forEach(Entity::discard);
        });
        sp.getServer().runCommand(String.format("tp @p %d %d %d", mx - 5, BY, BZ));
        context.waitTicks(5);
    }

    /**
     * Стек: два двигателя + бак + разделитель | двигатель + бак + командный модуль
     * на площадке 3×3 с пилоном высотой 7. Первая ступень заправлена на lowerFuelKg.
     */
    private void buildTwoStageStack(ClientGameTestContext context, TestSingleplayerContext sp,
                                    int x, double lowerFuelKg) {
        sp.getServer().runCommand(fill(x - 1, BY, BZ - 1, x + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(x - 2, BY + 1, BZ, x - 2, BY + 7, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(x - 2, BY, BZ, "spacereloaded:launch_pad"));
        // Два двигателя симметрично по бокам корпуса: без гиродинов асимметрия опрокинула бы стек
        sp.getServer().runCommand(set(x - 1, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(x, BY + 1, BZ, "spacereloaded:rocket_hull"));
        sp.getServer().runCommand(set(x + 1, BY + 1, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(x, BY + 2, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(x, BY + 3, BZ, "spacereloaded:stage_separator"));
        sp.getServer().runCommand(set(x, BY + 4, BZ, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(set(x, BY + 5, BZ, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(x, BY + 6, BZ, "spacereloaded:command_module"));
        context.waitTick();
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(new BlockPos(x, BY + 2, BZ))
                    instanceof FuelTankBlockEntity lower) {
                lower.setPropellant(lowerFuelKg, "spacereloaded:kerolox");
            }
            if (server.overworld().getBlockEntity(new BlockPos(x, BY + 5, BZ))
                    instanceof FuelTankBlockEntity upper) {
                upper.setPropellant(2000.0, "spacereloaded:kerolox");
            }
        });
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
        // Полёт 2.0: без спутникового покрытия лом рассеивается — для детерминизма
        // существующего сценария даём покрытие (снимается в конце сценария)
        sp.getServer().runOnServer(server -> org.alex_melan.spacereloaded.network.SpaceNetworkState
                .get(server).setCoverage(Level.OVERWORLD, 1));
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
            return TargetingDesignatorItem.remoteFire(server, designator, null).getString();
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
        // Вернуть мир к состоянию без покрытия (сценарии сетей и наведения проверяют его сами)
        sp.getServer().runOnServer(server -> org.alex_melan.spacereloaded.network.SpaceNetworkState
                .get(server).setCoverage(Level.OVERWORLD, 0));
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
            program.set(ModDataComponents.GUIDANCE_TIER, 2);
            String message = rockets.get(0).installProgram(server.overworld(), program).getString();
            return rockets.get(0).guidanceTier() == 2 ? message : "тир наведения не перенесён";
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
        // 007: герметичная, но пустая зона в вакууме непригодна — экран красный, пока нет газа
        int emptyStatus = 0;
        for (int waited = 0; waited < 200 && emptyStatus != 2; waited += 10) {
            context.waitTicks(10);
            emptyStatus = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(tsx + 6, BY + 1, BZ + 2))
                            .getValue(org.alex_melan.spacereloaded.sealing.TelemetryScreenBlock.STATUS));
        }
        assertThat(emptyStatus == 2, "Пустая зона без газа — экран красный (2), получено: " + emptyStatus);
        // Баллоны у контроллера: зона наполняется газом, экран зеленеет
        BlockPos o2 = new BlockPos(tsx + 2, BY, BZ + 2);
        BlockPos n2 = new BlockPos(tsx + 3, BY + 1, BZ + 2);
        sp.getServer().runCommand(set(o2.getX(), o2.getY(), o2.getZ(), "spacereloaded:gas_tank"));
        sp.getServer().runCommand(set(n2.getX(), n2.getY(), n2.getZ(), "spacereloaded:gas_tank"));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> {
            tank(server, o2).insert(org.alex_melan.spacereloaded.lifesupport.GasKind.OXYGEN, 20);
            tank(server, n2).insert(org.alex_melan.spacereloaded.lifesupport.GasKind.NITROGEN, 60);
        });
        int sealedStatus = 0;
        for (int waited = 0; waited < 900 && sealedStatus != 1; waited += 10) {
            context.waitTicks(10);
            sealedStatus = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(new BlockPos(tsx + 6, BY + 1, BZ + 2))
                            .getValue(org.alex_melan.spacereloaded.sealing.TelemetryScreenBlock.STATUS));
        }
        assertThat(sealedStatus == 1, "Экран должен показать ЗАМКНУТО (1), получено: " + sealedStatus);
        log("экран телеметрии: пустая зона красная, после наполнения из баллонов — ЗАМКНУТО ✓");
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

    // ---------- 26. Грузовая линия: терминал — отказ по Δv → заправка → автозапуск ----------

    private void testCargoLine(ClientGameTestContext context, TestSingleplayerContext sp) {
        int lx = BX + 720;
        moveTo(context, sp, lx - 5, BZ);
        var config = SpaceReloaded.config();
        int savedDwell = config.cargoLineDwellTicks;
        config.cargoLineDwellTicks = 60; // стенд: короткая выдержка
        try {
            sp.getServer().runCommand(fill(lx - 1, BY, BZ - 1, lx + 1, BY, BZ + 1, "spacereloaded:launch_pad"));
            sp.getServer().runCommand(fill(lx - 2, BY + 1, BZ, lx - 2, BY + 5, BZ, "spacereloaded:assembly_pylon"));
            sp.getServer().runCommand(set(lx - 2, BY, BZ, "spacereloaded:launch_pad"));
            sp.getServer().runCommand(set(lx, BY + 1, BZ, "spacereloaded:rocket_engine"));
            sp.getServer().runCommand(set(lx, BY + 2, BZ, "spacereloaded:fuel_tank"));
            sp.getServer().runCommand(set(lx, BY + 3, BZ, "spacereloaded:cargo_hold"));
            sp.getServer().runCommand(set(lx, BY + 4, BZ, "spacereloaded:command_module"));
            sp.getServer().runCommand(set(lx + 3, BY, BZ, "spacereloaded:cargo_terminal"));
            context.waitTick();
            sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                    server.overworld(), new BlockPos(lx - 2, BY + 3, BZ),
                    server.getPlayerList().getPlayers().get(0)));
            context.waitTicks(5);
            AABB area = new AABB(lx - 8, BY - 2, BZ - 8, lx + 8, BY + 12, BZ + 8);
            BlockPos terminalPos = new BlockPos(lx + 3, BY, BZ);

            // Программа линии: орбита Земли, маяк на орбите; режим AUTO
            String installed = sp.getServer().computeOnServer(server -> {
                if (!(server.overworld().getBlockEntity(terminalPos)
                        instanceof org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity terminal)) {
                    return "нет терминала";
                }
                ItemStack program = new ItemStack(ModItems.FLIGHT_PROGRAM);
                program.set(ModDataComponents.PROGRAM_DESTINATION,
                        Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit"));
                program.set(ModDataComponents.PROGRAM_PAD, GlobalPos.of(
                        ResourceKey.create(Registries.DIMENSION,
                                Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")),
                        new BlockPos(60, 101, 60)));
                String message = terminal.installProgram(program).getString();
                terminal.toggleMode();
                return terminal.mode().name() + ": " + message;
            });
            assertThat(installed.startsWith("AUTO"), "Терминал должен принять программу и перейти в AUTO: " + installed);
            log("терминал: " + installed + " ✓");

            // Пустой бак: после выдержки — честный отказ, борт на месте
            String refused = waitForTerminalState(context, sp, terminalPos, 400,
                    org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity.State.REFUSED);
            assertThat(refused.startsWith("REFUSED"), "Терминал должен отказать пустому борту, состояние: " + refused);
            boolean stillParked = sp.getServer().computeOnServer(server ->
                    !server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                            area, RocketEntity::isParked).isEmpty());
            assertThat(stillParked, "Борт без топлива должен остаться на площадке");
            log("грузовая линия: отказ — " + refused + " ✓");

            // Заправка → выдержка → автозапуск
            sp.getServer().runOnServer(server -> server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked)
                    .forEach(rocket -> rocket.refuel(2000)));
            String launched = waitForTerminalState(context, sp, terminalPos, 600,
                    org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity.State.LAUNCHED);
            assertThat(launched.startsWith("LAUNCHED"), "После заправки терминал должен отправить борт: " + launched);
            int departures = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockEntity(terminalPos)
                            instanceof org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity terminal
                            ? terminal.departures() : -1);
            assertThat(departures == 1, "Счётчик отправлений должен стать 1, получено: " + departures);
            boolean flying = sp.getServer().computeOnServer(server ->
                    server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                            new AABB(lx - 8, BY - 2, BZ - 8, lx + 8, BY + 400, BZ + 8),
                            rocket -> !rocket.isParked()).size() == 1
                    || server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                            area, RocketEntity::isParked).isEmpty());
            assertThat(flying, "Отправленный борт должен быть в полёте (или уже на орбите)");
            log("грузовая линия: заправка → автозапуск, рейс №" + departures + " ✓");
        } finally {
            config.cargoLineDwellTicks = savedDwell;
            sp.getServer().runOnServer(server -> server.overworld().getEntities(
                    EntityTypeTest.forClass(RocketEntity.class),
                    new AABB(lx - 40, BY - 200, BZ - 40, lx + 40, BY + 400, BZ + 40),
                    e -> true).forEach(Entity::discard));
        }
    }

    /** Ждёт состояние терминала; возвращает "STATE: detail" последнего опроса. */
    private static String waitForTerminalState(ClientGameTestContext context, TestSingleplayerContext sp,
                                               BlockPos terminalPos, int maxTicks,
                                               org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity.State expected) {
        String last = "";
        for (int waited = 0; waited < maxTicks; waited += 10) {
            context.waitTicks(10);
            last = sp.getServer().computeOnServer(server ->
                    server.overworld().getBlockEntity(terminalPos)
                            instanceof org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity terminal
                            ? terminal.state().name() + ": " + terminal.detail().getString()
                            : "нет терминала");
            if (last.startsWith(expected.name())) {
                return last;
            }
        }
        return last;
    }

    // ---------- 27. Wet workshop: борт → герметичный модуль ----------

    private void testWetWorkshop(ClientGameTestContext context, TestSingleplayerContext sp) {
        int wx = BX + 760;
        moveTo(context, sp, wx - 7, BZ);
        sp.getServer().runCommand(fill(wx - 2, BY, BZ - 2, wx + 2, BY, BZ + 2, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(wx - 3, BY + 1, BZ, wx - 3, BY + 8, BZ, "spacereloaded:assembly_pylon"));
        sp.getServer().runCommand(set(wx - 3, BY, BZ, "spacereloaded:launch_pad"));
        sp.getServer().runCommand(fill(wx - 2, BY + 1, BZ - 2, wx + 2, BY + 1, BZ + 2, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(fill(wx - 2, BY + 2, BZ - 2, wx + 2, BY + 6, BZ + 2, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(wx, BY + 7, BZ, "spacereloaded:command_module"));
        sp.getServer().runCommand(set(wx + 3, BY + 4, BZ, "spacereloaded:docking_port[facing=west]"));
        context.waitTick();
        sp.getServer().runOnServer(server -> RocketInteractions.assembleFromPylon(
                server.overworld(), new BlockPos(wx - 3, BY + 4, BZ),
                server.getPlayerList().getPlayers().get(0)));
        context.waitTicks(5);
        boolean assembled = sp.getServer().computeOnServer(server ->
                !server.overworld().getEntities(EntityTypeTest.forClass(RocketEntity.class),
                        new AABB(wx - 8, BY - 2, BZ - 8, wx + 8, BY + 12, BZ + 8), RocketEntity::isParked).isEmpty());
        assertThat(assembled, "Стек 5×5×7 должен собраться в борт");

        String converted = sp.getServer().computeOnServer(server ->
                org.alex_melan.spacereloaded.logistics.WetWorkshop.convert(server.overworld(),
                        new BlockPos(wx + 3, BY + 4, BZ), net.minecraft.core.Direction.WEST).getString());
        context.waitTick();
        int[] counts = sp.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            int air = 0;
            int hull = 0;
            int engines = 0;
            int hatch = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = wx - 2; x <= wx + 2; x++) {
                for (int z = BZ - 2; z <= BZ + 2; z++) {
                    for (int y = BY + 1; y <= BY + 7; y++) {
                        var state = level.getBlockState(cursor.set(x, y, z));
                        if (y == BY + 1 && state.is(ModBlocks.ROCKET_ENGINE)) {
                            engines++;
                        }
                        if (state.is(ModBlocks.MODULE_HULL)) {
                            hull++;
                        }
                        if (state.is(ModBlocks.HERMETIC_HATCH)) {
                            hatch++;
                        }
                        boolean interior = Math.abs(x - wx) <= 1 && Math.abs(z - BZ) <= 1
                                && y >= BY + 3 && y <= BY + 5;
                        if (interior && state.isAir()) {
                            air++;
                        }
                    }
                }
            }
            boolean command = level.getBlockState(new BlockPos(wx, BY + 7, BZ)).is(ModBlocks.COMMAND_MODULE);
            boolean hatchAtPort = level.getBlockState(new BlockPos(wx + 2, BY + 4, BZ)).is(ModBlocks.HERMETIC_HATCH);
            boolean noEntity = level.getEntities(EntityTypeTest.forClass(RocketEntity.class),
                    new AABB(wx - 8, BY - 2, BZ - 8, wx + 8, BY + 12, BZ + 8), e -> true).isEmpty();
            return new int[]{air, hull, engines, hatch, command ? 1 : 0, hatchAtPort ? 1 : 0, noEntity ? 1 : 0};
        });
        assertThat(counts[0] == 27, "Внутренний объём 3×3×3 должен стать воздухом, получено: " + counts[0] + " · " + converted);
        assertThat(counts[1] == 97 && counts[3] == 1 && counts[5] == 1,
                "Оболочка: 97 обшивки + люк напротив порта, получено обшивки " + counts[1] + ", люков " + counts[3]);
        assertThat(counts[2] == 25 && counts[4] == 1, "Двигатели (25) и командный модуль должны остаться");
        assertThat(counts[6] == 1, "Сущность борта должна исчезнуть после конверсии");
        log("wet workshop: 27 воздух / 97 обшивка + люк / 25 двигателей ✓ — " + converted);

        // Герметичность модуля штатным флудфиллом: контроллер внутри, люк закрыт
        sp.getServer().runCommand(set(wx, BY + 4, BZ, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(wx - 1, BY + 4, BZ, "spacereloaded:creative_power"));
        context.waitTick();
        BlockPos controller = new BlockPos(wx, BY + 4, BZ);
        SealingStatus status = SealingStatus.INVALID_ORIGIN;
        for (int waited = 0; waited < 600 && status != SealingStatus.SEALED; waited += 10) {
            context.waitTicks(10);
            status = sp.getServer().computeOnServer(server -> {
                SealedZone zone = ZoneManager.zoneAt(server.overworld(), controller);
                return zone == null ? SealingStatus.INVALID_ORIGIN : zone.status();
            });
        }
        assertThat(status == SealingStatus.SEALED, "Модуль после конверсии должен быть герметичен, статус: " + status);
        log("wet workshop: модуль герметичен ✓");
    }

    // ---------- 28. Электромагнитная катапульта (004, US1) ----------

    /**
     * Рельс из сверхпроводящих катушек на казённике: в оверворлде — запрет по атмосфере;
     * с профилем Луны (тестовая подмена тела) 40 секций — «рельс короток», 110 — выстрел со
     * списанием энергии ½mv²/η и записью капсулы в транзит.
     */
    private void testMassDriver(ClientGameTestContext context, TestSingleplayerContext sp) {
        int wx = BX + 800;
        moveTo(context, sp, wx + 60, BZ + 6);
        sp.getServer().runCommand(String.format("forceload add %d %d %d %d", wx - 8, BZ - 8, wx + 130, BZ + 8));
        sp.getServer().runCommand(set(wx, BY, BZ, "spacereloaded:mass_driver_breech[facing=east]"));
        sp.getServer().runCommand(fill(wx + 1, BY, BZ, wx + 40, BY, BZ, "spacereloaded:superconducting_coil"));
        sp.getServer().runCommand(fill(wx, BY, BZ - 2, wx, BY, BZ - 1, "spacereloaded:capacitor"));
        sp.getServer().runCommand(fill(wx - 1, BY, BZ - 2, wx - 1, BY, BZ - 1, "spacereloaded:creative_power"));
        context.waitTicks(5);
        BlockPos breechPos = new BlockPos(wx, BY, BZ);
        BlockPos catcherPos = new BlockPos(0, 100, 0);
        sp.getServer().runOnServer(server -> {
            if (server.overworld().getBlockEntity(breechPos)
                    instanceof org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity breech) {
                ItemStack program = new ItemStack(ModItems.FLIGHT_PROGRAM);
                program.set(org.alex_melan.spacereloaded.registry.ModDataComponents.PROGRAM_CATCHER,
                        net.minecraft.core.GlobalPos.of(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                Identifier.fromNamespaceAndPath("spacereloaded", "earth_orbit")), catcherPos));
                breech.setTargetFromProgram(program);
                breech.setItem(0, new ItemStack(ModItems.CARGO_POD));
                breech.setItem(1, new ItemStack(ModBlocks.MOON_REGOLITH, 64));
            }
        });
        context.waitTicks(3);
        try {
            String earth = solveReason(sp, breechPos);
            assertThat(earth.startsWith("ATMOSPHERE"), "В оверворлде катапульта запрещена атмосферой, получено: " + earth);
            log("катапульта: запрет в атмосфере Земли ✓ (" + earth + ")");

            sp.getServer().runOnServer(server -> org.alex_melan.spacereloaded.industry.IndustryStructures
                    .testBodyOverride = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(
                            server.overworld(), Identifier.fromNamespaceAndPath("spacereloaded", "moon")).orElseThrow());
            String shortRail = solveReason(sp, breechPos);
            assertThat(shortRail.startsWith("RAIL_SHORT"), "40 секций на Луне — рельс короток, получено: " + shortRail);
            log("катапульта: 40 секций — рельс короток ✓ (" + shortRail + ")");

            sp.getServer().runCommand(fill(wx + 41, BY, BZ, wx + 110, BY, BZ, "spacereloaded:superconducting_coil"));
            context.waitTicks(5);
            String ready = "";
            for (int waited = 0; waited < 400 && !ready.startsWith("OK"); waited += 10) {
                context.waitTicks(10);
                ready = solveReason(sp, breechPos);
            }
            assertThat(ready.startsWith("OK"), "110 секций + заряд — готова, получено: " + ready);
            prepareCamera(context, sp, wx - 4, BY + 4, BZ + 6, -115f, 20f);
            String fired = sp.getServer().computeOnServer(server -> {
                var breech = (org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity)
                        server.overworld().getBlockEntity(breechPos);
                long before = breech.storedEnergy(server.overworld());
                double podMass = breech.podMassKg(); // 100 кг + 64 реголита по таблице масс (15 кг)
                var solution = breech.tryFire(server.overworld(), null);
                long after = breech.storedEnergy(server.overworld());
                double expected = org.alex_melan.spacereloaded.core.industry.MassDriverBallistics.shotEnergyJ(
                        podMass, solution.vRequired(), 0.85) / 15000.0;
                boolean energyOk = Math.abs((before - after) - expected) <= expected * 0.01 + 1;
                boolean podGone = breech.getItem(0).isEmpty() && breech.getItem(1).isEmpty();
                boolean queued = org.alex_melan.spacereloaded.industry.PodTransitState.get(server).pods().stream()
                        .anyMatch(p -> p.targetPos().equals(catcherPos) && p.cargo().get(0).getCount() == 64);
                return solution.reason() + " m=" + Math.round(podMass) + " v=" + Math.round(solution.vRequired())
                        + " dE=" + (before - after)
                        + " ожид=" + Math.round(expected) + (energyOk ? " E✓" : " E✗") + (podGone ? " слоты✓" : " слоты✗")
                        + (queued ? " транзит✓" : " транзит✗");
            });
            assertThat(fired.startsWith("OK") && fired.contains("E✓") && fired.contains("слоты✓")
                    && fired.contains("транзит✓"), "Выстрел катапульты: " + fired);
            log("катапульта: выстрел 110 секций ✓ — " + fired);
            // Кадры анимации: волна по рельсу и возвращение салазок (визуальная проверка)
            snapshot(context, "mass_driver_wave", 2);
            snapshot(context, "mass_driver_sled", 60);
            sp.getServer().runCommand("gamemode survival @a");
        } finally {
            sp.getServer().runOnServer(server -> {
                org.alex_melan.spacereloaded.industry.IndustryStructures.testBodyOverride = null;
                org.alex_melan.spacereloaded.industry.PodTransitState.get(server)
                        .removeIf(p -> p.targetPos().equals(catcherPos));
            });
        }
    }

    private static String solveReason(TestSingleplayerContext sp, BlockPos breechPos) {
        return sp.getServer().computeOnServer(server -> {
            if (!(server.overworld().getBlockEntity(breechPos)
                    instanceof org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity breech)) {
                return "нет казённика";
            }
            var s = breech.solve(server.overworld());
            return s.reason() + " vmax=" + Math.round(s.vMax()) + " vreq=" + Math.round(s.vRequired())
                    + " rail=" + breech.railLength() + " caps=" + breech.capacitorCount()
                    + " missing=" + s.missingSections();
        });
    }

    // ---------- 29. Ловушка масс (004, US2) ----------

    private void testMassCatcher(ClientGameTestContext context, TestSingleplayerContext sp) {
        int cx = BX + 840;
        int cz = BZ + 30;
        moveTo(context, sp, cx - 6, cz);
        sp.getServer().runCommand(fill(cx - 2, BY, cz - 2, cx + 2, BY, cz + 2, "spacereloaded:catcher_net"));
        sp.getServer().runCommand(set(cx, BY, cz, "spacereloaded:mass_catcher"));
        context.waitTicks(5);
        BlockPos catcherPos = new BlockPos(cx, BY, cz);
        boolean previousAny = SpaceReloaded.config().massCatcherAnyDimension;
        try {
            sp.getServer().runOnServer(server -> {
                SpaceReloaded.config().massCatcherAnyDimension = true;
                org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server)
                        .setCoverage(net.minecraft.world.level.Level.OVERWORLD, 1);
            });
            double radius = sp.getServer().computeOnServer(server ->
                    ((org.alex_melan.spacereloaded.industry.MassCatcherBlockEntity)
                            server.overworld().getBlockEntity(catcherPos)).captureRadius());
            assertThat(Math.abs(radius - (1.5 + 0.6 * Math.sqrt(24))) < 1e-6,
                    "Радиус захвата сетки 5×5 (24 секции) = 1.5 + 0.6·√24, получено " + radius);

            // Round-trip кодека записи транзита (переживает перезапуск сервера)
            String codec = sp.getServer().computeOnServer(server -> {
                var transit = new org.alex_melan.spacereloaded.industry.PodTransitState.PodTransit(
                        java.util.UUID.randomUUID(), new ItemStack(ModItems.CARGO_POD),
                        List.of(new ItemStack(ModBlocks.MOON_REGOLITH, 64), new ItemStack(ModItems.SLAG, 7)),
                        net.minecraft.world.level.Level.OVERWORLD, catcherPos,
                        server.overworld().getGameTime(), 42L, java.util.Optional.empty(), false);
                var ops = server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                var tag = org.alex_melan.spacereloaded.industry.PodTransitState.PodTransit.CODEC
                        .encodeStart(ops, transit).getOrThrow();
                var back = org.alex_melan.spacereloaded.industry.PodTransitState.PodTransit.CODEC
                        .parse(ops, tag).getOrThrow();
                boolean same = back.cargo().size() == 2 && back.cargo().get(0).getCount() == 64
                        && back.cargo().get(1).is(ModItems.SLAG) && back.seed() == 42L
                        && back.targetPos().equals(catcherPos);
                org.alex_melan.spacereloaded.industry.PodTransitState.get(server).enqueue(back);
                return same ? "ok" : "расхождение";
            });
            assertThat(codec.equals("ok"), "Кодек транзита: " + codec);

            int caught = 0;
            for (int waited = 0; waited < 100 && caught == 0; waited += 10) {
                context.waitTicks(10);
                caught = sp.getServer().computeOnServer(server ->
                        ((org.alex_melan.spacereloaded.industry.MassCatcherBlockEntity)
                                server.overworld().getBlockEntity(catcherPos)).caught());
            }
            String contents = sp.getServer().computeOnServer(server -> {
                var catcher = (org.alex_melan.spacereloaded.industry.MassCatcherBlockEntity)
                        server.overworld().getBlockEntity(catcherPos);
                int regolith = 0;
                int pods = 0;
                int slag = 0;
                for (int slot = 0; slot < catcher.getContainerSize(); slot++) {
                    ItemStack stack = catcher.getItem(slot);
                    if (stack.is(ModBlocks.MOON_REGOLITH.asItem())) {
                        regolith += stack.getCount();
                    } else if (stack.is(ModItems.CARGO_POD)) {
                        pods += stack.getCount();
                    } else if (stack.is(ModItems.SLAG)) {
                        slag += stack.getCount();
                    }
                }
                return regolith + "/" + slag + "/" + pods;
            });
            assertThat(caught == 1 && contents.equals("64/7/1"),
                    "Ловушка должна принять груз и капсулу: принято " + caught + ", содержимое " + contents);
            log("ловушка масс: радиус " + String.format("%.2f", radius) + ", приём 64 реголита + 7 шлака + капсула ✓");
        } finally {
            sp.getServer().runOnServer(server -> {
                SpaceReloaded.config().massCatcherAnyDimension = previousAny;
                org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server)
                        .setCoverage(net.minecraft.world.level.Level.OVERWORLD, 0);
            });
        }
    }

    // ---------- 30. Реголитовый реактор (004, US3) ----------

    /** Камера для кадров: респаун (ранние сценарии убивают игрока), спектатор, точка, прогрузка чанков. */
    private void prepareCamera(ClientGameTestContext context, TestSingleplayerContext sp, double x, double y,
                               double z, float yaw, float pitch) {
        context.runOnClient(mc -> {
            if (mc.player != null && mc.player.isDeadOrDying()) {
                mc.player.respawn();
            }
        });
        context.waitTicks(10);
        context.setScreen(() -> null);
        sp.getServer().runCommand("gamemode spectator @a");
        sp.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f %.1f %.1f",
                x, y, z, yaw, pitch));
        context.waitTicks(20);
        sp.getClientLevel().waitForChunksRender();
    }

    private void snapshot(ClientGameTestContext context, String name, int waitTicks) {
        context.waitTicks(waitTicks);
        log("скриншот " + name + ": " + context.takeScreenshot("spacereloaded_" + name));
    }

    private void testRegolithReactor(ClientGameTestContext context, TestSingleplayerContext sp) {
        int rx = BX + 880;
        int rz = BZ + 60;
        moveTo(context, sp, rx - 6, rz);
        // Куб 3×3×3 за контроллером (фасад на север): z от rz до rz+2
        sp.getServer().runCommand(fill(rx - 1, BY, rz, rx + 1, BY + 2, rz + 2, "spacereloaded:refractory_lining"));
        sp.getServer().runCommand(set(rx, BY + 1, rz + 1, "minecraft:air"));
        sp.getServer().runCommand(set(rx, BY + 1, rz, "spacereloaded:regolith_reactor[facing=north]"));
        // Питание — через клетку оболочки (футеровка пробрасывает энергию контроллеру)
        sp.getServer().runCommand(set(rx + 2, BY + 1, rz + 1, "spacereloaded:creative_power"));
        context.waitTicks(3);
        BlockPos controller = new BlockPos(rx, BY + 1, rz);
        int previousCycle = SpaceReloaded.config().reactorCycleTicks;
        prepareCamera(context, sp, rx - 2.5, BY + 2.2, rz - 3.5, -30f, 12f);
        try {
            sp.getServer().runOnServer(server -> {
                SpaceReloaded.config().reactorCycleTicks = 20;
                var reactor = (org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity)
                        server.overworld().getBlockEntity(controller);
                reactor.setItem(0, new ItemStack(ModBlocks.MOON_REGOLITH, 4));
                ItemStack canister = new ItemStack(ModItems.OXYGEN_CANISTER);
                canister.setDamageValue(canister.getMaxDamage());
                reactor.setItem(1, canister);
            });
            snapshot(context, "regolith_reactor", 25);
            sp.getServer().runCommand("gamemode survival @a");
            String result = "";
            for (int waited = 0; waited < 300; waited += 10) {
                context.waitTicks(10);
                result = sp.getServer().computeOnServer(server -> {
                    var reactor = (org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity)
                            server.overworld().getBlockEntity(controller);
                    ItemStack canister = reactor.getItem(1);
                    double oxygen = (canister.getMaxDamage() - canister.getDamageValue())
                            * org.alex_melan.spacereloaded.lifesupport.OxygenCanisters.KG_PER_UNIT + reactor.oxygenBuffer();
                    return (reactor.formed() ? "formed" : "unformed") + " rego=" + reactor.getItem(0).getCount()
                            + " o2=" + String.format(java.util.Locale.ROOT, "%.2f", oxygen) + " fe=" + reactor.getItem(2).getCount()
                            + " slag=" + reactor.getItem(4).getCount();
                });
                if (result.contains("rego=0 ") && result.contains("slag=4")) {
                    break;
                }
            }
            assertThat(result.equals("formed rego=0 o2=12.00 fe=4 slag=4"),
                    "Реактор: 4 реголита → 12 кг O₂ (баллон + буфер), 4 железной пыли, 4 шлака; получено: " + result);
            log("реголитовый реактор: " + result + " ✓");

            sp.getServer().runCommand(set(rx + 1, BY + 2, rz + 2, "minecraft:air"));
            context.waitTicks(3);
            boolean formed = sp.getServer().computeOnServer(server ->
                    ((org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity)
                            server.overworld().getBlockEntity(controller)).formed());
            assertThat(!formed, "Дыра в футеровке — реактор не сформирован");
            log("реголитовый реактор: дыра в оболочке → не сформирован ✓");
        } finally {
            sp.getServer().runOnServer(server -> SpaceReloaded.config().reactorCycleTicks = previousCycle);
        }
    }

    // ---------- 31. Трансмиссия (005, US1) ----------

    private static double omegaAt(TestSingleplayerContext sp, BlockPos pos) {
        return sp.getServer().computeOnServer(server ->
                server.overworld().getBlockEntity(pos)
                        instanceof org.alex_melan.spacereloaded.kinetics.KineticBlockEntity be ? be.omega() : Double.NaN);
    }

    private static String netState(TestSingleplayerContext sp, BlockPos pos) {
        return sp.getServer().computeOnServer(server ->
                org.alex_melan.spacereloaded.kinetics.KineticNetworks.networkState(server.overworld(), pos));
    }

    /** Мотор → вал → малая шестерня → большая (по диагонали) → вал: 2:1, реверс; затем заклинивание. */
    private void testTransmission(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 920;
        int z = BZ;
        moveTo(context, sp, x0 - 5, z);
        sp.getServer().runCommand(set(x0 - 1, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:motor[axis=x]"));
        sp.getServer().runCommand(set(x0 + 1, BY, z, "spacereloaded:steel_shaft[axis=x]"));
        sp.getServer().runCommand(set(x0 + 2, BY, z, "spacereloaded:small_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 2, BY + 1, z + 1, "spacereloaded:large_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z + 1, "spacereloaded:steel_shaft[axis=x]"));
        BlockPos motor = new BlockPos(x0, BY, z);
        BlockPos out = new BlockPos(x0 + 1, BY + 1, z + 1);
        context.waitTicks(200);
        double wm = omegaAt(sp, motor);
        double wo = omegaAt(sp, out);
        String state = netState(sp, motor);
        assertThat(state.equals("running") && wm > 100, "Сеть должна разогнаться: " + state + " ω=" + wm);
        assertThat(Math.abs(wo / wm + 0.5) < 0.01, "Выходной вал: −½ скорости мотора, получено " + wo / wm);
        log(String.format(java.util.Locale.ROOT, "трансмиссия: мотор %.0f об/мин → выход %.0f об/мин (2:1, реверс) ✓",
                wm * 60 / (2 * Math.PI), wo * 60 / (2 * Math.PI)));
        prepareCamera(context, sp, x0 + 1.5, BY + 1.6, z - 2.5, 20f, 25f);
        snapshot(context, "transmission", 5);
        sp.getServer().runCommand("gamemode survival @a");

        // Классическое заклинивание: большая на оси малой + малая на оси большой, сцепленные по диагонали
        sp.getServer().runCommand(set(x0 + 3, BY, z, "spacereloaded:large_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 3, BY + 1, z + 1, "spacereloaded:small_gear[axis=x]"));
        context.waitTicks(10);
        String jammed = netState(sp, motor);
        assertThat(jammed.equals("jammed") && omegaAt(sp, motor) == 0.0, "Противоречивые передачи — заклинивание: " + jammed);
        sp.getServer().runCommand(set(x0 + 3, BY + 1, z + 1, "minecraft:air"));
        context.waitTicks(60);
        assertThat(netState(sp, motor).equals("running"), "После разбора сеть снова вращается: " + netState(sp, motor));
        log("трансмиссия: заклинивание и восстановление ✓");
    }

    // ---------- 32. Срез вала (005, US1) ----------

    private void testShaftShear(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 920;
        int z = BZ + 20;
        moveTo(context, sp, x0 - 5, z);
        double previous = SpaceReloaded.config().woodShaftShearPa;
        try {
            // Стенд проверяет механизм среза; реальные пределы (24.5/706 кН·м) — тесты ядра
            sp.getServer().runOnServer(server -> SpaceReloaded.config().woodShaftShearPa = 1e6);
            sp.getServer().runCommand(set(x0 - 1, BY, z, "spacereloaded:creative_power"));
            sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:motor[axis=x]"));
            sp.getServer().runCommand(set(x0 + 1, BY, z, "spacereloaded:small_gear[axis=x]"));
            sp.getServer().runCommand(set(x0 + 1, BY + 1, z + 1, "spacereloaded:large_gear[axis=x]"));
            sp.getServer().runCommand(set(x0 + 2, BY + 1, z + 1, "spacereloaded:wooden_shaft[axis=x]"));
            sp.getServer().runCommand(set(x0 + 3, BY + 1, z + 1, "spacereloaded:flywheel[axis=x]"));
            boolean broken = false;
            for (int waited = 0; waited < 60 && !broken; waited += 2) {
                context.waitTicks(2);
                broken = sp.getServer().computeOnServer(server ->
                        server.overworld().getBlockState(new BlockPos(x0 + 2, BY + 1, z + 1)).isAir());
            }
            assertThat(broken, "Деревянный вал за понижением при разгоне маховика должен срезаться");
            log("трансмиссия: деревянный вал срезан моментом разгона маховика ✓");
        } finally {
            sp.getServer().runOnServer(server -> SpaceReloaded.config().woodShaftShearPa = previous);
        }
    }

    // ---------- 33. Пресс и маховик (005, US2) ----------

    private void testPressFlywheel(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 920;
        int z = BZ + 40;
        moveTo(context, sp, x0 - 5, z);
        // Мотор → малая → большая (×2) → малая по оси → большая (×4) → пресс: 1500 → ~375 об/мин
        sp.getServer().runCommand(set(x0 - 1, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:motor[axis=x]"));
        sp.getServer().runCommand(set(x0 + 1, BY, z, "spacereloaded:small_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z + 1, "spacereloaded:large_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 2, BY + 1, z + 1, "spacereloaded:small_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 2, BY + 2, z + 2, "spacereloaded:large_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 3, BY + 2, z + 2, "spacereloaded:mechanical_press[axis=x]"));
        BlockPos motor = new BlockPos(x0, BY, z);
        BlockPos press = new BlockPos(x0 + 3, BY + 2, z + 2);
        context.waitTicks(200);
        double dropWithout = measurePressDrop(context, sp, motor, press);
        assertThat(dropWithout > 0.03, "Без маховика удар пресса заметно роняет обороты, просадка " + dropWithout);
        String plate = sp.getServer().computeOnServer(server -> ((org.alex_melan.spacereloaded.kinetics.PressBlockEntity)
                server.overworld().getBlockEntity(press)).getItem(1).getItem().toString());
        assertThat(plate.contains("steel_plate"), "Пресс делает стальной лист, выход: " + plate);

        // Маховик на валу мотора (с другой стороны)
        sp.getServer().runCommand(set(x0 - 1, BY, z, "spacereloaded:flywheel[axis=x]"));
        sp.getServer().runCommand(set(x0, BY - 1, z, "spacereloaded:creative_power"));
        context.waitTicks(400);
        double dropWith = measurePressDrop(context, sp, motor, press);
        assertThat(dropWith * 5 < dropWithout, String.format(java.util.Locale.ROOT,
                "Маховик гасит просадку в 5+ раз: без %.3f, с %.3f", dropWithout, dropWith));
        log(String.format(java.util.Locale.ROOT, "пресс: просадка без маховика %.1f %%, с маховиком %.2f %% ✓",
                dropWithout * 100, dropWith * 100));
    }

    /** Кладёт слиток в пресс и возвращает относительную просадку оборотов мотора за удар. */
    private double measurePressDrop(ClientGameTestContext context, TestSingleplayerContext sp, BlockPos motor,
                                    BlockPos press) {
        double base = omegaAt(sp, motor);
        sp.getServer().runOnServer(server -> {
            var be = (org.alex_melan.spacereloaded.kinetics.PressBlockEntity) server.overworld().getBlockEntity(press);
            be.setItem(1, ItemStack.EMPTY);
            be.setItem(0, new ItemStack(ModItems.STEEL_INGOT));
        });
        double min = base;
        for (int t = 0; t < 50; t++) {
            context.waitTick();
            min = Math.min(min, omegaAt(sp, motor));
        }
        return (base - min) / base;
    }

    // ---------- 34. Молот: электролизный стек и колонна (005, US3) ----------

    private void testStackColumn(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 960;
        int z = BZ;
        moveTo(context, sp, x0 - 5, z);
        // Электролизёр лицом на запад, 7 ячеек за задней гранью (на восток)
        sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:electrolyzer[facing=west]"));
        sp.getServer().runCommand(fill(x0 + 1, BY, z, x0 + 7, BY, z, "spacereloaded:electrolysis_cell"));
        sp.getServer().runCommand(set(x0, BY + 1, z, "spacereloaded:creative_power"));
        context.waitTicks(3);
        BlockPos key = new BlockPos(x0, BY, z);
        String formed = sp.getServer().computeOnServer(server -> {
            var be = (org.alex_melan.spacereloaded.machine.ElectrolyzerBlockEntity) server.overworld().getBlockEntity(key);
            be.hammer(server.overworld(), server.getPlayerList().getPlayers().get(0));
            be.setItem(0, new ItemStack(net.minecraft.world.item.Items.ICE, 16));
            return be.structure().formed() + "/" + be.structure().repeats();
        });
        assertThat(formed.equals("true/7"), "Молот формирует стек из 7 ячеек: " + formed);
        int left = 16;
        for (int waited = 0; waited < 300 && left == 16; waited += 10) {
            context.waitTicks(10);
            left = sp.getServer().computeOnServer(server ->
                    ((org.alex_melan.spacereloaded.machine.ElectrolyzerBlockEntity) server.overworld()
                            .getBlockEntity(key)).getItem(0).getCount());
        }
        assertThat(left == 8, "Стек N=7 перерабатывает 8 льда за цикл, осталось " + left);
        prepareCamera(context, sp, x0 - 2.5, BY + 2.5, z - 3.5, -40f, 25f);
        snapshot(context, "electrolysis_stack", 5);
        sp.getServer().runCommand("gamemode survival @a");
        sp.getServer().runCommand(set(x0 + 4, BY, z, "minecraft:air"));
        context.waitTicks(3);
        int repeats = sp.getServer().computeOnServer(server ->
                ((org.alex_melan.spacereloaded.machine.ElectrolyzerBlockEntity) server.overworld()
                        .getBlockEntity(key)).structure().repeats());
        assertThat(repeats == 3, "Без 4-й ячейки стек укорачивается до 3, получено " + repeats);
        log("молот: стек 7 ячеек → 8 льда за цикл; разрыв линии → 3 ячейки ✓");

        // Колонна: перегонный куб + 12 тарелок
        int cx = x0 + 12;
        sp.getServer().runCommand(set(cx, BY, z, "spacereloaded:refinery[facing=west]"));
        sp.getServer().runCommand(fill(cx, BY + 1, z, cx, BY + 12, z, "spacereloaded:distillation_tray"));
        context.waitTicks(3);
        BlockPos column = new BlockPos(cx, BY, z);
        double yield = sp.getServer().computeOnServer(server -> {
            var be = (org.alex_melan.spacereloaded.machine.RefineryBlockEntity) server.overworld().getBlockEntity(column);
            be.hammer(server.overworld(), server.getPlayerList().getPlayers().get(0));
            return be.fuelPerOperation();
        });
        assertThat(Math.abs(yield - 150) < 1.5, "Колонна 12 тарелок — 150 кг на сланец, получено " + yield);
        prepareCamera(context, sp, cx - 5.5, BY + 5, z - 5.5, -45f, 5f);
        snapshot(context, "distillation_column", 5);
        sp.getServer().runCommand("gamemode survival @a");
        // Ошибка: 3 тарелки — не сформирована
        sp.getServer().runCommand(set(cx, BY + 4, z, "minecraft:air"));
        context.waitTicks(3);
        double broken = sp.getServer().computeOnServer(server ->
                ((org.alex_melan.spacereloaded.machine.RefineryBlockEntity) server.overworld()
                        .getBlockEntity(column)).fuelPerOperation());
        assertThat(Math.abs(broken - 100) < 0.01, "3 тарелки — меньше минимума, наследные 100 кг, получено " + broken);
        log(String.format(java.util.Locale.ROOT, "молот: колонна 12 тарелок → %.0f кг/сланец; разрыв → 100 ✓", yield));
    }

    // ---------- 35. Качество двигателя (005, US4) ----------

    private void testEngineQuality(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 960;
        int z = BZ + 30;
        moveTo(context, sp, x0 - 5, z);
        // Реальная операция токарного станка от мотора через 2:1 (1500 → 750 об/мин — номинал шпинделя)
        sp.getServer().runCommand(set(x0 - 1, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:motor[axis=x]"));
        sp.getServer().runCommand(set(x0 + 1, BY, z, "spacereloaded:small_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z + 1, "spacereloaded:large_gear[axis=x]"));
        sp.getServer().runCommand(set(x0 + 2, BY + 1, z + 1, "spacereloaded:lathe[axis=x]"));
        BlockPos lathe = new BlockPos(x0 + 2, BY + 1, z + 1);
        context.waitTicks(100);
        sp.getServer().runOnServer(server -> ((org.alex_melan.spacereloaded.kinetics.LatheBlockEntity)
                server.overworld().getBlockEntity(lathe)).setItem(0, new ItemStack(ModItems.COPPER_PLATE)));
        String step = "";
        for (int waited = 0; waited < 200 && !step.startsWith("1"); waited += 10) {
            context.waitTicks(10);
            step = sp.getServer().computeOnServer(server -> {
                ItemStack out = ((org.alex_melan.spacereloaded.kinetics.LatheBlockEntity)
                        server.overworld().getBlockEntity(lathe)).getItem(1);
                Integer st = out.get(org.alex_melan.spacereloaded.registry.ModDataComponents.MACHINING_STEP);
                Float dq = out.get(org.alex_melan.spacereloaded.registry.ModDataComponents.MACHINING_DELTA_SQ);
                return st == null ? "" : st + " δ=" + (dq == null ? "?" : String.format(java.util.Locale.ROOT, "%.1f",
                        Math.sqrt(dq)));
            });
        }
        assertThat(step.startsWith("1"), "Токарный станок: медный лист → форсуночная головка шаг 1, получено " + step);
        log("токарный станок: полуфабрикат форсуночной головки, шаг " + step + " мкм ✓");

        // Сборка двигателя из деталей качества 1.0 на сборочном столе → уровень 10 → Isp +1.6 %
        int tx = x0 + 6;
        sp.getServer().runCommand(set(tx, BY, z, "spacereloaded:assembly_table"));
        sp.getServer().runCommand(set(tx + 1, BY, z, "spacereloaded:creative_power"));
        BlockPos table = new BlockPos(tx, BY, z);
        sp.getServer().runOnServer(server -> {
            var be = (org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity)
                    server.overworld().getBlockEntity(table);
            ItemStack[] parts = {new ItemStack(ModItems.TURBOPUMP), new ItemStack(ModItems.INJECTOR_PLATE),
                    new ItemStack(ModItems.REGEN_NOZZLE)};
            for (ItemStack p : parts) {
                p.set(org.alex_melan.spacereloaded.registry.ModDataComponents.PART_QUALITY, 1.0f);
            }
            be.setItem(0, parts[0]);
            be.setItem(1, parts[1]);
            be.setItem(2, parts[2]);
            be.setItem(3, new ItemStack(ModItems.TUNGSTEN_INGOT));
            be.setItem(4, new ItemStack(ModItems.STEEL_INGOT));
        });
        String engine = "";
        for (int waited = 0; waited < 400 && engine.isEmpty(); waited += 10) {
            context.waitTicks(10);
            engine = sp.getServer().computeOnServer(server -> {
                ItemStack out = ((org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity)
                        server.overworld().getBlockEntity(table)).getItem(
                        org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity.INPUT_SLOTS);
                var props = out.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
                if (out.isEmpty() || props == null) {
                    return "";
                }
                Integer q = props.get(org.alex_melan.spacereloaded.rocket.EngineBlock.QUALITY);
                var resolver = new org.alex_melan.spacereloaded.rocket.PartPropertiesResolver(server.overworld());
                var precise = resolver.resolve(ModBlocks.ROCKET_ENGINE.defaultBlockState()
                        .setValue(org.alex_melan.spacereloaded.rocket.EngineBlock.QUALITY, q)).orElseThrow();
                var table8 = resolver.resolve(ModBlocks.ROCKET_ENGINE.defaultBlockState()).orElseThrow();
                return q + " " + String.format(java.util.Locale.ROOT, "%.4f", precise.ispSec() / table8.ispSec());
            });
        }
        assertThat(engine.startsWith("10 1.016"), "Двигатель из деталей q=1: уровень 10, Isp ×1.016; получено " + engine);
        log("качество двигателя: уровень и множитель Isp " + engine + " ✓");
    }

    // ---------- 37. Химия кремния и металлов (006, US2/US5) ----------

    private static ItemStack lot(net.minecraft.world.item.Item item, int count, float purity) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(ModDataComponents.PURITY, purity);
        return stack;
    }

    private static float purityIn(ItemStack stack) {
        Float p = stack.get(ModDataComponents.PURITY);
        return p == null ? -1f : p;
    }

    /**
     * Все машины цепочки параллельно: карботермия в печи (MG-Si 2N + 2 CO), трихлорсилан в
     * Сабатье, ректификация на колонне 16 тарелок (2 → 8.4N; вторая партия 11N смешивается с
     * первой по массе примесей), осаждение Сименса (чистота наследуется, HCl 2 из 2.85),
     * хлор-щелочной электролиз, HF из флюорита, Холл–Эру в криолите (без O₂, анод сгорает).
     */
    private void testChemistryChain(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1040;
        int z = BZ;
        moveTo(context, sp, x0 + 10, z + 10);
        BlockPos furnace = new BlockPos(x0, BY, z);
        BlockPos sabatier = new BlockPos(x0 + 3, BY, z);
        BlockPos refinery = new BlockPos(x0 + 6, BY, z);
        BlockPos deposition = new BlockPos(x0 + 9, BY, z);
        BlockPos electrolyzer = new BlockPos(x0 + 12, BY, z);
        BlockPos reactor = new BlockPos(x0 + 15, BY, z);
        for (BlockPos p : List.of(furnace, sabatier, deposition, electrolyzer, reactor)) {
            sp.getServer().runCommand(set(p.getX(), BY - 1, z, "spacereloaded:creative_power"));
        }
        sp.getServer().runCommand(set(furnace.getX(), BY, z, "spacereloaded:electric_furnace"));
        sp.getServer().runCommand(set(sabatier.getX(), BY, z, "spacereloaded:sabatier_reactor"));
        sp.getServer().runCommand(set(refinery.getX(), BY, z, "spacereloaded:refinery[facing=west]"));
        sp.getServer().runCommand(set(refinery.getX() - 1, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(fill(refinery.getX(), BY + 1, z, refinery.getX(), BY + 16, z,
                "spacereloaded:distillation_tray"));
        sp.getServer().runCommand(set(deposition.getX(), BY, z, "spacereloaded:deposition_reactor"));
        sp.getServer().runCommand(set(electrolyzer.getX(), BY, z, "spacereloaded:electrolyzer"));
        sp.getServer().runCommand(set(reactor.getX(), BY, z, "spacereloaded:chemical_reactor"));
        context.waitTicks(3);
        String trays = sp.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            var column = (org.alex_melan.spacereloaded.machine.RefineryBlockEntity) level.getBlockEntity(refinery);
            column.hammer(level, server.getPlayerList().getPlayers().get(0));
            ((net.minecraft.world.Container) level.getBlockEntity(furnace)).setItem(0, new ItemStack(ModItems.SILICON_BLEND));
            var sab = (net.minecraft.world.Container) level.getBlockEntity(sabatier);
            sab.setItem(0, lot(ModItems.METALLURGICAL_SILICON, 1, 2.0f));
            sab.setItem(1, new ItemStack(ModItems.HYDROGEN_CHLORIDE, 3));
            column.setItem(0, lot(ModItems.TRICHLOROSILANE, 1, 2.0f));
            var dep = (net.minecraft.world.Container) level.getBlockEntity(deposition);
            dep.setItem(0, lot(ModItems.TRICHLOROSILANE, 1, 9.0f));
            dep.setItem(1, new ItemStack(net.minecraft.world.item.Items.ICE));
            ((net.minecraft.world.Container) level.getBlockEntity(electrolyzer)).setItem(0, new ItemStack(ModItems.BRINE));
            var chem = (net.minecraft.world.Container) level.getBlockEntity(reactor);
            chem.setItem(0, new ItemStack(ModItems.FLUORITE));
            chem.setItem(1, new ItemStack(ModItems.SULFURIC_ACID));
            return column.structure().formed() + "/" + column.stages();
        });
        assertThat(trays.equals("true/16"), "Колонна 16 тарелок сформирована: " + trays);
        context.waitTicks(260);
        String first = sp.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            var f = (net.minecraft.world.Container) level.getBlockEntity(furnace);
            var sab = (net.minecraft.world.Container) level.getBlockEntity(sabatier);
            var column = (net.minecraft.world.Container) level.getBlockEntity(refinery);
            var el = (net.minecraft.world.Container) level.getBlockEntity(electrolyzer);
            var chem = (net.minecraft.world.Container) level.getBlockEntity(reactor);
            return "mg=" + f.getItem(1).getCount() + "@" + purityIn(f.getItem(1)) + " co=" + f.getItem(2).getCount()
                    + " tcs=" + sab.getItem(2).getCount() + "@" + purityIn(sab.getItem(2))
                    + " col=" + String.format(java.util.Locale.ROOT, "%.2f", purityIn(column.getItem(2)))
                    + " hcl=" + el.getItem(2).getCount() + " naoh=" + el.getItem(3).getCount()
                    + " hf=" + chem.getItem(3).getCount() + " gyp=" + chem.getItem(4).getCount();
        });
        assertThat(first.equals("mg=1@2.0 co=2 tcs=1@2.0 col=8.40 hcl=1 naoh=1 hf=2 gyp=1"),
                "Печь, Сабатье, колонна, электролизёр, реактор: " + first);
        log("химия 006: " + first + " ✓");
        // вторая партия колонны: 8.4N → 11N, смешение в выходе по массе примесей
        sp.getServer().runOnServer(server -> ((net.minecraft.world.Container) server.overworld()
                .getBlockEntity(refinery)).setItem(0, lot(ModItems.TRICHLOROSILANE, 1, 8.4f)));
        context.waitTicks(360);
        String second = sp.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            ItemStack col = ((net.minecraft.world.Container) level.getBlockEntity(refinery)).getItem(2);
            var dep = (net.minecraft.world.Container) level.getBlockEntity(deposition);
            return col.getCount() + "@" + String.format(java.util.Locale.ROOT, "%.3f", purityIn(col))
                    + " poly=" + dep.getItem(3).getCount() + "@" + purityIn(dep.getItem(3))
                    + " hcl=" + dep.getItem(4).getCount();
        });
        String blend = String.format(java.util.Locale.ROOT, "%.3f",
                org.alex_melan.spacereloaded.core.electronics.Purity.blend(8.4f, 1, 11.0, 1));
        assertThat(second.equals("2@" + blend + " poly=1@9.0 hcl=2"),
                "Смешение партий колонны и реактор Сименса (ожидалось 2@" + blend + " poly=1@9.0 hcl=2): " + second);
        log("колонна: партии 8.4N и 11N → " + second + " ✓");

        // Холл–Эру: криолитовая ванна и угольный анод — алюминий без кислорода, энергия ×0.7
        int rx = x0 + 22;
        int rz = z + 10;
        sp.getServer().runCommand(fill(rx - 1, BY, rz, rx + 1, BY + 2, rz + 2, "spacereloaded:refractory_lining"));
        sp.getServer().runCommand(set(rx, BY + 1, rz + 1, "minecraft:air"));
        sp.getServer().runCommand(set(rx, BY + 1, rz, "spacereloaded:regolith_reactor[facing=north]"));
        sp.getServer().runCommand(set(rx + 2, BY + 1, rz + 1, "spacereloaded:creative_power"));
        context.waitTicks(3);
        BlockPos controller = new BlockPos(rx, BY + 1, rz);
        sp.getServer().runOnServer(server -> ((net.minecraft.world.Container) server.overworld()
                .getBlockEntity(controller)).setItem(0, new ItemStack(ModItems.CRYOLITE)));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> ((net.minecraft.world.Container) server.overworld()
                .getBlockEntity(controller)).setItem(0, new ItemStack(net.minecraft.world.item.Items.COAL, 2)));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> ((net.minecraft.world.Container) server.overworld()
                .getBlockEntity(controller)).setItem(0, new ItemStack(ModItems.ALUMINA, 2)));
        context.waitTicks(320);
        String hall = sp.getServer().computeOnServer(server -> {
            var r = (org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity) server.overworld()
                    .getBlockEntity(controller);
            return "al=" + r.getItem(2).getCount() + " o2=" + (int) r.oxygenBuffer() + " anode="
                    + String.format(java.util.Locale.ROOT, "%.2f", r.anodeCarbon()) + " bath="
                    + String.format(java.util.Locale.ROOT, "%.2f", r.bathCapacity());
        });
        assertThat(hall.equals("al=1 o2=0 anode=1.30 bath=48.45"),
                "Холл–Эру: 2 глинозёма → 1.55 Al (1 слиток + дробь), без O₂; получено " + hall);
        log("Холл–Эру: " + hall + " ✓");
    }

    // ---------- 38. Чохральский и пила (006, US2) ----------

    /**
     * Мотор 1500 об/мин через пять пар 2:1 → 47 об/мин на шпинделе установки Чохральского.
     * 3 куска поликремния 9N (7.8 кг) с фосфором: годная доля Шайля 46 % → 5 слитков по 0.69 кг,
     * бор уходит в хвост (слиток 9.07N, хвост 8.95N, 1 кусок + 1.73 кг в тигле-накопителе).
     * Пила от мотора напрямую ставит слиток на оправку и режет по пластине: 287 пластин на слиток.
     */
    private void testCrystalAndSaw(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1080;
        int z = BZ;
        moveTo(context, sp, x0 + 3, z + 3);
        sp.getServer().runCommand(set(x0 - 1, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:motor[axis=x]"));
        for (int k = 1; k <= 5; k++) {
            sp.getServer().runCommand(set(x0 + k, BY + k - 1, z + k - 1, "spacereloaded:small_gear[axis=x]"));
            sp.getServer().runCommand(set(x0 + k, BY + k, z + k, "spacereloaded:large_gear[axis=x]"));
        }
        BlockPos puller = new BlockPos(x0 + 6, BY + 5, z + 5);
        sp.getServer().runCommand(set(puller.getX(), puller.getY(), puller.getZ(), "spacereloaded:crystal_puller[axis=x]"));
        sp.getServer().runCommand(set(puller.getX(), puller.getY() + 1, puller.getZ(), "spacereloaded:creative_power"));
        context.waitTicks(100);
        double rpm = org.alex_melan.spacereloaded.kinetics.KineticBlockEntity.toRpm(Math.abs(omegaAt(sp, puller)));
        assertThat(rpm > 40 && rpm < 55, "Пять пар 2:1 дают ~47 об/мин на шпинделе, получено " + rpm);
        sp.getServer().runOnServer(server -> {
            var be = (net.minecraft.world.Container) server.overworld().getBlockEntity(puller);
            be.setItem(0, lot(ModItems.POLYSILICON, 3, 9.0f));
            be.setItem(2, new ItemStack(ModItems.QUARTZ_CRUCIBLE));
            be.setItem(3, new ItemStack(ModItems.PHOSPHORUS));
        });
        context.waitTicks(640);
        String pulled = sp.getServer().computeOnServer(server -> {
            var be = (net.minecraft.world.Container) server.overworld().getBlockEntity(puller);
            ItemStack boules = be.getItem(1);
            ItemStack tail = be.getItem(4);
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(boules.getItem()).getPath()
                    + "×" + boules.getCount() + "@" + String.format(java.util.Locale.ROOT, "%.2f", purityIn(boules))
                    + " tail×" + tail.getCount() + "@" + String.format(java.util.Locale.ROOT, "%.2f", purityIn(tail))
                    + " crucible=" + be.getItem(2).getCount() + " charge=" + be.getItem(0).getCount();
        });
        assertThat(pulled.equals("silicon_boule×5@9.07 tail×1@8.95 crucible=0 charge=0"),
                "Чохральский: 5 монокристаллов, бор в хвосте, тигель израсходован; получено " + pulled);
        log("Чохральский: " + pulled + " ✓");

        int sx = x0 + 14;
        sp.getServer().runCommand(set(sx - 1, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(set(sx, BY, z, "spacereloaded:motor[axis=x]"));
        sp.getServer().runCommand(set(sx + 1, BY, z, "spacereloaded:wafer_saw[axis=x]"));
        BlockPos saw = new BlockPos(sx + 1, BY, z);
        context.waitTicks(60);
        sp.getServer().runOnServer(server -> ((net.minecraft.world.Container) server.overworld().getBlockEntity(saw))
                .setItem(0, lot(ModItems.SILICON_BOULE, 1, 9.07f)));
        context.waitTicks(70);
        String cut = sp.getServer().computeOnServer(server -> {
            var be = (org.alex_melan.spacereloaded.electronics.WaferSawBlockEntity) server.overworld().getBlockEntity(saw);
            ItemStack wafers = be.getItem(1);
            return "wafers=" + wafers.getCount() + "@" + String.format(java.util.Locale.ROOT, "%.2f", purityIn(wafers))
                    + " left=" + be.wafersLeft() + " input=" + be.getItem(0).getCount();
        });
        assertThat(cut.equals("wafers=1@9.07 left=286 input=0"),
                "Пила: слиток на оправке, первая пластина, осталось 286; получено " + cut);
        log("прецизионная пила: " + cut + " ✓");
    }

    // ---------- 39. Чистая комната и маршрут фаба (006, US3) ----------

    /**
     * Комната 5×3×5 с четырьмя модулями HEPA в потолке: концентрация спадает по e^(−t/τ),
     * τ = V/(Q·η); пластина проходит окисление → экспонирование → травление HF внутри
     * (дефекты — от точной средней концентрации) и снаружи (воздух Земли — сотни дефектов/см²).
     */
    private void testCleanroomFab(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1120;
        int z = BZ;
        moveTo(context, sp, x0 - 4, z + 3);
        sp.getServer().runCommand(fill(x0, BY, z, x0 + 6, BY + 4, z + 6, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(x0 + 1, BY + 1, z + 1, x0 + 5, BY + 3, z + 5, "minecraft:air"));
        for (int[] c : new int[][] {{2, 2}, {4, 2}, {2, 4}, {4, 4}}) {
            sp.getServer().runCommand(set(x0 + c[0], BY + 4, z + c[1], "spacereloaded:fan_filter_unit"));
        }
        BlockPos furnace = new BlockPos(x0 + 1, BY + 1, z + 1);
        BlockPos litho = new BlockPos(x0 + 2, BY + 1, z + 1);
        BlockPos etch = new BlockPos(x0 + 3, BY + 1, z + 1);
        sp.getServer().runCommand(set(furnace.getX(), BY + 1, z + 1, "spacereloaded:diffusion_furnace"));
        sp.getServer().runCommand(set(litho.getX(), BY + 1, z + 1, "spacereloaded:lithography_station"));
        sp.getServer().runCommand(set(etch.getX(), BY + 1, z + 1, "spacereloaded:etch_bath"));
        sp.getServer().runCommand(fill(x0 + 1, BY + 2, z + 1, x0 + 3, BY + 2, z + 1, "spacereloaded:creative_power"));
        BlockPos controllerPos = new BlockPos(x0 + 5, BY + 1, z + 5);
        sp.getServer().runCommand(set(x0 + 5, BY + 1, z + 5, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(x0 + 4, BY + 1, z + 5, "spacereloaded:creative_power")); // сбоку: над контроллером начинается заливка зоны
        String sealed = "";
        for (int waited = 0; waited < 200 && !sealed.startsWith("SEALED"); waited += 10) {
            context.waitTicks(10);
            sealed = sp.getServer().computeOnServer(server -> {
                SealedZone zone = ZoneManager.zoneAt(server.overworld(), controllerPos);
                return zone == null ? "none" : zone.status() + " V=" + zone.volume().size();
            });
        }
        assertThat(sealed.equals("SEALED V=67"), "Чистая комната герметична, объём 67 м³: " + sealed);
        BlockPos probe = new BlockPos(x0 + 3, BY + 2, z + 3);
        double c1 = sp.getServer().computeOnServer(server ->
                org.alex_melan.spacereloaded.electronics.CleanroomTracker.concentration(server.overworld(), probe));
        context.waitTicks(200);
        double c2 = sp.getServer().computeOnServer(server ->
                org.alex_melan.spacereloaded.electronics.CleanroomTracker.concentration(server.overworld(), probe));
        double tau = 67 / (4 * org.alex_melan.spacereloaded.electronics.CleanroomTracker.FFU_FLOW
                * org.alex_melan.spacereloaded.core.electronics.CleanroomAir.HEPA);
        double expected = Math.exp(-200 / 1200.0 / tau);
        assertThat(c1 > 0 && Math.abs(c2 / c1 - expected) < 0.01,
                String.format(java.util.Locale.ROOT, "Спад частиц e^(−t/τ): %.4f против %.4f (C %.0f → %.0f)",
                        c2 / c1, expected, c1, c2));
        log(String.format(java.util.Locale.ROOT, "чистая комната: τ = %.2f мин, C %.0f → %.0f /м³ (ISO %d) ✓", tau, c1, c2,
                org.alex_melan.spacereloaded.core.electronics.CleanroomAir.isoClass(c2)));

        // маршрут: окисление → экспонирование логики → травление HF
        sp.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            var f = (net.minecraft.world.Container) level.getBlockEntity(furnace);
            f.setItem(0, lot(ModItems.SILICON_WAFER, 1, 9.0f));
            f.setItem(1, new ItemStack(ModItems.OXYGEN_CANISTER));
            var l = (net.minecraft.world.Container) level.getBlockEntity(litho);
            l.setItem(1, new ItemStack(ModItems.PHOTOMASK_LOGIC));
            l.setItem(2, new ItemStack(ModItems.PHOTORESIST));
            ((net.minecraft.world.Container) level.getBlockEntity(etch)).setItem(1, new ItemStack(ModItems.HYDROFLUORIC_ACID));
        });
        context.waitTicks(210);
        sp.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            var f = (net.minecraft.world.Container) level.getBlockEntity(furnace);
            ((net.minecraft.world.Container) level.getBlockEntity(litho)).setItem(0, f.removeItemNoUpdate(4));
        });
        context.waitTicks(110);
        sp.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            var l = (net.minecraft.world.Container) level.getBlockEntity(litho);
            ((net.minecraft.world.Container) level.getBlockEntity(etch)).setItem(0, l.removeItemNoUpdate(3));
        });
        context.waitTicks(110);
        String route = sp.getServer().computeOnServer(server -> {
            ItemStack w = ((net.minecraft.world.Container) server.overworld().getBlockEntity(etch)).getItem(2);
            return "step=" + w.getOrDefault(ModDataComponents.WAFER_STEP, -1) + " kind="
                    + w.getOrDefault(ModDataComponents.WAFER_KIND, -1) + " next="
                    + org.alex_melan.spacereloaded.electronics.WaferKind.next(w) + " D="
                    + String.format(java.util.Locale.ROOT, "%.4f", w.getOrDefault(ModDataComponents.WAFER_DEFECTS, -1f));
        });
        double defects = Double.parseDouble(route.substring(route.indexOf("D=") + 2));
        assertThat(route.startsWith("step=3 kind=0 next=OXIDIZE") && defects > 0.0100 && defects < 0.0243,
                "Уровень 1 маршрута в чистой комнате (дефекты между собственными 0.01 и 0.024 при C₀): " + route);
        log("маршрут фаба в комнате: " + route + " ✓");

        // та же операция на открытом воздухе Земли
        int ox = x0 - 3;
        sp.getServer().runCommand(set(ox, BY, z, "spacereloaded:diffusion_furnace"));
        sp.getServer().runCommand(set(ox, BY + 1, z, "spacereloaded:creative_power"));
        BlockPos open = new BlockPos(ox, BY, z);
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> {
            var f = (net.minecraft.world.Container) server.overworld().getBlockEntity(open);
            f.setItem(0, lot(ModItems.SILICON_WAFER, 1, 9.0f));
            f.setItem(1, new ItemStack(ModItems.OXYGEN_CANISTER));
        });
        context.waitTicks(210);
        float dirty = sp.getServer().computeOnServer(server -> ((net.minecraft.world.Container) server.overworld()
                .getBlockEntity(open)).getItem(4).getOrDefault(ModDataComponents.WAFER_DEFECTS, -1f));
        assertThat(dirty > 100, "Окисление на воздухе Земли (ISO 9): сотни дефектов/см², получено " + dirty);
        log(String.format(java.util.Locale.ROOT, "окисление на открытом воздухе: D = %.0f /см² — ноль годных ✓", dirty));
    }

    // ---------- 40. Суперсплав и программа T2 (006, US4/US5) ----------

    private void testSuperalloyEngine(ClientGameTestContext context, TestSingleplayerContext sp) {
        int tx = BX + 1160;
        int z = BZ;
        moveTo(context, sp, tx - 3, z);
        sp.getServer().runCommand(set(tx, BY, z, "spacereloaded:assembly_table"));
        sp.getServer().runCommand(set(tx + 1, BY, z, "spacereloaded:creative_power"));
        BlockPos table = new BlockPos(tx, BY, z);
        sp.getServer().runOnServer(server -> {
            var be = (org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity) server.overworld().getBlockEntity(table);
            ItemStack[] parts = {new ItemStack(ModItems.TURBOPUMP), new ItemStack(ModItems.INJECTOR_PLATE),
                    new ItemStack(ModItems.REGEN_NOZZLE)};
            for (ItemStack p : parts) {
                p.set(ModDataComponents.PART_QUALITY, 1.0f);
            }
            parts[0].set(ModDataComponents.TURBINE_SUPERALLOY, 1);
            for (int i = 0; i < 3; i++) {
                be.setItem(i, parts[i]);
            }
            be.setItem(3, new ItemStack(ModItems.TUNGSTEN_INGOT));
            be.setItem(4, new ItemStack(ModItems.STEEL_INGOT));
        });
        String engine = "";
        for (int waited = 0; waited < 400 && engine.isEmpty(); waited += 10) {
            context.waitTicks(10);
            engine = sp.getServer().computeOnServer(server -> {
                ItemStack out = ((org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity)
                        server.overworld().getBlockEntity(table)).getItem(
                        org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity.INPUT_SLOTS);
                var props = out.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
                if (out.isEmpty() || props == null) {
                    return "";
                }
                Boolean superalloy = props.get(org.alex_melan.spacereloaded.rocket.EngineBlock.SUPERALLOY);
                var resolver = new org.alex_melan.spacereloaded.rocket.PartPropertiesResolver(server.overworld());
                var base = ModBlocks.ROCKET_ENGINE.defaultBlockState()
                        .setValue(org.alex_melan.spacereloaded.rocket.EngineBlock.QUALITY, 10);
                var hot = resolver.resolve(base.setValue(org.alex_melan.spacereloaded.rocket.EngineBlock.SUPERALLOY, true))
                        .orElseThrow();
                var cold = resolver.resolve(base).orElseThrow();
                return superalloy + " " + String.format(java.util.Locale.ROOT, "%.3f", hot.thrustN() / cold.thrustN());
            });
        }
        assertThat(engine.equals("true 1.300"), "Колесо из суперсплава: SUPERALLOY и тяга ×1.3; получено " + engine);
        log("суперсплавный турбонасос: " + engine + " ✓");
    }

    // ---------- 41. Воздух кабины и шлюз (007, US1) ----------

    private static org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity tank(net.minecraft.server.MinecraftServer server,
                                                                                   BlockPos pos) {
        return (org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity) server.overworld().getBlockEntity(pos);
    }

    /**
     * Комната 3×3×3 (вакуумный режим): контроллер наполняет её из баллонов O₂/N₂ до 101 кПа —
     * масса газа уходит из баллонов; игрок дышит (CO₂ растёт по BVAD); картридж LiOH поглощает;
     * шлюз 1×2 с насосом: наддув из комнаты, откачка ≈ 79 с с возвратом газа в баллоны.
     */
    private void testCabinAir(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1200;
        int z0 = BZ;
        // игрок дышит (не наблюдатель) и не гибнет в вакууме, пока кабина наполняется (творческий)
        context.runOnClient(mc -> {
            if (mc.player != null && mc.player.isDeadOrDying()) {
                mc.player.respawn();
            }
        });
        context.waitTicks(10);
        sp.getServer().runCommand("gamemode creative @a");
        moveTo(context, sp, x0 - 6, z0 + 2);
        // комната: снаружи 5×5×5 из обшивки, полость 3×3×3
        sp.getServer().runCommand(fill(x0, BY, z0, x0 + 4, BY + 4, z0 + 4, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(x0 + 1, BY + 1, z0 + 1, x0 + 3, BY + 3, z0 + 3, "minecraft:air"));
        BlockPos controller = new BlockPos(x0 + 3, BY + 1, z0 + 3);
        sp.getServer().runCommand(set(x0 + 3, BY + 1, z0 + 3, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(x0 + 3, BY + 1, z0 + 4, "spacereloaded:creative_power"));
        BlockPos o2Tank = new BlockPos(x0 + 4, BY + 1, z0 + 3);
        BlockPos n2Tank = new BlockPos(x0 + 3, BY, z0 + 3);
        sp.getServer().runCommand(set(o2Tank.getX(), o2Tank.getY(), o2Tank.getZ(), "spacereloaded:gas_tank"));
        sp.getServer().runCommand(set(n2Tank.getX(), n2Tank.getY(), n2Tank.getZ(), "spacereloaded:gas_tank"));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> {
            tank(server, o2Tank).insert(org.alex_melan.spacereloaded.lifesupport.GasKind.OXYGEN, 50);
            tank(server, n2Tank).insert(org.alex_melan.spacereloaded.lifesupport.GasKind.NITROGEN, 100);
        });
        String state = "";
        for (int waited = 0; waited < 900; waited += 20) {
            context.waitTicks(20);
            state = sp.getServer().computeOnServer(server -> {
                SealedZone zone = ZoneManager.zoneAt(server.overworld(), controller);
                var gas = zone == null ? null : org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(), zone);
                return gas == null ? "none" : String.format(java.util.Locale.ROOT, "V=%.0f p=%.1f pO2=%.1f",
                        gas.volume(), gas.pressure(), gas.pO2());
            });
            if (state.contains("p=101.")) {
                break;
            }
        }
        double used = sp.getServer().computeOnServer(server ->
                50 - tank(server, o2Tank).mass() + 100 - tank(server, n2Tank).mass());
        double expected = org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.fill(101.325, 0.21, 26)[0]
                + org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.fill(101.325, 0.21, 26)[1];
        assertThat(state.startsWith("V=26 p=101.") && Math.abs(used - expected) < 1.0,
                String.format(java.util.Locale.ROOT, "Наполнение из баллонов: %s, израсходовано %.2f кг (ожидалось %.2f)",
                        state, used, expected));
        log(String.format(java.util.Locale.ROOT, "воздух кабины: %s из баллонов, %.1f кг газа ✓", state, used));

        // дыхание: игрок в комнате — CO₂ растёт на 1.01 кг за игровые сутки (наблюдатель не дышит)
        sp.getServer().runCommand(String.format("tp @p %d %d %d", x0 + 2, BY + 1, z0 + 2));
        context.waitTicks(40);
        double[] co2 = new double[2];
        co2[0] = sp.getServer().computeOnServer(server -> org.alex_melan.spacereloaded.lifesupport.LifeSupportState
                .now(server.overworld(), ZoneManager.zoneAt(server.overworld(), controller)).mCo2());
        context.waitTicks(400);
        co2[1] = sp.getServer().computeOnServer(server -> org.alex_melan.spacereloaded.lifesupport.LifeSupportState
                .now(server.overworld(), ZoneManager.zoneAt(server.overworld(), controller)).mCo2());
        double rate = (co2[1] - co2[0]) / (400 / 24000.0);
        assertThat(Math.abs(rate - 1.01) < 0.06, "Выдох CO₂ 1.01 кг/сут, получено " + rate);
        log(String.format(java.util.Locale.ROOT, "дыхание: CO₂ %.3f кг/игровые сутки ✓", rate));

        // картридж LiOH в стене комнаты
        BlockPos scrubber = new BlockPos(x0 + 2, BY + 2, z0);
        sp.getServer().runCommand(set(x0 + 2, BY + 2, z0, "spacereloaded:co2_scrubber"));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> ((net.minecraft.world.Container) server.overworld().getBlockEntity(scrubber))
                .setItem(0, new ItemStack(ModItems.LIOH_CARTRIDGE)));
        context.waitTicks(400);
        String scrub = sp.getServer().computeOnServer(server -> {
            ItemStack c = ((net.minecraft.world.Container) server.overworld().getBlockEntity(scrubber)).getItem(0);
            var gas = org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(),
                    ZoneManager.zoneAt(server.overworld(), controller));
            return c.getDamageValue() + "g pCO2=" + String.format(java.util.Locale.ROOT, "%.3f", gas.pCo2());
        });
        int grams = Integer.parseInt(scrub.substring(0, scrub.indexOf('g')));
        assertThat(grams > 0, "Картридж LiOH поглощает CO₂: " + scrub);
        log("поглотитель LiOH: " + scrub + " ✓");
        sp.getServer().runCommand(String.format("tp @p %d %d %d", x0 - 6, BY, z0 + 2));

        // шлюз: тамбур 1×2 за западной стеной, насос в полу, баллоны у насоса
        sp.getServer().runCommand(fill(x0 - 2, BY - 1, z0 + 1, x0 - 1, BY + 3, z0 + 3, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(x0 - 1, BY + 1, z0 + 2, x0 - 1, BY + 2, z0 + 2, "minecraft:air"));
        sp.getServer().runCommand(set(x0 - 1, BY, z0 + 2, "spacereloaded:airlock_pump"));
        // питание насоса: герметичный кабель в стене тамбура, источник снаружи
        sp.getServer().runCommand(set(x0 - 2, BY, z0 + 2, "spacereloaded:energy_cable"));
        sp.getServer().runCommand(set(x0 - 3, BY, z0 + 2, "spacereloaded:creative_power"));
        BlockPos pumpO2 = new BlockPos(x0 - 1, BY - 1, z0 + 2);
        BlockPos pumpN2 = new BlockPos(x0 - 1, BY, z0 + 1);
        sp.getServer().runCommand(set(pumpO2.getX(), pumpO2.getY(), pumpO2.getZ(), "spacereloaded:gas_tank"));
        sp.getServer().runCommand(set(pumpN2.getX(), pumpN2.getY(), pumpN2.getZ(), "spacereloaded:gas_tank"));
        sp.getServer().runCommand(fill(x0, BY + 1, z0 + 2, x0, BY + 2, z0 + 2, "spacereloaded:hermetic_hatch"));
        sp.getServer().runCommand(fill(x0 - 2, BY + 1, z0 + 2, x0 - 2, BY + 2, z0 + 2, "spacereloaded:hermetic_hatch"));
        context.waitTicks(40);
        BlockPos pump = new BlockPos(x0 - 1, BY, z0 + 2);
        // внутренний люк: наддув тамбура из комнаты
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z0 + 2, "minecraft:redstone_block"));
        context.waitTicks(SpaceReloaded.config().airlockCycleTicks + 40);
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z0 + 2, "minecraft:air"));
        context.waitTicks(60);
        String chamber = sp.getServer().computeOnServer(server -> {
            var zone = ZoneManager.zoneAt(server.overworld(), pump);
            var gas = zone == null ? null : org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(), zone);
            return gas == null ? "none" : String.format(java.util.Locale.ROOT, "%s V=%.0f p=%.0f",
                    zone.status(), gas.volume(), gas.pressure());
        });
        assertThat(chamber.startsWith("SEALED V=2 p=10") || chamber.startsWith("SEALED V=2 p=9"),
                "Тамбур после наддува из комнаты и закрытия люка: " + chamber);
        log("шлюз: наддув тамбура " + chamber + " ✓");
        // наружный люк: откачка насосом ≈ 79 с, газ в баллоны у насоса
        sp.getServer().runCommand(set(x0 - 3, BY + 2, z0 + 2, "minecraft:redstone_block"));
        context.waitTicks(1400);
        boolean stillClosed = sp.getServer().computeOnServer(server -> !server.overworld()
                .getBlockState(new BlockPos(x0 - 2, BY + 1, z0 + 2)).getValue(org.alex_melan.spacereloaded.sealing.HermeticHatchBlock.OPEN));
        assertThat(stillClosed, "За 70 с откачка ещё идёт — люк закрыт");
        context.waitTicks(260);
        String pumped = sp.getServer().computeOnServer(server -> {
            boolean open = server.overworld().getBlockState(new BlockPos(x0 - 2, BY + 1, z0 + 2))
                    .getValue(org.alex_melan.spacereloaded.sealing.HermeticHatchBlock.OPEN);
            return open + String.format(java.util.Locale.ROOT, " O2=%.2f N2=%.2f", tank(server, pumpO2).mass(),
                    tank(server, pumpN2).mass());
        });
        assertThat(pumped.startsWith("true"), "После ~79 с откачки люк открыт: " + pumped);
        double back = Double.parseDouble(pumped.substring(pumped.indexOf("O2=") + 3, pumped.indexOf(" N2")))
                + Double.parseDouble(pumped.substring(pumped.indexOf("N2=") + 3));
        assertThat(back > 1.8 && back < 2.3, "Насос вернул в баллоны ~2.1 кг из 2.4: " + pumped);
        sp.getServer().runCommand(set(x0 - 3, BY + 2, z0 + 2, "minecraft:air"));
        log("шлюз: откачка насосом, " + pumped + " кг в баллонах, стравлено ~0.33 кг ✓");
    }

    // ---------- 42. Оранжерея (007, US2) ----------

    /**
     * Комната 3×3×3 из баллонов, лоток пшеницы под фитолампой: без CO₂ (газ из баллонов) рост
     * стоит; с CO₂ выше насыщения лоток поглощает 77 г CO₂ и даёт 56 г O₂ за игровые сутки.
     */
    private void testGreenhouse(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1240;
        int z0 = BZ;
        moveTo(context, sp, x0 - 4, z0 + 2);
        sp.getServer().runCommand(fill(x0, BY, z0, x0 + 4, BY + 4, z0 + 4, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(x0 + 1, BY + 1, z0 + 1, x0 + 3, BY + 3, z0 + 3, "minecraft:air"));
        BlockPos controller = new BlockPos(x0 + 3, BY + 1, z0 + 3);
        sp.getServer().runCommand(set(x0 + 3, BY + 1, z0 + 3, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(x0 + 3, BY + 1, z0 + 4, "spacereloaded:creative_power"));
        BlockPos o2 = new BlockPos(x0 + 4, BY + 1, z0 + 3);
        BlockPos n2 = new BlockPos(x0 + 3, BY, z0 + 3);
        sp.getServer().runCommand(set(o2.getX(), o2.getY(), o2.getZ(), "spacereloaded:gas_tank"));
        sp.getServer().runCommand(set(n2.getX(), n2.getY(), n2.getZ(), "spacereloaded:gas_tank"));
        BlockPos tray = new BlockPos(x0 + 1, BY + 1, z0 + 1);
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z0 + 1, "spacereloaded:hydroponic_tray"));
        sp.getServer().runCommand(set(x0 + 1, BY + 2, z0 + 1, "spacereloaded:grow_lamp"));
        sp.getServer().runCommand(set(x0 + 1, BY + 3, z0 + 1, "spacereloaded:creative_power"));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> {
            tank(server, o2).insert(org.alex_melan.spacereloaded.lifesupport.GasKind.OXYGEN, 30);
            tank(server, n2).insert(org.alex_melan.spacereloaded.lifesupport.GasKind.NITROGEN, 60);
            var t = (org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlockEntity) server.overworld().getBlockEntity(tray);
            t.plant(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS));
            t.fertilize(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL));
            t.water();
        });
        for (int waited = 0; waited < 900; waited += 20) {
            context.waitTicks(20);
            boolean full = sp.getServer().computeOnServer(server -> {
                var gas = org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(),
                        ZoneManager.zoneAt(server.overworld(), controller));
                return gas != null && gas.pressure() > 100;
            });
            if (full) {
                break;
            }
        }
        double idle = sp.getServer().computeOnServer(server -> ((org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlockEntity)
                server.overworld().getBlockEntity(tray)).rate());
        assertThat(idle == 0, "Без CO₂ фотосинтеза нет, темп роста " + idle);
        // CO₂ 0.3 кПа — выше насыщения
        sp.getServer().runOnServer(server -> {
            var zone = ZoneManager.zoneAt(server.overworld(), controller);
            var gas = org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(), zone);
            org.alex_melan.spacereloaded.lifesupport.LifeSupportState.add(server.overworld(), zone, 0, 0,
                    org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.massFor(0.3,
                            org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.M_CO2, gas.volume(), 293.15));
        });
        context.waitTicks(60);
        double[] m0 = sp.getServer().computeOnServer(server -> {
            var gas = org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(),
                    ZoneManager.zoneAt(server.overworld(), controller));
            return new double[] {gas.mCo2(), ((org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlockEntity)
                    server.overworld().getBlockEntity(tray)).growth()};
        });
        context.waitTicks(400);
        double[] m1 = sp.getServer().computeOnServer(server -> {
            var gas = org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(server.overworld(),
                    ZoneManager.zoneAt(server.overworld(), controller));
            return new double[] {gas.mCo2(), ((org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlockEntity)
                    server.overworld().getBlockEntity(tray)).growth()};
        });
        double days = 400 / 24000.0;
        double uptake = (m0[0] - m1[0]) / days * 1000;
        double growth = (m1[1] - m0[1]) / days;
        assertThat(Math.abs(uptake - 77) < 8 && Math.abs(growth - 1) < 0.1,
                String.format(java.util.Locale.ROOT, "Пшеница под лампой: CO₂ %.1f г/сут (77), темп роста %.2f (1.0)", uptake, growth));
        log(String.format(java.util.Locale.ROOT, "оранжерея: лоток пшеницы поглощает %.1f г CO₂ в сутки, рост %.2f номинала ✓",
                uptake, growth));
    }

    // ---------- 43. Кольцо: вес из вращения (007, US3) ----------

    /** Гантель: ступица на оси X, спицы по 21 блоку вверх и вниз, площадки 3×3. */
    private void buildDumbbell(TestSingleplayerContext sp, int x0, int hy, int z0, boolean counterweight) {
        sp.getServer().runCommand(set(x0, hy, z0, "spacereloaded:spin_hub[axis=x]"));
        sp.getServer().runCommand(fill(x0, hy - 21, z0, x0, hy - 1, z0, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(x0 - 1, hy - 21, z0 - 1, x0 + 1, hy - 21, z0 + 1, "spacereloaded:hull_plating"));
        if (counterweight) {
            sp.getServer().runCommand(fill(x0, hy + 1, z0, x0, hy + 21, z0, "spacereloaded:hull_plating"));
            sp.getServer().runCommand(fill(x0 - 1, hy + 21, z0 - 1, x0 + 1, hy + 21, z0 + 1, "spacereloaded:hull_plating"));
        }
    }

    /**
     * Гантель r = 21 м: двигатели обода (2 × 2 кН на плече 21 м) раскручивают её по I·dω/dt = τ,
     * топливо расходуется F/(Isp·g₀); игрок у пола получает вес ω²·r; однобокая сборка срывает
     * подшипник, когда |Σm·ρ|·ω² превышает его грузоподъёмность.
     */
    private void testSpinRing(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1280;
        int z0 = BZ;
        int hy = BY + 40;
        moveTo(context, sp, x0 - 4, z0);
        sp.getServer().runCommand(String.format("forceload add %d %d %d %d", x0 - 8, z0 - 8, x0 + 8, z0 + 8));
        buildDumbbell(sp, x0, hy, z0, true);
        // двигатели: снизу сопло на юг, сверху на север — оба закручивают вокруг +X; сигнал — блок редстоуна
        sp.getServer().runCommand(set(x0, hy - 21, z0 + 2, "spacereloaded:rim_thruster[facing=south]"));
        sp.getServer().runCommand(set(x0, hy - 21, z0 + 3, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(set(x0, hy + 21, z0 - 2, "spacereloaded:rim_thruster[facing=north]"));
        sp.getServer().runCommand(set(x0, hy + 21, z0 - 3, "spacereloaded:hull_plating"));
        BlockPos tank = new BlockPos(x0 + 1, hy - 20, z0);
        sp.getServer().runCommand(set(tank.getX(), tank.getY(), tank.getZ(), "spacereloaded:fuel_tank"));
        BlockPos tank2 = new BlockPos(x0 + 1, hy + 20, z0);
        sp.getServer().runCommand(set(tank2.getX(), tank2.getY(), tank2.getZ(), "spacereloaded:fuel_tank"));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> {
            for (BlockPos t : List.of(tank, tank2)) {
                ((org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity) server.overworld().getBlockEntity(t))
                        .setPropellant(500, "spacereloaded:kerolox");
            }
        });
        BlockPos hub = new BlockPos(x0, hy, z0);
        context.waitTicks(60);
        double[] a = sp.getServer().computeOnServer(server -> {
            var h = (org.alex_melan.spacereloaded.station.SpinHubBlockEntity) server.overworld().getBlockEntity(hub);
            return new double[] {h.omega(), h.assembly().inertia(), h.isolated() ? 1 : 0, h.assembly().imbalance()};
        });
        assertThat(a[2] == 1 && a[1] > 0 && a[0] == 0, "Гантель изолирована, покоится, имеет момент инерции: "
                + java.util.Arrays.toString(a));
        // импульс тяги: сигнал на ~1.5 с (редстоун того же веса, что обшивка — масса сборки та же)
        sp.getServer().runCommand(set(x0, hy - 21, z0 + 3, "minecraft:redstone_block"));
        sp.getServer().runCommand(set(x0, hy + 21, z0 - 3, "minecraft:redstone_block"));
        context.waitTicks(30);
        sp.getServer().runCommand(set(x0, hy - 21, z0 + 3, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(set(x0, hy + 21, z0 - 3, "spacereloaded:hull_plating"));
        context.waitTicks(40);
        double[] b = sp.getServer().computeOnServer(server -> {
            var h = (org.alex_melan.spacereloaded.station.SpinHubBlockEntity) server.overworld().getBlockEntity(hub);
            double fuel = ((org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity) server.overworld().getBlockEntity(tank)).propellantKg();
            return new double[] {h.omega(), fuel};
        });
        double step = 2 * 2000 * 21 / a[1]; // Δω за секунду тяги
        long steps = Math.round(b[0] / step);
        assertThat(steps >= 1 && steps <= 2 && Math.abs(b[0] - steps * step) < step * 0.02,
                String.format(java.util.Locale.ROOT, "Раскрутка I·Δω = τ·t: ω %.5f, шаг τ/I %.5f", b[0], step));
        log(String.format(java.util.Locale.ROOT, "кольцо: I = %.3g кг·м², %d с тяги → ω = %.4f рад/с (τ·t/I) ✓",
                a[1], steps, b[0]));
        // игрок на нижней площадке: вес ω²·r
        sp.getServer().runCommand("gamemode creative @a");
        sp.getServer().runCommand(String.format("tp @p %d %d %d", x0 + 1, hy - 20, z0 + 1));
        context.waitTicks(60);
        String weight = sp.getServer().computeOnServer(server -> {
            var h = (org.alex_melan.spacereloaded.station.SpinHubBlockEntity) server.overworld().getBlockEntity(hub);
            var player = server.getPlayerList().getPlayers().get(0);
            double attr = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
            double g = attr / 0.08 * 9.81;
            double expectedG = org.alex_melan.spacereloaded.core.station.SpinGravity.gravity(Math.abs(h.omega()),
                    hub.getY() + 0.5 - player.getY());
            return String.format(java.util.Locale.ROOT, "%.3f %.3f", g, expectedG);
        });
        double g = Double.parseDouble(weight.split(" ")[0]);
        double eg = Double.parseDouble(weight.split(" ")[1]);
        assertThat(eg > 1 && Math.abs(g - eg) < 0.05, "Вес у пола кольца ω²·r: получено " + weight);
        log("кольцо: вес у пола " + weight.split(" ")[0] + " м/с² (ω²·r) ✓");
        sp.getServer().runCommand(String.format("tp @p %d %d %d", x0 - 4, BY, z0));

        // однобокая сборка (без противовеса) срывает подшипник
        int x1 = x0 + 12;
        buildDumbbell(sp, x1, hy, z0, false);
        sp.getServer().runCommand(set(x1, hy - 21, z0 + 2, "spacereloaded:rim_thruster[facing=south]"));
        sp.getServer().runCommand(set(x1, hy - 21, z0 + 3, "minecraft:redstone_block"));
        BlockPos tank3 = new BlockPos(x1 + 1, hy - 20, z0);
        sp.getServer().runCommand(set(tank3.getX(), tank3.getY(), tank3.getZ(), "spacereloaded:fuel_tank"));
        context.waitTicks(2);
        sp.getServer().runOnServer(server -> ((org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity) server.overworld()
                .getBlockEntity(tank3)).setPropellant(1000, "spacereloaded:kerolox"));
        BlockPos hub2 = new BlockPos(x1, hy, z0);
        boolean broken = false;
        for (int waited = 0; waited < 2400 && !broken; waited += 20) {
            context.waitTicks(20);
            broken = sp.getServer().computeOnServer(server -> !server.overworld().getBlockState(hub2).is(ModBlocks.SPIN_HUB));
        }
        assertThat(broken, "Однобокая сборка должна сорвать подшипник ступицы");
        log("кольцо: без противовеса дисбаланс |Σm·ρ|·ω² сорвал подшипник ✓");
    }

    /**
     * Ровер на полосе: камень — твёрдая поверхность (сопротивление 0.015·m·g), песок — грунт Земли
     * по Беккеру. На камне проверяется теорема об энергии η·ΔE_бат = ½·m·v² + R·s (батарея платит за
     * кинетическую энергию и работу сопротивления, не больше), на песке — установившаяся скорость
     * v = η·P/R_c (мощность моторов уходит на уплотнение грунта).
     */
    private void testRover(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1320;
        int z0 = BZ;
        moveTo(context, sp, x0 - 4, z0);
        sp.getServer().runCommand(String.format("forceload add %d %d %d %d", x0 - 8, z0 - 8, x0 + 16, z0 + 140));
        sp.getServer().runCommand(fill(x0 - 2, BY - 1, z0, x0 + 2, BY - 1, z0 + 130, "minecraft:stone"));
        int xs = x0 + 10; // песчаная полоса на каменном основании
        sp.getServer().runCommand(fill(xs - 2, BY - 2, z0, xs + 2, BY - 2, z0 + 60, "minecraft:stone"));
        sp.getServer().runCommand(fill(xs - 2, BY - 1, z0, xs + 2, BY - 1, z0 + 60, "minecraft:sand"));
        sp.getServer().runCommand(String.format("summon spacereloaded:rover %d.5 %d %d.5 {Rotation:[0f,0f]}", x0, BY, z0 + 2));
        sp.getServer().runCommand(String.format("summon spacereloaded:rover %d.5 %d %d.5 {Rotation:[0f,0f]}", xs, BY, z0 + 2));
        context.waitTicks(10);
        net.minecraft.world.phys.AABB hard = new net.minecraft.world.phys.AABB(x0 - 3, BY - 2, z0 - 2, x0 + 3, BY + 3, z0 + 140);
        net.minecraft.world.phys.AABB loose = new net.minecraft.world.phys.AABB(xs - 3, BY - 2, z0 - 2, xs + 3, BY + 3, z0 + 70);
        double full = org.alex_melan.spacereloaded.vehicle.RoverEntity.capacityE();
        sp.getServer().runOnServer(server -> {
            for (var box : List.of(hard, loose)) {
                server.overworld().getEntitiesOfClass(org.alex_melan.spacereloaded.vehicle.RoverEntity.class, box)
                        .forEach(r -> r.testSetup(4, full, true));
            }
        });
        context.waitTicks(200);
        double[] r = sp.getServer().computeOnServer(server -> {
            var a = server.overworld().getEntitiesOfClass(org.alex_melan.spacereloaded.vehicle.RoverEntity.class, hard).get(0);
            var b = server.overworld().getEntitiesOfClass(org.alex_melan.spacereloaded.vehicle.RoverEntity.class, loose).get(0);
            a.testForward(false);
            b.testForward(false);
            return new double[] {a.mass(), a.speed(), a.odometer(), full - a.charge(), b.speed(), b.odometer()};
        });
        double m = r[0];
        double g = 9.81;
        double eta = org.alex_melan.spacereloaded.vehicle.RoverEntity.EFFICIENCY;
        double spentJ = r[3] / org.alex_melan.spacereloaded.lifesupport.EnergyScale.E_PER_KWH * 3.6e6;
        double work = 0.5 * m * r[1] * r[1] + 0.015 * m * g * r[2];
        assertThat(r[1] > 2 && Math.abs(eta * spentJ - work) < 0.03 * work, String.format(java.util.Locale.ROOT,
                "Камень: η·ΔE = ½mv² + R·s — v %.2f м/с, s %.1f м, η·ΔE %.0f Дж, работа %.0f Дж", r[1], r[2], eta * spentJ, work));
        log(String.format(java.util.Locale.ROOT, "ровер: камень, m = %.0f кг, за 10 с v = %.2f м/с, s = %.1f м, η·ΔE = %.0f Дж = ½mv² + R·s ✓",
                m, r[1], r[2], eta * spentJ));
        var soil = new org.alex_melan.spacereloaded.core.vehicle.Terramechanics.Soil(1.1, 0.0625, 0.964, 0.104, 28, 2.5);
        double rc = 4 * org.alex_melan.spacereloaded.core.vehicle.Terramechanics.compactionN(soil,
                org.alex_melan.spacereloaded.vehicle.RoverEntity.WHEEL, m * g / 4);
        double vSand = eta * org.alex_melan.spacereloaded.vehicle.RoverEntity.MOTOR_W / rc;
        assertThat(Math.abs(r[4] - vSand) < 0.03 * vSand && r[1] > 3 * r[4], String.format(java.util.Locale.ROOT,
                "Песок: v = η·P/R_c = %.3f м/с, получено %.3f м/с (R_c %.0f Н)", vSand, r[4], rc));
        log(String.format(java.util.Locale.ROOT, "ровер: песок, R_c = %.0f Н (Беккер), v = %.3f м/с = η·P/R_c ✓", rc, r[4]));
        context.waitTicks(100);
        double stopped = sp.getServer().computeOnServer(server -> server.overworld()
                .getEntitiesOfClass(org.alex_melan.spacereloaded.vehicle.RoverEntity.class, hard).get(0).speed());
        assertThat(stopped < r[1] - 0.015 * g * 4.5 && stopped > r[1] - 0.015 * g * 5.5, "Накат: сопротивление качению тормозит ровер, v = " + stopped);
        log(String.format(java.util.Locale.ROOT, "ровер: накат 5 с, v %.2f → %.2f м/с ✓", r[1], stopped));
    }

    /**
     * Съёмка с орбиты Земли: телескоп 10 см с 200 км различает 1.22·λ·h/D = 1.34 м, поэтому самый мелкий
     * масштаб — 1 (2 м/пикс); ожидание не дольше T_cov/N; на снимке видна поверхность (синяя шерсть),
     * но не руда под камнем.
     */
    private static net.minecraft.world.item.ItemStack slotOf(net.minecraft.world.entity.player.Player player,
                                                            net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return player.getInventory().getItem(i);
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    private void testOrbitalImaging(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1340;
        int z0 = BZ;
        moveTo(context, sp, x0 - 4, z0);
        sp.getServer().runCommand(String.format("forceload add %d %d %d %d", x0 - 8, z0 - 8, x0 + 16, z0 + 24));
        sp.getServer().runCommand(set(x0, BY, z0, "spacereloaded:mission_control"));
        sp.getServer().runCommand(fill(x0 + 4, BY, z0, x0 + 11, BY, z0 + 7, "minecraft:blue_wool"));
        sp.getServer().runCommand(fill(x0 + 4, BY, z0 + 12, x0 + 11, BY, z0 + 19, "minecraft:diamond_ore"));
        sp.getServer().runCommand(fill(x0 + 4, BY + 1, z0 + 12, x0 + 11, BY + 1, z0 + 19, "minecraft:stone"));
        context.waitTicks(5);
        BlockPos mc = new BlockPos(x0, BY, z0);
        String ordered = sp.getServer().computeOnServer(server -> {
            var level = server.overworld();
            var player = server.getPlayerList().getPlayers().get(0);
            player.getInventory().clearContent();
            var network = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server);
            network.setImagingSats(level.dimension(), 0);
            var map = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAP, 2);
            org.alex_melan.spacereloaded.orbit.OrbitalImages.order(level, mc, player, map);
            boolean refused = map.getCount() == 2;
            network.setImagingSats(level.dimension(), 1);
            var o = org.alex_melan.spacereloaded.orbit.OrbitalImages.optics(level).orElseThrow();
            map.set(ModDataComponents.IMAGE_SCALE, 0);
            org.alex_melan.spacereloaded.orbit.OrbitalImages.order(level, mc, player, map);
            boolean diffraction = map.getCount() == 2;
            map.set(ModDataComponents.IMAGE_SCALE, 1);
            long now = level.getGameTime();
            org.alex_melan.spacereloaded.orbit.OrbitalImages.order(level, mc, player, map);
            var image = slotOf(player, ModItems.ORBITAL_IMAGE);
            var order = image.get(ModDataComponents.IMAGE_ORDER);
            long worst = org.alex_melan.spacereloaded.core.orbit.OrbitalImaging.waitTicks(o.radius(), o.altitude(), o.mu(), 1, 1, 1.0);
            // до срока снимок не проявляется
            org.alex_melan.spacereloaded.orbit.OrbitalImages.develop(player, image, order);
            boolean early = image.is(ModItems.ORBITAL_IMAGE);
            // стенд не ждёт пролёта: срок наступил
            var due = new org.alex_melan.spacereloaded.orbit.ImageOrder(order.dimension(), order.x(), order.z(), order.scale(), now);
            image.set(ModDataComponents.IMAGE_ORDER, due);
            org.alex_melan.spacereloaded.orbit.OrbitalImages.develop(player, image, due);
            return String.format(java.util.Locale.ROOT, "%b %b %.3f %d %d %d %b", refused, diffraction, o.gsd(), o.minScale(),
                    order.readyTick() - now, worst, early);
        });
        String[] f = ordered.split(" ");
        long wait = Long.parseLong(f[4]);
        long worst = Long.parseLong(f[5]);
        assertThat(f[0].equals("true") && f[1].equals("true") && f[3].equals("1") && wait >= 0 && wait <= worst
                        && f[6].equals("true"),
                "Заказ: без спутника отказ, масштаб 0 — дифракция, ожидание в [0, T_cov/N], до срока не проявляется: " + ordered);
        log("съёмка: GSD " + f[2] + " м с 200 км → масштаб ≥ " + f[3] + "; ожидание " + wait + " из " + worst
                + " тиков (T_cov/N÷130) ✓");
        context.waitTicks(40);
        String pixels = sp.getServer().computeOnServer(server -> {
            var level = server.overworld();
            var player = server.getPlayerList().getPlayers().get(0);
            var filled = slotOf(player, net.minecraft.world.item.Items.FILLED_MAP);
            if (filled.isEmpty()) {
                return "no map";
            }
            var data = net.minecraft.world.item.MapItem.getSavedData(filled, level);
            int step = 1 << data.scale;
            int left = data.centerX - 64 * step;
            int top = data.centerZ - 64 * step;
            int wool = (data.colors[(z0 + 4 - top) / step * 128 + (x0 + 8 - left) / step] & 0xFF) >> 2;
            int ore = (data.colors[(z0 + 16 - top) / step * 128 + (x0 + 8 - left) / step] & 0xFF) >> 2;
            return wool + " " + ore + " " + data.locked + " " + data.scale;
        });
        String expected = net.minecraft.world.level.material.MapColor.COLOR_BLUE.id + " "
                + net.minecraft.world.level.material.MapColor.STONE.id + " true 1";
        assertThat(pixels.equals(expected), "Снимок: шерсть синяя, над рудой камень, карта заперта — ожидалось " + expected
                + ", получено " + pixels);
        log("съёмка: снимок проявлен в запертую карту масштаба 1, поверхность видна, руда под камнем — нет ✓");
    }

    // ---------- 36. Взрыв и герметичность (T024) ----------

    private void testExplosionSealing(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 960;
        int z = BZ + 60;
        moveTo(context, sp, x0 - 6, z);
        sp.getServer().runCommand(fill(x0, BY, z, x0 + 4, BY + 4, z + 4, "spacereloaded:hull_plating"));
        sp.getServer().runCommand(fill(x0 + 1, BY + 1, z + 1, x0 + 3, BY + 3, z + 3, "minecraft:air"));
        sp.getServer().runCommand(set(x0 + 2, BY + 1, z + 2, "spacereloaded:atmosphere_controller"));
        sp.getServer().runCommand(set(x0 + 1, BY + 1, z + 2, "spacereloaded:creative_power"));
        sp.getServer().runCommand("spacereloaded debug vacuum on");
        BlockPos controller = new BlockPos(x0 + 2, BY + 1, z + 2);
        SealingStatus status = SealingStatus.INVALID_ORIGIN;
        for (int waited = 0; waited < 300 && status != SealingStatus.SEALED; waited += 10) {
            context.waitTicks(10);
            status = sp.getServer().computeOnServer(server -> {
                SealedZone zone = ZoneManager.zoneAt(server.overworld(), controller);
                return zone == null ? SealingStatus.INVALID_ORIGIN : zone.status();
            });
        }
        assertThat(status == SealingStatus.SEALED, "Комната должна быть герметична до взрыва: " + status);
        sp.getServer().runOnServer(server -> server.overworld().explode(null, x0 + 5.5, BY + 2.5, z + 2.5, 4.0f,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT));
        SealingStatus after = status;
        for (int waited = 0; waited < 300 && after == SealingStatus.SEALED; waited += 10) {
            context.waitTicks(10);
            after = sp.getServer().computeOnServer(server -> {
                SealedZone zone = ZoneManager.zoneAt(server.overworld(), controller);
                return zone == null ? SealingStatus.INVALID_ORIGIN : zone.status();
            });
        }
        assertThat(after != SealingStatus.SEALED, "Взрыв пробил стену — зона должна потерять герметичность: " + after);
        log("взрыв → герметичность обновлена (" + after + ") ✓ — T024 закрыт");
    }

    // ---------- 37. Витрина облика: рабочие состояния (005, визуальный проход) ----------

    private void testVisualShowcase(ClientGameTestContext context, TestSingleplayerContext sp) {
        int x0 = BX + 1000;
        int z = BZ;
        moveTo(context, sp, x0 - 6, z);
        sp.getServer().runCommand("time set 3000");
        sp.getServer().runCommand(fill(x0 - 1, BY - 1, z - 1, x0 + 12, BY - 1, z + 3, "minecraft:smooth_stone"));
        sp.getServer().runCommand(set(x0, BY, z, "spacereloaded:creative_power"));
        sp.getServer().runCommand(fill(x0, BY, z + 1, x0 + 10, BY, z + 1, "spacereloaded:energy_cable"));
        sp.getServer().runCommand(set(x0 + 2, BY, z, "spacereloaded:electric_furnace[facing=north]"));
        sp.getServer().runCommand(set(x0 + 4, BY, z, "spacereloaded:coal_generator[facing=north]"));
        sp.getServer().runCommand(set(x0 + 6, BY, z, "spacereloaded:electrolyzer[facing=north]"));
        sp.getServer().runCommand(set(x0 + 8, BY, z, "spacereloaded:capacitor"));
        sp.getServer().runCommand(set(x0 + 10, BY, z, "spacereloaded:crusher[facing=north]"));
        sp.getServer().runCommand(set(x0 + 1, BY, z + 3, "spacereloaded:solar_panel"));
        sp.getServer().runCommand(set(x0 + 3, BY, z + 3, "spacereloaded:fuel_tank"));
        sp.getServer().runCommand(set(x0 + 5, BY, z + 3, "spacereloaded:hermetic_hatch[open=true]"));
        sp.getServer().runCommand(set(x0 + 7, BY, z + 3, "spacereloaded:rocket_engine"));
        sp.getServer().runCommand(fill(x0 - 1, BY - 1, z + 4, x0 + 12, BY - 1, z + 7, "minecraft:smooth_stone"));
        String[] wave2 = {"command_module", "satellite", "power_satellite", "orbital_cannon", "mission_control",
                "rectenna", "atmosphere_controller"};
        for (int i = 0; i < wave2.length; i++) {
            sp.getServer().runCommand(set(x0 + i * 2, BY, z + 6, "spacereloaded:" + wave2[i]));
        }
        context.waitTicks(3);
        sp.getServer().runOnServer(server -> {
            var level = server.overworld();
            if (level.getBlockEntity(new BlockPos(x0 + 2, BY, z))
                    instanceof org.alex_melan.spacereloaded.machine.ProcessingMachineBlockEntity furnace) {
                furnace.setItem(0, new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 64));
            }
            if (level.getBlockEntity(new BlockPos(x0 + 4, BY, z))
                    instanceof org.alex_melan.spacereloaded.machine.ProcessingMachineBlockEntity gen) {
                gen.setItem(0, new ItemStack(net.minecraft.world.item.Items.COAL, 64));
            }
            if (level.getBlockEntity(new BlockPos(x0 + 6, BY, z))
                    instanceof org.alex_melan.spacereloaded.machine.ProcessingMachineBlockEntity el) {
                el.setItem(0, new ItemStack(net.minecraft.world.item.Items.ICE, 64));
            }
            if (level.getBlockEntity(new BlockPos(x0 + 10, BY, z))
                    instanceof org.alex_melan.spacereloaded.machine.ProcessingMachineBlockEntity cr) {
                cr.setItem(0, new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 64));
            }
            if (level.getBlockEntity(new BlockPos(x0 + 3, BY, z + 3)) instanceof FuelTankBlockEntity tank) {
                tank.fill(1000, "spacereloaded:kerolox");
            }
        });
        context.waitTicks(80);
        String states = sp.getServer().computeOnServer(server -> {
            var level = server.overworld();
            boolean furnace = level.getBlockState(new BlockPos(x0 + 2, BY, z))
                    .getValue(org.alex_melan.spacereloaded.machine.MachineActivity.ACTIVE);
            boolean cable = level.getBlockState(new BlockPos(x0 + 3, BY, z + 1))
                    .getValue(org.alex_melan.spacereloaded.energy.CableBlock.ENERGIZED);
            int tank = level.getBlockState(new BlockPos(x0 + 3, BY, z + 3))
                    .getValue(org.alex_melan.spacereloaded.rocket.FuelTankBlock.LEVEL);
            return "печь=" + furnace + " кабель=" + cable + " бак=" + tank;
        });
        assertThat(states.equals("печь=true кабель=true бак=2"), "Облик рабочих состояний: " + states);
        prepareCamera(context, sp, x0 + 5, BY + 2.5, z - 4.5, 0f, 22f);
        snapshot(context, "showcase_front", 5);
        prepareCamera(context, sp, x0 + 4, BY + 3, z + 7.5, 180f, 28f);
        snapshot(context, "showcase_back", 5);
        prepareCamera(context, sp, x0 + 6, BY + 3.5, z + 12.5, 180f, 25f);
        snapshot(context, "showcase_models", 5);
        sp.getServer().runCommand("gamemode survival @a");
        log("облик: " + states + " ✓");
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

        // 003 (FR-130): подповерхностный марсианский лёд — сырьё реактора Сабатье
        int ice = sp.getServer().computeOnServer(SpaceReloadedClientGameTest::marsIceCount);
        assertThat(ice >= 1, "В 3×3 чанках Марса должен найтись марсианский лёд, найдено: " + ice);
        log("марсианский лёд: " + ice + " блоков в 3×3 чанках ✓");
    }

    /** Мин/макс высоты поверхности по сетке 65×65 вокруг начала координат. */
    /** 003 (FR-130): подповерхностный марсианский лёд в области 3×3 чанка. */
    private static int marsIceCount(net.minecraft.server.MinecraftServer server) {
        ServerLevel mars = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("spacereloaded", "mars")));
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                var chunk = mars.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 4; y <= 90; y++) {
                            if (chunk.getBlockState(cursor.set(x, y, z)).is(ModBlocks.MARS_ICE)) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

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
