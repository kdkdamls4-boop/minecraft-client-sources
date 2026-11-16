package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class HeldItemRenderContext {
   public boolean mainItemChanged;
   public ItemStack item;
   public SubmitNodeCollector collector;
   public boolean mainSwingStarted;
   public boolean right;
   public boolean mainEvent;
   public boolean offSwingStarted;
   public float partialTick;
   public float swing;
   public float mainSwing;
   public boolean usingItem;
   public int light;
   public float delta;
   public float equip;
   public boolean main;
   public float offSwing;
   public float attackStrength = 1.0F;
   public boolean offEvent;
   public boolean breakingBlock;
   public AbstractClientPlayer player;
   public PoseStack pose;
   public float pitch;
   public boolean offItemChanged;
   public InteractionHand hand;
   public int side;
}
