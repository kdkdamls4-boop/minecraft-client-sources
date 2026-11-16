package deluxe.client.render;

import deluxe.client.util.GuiScale;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2f;

public final class ScissorStack {
   private static final ScreenRect field13693 = new ScreenRect(0, 0, 0, 0);
   private static final Deque<ScreenRect> field13694 = new ArrayDeque<>();

   public static void method3967(Matrix3x2f matrix3x2f, float local6584_1, float local6584_2, float local6584_3, float local6584_4) {
      if (matrix3x2f == null) {
         method3968(local6584_1, local6584_2, local6584_3, local6584_4);
      } else {
         ScreenRect screenRect = new ScreenRect(
               Math.round(local6584_1), Math.round(local6584_2), Math.max(0, Math.round(local6584_3)), Math.max(0, Math.round(local6584_4))
            )
            .transformEachVertex(matrix3x2f);
         method3970(screenRect);
      }
   }

   public static void method3968(float local6586_0, float local6586_1, float local6586_2, float local6586_3) {
      int local6586_4 = GuiScale.method0798(local6586_0);
      int local6586_5 = GuiScale.method0798(local6586_1);
      int local6586_6 = GuiScale.method0798(local6586_0 + local6586_2);
      int local6586_7 = GuiScale.method0798(local6586_1 + local6586_3);
      method3970(new ScreenRect(local6586_4, local6586_5, Math.max(0, local6586_6 - local6586_4), Math.max(0, local6586_7 - local6586_5)));
   }

   public static ScreenRect method3969() {
      return field13694.peek();
   }

   private ScissorStack() {
   }

   private static void method3970(ScreenRect screenRect) {
      ScreenRect screenRect2 = field13694.peek();
      ScreenRect screenRect3 = screenRect2 == null ? screenRect : screenRect2.intersection(screenRect);
      field13694.push(screenRect3 == null ? field13693 : screenRect3);
   }

   public static void method3971() {
      field13694.clear();
   }

   public static void method3972() {
      if (!field13694.isEmpty()) {
         field13694.pop();
      }
   }
}
