package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.CosmeticState;

import java.util.Locale;

/** Газовый бак (007): род газа и уровень заполнения видны снаружи; ПКМ — масса. */
public class GasTankBlock extends Block implements EntityBlock, CosmeticState {

    public static final EnumProperty<GasKind> GAS = EnumProperty.create("gas", GasKind.class);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);

    public GasTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(GAS, GasKind.NONE).setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GAS, FILL);
    }

    @Override
    public boolean onlyCosmetic(BlockState before, BlockState after) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GasTankBlockEntity tank
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.spacereloaded.gas_tank",
                    Component.translatable("gas.spacereloaded." + tank.kind().getSerializedName()),
                    String.format(Locale.ROOT, "%.1f", tank.mass()),
                    String.format(Locale.ROOT, "%.0f", tank.kind().capacityKg())));
        }
        return InteractionResult.SUCCESS;
    }
}
