# 005 — API cheat-sheet (MC 26.2, Mojang names, verified by javap 2026-09-24)

Источники: `~/.gradle/caches/fabric-loom/26.2/minecraft-{client-only,common}.jar`,
`fabric-model-loading-api-v1-8.0.15+c80601bb9e.jar` (входит в fabric_api 0.154.2+26.2, мод тянет весь fabric-api),
joml 1.10.8. База BER/BlockModelRenderState/свет/синк — см. `specs/004-lunar-industry/research-api.md` §1, §2, §4.

## 1. Вращающаяся деталь в BlockEntityRenderer

`com.mojang.math.Axis` (interface): `XP, XN, YP, YN, ZP, ZN`, `static Axis of(Vector3f)`,
`Quaternionf rotation(float radians)`, `default Quaternionf rotationDegrees(float)`.
`PoseStack.mulPose(Quaternionfc)` / `mulPose(Matrix4fc)`, `translate(float,float,float)`, `pushPose()/popPose()`.

Поворот вокруг оси через центр блока (поза BER стоит в углу блока):
```java
// extract: угол — функция времени, клиентского состояния не нужно
long t = be.getLevel() == null ? 0 : be.getLevel().getGameTime();       // LevelTimeAccess#getGameTime(): long
float period = 360f / degPerTick;                                       // модуль ДО float: иначе теряется точность
state.angleDeg = ((t % (long) period) + partialTick) * degPerTick + state.phaseDeg;
// submit:
pose.pushPose();
pose.translate(0.5f, 0.5f, 0.5f);
pose.mulPose(switch (state.axis) {                                      // Direction.Axis из blockstate
    case X -> Axis.XP.rotationDegrees(state.angleDeg);
    case Y -> Axis.YP.rotationDegrees(state.angleDeg);
    case Z -> Axis.ZP.rotationDegrees(state.angleDeg); });
pose.translate(-0.5f, -0.5f, -0.5f);
state.rotor.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
pose.popPose();
```
Если `degPerTick` не делит 360 нацело — хранить фазу `phaseDeg` и считать `(t - t0) * ω` с `t0`, пересинхронизируемым
при смене скорости (см. §4). Согласованность шестерён: одинаковое ω·(t) у всех валов сети → одинаковая фаза без синка.

**Производительность.** BER-экземпляр один на тип (`BlockEntityRendererRegistry.register`), на каждый BE — свой
`RenderState` + `extractRenderState` каждый кадр. `submit` → `SubmitNodeCollector.submitBlockModel(...)` — кусок
feature-батча (запечённые квады, не пересборка чанка). 200 валов × ~6–24 квада — пренебрежимо (сотни сундуков в
ваниле — тот же путь). Держать `shouldRenderOffScreen()=false` (дефолт) → отсечение по видимости секции чанка;
`getViewDistance()` можно уменьшить до 32–48 для мелких деталей. Не аллоцировать в extract (переиспользовать поля state).

**Статичная часть + вращающаяся часть.** Рекомендуется: блок-модель (JSON blockstate) рисует только
статичный корпус (чанк-меш, бесплатно), ротор — отдельная модель, **не** являющаяся blockstate-ом
никакого блока, рисуется только BER. Вариант без Fabric API — фиктивный blockstate (напр. свойство `part=rotor`
у служебного незарегистрированного-в-мире блока, как `MASS_DRIVER_SLED` в 004) + `BlockModelResolver.update`.
Чище — Fabric Model Loading API (extra model):

