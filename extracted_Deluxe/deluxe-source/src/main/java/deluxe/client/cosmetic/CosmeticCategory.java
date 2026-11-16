package deluxe.client.cosmetic;

public enum CosmeticCategory {
   BODYWEAR("Рюкзаки", "Рюкзак", "bodywear", "cos_bodywear.png"),
   WINGS("Крылья", "Крылья", "wings", "cos_wings.png"),
   HAT("Головные уборы", "Убор", "hat", "cos_hat.png"),
   ACCESSORY("Аксессуары", "Аксессуар", "accessory", "cos_accessory.png"),
   PET("Питомцы", "Питомец", "pet", "cos_pet.png"),
   SWORD("Мечи", "Меч", "sword", "cos_sword.png", true),
   MACE("Булавы", "Булава", "mace", "cos_mace.png", true),
   SHIELD("Щиты", "Щит", "shield", "cos_shield.png", true);

   private final String field14600;
   private final String field14601;
   private final String field14602;
   private final String field14603;
   private final boolean field14604;

   private CosmeticCategory(String local7227_3, String local7227_4, String local7227_5, String local7227_6) {
      this(local7227_3, local7227_4, local7227_5, local7227_6, false);
   }

   private CosmeticCategory(String local7228_3, String local7228_4, String local7228_5, String local7228_6, boolean local7228_7) {
      this.field14600 = local7228_3;
      this.field14601 = local7228_4;
      this.field14602 = local7228_5;
      this.field14603 = "assets/deluxe/textures/gui/mainmenu/icons/" + local7228_6;
      this.field14604 = local7228_7;
   }

   public boolean method4308() {
      return this.field14604;
   }

   public String method4309() {
      return this.field14600;
   }

   public String method4310() {
      return this.field14601;
   }

   public String method4311() {
      return this.field14602;
   }

   public String method4312() {
      return this.field14603;
   }
}
