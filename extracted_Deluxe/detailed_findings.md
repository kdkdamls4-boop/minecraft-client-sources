# Detailed Audit Findings for Deluxe Client

## Category: BASE64_DECODE (1 items)

- **File**: `src\main\java\deluxe\client\module\render\ParticlesModule.java:1496`
  ```java
  ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(asString.substring(asString.indexOf(44) + 1))).order(ByteOrder.LITTLE_ENDIAN);
  ```

## Category: CREDENTIALS_MINING (1 items)

- **File**: `src\main\java\deluxe\mixin\MinecraftClientMixin9.java:22`
  ```java
  return !session.getAccessToken().isBlank() && session.getUuidOrNull() != null && session.getUuidOrNull().version() == 4
  ```

## Category: DREAMIX_PROFILE (99 items)

- **File**: `SOURCE_GUIDE.md:33`
  ```java
  `ru.dreamix` и `com.deluxe.lib.discord` сохранены отдельно, поскольку их имена связаны с JNI/JNA. В JAR нет реализации шести `native`-методов. `ProfileAPI.create` уже в оригинале перехватывает отсутст
  ```

- **File**: `src\main\java\com\deluxe\lib\discord\DiscordNative$Handlers.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\com\deluxe\lib\discord\DiscordNative$Presence.java:3`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\com\deluxe\lib\discord\DiscordNative$User.java:3`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\client\DeluxeClient.java:23`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\DeluxeClient.java:38`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\deluxe\client\DeluxeClient.java:218`
  ```java
  ProfileAPI.create("user", 1, RoleObject.ADMIN);
  ```

- **File**: `src\main\java\deluxe\client\command\IrcCommand.java:4`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\command\IrcCommand.java:6`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\deluxe\client\command\IrcCommand.java:27`
  ```java
  return ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN);
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:67`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:69`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:115`
  ```java
  || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.BETA)) {
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:116`
  ```java
  if (!(module instanceof YTMODEModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.YOUTUBE)) {
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:117`
  ```java
  if (!(module instanceof AdminShardModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN)) {
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:118`
  ```java
  if (!(module instanceof AdminRecordModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN)) {
  ```

- **File**: `src\main\java\deluxe\client\module\ModuleManager.java:172`
  ```java
  if (ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.BETA)) {
  ```

- **File**: `src\main\java\deluxe\client\module\misc\DiscordRPCModule.java:9`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\DiscordRPCModule.java:53`
  ```java
  ProfileAPI.get().getModel().getName();
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:5`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:42`
  ```java
  return ProfileAPI.get() != null
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:43`
  ```java
  && ProfileAPI.get().getModel() != null
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:44`
  ```java
  && ProfileAPI.get().getModel().getName() != null
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:45`
  ```java
  && !ProfileAPI.get().getModel().getName().isBlank()
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:46`
  ```java
  ? ProfileAPI.get().getModel().getName()
  ```

- **File**: `src\main\java\deluxe\client\ui\WelcomeScreen.java:7`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\ui\WelcomeScreen.java:123`
  ```java
  ProfileAPI.get() != null
  ```

- **File**: `src\main\java\deluxe\client\ui\WelcomeScreen.java:124`
  ```java
  && ProfileAPI.get().getModel() != null
  ```

- **File**: `src\main\java\deluxe\client\ui\WelcomeScreen.java:125`
  ```java
  && ProfileAPI.get().getModel().getName() != null
  ```

- **File**: `src\main\java\deluxe\client\ui\WelcomeScreen.java:126`
  ```java
  && !ProfileAPI.get().getModel().getName().isBlank()
  ```

- **File**: `src\main\java\deluxe\client\ui\WelcomeScreen.java:127`
  ```java
  ? ProfileAPI.get().getModel().getName()
  ```

- **File**: `src\main\java\deluxe\client\ui\hud\WatermarkHud.java:14`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\ui\hud\WatermarkHud.java:72`
  ```java
  String name = ProfileAPI.get().getModel().getName();
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:9`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:10`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:72`
  ```java
  jsonObject.addProperty("name", ProfileAPI.get().getModel().getName());
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:73`
  ```java
  jsonObject.addProperty("role", ProfileAPI.get().getModel().getRole().name());
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:197`
  ```java
  String name = ProfileAPI.get().getModel().getName();
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:222`
  ```java
  HoverEvent hoverEvent = ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN) ? new ShowText(text) : null;
  ```