```java
// net.fabricmc.fabric.api.client.model.loading.v1
interface ModelLoadingPlugin { static void register(ModelLoadingPlugin); void initialize(ModelLoadingPlugin.Context); }
interface ModelLoadingPlugin.Context { <T> void addModel(ExtraModelKey<T>, UnbakedExtraModel<T>); /* + registerBlockStateResolver, modify*... */ }
final class ExtraModelKey<T> { static <T> ExtraModelKey<T> create(); static <T> ExtraModelKey<T> create(Supplier<String> debugName); }
final class SimpleUnbakedExtraModel<T> implements UnbakedExtraModel<T> {
  SimpleUnbakedExtraModel(Identifier, BiFunction<ResolvedModel, ModelBaker, T>);
  static SimpleUnbakedExtraModel<BlockStateModel> blockStateModel(Identifier modelId);
  static SimpleUnbakedExtraModel<BlockStateModel> blockStateModel(Identifier modelId, ModelState); // BlockModelRotation
}
interface FabricModelManager { default <T> T getModel(ExtraModelKey<T>); } // реализуется ModelManager через mixin
```
Интерфейс **не** injected (в fabric.mod.json нет `loom:injected_interfaces`) → нужен каст.
`BlockStateModel` = `net.minecraft.client.renderer.block.dispatch.BlockStateModel`:
`collectParts(RandomSource, List<BlockStateModelPart>)`, `particleMaterial()`, `materialFlags()`, `hasMaterialFlag(int)`;
`BakedQuad.FLAG_TRANSLUCENT = 1`, `FLAG_ANIMATED = 2`. `BlockModelRotation.IDENTITY`, `BlockModelRotation.get(OctahedralGroup)`.

```java
// client: ModModels.java
public static final ExtraModelKey<BlockStateModel> SHAFT_ROTOR = ExtraModelKey.create(() -> "spacereloaded:shaft_rotor");
public static void init() {   // из SpaceReloadedClient.onInitializeClient
    ModelLoadingPlugin.register(ctx -> ctx.addModel(SHAFT_ROTOR, SimpleUnbakedExtraModel.blockStateModel(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "block/shaft_rotor")))); // models/block/shaft_rotor.json
}
public static BlockStateModel get(ExtraModelKey<BlockStateModel> key) {
    return ((FabricModelManager) Minecraft.getInstance().getModelManager()).getModel(key); // Minecraft#getModelManager()
}
```
Заполнение `BlockModelRenderState` из произвольного `BlockStateModel` — ровно как ванильный
`BlockStateModelWrapper.update(state, blockState, ctx, seed)`:
```java
// extract (модель брать каждый раз — lookup дешёвый, а после F3+T ссылка меняется)
BlockStateModel model = ModModels.get(ModModels.SHAFT_ROTOR);
state.rotor.clear();
if (model != null) {
    List<BlockStateModelPart> parts = state.rotor.setupModel(new Matrix4f() /*identity→null*/,  // static final поле
            model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT));  // true → Sheets.translucentBlockItemSheet(), иначе cutout
    model.collectParts(state.rotor.scratchRandomSource(42L), parts);
    // tint не нужен: tintLayers() пуст → EMPTY_TINTS
}
// submit: state.rotor.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
```
`BlockModelRenderState`: `clear()`, `setupModel(Matrix4fc, boolean translucent) → List<BlockStateModelPart>`,
`scratchRandomSource(long)`, `tintLayers() → IntList`, `public int blockLightCoords`, `submit/submitWithZOffset/
submitOnlyOutline(PoseStack, SubmitNodeCollector, int light, int overlay, int outlineColor)`, `isEmpty()`.
Внутри `submit` → `SubmitNodeCollector.submitBlockModel(PoseStack, RenderType, List<BlockStateModelPart>, int[] tints,
int light, int overlay, int outline)` (свет = `LightCoordsUtil.max(light, blockLightCoords)`). Напрямую тоже можно.

## 2. Предмет, кликающий по блоку

```java
@Override public InteractionResult useOn(UseOnContext ctx) {       // net.minecraft.world.item.Item
    Level level = ctx.getLevel();
    if (level.isClientSide()) return InteractionResult.SUCCESS;     // махнуть рукой, сервер решит
    BlockPos pos = ctx.getClickedPos(); Direction face = ctx.getClickedFace();
    Vec3 hit = ctx.getClickLocation(); Player p = ctx.getPlayer();  // p может быть null (раздатчик)
    ...; return InteractionResult.SUCCESS_SERVER;                    // или PASS / FAIL
}
```
`UseOnContext`: `getClickedPos()`, `getClickedFace()`, `getClickLocation()`, `isInside()`, `getItemInHand()`,
`getPlayer()`, `getHand()`, `getLevel()`, `getHorizontalDirection()`, `isSecondaryUseActive()`, `getRotation()`.
Прочее у Item: `use(Level, Player, InteractionHand)` (клик в воздух). Глобальный хук — `UseBlockCallback` (уже в `SpaceReloaded`).

