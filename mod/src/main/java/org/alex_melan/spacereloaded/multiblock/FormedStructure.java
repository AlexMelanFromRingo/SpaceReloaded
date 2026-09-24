package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.multiblock.MultiblockTemplate;
import org.alex_melan.spacereloaded.industry.IndustryStructures;

import java.util.Optional;

/**
 * Сформированность мультиблока в ключевом блоке (005, FR-320/FR-324, D55): формирует молот,
 * снимает событие изменения любой клетки (претензии клеток 004). Пока структура цела, добавленные
 * или убранные повторы сегмента пересчитываются; разрушение шаблона — расформирование.
 */
public final class FormedStructure {

    private int repeats;
    private boolean formed;
    private boolean dirty;

    public boolean formed() {
        return formed;
    }

    public int repeats() {
        return formed ? repeats : 0;
    }

    public void markDirty() {
        dirty = true;
    }

    /** Удар молотом: проверка и формирование; сообщение и маркер ошибки. */
    public boolean hammer(ServerLevel level, BlockPos key, Direction face, ServerPlayer player) {
        Optional<MultiblockTemplates.TemplateEntry> template =
                MultiblockTemplates.forKey(level.registryAccess(), level.getBlockState(key).getBlock());
        if (template.isEmpty()) {
            return false;
        }
        MultiblockTemplate.Result result = MultiblockTemplates.match(level, key, face, template.get());
        if (!result.formed()) {
            formed = false;
            BlockPos bad = MultiblockTemplates.worldPos(key, face, result.bad()[0], result.bad()[1], result.bad()[2]);
            player.sendSystemMessage(Component.translatable("message.spacereloaded.hammer.bad_cell",
                    bad.toShortString(), describe(result.expected())));
            level.sendParticles(player, ParticleTypes.ANGRY_VILLAGER, true, false,
                    bad.getX() + 0.5, bad.getY() + 0.5, bad.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
            return false;
        }
        formed = true;
        repeats = result.repeats();
        var cells = MultiblockTemplates.cells(key, face, template.get(), repeats);
        IndustryStructures.claim(level, key, cells);
        FormableBlock.apply(level, cells, true);
        level.playSound(null, key, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.CRIT, key.getX() + 0.5, key.getY() + 1.0, key.getZ() + 0.5, 20,
                0.4, 0.4, 0.4, 0.1);
        player.sendSystemMessage(Component.translatable("message.spacereloaded.hammer.formed",
                Component.translatable(level.getBlockState(key).getBlock().getDescriptionId()), repeats));
        return true;
    }

    /** Имя блока/тега для сообщения. */
    public static Component describe(String matcher) {
        if (matcher == null) {
            return Component.literal("?");
        }
        if (matcher.startsWith("#")) {
            return Component.literal(matcher);
        }
        var id = net.minecraft.resources.Identifier.parse(matcher);
        return Component.translatable(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id)
                .getDescriptionId());
    }

    /** Переоценка по событию (тик ключевого блока). @return изменилось ли состояние */
    public boolean revalidate(ServerLevel level, BlockPos key, Direction face) {
        if (!dirty) {
            return false;
        }
        dirty = false;
        if (!formed) {
            return false;
        }
        Optional<MultiblockTemplates.TemplateEntry> template =
                MultiblockTemplates.forKey(level.registryAccess(), level.getBlockState(key).getBlock());
        if (template.isEmpty()) {
            formed = false;
            return true;
        }
        MultiblockTemplate.Result result = MultiblockTemplates.match(level, key, face, template.get());
        if (!result.formed()) {
            formed = false;
            FormableBlock.apply(level, MultiblockTemplates.cells(key, face, template.get(), template.get().repeat()
                    .map(MultiblockTemplates.RepeatEntry::max).orElse(0)), false);
            IndustryStructures.release(level, key);
            return true;
        }
        boolean changed = result.repeats() != repeats;
        if (result.repeats() < repeats) {
            // Отрезанный хвост сегмента — снова обычные блоки
            FormableBlock.apply(level, MultiblockTemplates.cells(key, face, template.get(), repeats), false);
        }
        repeats = result.repeats();
        var cells = MultiblockTemplates.cells(key, face, template.get(), repeats);
        IndustryStructures.claim(level, key, cells);
        FormableBlock.apply(level, cells, true);
        return changed;
    }

    /** Ключевой блок сломан: вернуть членам обычный облик и снять претензии. */
    public void dismantle(ServerLevel level, BlockPos key, Direction face, net.minecraft.world.level.block.Block keyBlock) {
        if (!formed) {
            return;
        }
        formed = false;
        MultiblockTemplates.forKey(level.registryAccess(), keyBlock).ifPresent(t ->
                FormableBlock.apply(level, MultiblockTemplates.cells(key, face, t, repeats), false));
        IndustryStructures.release(level, key);
    }

    /** Первичная регистрация претензий после загрузки (сформированная структура). */
    public void reclaim(ServerLevel level, BlockPos key, Direction face) {
        if (formed) {
            dirty = true;
            MultiblockTemplates.forKey(level.registryAccess(), level.getBlockState(key).getBlock())
                    .ifPresent(t -> IndustryStructures.claim(level, key,
                            MultiblockTemplates.cells(key, face, t, repeats)));
        }
    }

    public void save(ValueOutput output) {
        output.putBoolean("mb_formed", formed);
        output.putInt("mb_repeats", repeats);
    }

    public void load(ValueInput input) {
        formed = input.getBooleanOr("mb_formed", false);
        repeats = input.getIntOr("mb_repeats", 0);
    }
}
