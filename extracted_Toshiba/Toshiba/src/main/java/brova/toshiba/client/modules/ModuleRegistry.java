package brova.toshiba.client.modules;

import brova.toshiba.client.modules.impl.combat.AimAssist;
import brova.toshiba.client.modules.impl.combat.AntiBot;
import brova.toshiba.client.modules.impl.combat.AttackAura;
import brova.toshiba.client.modules.impl.combat.AutoExplosion;
import brova.toshiba.client.modules.impl.combat.AutoSwap;
import brova.toshiba.client.modules.impl.combat.AutoTotem;
import brova.toshiba.client.modules.impl.combat.CrystalAura;
import brova.toshiba.client.modules.impl.combat.ElytraMotion;
import brova.toshiba.client.modules.impl.combat.ElytraTarget;
import brova.toshiba.client.modules.impl.combat.HitBox;
import brova.toshiba.client.modules.impl.combat.MaceAura;
import brova.toshiba.client.modules.impl.combat.MaceSwap;
import brova.toshiba.client.modules.impl.combat.PacketCriticals;
import brova.toshiba.client.modules.impl.combat.TapeMouse;
import brova.toshiba.client.modules.impl.combat.TpsSync;
import brova.toshiba.client.modules.impl.combat.TriggerBot;
import brova.toshiba.client.modules.impl.combat.WebTrap;
import brova.toshiba.client.modules.impl.ghost.Panic;
import brova.toshiba.client.modules.impl.misc.AppleFarmer;
import brova.toshiba.client.modules.impl.misc.AuctionHelper;
import brova.toshiba.client.modules.impl.misc.AutoCodes;
import brova.toshiba.client.modules.impl.misc.AutoLeave;
import brova.toshiba.client.modules.impl.misc.AutoTPAccept;
import brova.toshiba.client.modules.impl.misc.ClickPearl;
import brova.toshiba.client.modules.impl.misc.ClientSounds;
import brova.toshiba.client.modules.impl.misc.ElytraSwap;
import brova.toshiba.client.modules.impl.misc.FakePlayer;
import brova.toshiba.client.modules.impl.misc.NameProtect;
import brova.toshiba.client.modules.impl.misc.ServerHelper;
import brova.toshiba.client.modules.impl.misc.SpaceJoiner;
import brova.toshiba.client.modules.impl.movement.AirStuck;
import brova.toshiba.client.modules.impl.movement.Fly;
import brova.toshiba.client.modules.impl.movement.FreeCam;
import brova.toshiba.client.modules.impl.movement.GuiMove;
import brova.toshiba.client.modules.impl.movement.HighJump;
import brova.toshiba.client.modules.impl.movement.NoSlow;
import brova.toshiba.client.modules.impl.movement.NoWeb;
import brova.toshiba.client.modules.impl.movement.Speed;
import brova.toshiba.client.modules.impl.movement.Sprint;
import brova.toshiba.client.modules.impl.movement.SuperFirework;
import brova.toshiba.client.modules.impl.movement.Velocity;
import brova.toshiba.client.modules.impl.player.AutoEat;
import brova.toshiba.client.modules.impl.player.AutoFish;
import brova.toshiba.client.modules.impl.player.AutoSell;
import brova.toshiba.client.modules.impl.player.AutoTool;
import brova.toshiba.client.modules.impl.player.ChestStealer;
import brova.toshiba.client.modules.impl.player.CommandFix;
import brova.toshiba.client.modules.impl.player.CordDropper;
import brova.toshiba.client.modules.impl.player.FastBreak;
import brova.toshiba.client.modules.impl.player.ItemScroller;
import brova.toshiba.client.modules.impl.player.LockSlot;
import brova.toshiba.client.modules.impl.player.NoDelay;
import brova.toshiba.client.modules.impl.player.NoInteract;
import brova.toshiba.client.modules.impl.player.NoPush;
import brova.toshiba.client.modules.impl.player.PvPSave;
import brova.toshiba.client.modules.impl.player.ScoreboardHP;
import brova.toshiba.client.modules.impl.player.ServerRPSpoofer;
import brova.toshiba.client.modules.impl.player.TargetPearl;
import brova.toshiba.client.modules.impl.player.WindHop;
import brova.toshiba.client.modules.impl.render.Arrows;
import brova.toshiba.client.modules.impl.render.AspectRatio;
import brova.toshiba.client.modules.impl.render.AttackEffect;
import brova.toshiba.client.modules.impl.render.BlockESP;
import brova.toshiba.client.modules.impl.render.BlockOverlay;
import brova.toshiba.client.modules.impl.render.ColorSaturation;
import brova.toshiba.client.modules.impl.render.CustomSize;
import brova.toshiba.client.modules.impl.render.CustomWorld;
import brova.toshiba.client.modules.impl.render.DamagePop;
import brova.toshiba.client.modules.impl.render.FireworkESP;
import brova.toshiba.client.modules.impl.render.FullBright;
import brova.toshiba.client.modules.impl.render.Hud;
import brova.toshiba.client.modules.impl.render.ItemTime;
import brova.toshiba.client.modules.impl.render.JumpCircle;
import brova.toshiba.client.modules.impl.render.NameTags;
import brova.toshiba.client.modules.impl.render.NoRender;
import brova.toshiba.client.modules.impl.render.Particle;
import brova.toshiba.client.modules.impl.render.Predictions;
import brova.toshiba.client.modules.impl.render.SaturationEat;
import brova.toshiba.client.modules.impl.render.SeeInvisibles;
import brova.toshiba.client.modules.impl.render.ShaderESP;
import brova.toshiba.client.modules.impl.render.ShaderHands;
import brova.toshiba.client.modules.impl.render.ShulkerPreview;
import brova.toshiba.client.modules.impl.render.Souls;
import brova.toshiba.client.modules.impl.render.SpinBot;
import brova.toshiba.client.modules.impl.render.SwingAnimations;
import brova.toshiba.client.modules.impl.render.TargetESP;
import brova.toshiba.client.modules.impl.render.TotemPop;
import brova.toshiba.client.modules.impl.render.Trajectory;
import brova.toshiba.client.modules.impl.render.ViewModel;
import brova.toshiba.client.modules.impl.render.WardenEsp;
import brova.toshiba.client.storages.ObjectStorage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class ModuleRegistry extends ObjectStorage<Module> {

    public static final ModuleRegistry INSTANCE = new ModuleRegistry();

    public static final AntiBot antibot = new AntiBot();

    public static final AirStuck airStuck = new AirStuck();

    public static final Arrows arrows = new Arrows();

    public static final AspectRatio aspectRatio = new AspectRatio();

    public static final AimAssist aimAssist = new AimAssist();

    public static final HitBox hitBox = new HitBox();

    public static final AttackAura aura = new AttackAura();

    public static final MaceAura maceAura = new MaceAura();

    public static final AutoTool autoTool = new AutoTool();

    public static final MaceSwap maceSwap = new MaceSwap();

    public static final TapeMouse tapeMouse = new TapeMouse();

    public static final AutoTPAccept autoAccept = new AutoTPAccept();

    public static final AuctionHelper auctionHelper = new AuctionHelper();

    public static final AutoFish autoFish = new AutoFish();

    public static final AutoCodes autoCodes = new AutoCodes();

    public static final TriggerBot triggerBot = new TriggerBot();

    public static final SpaceJoiner spaceJoiner = new SpaceJoiner();

    public static final AutoEat autoEat = new AutoEat();

    public static final AutoExplosion autoExplosion = new AutoExplosion();

    public static final CrystalAura crystalAura = new CrystalAura();

    public static final WebTrap webTrap = new WebTrap();

    public static final NameProtect nameProtect = NameProtect.INSTANCE;

    public static final ChestStealer chestStealer = new ChestStealer();

    public static final ClientSounds clientSounds = new ClientSounds();

    public static final ColorSaturation colorSaturation = new ColorSaturation();

    public static final DamagePop damagePop = new DamagePop();

    public static final AttackEffect attackEffect = new AttackEffect();

    public static final Panic panic = new Panic();

    public static final AutoTotem autoTotem = new AutoTotem();

    public static final BlockOverlay blockOverlay = new BlockOverlay();

    public static final ClickPearl clickPearl = new ClickPearl();

    public static final BlockESP blockesp = new BlockESP();

    public static final SuperFirework elytraBoost = new SuperFirework();

    public static final ElytraMotion elytraMotion = new ElytraMotion();

    public static final ElytraSwap elytraSwap = new ElytraSwap();

    public static final ElytraTarget elytraTarget = new ElytraTarget();

    public static final NameTags nameTags = new NameTags();

    public static final Fly fly = new Fly();

    public static final FreeCam freeCam = new FreeCam();

    public static final FullBright fullBright = new FullBright();

    public static final HighJump highJump = new HighJump();

    public static final Hud interfaceModule = new Hud();

    public static final AutoLeave autoLeave = new AutoLeave();

    public static final GuiMove guiMove = new GuiMove();

    public static final ItemScroller itemScroller = new ItemScroller();

    public static final JumpCircle jumpCircle = new JumpCircle();

    public static final NoDelay noDelay = new NoDelay();

    public static final NoPush noPush = new NoPush();

    public static final Velocity velocity = new Velocity();

    public static final NoWeb noWeb = new NoWeb();

    public static final PacketCriticals packetCriticals = new PacketCriticals();

    public static final Particle particle = new Particle();

    public static final FireworkESP fireworkESP = new FireworkESP();

    public static final FastBreak fastBreak = new FastBreak();

    public static final NoRender noRender = new NoRender();

    public static final ServerRPSpoofer rPSpoofer = new ServerRPSpoofer();

    public static final SeeInvisibles seeInvisibles = new SeeInvisibles();

    public static final ShaderESP shaderEsp = new ShaderESP();

    public static final ShaderHands shaderHands = new ShaderHands();

    public static final ShulkerPreview shulkerPreview = new ShulkerPreview();

    public static final Souls souls = new Souls();

    public static final Speed speed = new Speed();

    public static final SpinBot spinBot = new SpinBot();

    public static final Sprint sprint = new Sprint();

    public static final SwingAnimations swingAnimations = new SwingAnimations();

    public static final TargetESP targetESP = new TargetESP();

    public static final TargetPearl targetPearl = new TargetPearl();

    public static final ScoreboardHP scoreboardHP = new ScoreboardHP();

    public static final TotemPop totemAngel = new TotemPop();

    public static final TpsSync tpsSync = new TpsSync();

    public static final ViewModel viewModel = new ViewModel();

    public static final CustomSize customSize = new CustomSize();

    public static final CustomWorld worldTweaks = new CustomWorld();

    public static final AutoSwap autoSwap = new AutoSwap();

    public static final NoSlow noSlow = new NoSlow();

    public static final ServerHelper serverHelper = new ServerHelper();

    public static final Trajectory trajectories = new Trajectory();

    public static final Predictions predictions = new Predictions();

    public static final FakePlayer fakePlayer = new FakePlayer();

    public static final CommandFix commandFix = new CommandFix();

    public static final WindHop windHop = new WindHop();

    public static final NoInteract noInteract = new NoInteract();

    public static final WardenEsp wardenEsp = new WardenEsp();

    public static final AutoSell autoSell = new AutoSell();

    public static final AppleFarmer appleFarmer = new AppleFarmer();

    public static final ItemTime itemTime = new ItemTime();

    public static final PvPSave pvpSave = new PvPSave();

    public static final LockSlot lockSlot = new LockSlot();

    public static final SaturationEat saturationEat = new SaturationEat();

    public static final CordDropper cordDropper = new CordDropper();

    public void initialize() {
        ArrayList<brova.toshiba.client.modules.Module> values = new ArrayList<>();
        for (Field field : ModuleRegistry.class.getDeclaredFields()) {
            if (Module.class.isAssignableFrom(field.getType())) {
                try {
                    Module module = (Module) field.get(null);
                    if (module != null) {
                        values.add(module);
                    }
                } catch (IllegalAccessException failure) {
                    failure.printStackTrace();
                }
            }
        }
        this.getObject().addAll(values);
        var iterator = brova.toshiba.client.utils.runtime.AccessGuard.invoke(this, new Object[] {}, (accessReceiver, accessArguments) -> ((ModuleRegistry) accessReceiver).getObject()).iterator();
        while (iterator.hasNext()) {
            Module moduleValue = iterator.next();
            try {
                Field declaredField = moduleValue.getClass().getDeclaredField("INSTANCE");
                declaredField.setAccessible(true);
                declaredField.set(null, moduleValue);
            } catch (Exception failureValue) {
            }
        }
    }
}
