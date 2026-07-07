package org.alex_melan.spacereloaded.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;
import org.alex_melan.spacereloaded.machine.CrusherBlockEntity;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;
import org.alex_melan.spacereloaded.rocket.RocketEntity;
import org.alex_melan.spacereloaded.rocket.RocketInteractions;
import org.alex_melan.spacereloaded.sealing.SealedZone;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

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
        }
    }

    // ---------- 1. Герметичность ----------

    /** Телепорт игрока к площадке (тикающие чанки) на обсидиановый пятачок. */
    private void moveTo(ClientGameTestContext context, TestSingleplayerContext sp, int x, int z) {
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

        // Починка — снова герметичен
        sp.getServer().runCommand(set(BX + 4, BY + 4, BZ + 4, "spacereloaded:hull_plating"));
        SealingStatus repaired = waitForStatus(context, sp, 200, SealingStatus.SEALED);
        assertThat(repaired == SealingStatus.SEALED,
                "После починки угла зона должна восстановиться, получено: " + repaired);
        log("починка угла → SEALED ✓");
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