- **File**: `src\main\java\deluxe\mixin\AbstractClientPlayerEntityMixin.java:6`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\BlindnessEffectFogModifierMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\ChatInputSuggestorMixin.java:5`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\ChatScreenMixin.java:8`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\ClientConnectionMixin.java:6`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\ClientPlayerEntityMixin2.java:15`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\ClientWorld_PropertiesMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\EntityMixin.java:15`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\FogRendererMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\GameRendererMixin.java:17`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\GuiRendererMixin.java:31`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\GuiRenderStateMixin.java:5`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\HandledScreenMixin2.java:9`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\HeldItemRendererMixin.java:11`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\InGameHudMixin.java:15`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\InGameHudMixin2.java:3`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\InGameHud_HeartTypeMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\InGameOverlayRendererMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\KeyboardInputMixin.java:7`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\LavaFogModifierMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\LightmapTextureManagerMixin.java:6`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\LivingEntityRendererMixin.java:8`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\MinecraftClientMixin8.java:5`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\SpecialGuiElementRendererMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\SplashOverlayMixin.java:7`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\WeatherRenderingMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\WorldBorderMixin.java:5`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\deluxe\mixin\WorldMixin.java:4`
  ```java
  import ru.dreamix.fabricloader.VMBridge;
  ```

- **File**: `src\main\java\ru\dreamix\fabricloader\VMBridge.java:1`
  ```java
  package ru.dreamix.fabricloader;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\IApi.java:1`
  ```java
  package ru.dreamix.protection.api;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:1`
  ```java
  package ru.dreamix.protection.api.bridge;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:3`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProtectionNativeBridge.java:1`
  ```java
  package ru.dreamix.protection.api.bridge;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\exceptions\ApiException.java:1`
  ```java
  package ru.dreamix.protection.api.exceptions;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:1`
  ```java
  package ru.dreamix.protection.api.impl.profile;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:3`
  ```java
  import ru.dreamix.protection.api.exceptions.ApiException;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:4`
  ```java
  import ru.dreamix.protection.api.IApi;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:5`
  ```java
  import ru.dreamix.protection.api.impl.profile.models.ProfileInformationModel;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:6`
  ```java
  import ru.dreamix.protection.api.bridge.ProfileNativeBridge;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:7`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:8`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.SubscriptionObject;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:11`
  ```java
  public class ProfileAPI implements IApi<ProfileInformationModel> {
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:13`
  ```java
  private static ProfileAPI allocatedProfile;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:15`
  ```java
  public static ProfileAPI get() {
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:23`
  ```java
  private static ProfileAPI createNativeProfile() throws UnsatisfiedLinkError {
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:24`
  ```java
  return new ProfileAPI(
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:35`
  ```java
  public static ProfileAPI create(String name, int userIdentifier, RoleObject role) {
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:43`
  ```java
  return allocatedProfile = new ProfileAPI(new ProfileInformationModel(name, new SubscriptionObject("01.01.2048"), userIdentifier, role, ""));
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:47`
  ```java
  public static ProfileAPI createWithProduct(String name, int userIdentifier, RoleObject role, String launchedProduct) {
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:55`
  ```java
  return allocatedProfile = new ProfileAPI(
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\ProfileAPI.java:65`
  ```java
  public ProfileAPI(ProfileInformationModel initialProfileInformationModel) {
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\models\ProfileInformationModel.java:1`
  ```java
  package ru.dreamix.protection.api.impl.profile.models;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\models\ProfileInformationModel.java:3`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\models\ProfileInformationModel.java:4`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.SubscriptionObject;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\objects\RoleObject.java:1`
  ```java
  package ru.dreamix.protection.api.impl.profile.objects;
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\impl\profile\objects\SubscriptionObject.java:1`
  ```java
  package ru.dreamix.protection.api.impl.profile.objects;
  ```

- **File**: `src\test\java\deluxe\RecoveryChecks.java:12`
  ```java
  import ru.dreamix.protection.api.impl.profile.ProfileAPI;
  ```

- **File**: `src\test\java\deluxe\RecoveryChecks.java:13`
  ```java
  import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
  ```

- **File**: `src\test\java\deluxe\RecoveryChecks.java:61`
  ```java
  ProfileAPI.create("development", 1, RoleObject.DEFAULT);
  ```

- **File**: `src\test\java\deluxe\RecoveryChecks.java:62`
  ```java
  assert ProfileAPI.get().getModel().getName().equals("development");
  ```

