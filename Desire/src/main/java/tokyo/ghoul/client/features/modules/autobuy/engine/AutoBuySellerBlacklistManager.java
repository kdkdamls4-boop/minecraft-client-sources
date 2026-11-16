package tokyo.ghoul.client.features.modules.autobuy.engine;

import tokyo.ghoul.api.system.files.AbstractFile;

import java.util.Locale;

public class AutoBuySellerBlacklistManager extends AbstractFile {
    private static final String[] DEFAULT_IGNORED_SELLERS = {
            "ToplecCHlKA", "DEXLX7", "Asya_Masya", "That0neBear", "CAMAPA", "n3ons", "HackerCat777",
            "Derty_1001", "NeKycok", "HiliGHo09", "_KArTOfANcHIcK_", "Heldyy", "_Baba_Lyba_2045_",
            "bodeero", "Ne_hopemine", "feliks_stream12", "TSN_SliZz", "MrPastaKing", "gromaforz",
            "Scary_Pumpkin", "_kirusha_buldog_", "Im_Alpaka", "__sny__", "_wavyy", "Fl1ckzzz", "MeYuugao",
            "BOBURJON", "Zardem1", "zerqq", "howlie", "be_pivo_er", "Kapibara442", "SomeOneDay",
            "Hanrasen", "Deri_", "babka_kotofalke", "LonelyConfident", "fxctar", "alarax", "canute",
            "Sp1doznii", "Its_ledsaed", "Grizzly_", "X12x12X", "ImskovorodaYT", "Nick122", "DistaWaka",
            "1k_", "egorhack69", "Microsoft_Client", "sweaterholes", "capybara_friends", "Mystical_player2",
            "Capi_SUN", "cr7_mentality", "Ckills", "wincody", "imbruh", "LastHitKing", "ayawouki",
            "neskywhyw0lker", "Klemzy", "AnDrEkA256", "ChereIIaIIIka", "MainPLAY2024", "1pirs",
            "RaZn0s_softov", "DizzyX", "MasterFlomik", "_25", "Welly_XD", "Shotik4562", "LIIAX_u_MAT",
            "n1rkoz", "_KiKLoP_", "DioBrando226", "taschenratte", "Flowiii", "Sunsssseti4", "RonaroYT",
            "BanCheat", "SexyBonya", "Insaineless", "xelzi4ch", "zxcMuytuik52", "S1ntel", "Vetera",
            "WhiteHater", "Berry_lov", "Tr1ggered", "dimuch75555", "alewinner", "Dodep228", "Holfys",
            "dark52521", "Nurk_AAP", "Sh1roExe", "Tvinkfozov", "NeyRey64", "MatGab11", "KiryIIIa",
            "igarekchotkiy", "Sveta_300m_otvas", "XdeadForSpaseX", "Inosonwear", "To4no_Ne_Borzuu",
            "waca_Bober", "Nedzo", "goatMessi666", "F3ARL3SS11", "makartni", "TTcuXoTTATKA",
            "ultra_nothing", "LastuWhy", "KirilyIIIka", "xzzc", "6jesss", "farmilanaulte", "kam1ych",
            "turkeyGod", "sleater", "kisskxd", "kotic_motic", "bulbulatorrrr", "CUDHbIY", "univerdeath",
            "HoruMa", "xv_Dosh1k", "vkycnika", "Sushi_Roll", "aqvip445", "Recycle_Bin", "a11ways",
            "amogys345", "_Winged_ange_", "collega228", "SaveTrueTriade", "Dobr1k_07", "amogys345", "grizon"
    };

    private static final AutoBuySellerBlacklistManager INSTANCE = new AutoBuySellerBlacklistManager();

    public static AutoBuySellerBlacklistManager getInstance() {
        return INSTANCE;
    }

    public AutoBuySellerBlacklistManager() {
        super();
        this.load();
    }

    @Override
    public String fileName() {
        return "blacklist";
    }

    public boolean isBlacklisted(String name) {
        String normalizedName = normalizeSellerName(name);
        return !normalizedName.isBlank()
                && this.getData().stream().anyMatch(seller -> normalizedName.equals(normalizeSellerName(seller)));
    }

    @Override
    public void add(String item) {
        if (item == null || item.isBlank() || this.isBlacklisted(item)) {
            return;
        }
        super.add(cleanSellerName(item));
    }

    public boolean removeSeller(String name) {
        if (name == null) {
            return false;
        }
        String target = this.getData().stream()
                .filter(seller -> normalizeSellerName(seller).equals(normalizeSellerName(name)))
                .findFirst()
                .orElse(null);
        return target != null && this.remove(target);
    }

    @Override
    public void load() {
        super.load();
        if (ensureDefaultIgnoredSellers()) {
            this.save();
        }
    }

    public void reloadBeforeAutoBuy() {
        this.load();
    }

    private boolean ensureDefaultIgnoredSellers() {
        if (DEFAULT_IGNORED_SELLERS == null) {
            return false;
        }
        boolean changed = false;
        for (String seller : DEFAULT_IGNORED_SELLERS) {
            if (!this.isBlacklisted(seller)) {
                super.add(cleanSellerName(seller));
                changed = true;
            }
        }
        return changed;
    }

    private String cleanSellerName(String name) {
        return name == null ? "" : name.replace("\\", "").replace("*", "").trim();
    }

    private String normalizeSellerName(String name) {
        return cleanSellerName(name).toLowerCase(Locale.ROOT);
    }
}
