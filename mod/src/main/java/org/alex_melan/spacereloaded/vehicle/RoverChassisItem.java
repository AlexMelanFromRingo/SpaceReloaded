package org.alex_melan.spacereloaded.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.alex_melan.spacereloaded.registry.ModEntities;

/** Шасси ровера: ПКМ по земле — рама встаёт на место, дальше ставят колёса и батарею. */
public class RoverChassisItem extends Item {

    public RoverChassisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        RoverEntity rover = ModEntities.ROVER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (rover == null) {
            return InteractionResult.FAIL;
        }
        rover.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                context.getPlayer() == null ? 0 : context.getPlayer().getYRot(), 0);
        level.addFreshEntity(rover);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.SUCCESS_SERVER;
    }
}
