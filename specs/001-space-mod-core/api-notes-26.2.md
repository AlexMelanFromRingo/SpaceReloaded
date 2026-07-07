# Шпаргалка API Minecraft 26.2 + Fabric (верифицировано javap по deobf-jar, 2026-07-07)

Все сигнатуры проверены по `minecraft-common-deobf-26.2.jar` и модулям Fabric API 0.154.2+26.2 (суффикс хэша `9e`). **Не писать API по памяти** — официальные имена расходятся и со старыми mojmap, и с yarn.

## Критические переименования

| Было (память/yarn/mojmap) | Стало в 26.2 |
|---|---|
| `ResourceLocation` | **`net.minecraft.resources.Identifier`** (`fromNamespaceAndPath`, `parse`; метода `of` НЕТ) |
| `ResourceKey.location()` | `ResourceKey.identifier()` (у TagKey остался `location()`) |
| `net.minecraft.Util` | `net.minecraft.util.Util`; `backgroundExecutor()` → `TracingExecutor implements Executor` |
| `addRegionTicket/removeRegionTicket` | `ServerChunkCache.addTicketWithRadius / addTicketAndLoadWithRadius / removeTicketWithRadius` |
| `TicketType.create(...)` c компаратором | `new TicketType(long timeout, int flags)` + `Registry.register(BuiltInRegistries.TICKET_TYPE, id, type)`; флаги: PERSIST/LOADING/SIMULATION/KEEP_DIMENSION_ACTIVE/CAN_EXPIRE_IF_UNLOADED |
| `Entity.hurt()` → boolean | `hurt()` — final void; используем **`hurtServer(ServerLevel, DamageSource, float)`** |
| `saveAdditional(CompoundTag)` | **`saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`** (`putX`/`getXOr`, `store/read` с Codec) |
| `BlockEntityType.Builder` | `new BlockEntityType<>(BE::new, Set.of(BLOCK))` или Fabric `FabricBlockEntityTypeBuilder` |
| `FabricBlockSettings/FabricItemSettings` | Удалены — только ванильные `BlockBehaviour.Properties` / `Item.Properties` |
| `PayloadTypeRegistry.playS2C()/playC2S()` | `clientboundPlay()/serverboundPlay()` |
| `ServerWorldEvents`, `END_WORLD_TICK` | `ServerLevelEvents` (LOAD/UNLOAD), `END_LEVEL_TICK` |
| `HudRenderCallback` | `HudElementRegistry.addLast(id, HudElement)`; `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` |
| `ItemGroupEvents` (item-group-api) | `CreativeModeTabEvents` (fabric-creative-tab-api-v1), `FabricCreativeModeTab.builder()` |
| `source.hasPermission(2)` | `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` — Predicate для `.requires(...)`; PermissionSet/PermissionLevel вместо int |
| `state.is(...)` объявлен на BlockStateBase | Теперь default-методы интерфейса `net.minecraft.core.TypedInstance<T>` (важно для миксинов) |

## Регистрация блока/предмета (контракт setId, 26.2)

```java
Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
ResourceKey<Block> bk = ResourceKey.create(Registries.BLOCK, id);
Block b = Registry.register(BuiltInRegistries.BLOCK, bk, new Block(
        BlockBehaviour.Properties.of().setId(bk).strength(3f).sound(SoundType.METAL)));
ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, id);
Registry.register(BuiltInRegistries.ITEM, ik,
        new BlockItem(b, new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
```
ID ванили теперь в `net.minecraft.references.{BlockIds,ItemIds,BlockItemIds}` («отделяй id от блоков»).

## Снапшоты чанков для фоновых расчётов

- `LevelChunkSection.getStates()` → `PalettedContainer<BlockState>`, у него есть **`copy()`** (дёшево, O(палитра)) и `get(x,y,z)` (локальные 0–15).
- `chunk.getSectionIndexFromSectionY(sy)`, `level.getMinY()/getMaxY()/getMinSectionY()` — из `LevelHeightAccessor`.
- `chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)` — высота верхнего блока (колонка heightmap).
- `ServerChunkCache.getChunkNow(cx, cz)` — без синхронной загрузки, null если не прогружен.
- `MinecraftServer implements Executor` → `server.execute(...)`, `server.isSameThread()`.

## Прочее проверенное

- `DamageSources.source(key)` приватен: `new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(KEY))`; JSON: `data/<ns>/damage_type/<name>.json` `{message_id, scaling: never|always|when_caused_by_living_non_player, exhaustion, effects}`.
- Экипировка предмета: `Item.Properties().component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build())`.
- `Level.explode(...)` — все перегрузки void; `Level.ExplosionInteraction.{NONE,BLOCK,MOB,TNT,TRIGGER}`.
- Пуш сущности: `entity.push(Vec3)` + **`entity.hurtMarked = true`** для синка скорости на клиент.
- Сообщения: `ServerPlayer.sendSystemMessage(Component)`, оверлей — `sendOverlayMessage(Component)` (не displayClientMessage).
- Fabric-события места блока НЕТ (`*Place*` отсутствует): ловить `UseBlockCallback`/`BlockEvents.USE_ITEM_ON` + отложенная проверка в конце тика.
- События взрыва в Fabric API 26.2 нет — только mixin.
- Форматы данных: item-модели `assets/<ns>/items/*.json` (`{"model":{"type":"minecraft:model","model":...}}`), лут `data/<ns>/loot_table/blocks/`, теги `data/<ns>/tags/block/`.
- Рендер: `ChunkSectionLayer` вместо RenderType для террейна; Vulkan experimental; только Blaze3D-абстракции.
- Gametest: `@net.fabricmc.fabric.api.gametest.v1.GameTest` — модуль жив для 26.2.
- Team Reborn Energy **5.0.0**: `minecraft >=26.1-`, Java 25, зависит от `fabric-transfer-api-v1` (maven.fabricmc.net, координаты `teamreborn:energy:5.0.0`).
