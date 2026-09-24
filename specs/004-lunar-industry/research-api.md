# 004 — API cheat-sheet (MC 26.2, Mojang names, verified by javap 2026-09-24)

Источники: `~/.gradle/caches/fabric-loom/26.2/minecraft-{client-only,common}.jar`,
`fabric-rendering-v1-25.3.1+6988455e9e.jar` (fabric_api 0.154.2+26.2). Всё ниже —
сигнатуры из javap, не по памяти. `ResourceLocation` в 26.2 = `net.minecraft.resources.Identifier`.

## 1. BlockEntityRenderer

```java
// net.minecraft.client.renderer.blockentity
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> {
  S createRenderState();
  default void extractRenderState(T be, S state, float partialTick, Vec3 cameraPos,
                                  ModelFeatureRenderer.CrumblingOverlay breakProgress); // default = extractBase
  void submit(S state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera);
  default boolean shouldRenderOffScreen();          // false
  default int getViewDistance();                    // 64
  default boolean shouldRender(T be, Vec3 cameraPos); // Vec3.atCenterOf(pos).closerThan(cam, getViewDistance())
}
public interface BlockEntityRendererProvider<T, S> { BlockEntityRenderer<T,S> create(Context ctx); }
```
`BlockEntityRenderState` (…blockentity.state): public `BlockPos blockPos`, `BlockEntityType<?> blockEntityType`,
`int lightCoords`, `CrumblingOverlay breakProgress`; `blockState` — **private** (нет геттера → копируйте в свой state).
`static extractBase(BlockEntity, BlockEntityRenderState, CrumblingOverlay)` заполняет pos/type/state и
`lightCoords = LightCoordsUtil.getLightCoords(level, pos)` → при override **первой строкой** вызывайте
`BlockEntityRenderer.super.extractRenderState(be, state, pt, cam, crumbling)` (или `extractBase`).

`BlockEntityRendererProvider.Context` — **record**: `blockEntityRenderDispatcher()`, `blockModelResolver()`,
`itemModelResolver()`, `entityRenderer()`, `entityModelSet()`, `font()`, `sprites()`,
`playerSkinRenderCache()`, `bakeLayer(ModelLayerLocation)`. (Не `getBlockModelResolver()` — это у Entity-контекста.)

Регистрация (клиент, рядом с `EntityRendererRegistry` в `SpaceReloadedClient`):
```java
// net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
static <E extends BlockEntity, S extends BlockEntityRenderState> void register(
        BlockEntityType<E> type, BlockEntityRendererProvider<? super E, ? super S> provider);
BlockEntityRendererRegistry.register(ModBlockEntities.MASS_DRIVER, MassDriverRenderer::new);
```
(ванильный `BlockEntityRenderers.register` — private.)

Скелет (паттерн RocketRenderer: resolver → BlockModelRenderState → submit):
```java
public class MassDriverRenderer implements BlockEntityRenderer<MassDriverBlockEntity, MassDriverRenderState> {
  private final BlockModelResolver blocks;
  public MassDriverRenderer(BlockEntityRendererProvider.Context ctx) { blocks = ctx.blockModelResolver(); }
  @Override public MassDriverRenderState createRenderState() { return new MassDriverRenderState(); }
  @Override public void extractRenderState(MassDriverBlockEntity be, MassDriverRenderState s, float pt,
                                           Vec3 cam, ModelFeatureRenderer.CrumblingOverlay crumbling) {
    BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
    s.progress = be.clientProgress(pt);                        // анимация: лерп на partialTick
    blocks.update(s.sled, ModBlocks.SLED.defaultBlockState(), s.ctx); // s.ctx = BlockDisplayContext.create()
  }
  @Override public void submit(MassDriverRenderState s, PoseStack pose, SubmitNodeCollector c, CameraRenderState cam) {
    pose.pushPose();
    pose.translate(0.0f, 1.0f + s.progress * 18.0f, 0.0f);    // смещение от угла блока BE
    pose.mulPose(Axis.YP.rotationDegrees(s.yaw));
    s.sled.submit(pose, c, s.lightCoords, OverlayTexture.NO_OVERLAY, 0); // (pose, collector, light, overlay, outlineColor)
    pose.popPose();
  }
  @Override public boolean shouldRenderOffScreen() { return true; }  // см. ниже
  @Override public int getViewDistance() { return 256; }
}
```
- `BlockModelResolver.update(BlockModelRenderState, BlockState, BlockDisplayContext)`;
  `BlockModelRenderState.submit(PoseStack, SubmitNodeCollector, int light, int overlay, int outlineColor)`;
  есть `submitWithZOffset(...)` (та же сигнатура) и `isEmpty()`. `BlockDisplayContext.create()` — static.
