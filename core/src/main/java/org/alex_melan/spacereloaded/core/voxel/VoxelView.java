package org.alex_melan.spacereloaded.core.voxel;

/**
 * Иммутабельный снимок региона мира для фоновых расчётов.
 *
 * <p>Контракт потокобезопасности (конституция, принцип IV): реализация обязана
 * быть неизменяемой после создания; снимок снимается в главном потоке сервера,
 * читается из фоновых. Реализации не имеют права держать ссылки на живой мир.
 */
public interface VoxelView {
    /**
     * @param packedPos позиция в формате {@code PackedPos}/{@code BlockPos.asLong()}
     */
    GasPermeability permeabilityAt(long packedPos);
}