## 3. Экран-справочник (guide book)

Экраны мода: `extends Screen`, рисование в `extractRenderState(GuiGraphicsExtractor, int mx, int my, float pt)` /
`extractBackground(...)` (так сделан `ScanReportScreen`), `isPauseScreen()`, `onClose()`, `protected init()`.
Ввод: `mouseScrolled(double mx, double my, double scrollX, double scrollY)`, `mouseClicked(MouseButtonEvent, boolean doubleClick)`,
`keyPressed(KeyEvent)` (все boolean).

`GuiGraphicsExtractor` (26.2 — замена GuiGraphics):
- текст: `text(Font, String|Component|FormattedCharSequence, int x, int y, int argb[, boolean shadow])`,
  `centeredText(...)`, `textWithWordWrap(Font, FormattedText, int x, int y, int width, int argb[, boolean shadow])`;
- перенос вручную: `Font.split(FormattedText, int width) → List<FormattedCharSequence>`, `Font.wordWrapHeight(FormattedText,int)`,
  `Font.width(...)`, `font.lineHeight` (поле);
- иконки: `item(ItemStack, int x, int y)`, `item(ItemStack, x, y, int seed)`, `fakeItem(...)`,
  `itemDecorations(Font, ItemStack, x, y[, String count])`, тултип `setTooltipForNextFrame(Font, ItemStack, mx, my)`;
- фигуры/текстуры: `fill(x0,y0,x1,y1,argb)`, `fillGradient(...)`, `outline(x,y,w,h,argb)`, `horizontalLine/verticalLine`,
  `blit(RenderPipeline, Identifier, x, y, u, v, w, h, texW, texH)` (+перегрузки), `blitSprite(RenderPipeline, Identifier, x, y, w, h)`;
- прокрутка: `enableScissor(x0, y0, x1, y1)` / `disableScissor()` + смещение `scrollY` из `mouseScrolled`;
  `containsPointInScissor(x,y)` для hover. Готовые виджеты: `components.AbstractScrollArea`, `components.ScrollableLayout`.
- трансформации: `pose()` → `org.joml.Matrix3x2fStack` (**2D**): `pushMatrix()`, `popMatrix()`, `translate(float,float)`, `scale(float,float)`.

```java
@Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    scroll = Mth.clamp(scroll - (int) (sy * 12), 0, maxScroll); return true; }
// в extractRenderState:
gfx.enableScissor(left, top, left + w, top + h);
int y = top - scroll;
for (FormattedCharSequence line : font.split(page.body(), w - 8)) { gfx.text(font, line, left + 4, y, 0xFF202020, false); y += font.lineHeight + 1; }
gfx.pose().pushMatrix(); gfx.pose().translate(left + 4, y); gfx.pose().scale(2f, 2f);
gfx.item(new ItemStack(ModBlocks.SHAFT), 0, 0);   // блок-предмет уже рисуется изометрическим 3D-кубиком
gfx.pose().popMatrix();
gfx.disableScissor();
```
**3D-превью в GUI.** Одиночный блок = `item(...)` блок-предмета (изометрия GUI-display модели), масштаб — `pose().scale`.
Мультиблок в 3D — только через свой `gui.render.pip.PictureInPictureRenderer` (как `GuiEntityRenderer`/
`GuiBookModelRenderer`; у `GuiGraphicsExtractor` нет generic-метода для чужого PiP, нужен mixin/регистрация — дорого).
**Рекомендация:** 2D-послойные срезы: сетка иконок `item(...)` по слоям Y (кнопки «слой ▲/▼»), 16px на клетку,
пустые клетки — `outline`. Дёшево, читаемо, без рисков пайплайна.

## 4. Клиентский тик / плавное вращение