- Предмет: `ItemModelResolver.updateForTopItem(ItemStackRenderState, ItemStack, ItemDisplayContext, Level, ItemOwner, int seed)`
  (или `updateForNonLiving(state, stack, ctx, Entity)`); затем `ItemStackRenderState.submit(PoseStack, SubmitNodeCollector, int light, int overlay, int outlineColor)`.
- **Нет AABB-API для BE-рендера** (нет getRenderBoundingBox). Культинг = видимость секции чанка, где стоит BE.
  `shouldRenderOffScreen()==true` → `ClientLevel.onBlockEntityAdded` кладёт BE в глобальный список, рисуется
  всегда (как маяк/TestInstance); дистанцию режет `shouldRender`/`getViewDistance()`. Для колонны 3x3x20
  → `shouldRenderOffScreen() = true` + `getViewDistance()` ~128–256 (маяк: `renderDistance*16`).
- Свет по высоте: `lightCoords` взят в позиции BE; для верха колонны можно
  `LightCoordsUtil.getLightCoords(be.getLevel(), pos.above(19))` в extract.

## 2. Эмиссивная геометрия / full bright

`net.minecraft.util.LightCoordsUtil.FULL_BRIGHT = 15728880` (0xF000F0), `FULL_SKY = 15728640`,
`pack(int block, int sky)`. **`LightTexture` в 26.2 нет.**
- Блок-модель «светится»: `s.model.submit(pose, c, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)`.
- Своя геометрия:
```java
// OrderedSubmitNodeCollector
void submitCustomGeometry(PoseStack, RenderType, SubmitNodeCollector.CustomGeometryRenderer);
// CustomGeometryRenderer: void render(PoseStack.Pose pose, VertexConsumer vc);
c.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {   // position+color, без текстуры/света
  vc.addVertex(p, 0, 0, 0).setColor(0x8040C0FF);  // ARGB int
  vc.addVertex(p, 1, 0, 0).setColor(0x8040C0FF);
  vc.addVertex(p, 1, 1, 0).setColor(0x8040C0FF);
  vc.addVertex(p, 0, 1, 0).setColor(0x8040C0FF);
});
```
Текстурный вариант — как `BeaconRenderer.addVertex`: `vc.addVertex(pose,x,y,z).setColor(argb).setUv(u,v)
.setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,1,0)` с
`RenderTypes.beaconBeam(Identifier tex, boolean translucent)` или `RenderTypes.entityTranslucentEmissive(Identifier)`
/ `RenderTypes.eyes(Identifier)`. Прочие: `energySwirl(Identifier,float,float)`, `debugQuads()`, `debugFilledBox()`.
Готовый луч: `BeaconRenderer.submitBeaconBeam(PoseStack, SubmitNodeCollector, Identifier tex, float partialTickTime,
float scale, int beamStart, int height, int color, float solidRadius, float glowRadius)` (public static).
`RenderTypes` в пакете `net.minecraft.client.renderer.rendertype` (+ `RenderType` там же).

## 3. Звуки

