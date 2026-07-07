package org.alex_melan.spacereloaded.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.rocketry.PartProperties;
import org.alex_melan.spacereloaded.core.rocketry.PartRole;
import org.alex_melan.spacereloaded.core.rocketry.PlacedPart;
import org.alex_melan.spacereloaded.core.rocketry.RocketStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Сериализуемая структура ракеты (T052): блоки в локальных координатах +
 * физические свойства деталей. Один Codec обслуживает и персистентность
 * (ValueOutput.store), и синхронизацию на клиент (synched CompoundTag).
 */
public record RocketData(List<Entry> blocks, double propellantKg) {

    /** Блок структуры: состояние + локальная позиция + свойства детали. */
    public record Entry(BlockState state, long localPos, double massKg, String role,
                        double thrustN, double ispSec, String fuel,
                        double capacityKg, double gyroTorqueNm) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.LONG.fieldOf("pos").forGetter(Entry::localPos),
                Codec.DOUBLE.fieldOf("mass").forGetter(Entry::massKg),
                Codec.STRING.fieldOf("role").forGetter(Entry::role),
                Codec.DOUBLE.optionalFieldOf("thrust", 0.0).forGetter(Entry::thrustN),
                Codec.DOUBLE.optionalFieldOf("isp", 0.0).forGetter(Entry::ispSec),
                Codec.STRING.optionalFieldOf("fuel", "").forGetter(Entry::fuel),
                Codec.DOUBLE.optionalFieldOf("capacity", 0.0).forGetter(Entry::capacityKg),
                Codec.DOUBLE.optionalFieldOf("gyro", 0.0).forGetter(Entry::gyroTorqueNm)
        ).apply(instance, Entry::new));

        public PartProperties toProperties() {
            return new PartProperties(massKg, PartRole.valueOf(role.toUpperCase(Locale.ROOT)),
                    thrustN, ispSec, fuel, capacityKg, gyroTorqueNm);
        }

        public static Entry of(RocketAssembler.ScannedBlock block) {
            PartProperties p = block.properties();
            return new Entry(block.state(), block.localPos(), p.massKg(),
                    p.role().name().toLowerCase(Locale.ROOT), p.thrustN(), p.ispSec(),
                    p.fuelType(), p.propellantCapacityKg(), p.gyroTorqueNm());
        }
    }

    public static final Codec<RocketData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("blocks").forGetter(RocketData::blocks),
            Codec.DOUBLE.fieldOf("propellant").forGetter(RocketData::propellantKg)
    ).apply(instance, RocketData::new));

    public static RocketData fromScan(List<RocketAssembler.ScannedBlock> blocks, double propellantKg) {
        return new RocketData(blocks.stream().map(Entry::of).toList(), propellantKg);
    }

    public RocketStructure toStructure() {
        List<PlacedPart> parts = new ArrayList<>(blocks.size());
        double capacity = 0;
        for (Entry entry : blocks) {
            capacity += entry.capacityKg();
        }
        double fraction = capacity <= 0 ? 0 : Math.clamp(propellantKg / capacity, 0, 1);
        for (Entry entry : blocks) {
            PartProperties props = entry.toProperties();
            parts.add(new PlacedPart(entry.localPos(), props,
                    props.propellantCapacityKg() * fraction));
        }
        return new RocketStructure(parts);
    }

    /** Локальные позиции кресел (role=seat), отсортированы детерминированно. */
    public java.util.List<Long> seatLocals() {
        return blocks.stream()
                .filter(e -> e.role().equals("seat"))
                .map(Entry::localPos)
                .sorted()
                .toList();
    }

    /** Локальная позиция командного модуля (кресло пилота); origin — если нет. */
    public long commandLocalPos() {
        for (Entry entry : blocks) {
            if (entry.role().equals("command")) {
                return entry.localPos();
            }
        }
        return PackedPos.pack(0, 0, 0);
    }
}