`EntityBlock.getTicker(Level, BlockState, BlockEntityType<T>)` вызывается и на клиенте (у клиентских BE тоже есть тикер);
в моде все тикеры сейчас делают `if (level.isClientSide()) return null;` (напр. `MassCatcherBlock`). `Level.isClientSide()` — метод.
`BlockEntityTicker<T>.tick(Level, BlockPos, BlockState, T)`.
**Рекомендация — тикер на клиенте не нужен:** угол = f(`level.getGameTime()` + partialTick) прямо в `extractRenderState`
(BER имеет `be.getLevel()`; так уже делает `MassDriverRenderer`). `getGameTime()` на клиенте синхронизируется сервером.
Скорость/фаза: хранить в BE, отдавать клиенту `getUpdatePacket()/getUpdateTag()` (004 §4) **только при смене** ω
(+ `t0`/`phaseDeg`, чтобы не было скачка). Если ω — дискретна (off/slow/fast), проще свойство blockstate → клиент видит
без BE-синка (но смена state = пересборка чанк-секции, реже чем раз в секунду — ок).

## 5. Взрывы и герметичность (T024 — закрыт в 005, T528)

Цепочка (javap -c): `ServerExplosion.explode()` → `calculateExplodedPositions()` → `hurtEntities()` →
`interactWithBlocks(List<BlockPos>)` → для каждой: `BlockState.onExplosionHit(ServerLevel, BlockPos, Explosion, BiConsumer<ItemStack,BlockPos>)`
→ (`BlockBehaviour.onExplosionHit`) дропы + `level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)` →
`Level.setBlock` → при `(flags & 2) != 0` и чанке ≥ BLOCK_TICKING → **`sendBlockUpdated(pos, old, new, flags)`**.
⇒ Ванильные взрывы **уже проходят через `ServerLevelMixin#sendBlockUpdated`** (old≠new) — T024 (закрыт T528, стенд testExplosionSealing), скорее всего,
закрыт с 004. Проверить gametest-ом: зона + `level.explode(...)` рядом со стеной → зона должна разгерметизироваться.

Если нужен явный хук (напр. блоки, переопределяющие `onExplosionHit` без setBlock, или пакетная обработка):
```java
// net.minecraft.world.level.ServerExplosion (public class implements Explosion)
public ServerExplosion(ServerLevel, Entity, DamageSource, ExplosionDamageCalculator, Vec3 center, float radius,
                       boolean fire, Explosion.BlockInteraction);
public int explode();
private List<BlockPos> calculateExplodedPositions();
private void interactWithBlocks(List<BlockPos>);   // сюда входят только позиции, которые будут разрушены
private boolean interactsWithBlocks();
public ServerLevel level(); public Vec3 center(); public float radius();
// ServerLevel.explode(Entity, DamageSource, ExplosionDamageCalculator, double x, double y, double z, float radius,
//   boolean fire, Level.ExplosionInteraction, ParticleOptions small, ParticleOptions large,
//   WeightedList<ExplosionParticleInfo>, Holder<SoundEvent>)  — создаёт ServerExplosion и зовёт explode()
```
```java
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Inject(method = "interactWithBlocks", at = @At("TAIL"))
    private void spacereloaded$afterBlocks(List<BlockPos> positions, CallbackInfo ci) {
        ServerLevel level = ((ServerExplosion) (Object) this).level();
        for (BlockPos pos : positions) { ZoneManager.markBlockChanged(level, pos); CableNetworkManager.markBlockChanged(level, pos); }
    }
}
```
Регистрация: `mod/src/main/resources/spacereloaded.mixins.json` → `"mixins": ["ServerLevelMixin", "ServerExplosionMixin"]`
(package `org.alex_melan.spacereloaded.mixin`, `compatibilityLevel JAVA_25`, `defaultRequire 1`). Клиентский конфиг —
`mod/src/client/resources/spacereloaded.client.mixins.json` (сейчас `SoundEngineMixin`). Обработка — в отложенной очереди
конца тика (`END_LEVEL_TICK`), так что двойная пометка от обоих миксинов безвредна (множество).