```java
public static SoundEvent createVariableRangeEvent(Identifier);        // net.minecraft.sounds.SoundEvent
public static SoundEvent createFixedRangeEvent(Identifier, float range);
static <V,T extends V> T register(Registry<V>, Identifier, T);        // net.minecraft.core.Registry
static <R,T extends R> Holder.Reference<T> registerForHolder(Registry<R>, Identifier, T);
```
```java
public static final Identifier MASS_DRIVER_FIRE_ID = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "mass_driver.fire");
public static final SoundEvent MASS_DRIVER_FIRE = Registry.register(BuiltInRegistries.SOUND_EVENT,
        MASS_DRIVER_FIRE_ID, SoundEvent.createVariableRangeEvent(MASS_DRIVER_FIRE_ID));
```
`assets/spacereloaded/sounds.json` (файла ещё нет; ogg → `assets/spacereloaded/sounds/mass_driver/fire.ogg`):
```json
{ "mass_driver.fire": { "subtitle": "subtitles.spacereloaded.mass_driver.fire",
    "sounds": [ { "name": "spacereloaded:mass_driver/fire", "volume": 1.0, "stream": false } ] } }
```
Проигрывание с сервера (`Level`, как `HermeticHatchBlock`): `level.playSound(null, pos, SOUND, SoundSource.BLOCKS, vol, pitch)`.
Перегрузки: `(Entity, BlockPos, SoundEvent, SoundSource, float, float)`, `(Entity, double,double,double, SoundEvent, SoundSource[, float, float])`,
`(Entity, double,double,double, Holder<SoundEvent>, SoundSource, float, float)`, `(Entity, Entity, SoundEvent, SoundSource, float, float)`.
Holder нужен только в xyz-варианте/кодеках; `SoundEvents.*` в 26.2 — смешаны (часть Holder.Reference), свой — plain SoundEvent.
Клиент: `playLocalSound(BlockPos, SoundEvent, SoundSource, float, float, boolean distanceDelay)`.

## 4. BE: тик, сохранение, синхронизация с клиентом

Тикер в моде — лямбда в `getTicker` блока (см. `ChemMachineBlock`): сервер-только, `return null` на клиенте.
Для клиентской анимации — отдельный клиентский тикер или лерп по `level.getGameTime()+partialTick` в extract.
Хелпер: `BaseEntityBlock.createTickerHelper(BlockEntityType<A>, BlockEntityType<E>, BlockEntityTicker<? super E>)` (protected static).

Save/load (как во всём моде):
```java
@Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putInt("phase", phase); }
@Override protected void loadAdditional(ValueInput in)   { super.loadAdditional(in); phase = in.getIntOr("phase", 0); }
```
ValueOutput: `putBoolean/Byte/Short/Int/Long/Float/Double/String/IntArray, store, storeNullable, child, list, discard`.
ValueInput: `getBooleanOr/ByteOr/ShortOr/IntOr/LongOr/FloatOr/DoubleOr/StringOr, getInt/getLong/getString/getIntArray (Optional), read, child, childOrEmpty, list, listOrEmpty, childrenList(OrEmpty)`.

Синк BE→клиент: **в моде сейчас нигде не используется** (`getUpdatePacket` нет; GUI — через Menu/ContainerData,
прочее — CustomPacketPayload в `network/`). Ванильный путь:
```java
@Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
// после изменения анимационного состояния на сервере:
setChanged();
level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); // 2
```
`ClientboundBlockEntityDataPacket.create(BlockEntity)` / `create(BlockEntity, BiFunction<BlockEntity,RegistryAccess,CompoundTag>)`.
Клиент применяет тег через `loadAdditional` → пишите туда и клиентские поля. `getUpdateTag` также уходит с чанком.
Подвох: `ServerLevelMixin` ловит `sendBlockUpdated` для инвалидации зон/кабелей, но только при `oldState != newState` —
передавайте одинаковый state, и шлите редко (смена фазы, не каждый тик; клиент доинтерполирует сам).
Флаги `Block`: UPDATE_NEIGHBORS=1, UPDATE_CLIENTS=2, UPDATE_ALL=3, UPDATE_KNOWN_SHAPE=16, UPDATE_NONE=260.

## 5. Worldgen POI