## Category: HTTP_CLIENT (58 items)

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:12`
  ```java
  import java.net.http.HttpClient;
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:13`
  ```java
  import java.net.http.HttpRequest;
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:14`
  ```java
  import java.net.http.HttpResponse;
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:15`
  ```java
  import java.net.http.HttpClient.Redirect;
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:16`
  ```java
  import java.net.http.HttpResponse.BodyHandlers;
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:39`
  ```java
  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).followRedirects(Redirect.NORMAL).build();
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:106`
  ```java
  HttpResponse<String> local5944_0 = HTTP.send(
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:107`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/cosmetics/manifest.php")).timeout(Duration.ofSeconds(8L)).GET().build(),
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:184`
  ```java
  HttpResponse<byte[]> local5947_4;
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:187`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/cosmetics/" + local5947_0 + "?v=" + local5947_3))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:20`
  ```java
  import java.net.http.HttpClient;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:21`
  ```java
  import java.net.http.HttpRequest;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:22`
  ```java
  import java.net.http.HttpResponse;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:23`
  ```java
  import java.net.http.HttpClient.Redirect;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:24`
  ```java
  import java.net.http.HttpRequest.BodyPublishers;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:25`
  ```java
  import java.net.http.HttpResponse.BodyHandlers;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:55`
  ```java
  private final HttpClient field13036 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).followRedirects(Redirect.NORMAL).build();
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:235`
  ```java
  private boolean method3765(HttpResponse<String> local6219_1) {
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:310`
  ```java
  HttpResponse<String> local6225_1 = this.field13036
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:312`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:366`
  ```java
  HttpResponse<String> local6231_2 = this.field13036
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:368`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/orders/trade"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:410`
  ```java
  private void method3776(HttpResponse<String> local6232_1) {
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:489`
  ```java
  HttpResponse<String> local6238_2 = this.field13036
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:491`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/orders/" + (local6238_1.startsWith("#") ? local6238_1.substring(1) : local6238_1) + "/"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:526`
  ```java
  HttpRequest.newBuilder(URI.create(matcher.group(1).startsWith("http") ? matcher.group(1) : "https://funpay.com" + matcher.group(1)))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:700`
  ```java
  private HttpResponse<String> method3794(String local6251_1, String local6251_2) throws Exception {
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:707`
  ```java
  HttpResponse<String> local6251_4 = this.field13036
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:709`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/runner/"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:25`
  ```java
  import java.net.http.HttpClient;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:26`
  ```java
  import java.net.http.HttpRequest;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:27`
  ```java
  import java.net.http.HttpResponse;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:28`
  ```java
  import java.net.http.HttpRequest.BodyPublishers;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:29`
  ```java
  import java.net.http.HttpResponse.BodyHandlers;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:58`
  ```java
  private final HttpClient field00893 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:287`
  ```java
  HttpResponse<String> local815_2 = this.field00893
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:289`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/cosmetics/sync.php"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:26`
  ```java
  import java.net.http.HttpClient;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:27`
  ```java
  import java.net.http.HttpRequest;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:28`
  ```java
  import java.net.http.HttpResponse;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:29`
  ```java
  import java.net.http.HttpRequest.BodyPublishers;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:30`
  ```java
  import java.net.http.HttpResponse.BodyHandlers;
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:59`
  ```java
  private final HttpClient field10051 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:458`
  ```java
  HttpResponse<String> local4691_2 = this.field10051
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:460`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/party/party.php"))
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:9`
  ```java
  import java.net.http.HttpClient;
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:10`
  ```java
  import java.net.http.HttpRequest;
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:11`
  ```java
  import java.net.http.HttpResponse;
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:12`
  ```java
  import java.net.http.HttpResponse.BodyHandlers;
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:32`
  ```java
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:136`
  ```java
  HttpResponse<InputStream> local505_1 = HTTP.send(
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:137`
  ```java
  HttpRequest.newBuilder(URI.create(local505_0)).header("Accept", "application/json").GET().build(), BodyHandlers.ofInputStream()
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:16`
  ```java
  import java.net.http.HttpClient;
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:17`
  ```java
  import java.net.http.HttpRequest;
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:18`
  ```java
  import java.net.http.HttpRequest.BodyPublishers;
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:19`
  ```java
  import java.net.http.HttpResponse.BodyHandlers;
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:48`
  ```java
  private final HttpClient field09535 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:154`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/irc/irc.php"))
  ```

## Category: HWID_SYSTEM_ID (2 items)

- **File**: `src\main\java\deluxe\client\storage\AccountEntry.java:81`
  ```java
  return UUID.nameUUIDFromBytes(("OfflinePlayer:" + local5625_0).getBytes(StandardCharsets.UTF_8));
  ```

- **File**: `src\main\java\deluxe\client\ui\hud\StaffHud.java:327`
  ```java
  UUID uUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + local3495_1).getBytes(StandardCharsets.UTF_8));
  ```

## Category: NATIVE_METHOD (6 items)

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:7`
  ```java
  public static native String getUsername();
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:9`
  ```java
  public static native String getSubscriptionTill();
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:11`
  ```java
  public static native int getUserIdentifier();
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:13`
  ```java
  public static native RoleObject getRole();
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProfileNativeBridge.java:15`
  ```java
  public static native String getLaunchedProduct();
  ```

- **File**: `src\main\java\ru\dreamix\protection\api\bridge\ProtectionNativeBridge.java:4`
  ```java
  public static native void fillClassPool(Class<?> var0, String var1);
  ```

## Category: SYSTEM_LOAD (1 items)

- **File**: `src\main\java\deluxe\client\util\DiscordRpcLibrary.java:12`
  ```java
  DiscordRpcLibrary discordRpcLibrary = (DiscordRpcLibrary)Native.load("discord-rpc", DiscordRpcLibrary.class);
  ```

## Category: URL_HTTP (24 items)

- **File**: `gradlew:10`
  ```java
  #      https://www.apache.org/licenses/LICENSE-2.0
  ```

- **File**: `gradlew:60`
  ```java
  #       https://github.com/gradle/gradle/blob/3d91ce3b8caaf77ad09f381f43615b715b53f72c/platforms/jvm/plugins-application/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
  ```

- **File**: `gradlew:63`
  ```java
  #       You can find Gradle at https://github.com/gradle/gradle/.
  ```

- **File**: `gradlew:88`
  ```java
  # Discard cd standard output in case $CDPATH is set (https://github.com/gradle/gradle/issues/25036)
  ```

- **File**: `gradlew.bat:8`
  ```java
  @rem      https://www.apache.org/licenses/LICENSE-2.0
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:107`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/cosmetics/manifest.php")).timeout(Duration.ofSeconds(8L)).GET().build(),
  ```

- **File**: `src\main\java\deluxe\client\cosmetic\CosmeticPreviewAtlas.java:187`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/cosmetics/" + local5947_0 + "?v=" + local5947_3))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:90`
  ```java
  "✅ Готово, валюта у вас — приятной игры!\n\n\ud83d\ude4f Подтвердите заказ и оставьте отзыв, мне это очень поможет:\n\ud83d\udd17 https://funpay.com/orders/"
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:312`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:368`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/orders/trade"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:491`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/orders/" + (local6238_1.startsWith("#") ? local6238_1.substring(1) : local6238_1) + "/"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:526`
  ```java
  HttpRequest.newBuilder(URI.create(matcher.group(1).startsWith("http") ? matcher.group(1) : "https://funpay.com" + matcher.group(1)))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\AutoDeliverModule.java:709`
  ```java
  HttpRequest.newBuilder(URI.create("https://funpay.com/runner/"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\DiscordRPCModule.java:57`
  ```java
  presence.largeImageKey = "https://i.ibb.co/hxsyxR5h/ASD1-ASD0000-0200-2.gif";
  ```

- **File**: `src\main\java\deluxe\client\module\misc\DiscordRPCModule.java:65`
  ```java
  presence.button_url_1 = "https://t.me/delnmt";
  ```

- **File**: `src\main\java\deluxe\client\module\misc\DiscordRPCModule.java:67`
  ```java
  presence.button_url_2 = "https://discord.gg/deluxeclient";
  ```

- **File**: `src\main\java\deluxe\client\module\misc\DiscordRPCModule.java:85`
  ```java
  this.field11626 = "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png";
  ```

- **File**: `src\main\java\deluxe\client\module\misc\GlobalsModule.java:289`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/cosmetics/sync.php"))
  ```

- **File**: `src\main\java\deluxe\client\module\misc\PartyModule.java:460`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/party/party.php"))
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:89`
  ```java
  Matcher matcher = field00018.matcher(method0457("https://api.mojang.com/users/profiles/minecraft/" + local503_1));
  ```

- **File**: `src\main\java\deluxe\client\render\AccountPreviewRenderer.java:95`
  ```java
  method0457("https://sessionserver.mojang.com/session/minecraft/profile/" + matcher.group(1) + "?unsigned=false")
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:445`
  ```java
  () -> Util.getOperatingSystem().open("https://t.me/delnmt")
  ```

- **File**: `src\main\java\deluxe\client\ui\MainMenuScreen.java:455`
  ```java
  () -> Util.getOperatingSystem().open("https://discord.gg/wWsxQkF2tP")
  ```

- **File**: `src\main\java\deluxe\client\util\IrcClient.java:154`
  ```java
  HttpRequest.newBuilder(URI.create("http://deluxegaymarket.fun/irc/irc.php"))
  ```

