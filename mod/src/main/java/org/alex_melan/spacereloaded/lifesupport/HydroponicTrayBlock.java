package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.alex_melan.spacereloaded.registry.CosmeticState;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

import java.util.Locale;

/**
 * Гидропонный лоток (007): ПКМ семенем — посадка, костной мукой — удобрение, ведром воды — полив,
 * пустой рукой — урожай (если созрел) или отчёт; Sneak+ПКМ — выкопать культуру (семя назад).
 */
public class HydroponicTrayBlock extends Block implements EntityBlock, CosmeticState {

    public enum Crop implements StringRepresentable {
        NONE, WHEAT, POTATO, OTHER;

        static Crop of(Identifier id) {
            if (id == null) {
                return NONE;
            }
            return switch (id.getPath()) {
                case "wheat" -> WHEAT;
                case "potato" -> POTATO;
                default -> OTHER;
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Crop> CROP = EnumProperty.create("crop", Crop.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public HydroponicTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(CROP, Crop.NONE).setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CROP, STAGE);
    }

    @Override
    public boolean onlyCosmetic(BlockState before, BlockState after) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydroponicTrayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.HYDROPONIC_TRAY) {
            return null;
        }
        return (l, pos, s, be) -> HydroponicTrayBlockEntity.serverTick((ServerLevel) l, pos, (HydroponicTrayBlockEntity) be);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof HydroponicTrayBlockEntity tray)) {
            return InteractionResult.PASS;
        }
        if (stack.is(Items.BONE_MEAL)) {
            tray.fertilize(stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(Items.WATER_BUCKET)) {
            tray.water();
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            return InteractionResult.SUCCESS_SERVER;
        }
        return tray.plant(stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HydroponicTrayBlockEntity tray && player instanceof ServerPlayer sp) {
            if (player.isSecondaryUseActive()) {
                give(sp, tray.uproot());
            } else if (tray.mature()) {
                tray.harvest((ServerLevel) level).forEach(item -> give(sp, item));
            } else {
                var p = tray.profile();
                sp.sendOverlayMessage(Component.translatable("message.spacereloaded.tray",
                        p == null ? Component.translatable("message.spacereloaded.tray.empty")
                                : Component.translatable(p.harvestItem().getDescriptionId()),
                        String.format(Locale.ROOT, "%.0f", 100 * tray.rate()),
                        p == null ? "0" : String.format(Locale.ROOT, "%.0f",
                                100 * tray.growth() / (p.cycleDays() / HydroponicTrayBlockEntity.CYCLE_COMPRESSION)),
                        String.format(Locale.ROOT, "%.0f", tray.fertilizer()),
                        String.format(Locale.ROOT, "%.0f", tray.waterKg())));
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
