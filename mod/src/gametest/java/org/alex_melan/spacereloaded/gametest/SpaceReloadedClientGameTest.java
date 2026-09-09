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
            testStaging(context, sp);
            testAttitude(context, sp);
            testAtmosphere(context, sp);
            testStrikeGuidance(context, sp);
            testCargoLine(context, sp);
            testWetWorkshop(context, sp);
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
