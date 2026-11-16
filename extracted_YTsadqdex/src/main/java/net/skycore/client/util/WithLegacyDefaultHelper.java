package net.skycore.client.util;

import com.mojang.math.Transformation;
import java.util.Optional;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WithLegacyDefaultHelper {
   private WithLegacyDefaultHelper() {
   }

   public static Optional<Transformation> withLegacyDefault(Optional<Transformation> optional) {
      if (optional.isPresent()) {
         return optional;
      }

      Vector3f vector = new Vector3f(0.5F, 0.0F, 0.5F);
      Quaternionf rotation = new Quaternionf(1.0F, 0.0F, 0.0F, 0.0F);
      Vector3f vector2 = new Vector3f(1.0F);
      Quaternionf rotation2 = new Quaternionf();
      Transformation transformation = new Transformation(vector, rotation, vector2, rotation2);
      return Optional.of(transformation);
   }
}
