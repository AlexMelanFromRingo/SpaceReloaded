package org.alex_melan.spacereloaded.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Тарелка-перехватчик (Phase 12 CTF): регистрируется в сети как приёмник,
 * уводящий грузы с прослушиваемого канала на СВОЮ позицию. Слушает открытый
 * канал (частота 0) по умолчанию; настройка ключом связи на конкретный канал —
 * задел на «взлом» защищённых частот (пока перехват только открытых).
 */
public class InterceptorDishBlockEntity extends BlockEntity {

    private int listenFrequency;

    public InterceptorDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTERCEPTOR_DISH, pos, state);
    }

    public int listenFrequency() {
        return listenFrequency;
    }

    public void setListenFrequency(int frequency) {
        this.listenFrequency = frequency;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            register(serverLevel);
        }
    }

    private GlobalPos self() {
        return GlobalPos.of(level.dimension(), getBlockPos());
    }

    private void register(ServerLevel level) {
        SpaceNetworkState.get(level.getServer()).registerInterceptor(self(), listenFrequency);
    }

    public static void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 40 == 0
                && level.getBlockEntity(pos) instanceof InterceptorDishBlockEntity dish) {
            dish.register(level); // поддерживаем регистрацию (переживает перезагрузку чанка)
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel serverLevel) {
            SpaceNetworkState.get(serverLevel.getServer()).unregisterInterceptor(self());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("listen", listenFrequency);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        listenFrequency = input.getIntOr("listen", 0);
    }
}
