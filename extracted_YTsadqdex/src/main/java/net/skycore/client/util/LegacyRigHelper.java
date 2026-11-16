package net.skycore.client.util;

import net.minecraft.resources.Identifier;

public enum LegacyRigHelper {
   DOG("Dog", "dog_mesh.json", "dog.png", 1.0F, false, true),
   DRAGON("Dragon", "dragon_mesh.json", "dragon.png", 1.0F, true, true),
   SPIKE("Spike", "spike_mesh.json", "spike.png", 1.18F, false, false),
   MINUN("Minun", "minun_mesh.json", "minun.png", 0.9F, false, false),
   PENGUIN("Mama ptica", "penguin_mesh.json", "penguin.png", 0.75F, false, false),
   ANGRY_BIRD("AngryBird", "angry_birds_mesh.json", "angry_birds.png", 0.85F, false, false);

   private final Identifier texture;
   private final boolean legacyRig;
   private final String label;
   private final float modelScale;
   private final Identifier mesh;
   private final boolean flying;

   public boolean legacyRig() {
      return this.legacyRig;
   }

   public float modelScale() {
      return this.modelScale;
   }

   public Identifier mesh() {
      return this.mesh;
   }

   public Identifier texture() {
      return this.texture;
   }

   public static LegacyRigHelper fromLabel(String text) {
      if (text != null) {
         for (LegacyRigHelper legacyRigHelper : values()) {
            if (legacyRigHelper.label.equalsIgnoreCase(text)) {
               return legacyRigHelper;
            }
         }
      }

      return DOG;
   }

   public String label() {
      return this.label;
   }

   public boolean flying() {
      return this.flying;
   }

   LegacyRigHelper(String label2, String text, String text2, float modelScale2, boolean flying2, boolean legacyRig2) {
      this.label = label2;
      this.mesh = Identifier.parse("skycore:models/pet/" + text);
      this.texture = Identifier.parse("skycore:textures/pet/" + text2);
      this.modelScale = modelScale2;
      this.flying = flying2;
      this.legacyRig = legacyRig2;
   }
}