Как сделан кратер: `Feature<NoneFeatureConfiguration>` (`worldgen/CraterFeature.java`, `place(FeaturePlaceContext)`),
регистрация в `registry/ModWorldgen.init()`: `Registry.register(BuiltInRegistries.FEATURE, id("crater"), CRATER)`;
JSON: `worldgen/configured_feature/moon_crater.json` = `{"type":"spacereloaded:crater","config":{}}`,
`placed_feature/moon_crater.json` = `rarity_filter{chance:3}` + `in_square` + `heightmap OCEAN_FLOOR_WG`;
подключён прямо в `worldgen/biome/moon_plains.json` → `"features"` (не через BiomeModifications — это только Оверворлд-руды).

Шаблон `.nbt` из Feature (путь ровно как у `FossilFeature`):
```java
WorldGenLevel level = ctx.level();
StructureTemplateManager mgr = level.getLevel().getServer().getStructureManager();
StructureTemplate tpl = mgr.getOrCreate(Identifier.fromNamespaceAndPath(MOD_ID, "moon/derelict_lander"));
// или Optional<StructureTemplate> mgr.get(Identifier)
StructurePlaceSettings s = new StructurePlaceSettings()
    .setRotation(Rotation.getRandom(ctx.random())).setRandom(ctx.random())
    .setBoundingBox(chunkBox)          // обрезка по текущему чанку(ам), как у Fossil
    .setIgnoreEntities(true);          // + addProcessor(...), setKnownShape, setLiquidSettings
boolean ok = tpl.placeInWorld(level, pos, pos, s, ctx.random(), Block.UPDATE_NONE /*260, как Fossil*/);
// placeInWorld(ServerLevelAccessor, BlockPos pos, BlockPos pivot, StructurePlaceSettings, RandomSource, int flags)
```
Файл: `data/spacereloaded/structure/moon/derelict_lander.nbt` (`STRUCTURE_DIRECTORY_NAME = "structure"`).
Размер: `tpl.getSize()` / `getSize(Rotation)`; `getBoundingBox(StructurePlaceSettings, BlockPos)`.

**Рекомендация:** процедурный Feature (как CraterFeature) — ноль бинарных ассетов, диффится, проверяется
gametest-ом, данные (регoлит/блоки мода) правятся кодом. Feature пишет только в ±1 чанк (3x3 чанков вокруг
origin в decoration-стадии) — POI держать ≤ ~16x16 в плане и ставить в центр чанка (`in_square` уже даёт
случайное смещение; при крупной постройке — `origin` выровнять к `chunkPos.getMiddleBlockX/Z`). `.nbt` оправдан
только для сложной «рукотворной» постройки; настоящий `Structure`/`structure_set` — избыточен для POI.

## 6. Летящая капсула: клиентская интерполяция

Как `KineticProjectileEntity`: сервер считает физику, `setPos(to)` + `setDeltaMovement(to.subtract(from))`,
тип с `.clientTrackingRange(24).updateInterval(1)` (`ModEntities`) — позиция приходит каждый тик,
а `EntityRenderState.x/y/z` рендерер лерпит между `xo` и `x` по partialTick сам. Своего InterpolationHandler у снаряда нет.
Если `updateInterval > 1` или есть пассажир — как `RocketEntity`:
```java
private final InterpolationHandler interpolation = new InterpolationHandler(this); // (Entity[, int steps][, Consumer])
@Override public InterpolationHandler getInterpolation() { return interpolation; } // ванильный Entity → null
@Override public void tick() { super.tick(); if (level().isClientSide()) { interpolation.interpolate(); return; } ... }
```
Прочее: `InterpolationHandler.setInterpolationLength(int)`, `interpolateTo(Vec3,float,float)`, `cancel()`;
`Entity.lerpMotion(Vec3)`, `moveOrInterpolateTo(Vec3[,float,float])`. Рендер: `EntityRenderer.getBoundingBoxForCulling(T)`
(protected, AABB), `affectedByCulling(T)`, public поля state: `x,y,z,ageInTicks,lightCoords,outlineColor`.
