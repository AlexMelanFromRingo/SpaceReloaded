package org.alex_melan.spacereloaded.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.rocket.FlightProgramItem;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Грузовой терминал (003, FR-110…FR-116, D24): автомат челночной линии
 * «пэд A ↔ пэд B». Хранит полётную программу (цель, маяк, канал), раз в
 * {@code cargoLineCheckIntervalTicks} смотрит на припаркованный беспилотный борт
 * в своей зоне (та же геометрия, что у погрузчика), ждёт, пока груз и топливо
 * не перестанут меняться (погрузчики/колонки закончили), и запускает борт через
 * честные проверки {@link RocketEntity#tryLaunchUnmanned}: бюджет Δv маршрута,
 * окно перелёта, покрытие. Отказ — состояние с причиной и цифрами, старт не
 * выполняется, проверка повторяется. По умолчанию HOLD — чужой борт не улетит.
 */
public class CargoTerminalBlockEntity extends BlockEntity {

    public enum Mode {
        HOLD, AUTO;

        public Mode next() {
            return this == HOLD ? AUTO : HOLD;
        }
    }

    public enum State {
        NO_PROGRAM, HOLD, WAITING_CRAFT, CREW_ABOARD, SERVICING, WAITING_WINDOW, NO_COVERAGE, REFUSED, LAUNCHED
    }

    private static final int HORIZONTAL_RANGE = 8;
    private static final int VERTICAL_RANGE = 48;
    private static final int SEEN_LIMIT = 16;

    private Identifier destinationDimension;
    private GlobalPos pad;
    private int frequency;
    private Mode mode = Mode.HOLD;
    private State state = State.NO_PROGRAM;
    /** Подробность состояния (причина отказа с цифрами); не сохраняется — пересчитывается. */
    private Component detail = Component.empty();
    private long dwellStart;
    private int lastCargo = -1;
    private double lastFuel = -1;
    private UUID servicing;
    private final ArrayDeque<UUID> seen = new ArrayDeque<>();
    private int departures;
    private int arrivals;
    private long lastLaunchTick;

    public CargoTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_TERMINAL, pos, state);
    }

    // ---------- Программа и режим ----------

    /** Загрузка программы из предмета: цель обязательна, маяк — для целей со спуском. */
    public Component installProgram(ItemStack program) {
        Identifier destination = program.get(ModDataComponents.PROGRAM_DESTINATION);
        if (destination == null) {
            return Component.translatable("message.spacereloaded.terminal.program_needs_destination");
        }
        this.destinationDimension = destination;
        this.pad = program.get(ModDataComponents.PROGRAM_PAD);
        this.frequency = program.getOrDefault(ModDataComponents.PROGRAM_FREQUENCY, 0);
        this.state = mode == Mode.HOLD ? State.HOLD : State.WAITING_CRAFT;
        setChanged();
        return Component.translatable("message.spacereloaded.terminal.program_installed",
                FlightProgramItem.describe(program));
    }

    public Mode toggleMode() {
        mode = mode.next();
        if (mode == Mode.HOLD) {
            state = destinationDimension == null ? State.NO_PROGRAM : State.HOLD;
        }
        setChanged();
        return mode;
    }

    public Mode mode() {
        return mode;
    }

    public State state() {
        return state;
    }

    public Component detail() {
        return detail;
    }

    public int departures() {
        return departures;
    }

    public int arrivals() {
        return arrivals;
    }

    public Identifier destinationDimension() {
        return destinationDimension;
    }

    public GlobalPos pad() {
        return pad;
    }

    /** Ключ локализации состояния: message.spacereloaded.terminal.state.<state>. */
    public String stateKey() {
        return "message.spacereloaded.terminal.state." + state.name().toLowerCase(Locale.ROOT);
    }

    /** Строки статуса для чата/ЦУПа. */
    public List<Component> statusLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("message.spacereloaded.terminal.status.mode",
                Component.translatable("message.spacereloaded.terminal.mode." + mode.name().toLowerCase(Locale.ROOT))));
        lines.add(Component.translatable("message.spacereloaded.terminal.status.program",
                destinationDimension == null ? "—"
                        : Component.translatable("planet.spacereloaded." + FlightProgramItem.planetKey(destinationDimension)),
                pad == null ? "—" : pad.pos().toShortString() + " @ " + pad.dimension().identifier().getPath(),
                frequency));
        lines.add(Component.translatable("message.spacereloaded.terminal.status.state",
                Component.translatable(stateKey()), detail));
        lines.add(Component.translatable("message.spacereloaded.terminal.status.counters", departures, arrivals));
        return lines;
    }

    // ---------- Тик ----------

    public static void serverTick(CargoTerminalBlockEntity terminal, ServerLevel level) {
        var config = SpaceReloaded.config();
        if (level.getGameTime() % config.cargoLineCheckIntervalTicks != 0) {
            return;
        }
        terminal.check(level);
    }

    private void check(ServerLevel level) {
        var config = SpaceReloaded.config();
        long now = level.getGameTime();
        if (destinationDimension == null) {
            set(State.NO_PROGRAM, Component.empty());
            return;
        }
        RocketEntity craft = nearestCraft(level);
        if (craft == null) {
            servicing = null;
            set(State.WAITING_CRAFT, Component.empty());
            return;
        }
        if (!seen.contains(craft.getUUID())) {
            seen.addLast(craft.getUUID());
            while (seen.size() > SEEN_LIMIT) {
                seen.pollFirst();
            }
            arrivals++;
            setChanged();
        }
        if (!craft.getPassengers().isEmpty()) {
            servicing = null;
            set(State.CREW_ABOARD, Component.empty());
            return;
        }
        if (mode == Mode.HOLD) {
            set(State.HOLD, Component.empty());
            return;
        }
        // Выдержка: груз и топливо не менялись — обслуживание закончено (FR-112)
        int cargo = craft.cargoCount();
        double fuel = craft.propellantKg();
        if (!craft.getUUID().equals(servicing) || cargo != lastCargo || Math.abs(fuel - lastFuel) > 0.5) {
            servicing = craft.getUUID();
            lastCargo = cargo;
            lastFuel = fuel;
            dwellStart = now;
            setChanged();
        }
        long elapsed = now - dwellStart;
        if (elapsed < config.cargoLineDwellTicks) {
            set(State.SERVICING, Component.literal(elapsed / 20 + "/" + config.cargoLineDwellTicks / 20 + " s"));
            return;
        }
        // Готов: программа в борт, честный старт (FR-113/FR-114)
        if (!craft.installRoute(level, destinationDimension, pad, frequency)) {
            set(State.REFUSED, Component.translatable("message.spacereloaded.program.unreachable",
                    destinationDimension.toString()));
            return;
        }
        RocketEntity.LaunchResult result = craft.tryLaunchUnmanned(level);
        if (result.launched()) {
            departures++;
            lastLaunchTick = now;
            servicing = null;
            set(State.LAUNCHED, result.message());
            SpaceReloaded.LOGGER.info("Терминал {}: отправлен борт {} ({} предм., {} кг), рейс №{}",
                    getBlockPos().toShortString(), craft.getUUID(), cargo, Math.round(fuel), departures);
            setChanged();
            return;
        }
        switch (result.kind()) {
            case WINDOW -> set(State.WAITING_WINDOW, result.message());
            case NO_COVERAGE -> set(State.NO_COVERAGE, result.message());
            default -> set(State.REFUSED, result.message());
        }
    }

    private void set(State newState, Component newDetail) {
        if (newState != state) {
            setChanged();
        }
        state = newState;
        detail = newDetail;
    }

    /** Ближайший припаркованный не-обломок в зоне терминала (пассажиры проверяются отдельно). */
    private RocketEntity nearestCraft(ServerLevel level) {
        BlockPos pos = getBlockPos();
        AABB area = new AABB(
                pos.getX() - HORIZONTAL_RANGE, pos.getY(), pos.getZ() - HORIZONTAL_RANGE,
                pos.getX() + HORIZONTAL_RANGE + 1, pos.getY() + VERTICAL_RANGE,
                pos.getZ() + HORIZONTAL_RANGE + 1);
        List<RocketEntity> rockets = level.getEntities(EntityTypeTest.forClass(RocketEntity.class), area,
                rocket -> rocket.isParked() && !rocket.isDebris());
        RocketEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (RocketEntity rocket : rockets) {
            double distance = rocket.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (distance < bestDistance) {
                best = rocket;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---------- NBT ----------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (destinationDimension != null) {
            output.putString("destination", destinationDimension.toString());
        }
        if (pad != null) {
            output.putString("pad_dim", pad.dimension().identifier().toString());
            output.putLong("pad_pos", pad.pos().asLong());
        }
        output.putInt("frequency", frequency);
        output.putInt("mode", mode.ordinal());
        output.putInt("state", state.ordinal());
        output.putLong("dwell_start", dwellStart);
        output.putInt("last_cargo", lastCargo);
        output.putDouble("last_fuel", lastFuel);
        if (servicing != null) {
            output.putString("servicing", servicing.toString());
        }
        output.putInt("departures", departures);
        output.putInt("arrivals", arrivals);
        output.putLong("last_launch", lastLaunchTick);
        output.putString("seen", String.join(",", seen.stream().map(UUID::toString).toList()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String destination = input.getStringOr("destination", "");
        destinationDimension = destination.isEmpty() ? null : Identifier.parse(destination);
        String padDim = input.getStringOr("pad_dim", "");
        pad = padDim.isEmpty() ? null : GlobalPos.of(
                ResourceKey.create(Registries.DIMENSION, Identifier.parse(padDim)),
                BlockPos.of(input.getLongOr("pad_pos", 0L)));
        frequency = input.getIntOr("frequency", 0);
        mode = Mode.values()[Math.floorMod(input.getIntOr("mode", 0), Mode.values().length)];
        state = State.values()[Math.floorMod(input.getIntOr("state", 0), State.values().length)];
        dwellStart = input.getLongOr("dwell_start", 0L);
        lastCargo = input.getIntOr("last_cargo", -1);
        lastFuel = input.getDoubleOr("last_fuel", -1);
        String servicingId = input.getStringOr("servicing", "");
        servicing = servicingId.isEmpty() ? null : UUID.fromString(servicingId);
        departures = input.getIntOr("departures", 0);
        arrivals = input.getIntOr("arrivals", 0);
        lastLaunchTick = input.getLongOr("last_launch", 0L);
        seen.clear();
        for (String id : input.getStringOr("seen", "").split(",")) {
            if (!id.isBlank()) {
                seen.addLast(UUID.fromString(id));
            }
        }
    }
}
