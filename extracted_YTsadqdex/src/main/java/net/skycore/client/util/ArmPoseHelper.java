package net.skycore.client.util;

public final class ArmPoseHelper {
   public static double easeInOutQuint(double progress) {
      return progress < 0.5 ? 16.0 * progress * progress * progress * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 5.0) / 2.0;
   }

   public static double easeInOutBack(double progress) {
      double value = 1.70158F;
      double computedValue = value * 1.525F;
      if (progress < 0.5) {
         return Math.pow(2.0 * progress, 2.0) * ((computedValue + 1.0) * 2.0 * progress - computedValue) / 2.0;
      }

      double computedValue2 = 2.0 * progress - 2.0;
      return (Math.pow(computedValue2, 2.0) * ((computedValue + 1.0) * (progress * 2.0 - 2.0) + computedValue) + 2.0) / 2.0;
   }

   public static double easeOutBounce(double progress) {
      double value = 7.5625;
      double value2 = 2.75;
      if (progress < 1.0 / value2) {
         return value * progress * progress;
      } else if (progress < 2.0 / value2) {
         double value3;
         return value * (value3 = progress - 1.5 / value2) * value3 + 0.75;
      } else {
         double value4;
         double value5;
         return progress < 2.5 / value2 ? value * (value4 = progress - 2.25 / value2) * value4 + 0.9375 : value * (value5 = progress - 2.625 / value2) * value5 + 0.984375;
      }
   }

   public static double easeInOutBounce(double progress) {
      return progress < 0.5 ? (1.0 - easeOutBounce(1.0 - 2.0 * progress)) / 2.0 : (1.0 + easeOutBounce(2.0 * progress - 1.0)) / 2.0;
   }

   public static double easeInBounce(double progress) {
      return 1.0 - easeOutBounce(1.0 - progress);
   }

   public static double easeOutCubic(double progress) {
      return 1.0 - Math.pow(1.0 - progress, 3.0);
   }

   private ArmPoseHelper() {
   }

   public static double easeInElastic(double progress) {
      double computedValue = Math.PI * 2.0 / 3.0;
      if (progress == 0.0) {
         return 0.0;
      }

      if (progress == 1.0) {
         return 1.0;
      }

      double intermediate = -Math.pow(2.0, 10.0 * progress - 10.0);
      double intermediate2 = (progress * 10.0 - 10.75) * computedValue;
      return intermediate * Math.sin(intermediate2);
   }

   public static double easeOutSine(double progress) {
      double computedValue = progress * Math.PI / 2.0;
      return Math.sin(computedValue);
   }

   public static double easeOutQuad(double progress) {
      return 1.0 - (1.0 - progress) * (1.0 - progress);
   }

   public static double easeOutQuart(double progress) {
      double computedValue = 1.0 - progress;
      return 1.0 - Math.pow(computedValue, 4.0);
   }

   public static double easeInQuint(double progress) {
      return progress * progress * progress * progress * progress;
   }

   public static double easeOutExpo(double progress) {
      if (progress == 1.0) {
         return 1.0;
      }

      double intermediate = -10.0 * progress;
      return 1.0 - Math.pow(2.0, intermediate);
   }

   public static double easeOutCirc(double progress) {
      double computedValue = 1.0 - Math.pow(progress - 1.0, 2.0);
      return Math.sqrt(computedValue);
   }

   public static double easeInBack(double progress) {
      double value = 1.70158F;
      double computedValue = value + 1.0;
      return computedValue * progress * progress * progress - value * progress * progress;
   }

   public static double easeOutQuint(double progress) {
      return 1.0 - Math.pow(1.0 - progress, 5.0);
   }

   public static double easeInOutExpo(double progress) {
      if (progress == 0.0) {
         return 0.0;
      }

      if (progress == 1.0) {
         return 1.0;
      }

      if (progress < 0.5) {
         return Math.pow(2.0, 20.0 * progress - 10.0) / 2.0;
      }

      double intermediate = -20.0 * progress + 10.0;
      return (2.0 - Math.pow(2.0, intermediate)) / 2.0;
   }

   public static double easeInOutQuart(double progress) {
      if (progress < 0.5) {
         return 8.0 * progress * progress * progress * progress;
      }

      double computedValue = -2.0 * progress + 2.0;
      return 1.0 - Math.pow(computedValue, 4.0) / 2.0;
   }

   public static double easeInExpo(double progress) {
      if (progress == 0.0) {
         return 0.0;
      }

      double intermediate = 10.0 * progress - 10.0;
      return Math.pow(2.0, intermediate);
   }

   public static double easeInOutCubic(double progress) {
      return progress < 0.5 ? 4.0 * progress * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 3.0) / 2.0;
   }

   public static double easeInOutElastic(double progress) {
      double computedValue = Math.PI * 4.0 / 9.0;
      if (progress == 0.0) {
         return 0.0;
      } else if (progress == 1.0) {
         return 1.0;
      } else if (progress < 0.5) {
         double computedValue2 = 20.0 * progress - 10.0;
         return -(Math.pow(2.0, computedValue2) * Math.sin((20.0 * progress - 11.125) * computedValue)) / 2.0;
      } else {
         return Math.pow(2.0, -20.0 * progress + 10.0) * Math.sin((20.0 * progress - 11.125) * computedValue) / 2.0 + 1.0;
      }
   }

   public static double easeInOutSine(double progress) {
      double computedValue = Math.PI * progress;
      return -(Math.cos(computedValue) - 1.0) / 2.0;
   }

   public static double easeInQuad(double progress) {
      return progress * progress;
   }

   public static double easeInCubic(double progress) {
      return progress * progress * progress;
   }

   public static double easeInSine(double progress) {
      return 1.0 - Math.cos(progress * Math.PI / 2.0);
   }

   public static double smoothStep(double progress) {
      return progress * progress * (3.0 - 2.0 * progress);
   }

   public static double easeInQuart(double progress) {
      return progress * progress * progress * progress;
   }

   public static double easeInOutQuad(double progress) {
      return progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
   }

   public static double easeOutElastic(double progress) {
      double computedValue = Math.PI * 2.0 / 3.0;
      if (progress == 0.0) {
         return 0.0;
      } else {
         return progress == 1.0 ? 1.0 : Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * computedValue) + 1.0;
      }
   }

   public static double easeInOutCirc(double progress) {
      if (progress < 0.5) {
         double intermediate = 2.0 * progress;
         return (1.0 - Math.sqrt(1.0 - Math.pow(intermediate, 2.0))) / 2.0;
      } else {
         return (Math.sqrt(1.0 - Math.pow(-2.0 * progress + 2.0, 2.0)) + 1.0) / 2.0;
      }
   }

   public static double easeOutBack(double progress) {
      double value = 1.70158F;
      double computedValue = value + 1.0;
      double intermediate = progress - 1.0;
      double intermediate2 = 1.0 + computedValue * Math.pow(intermediate, 3.0);
      intermediate = progress - 1.0;
      return intermediate2 + value * Math.pow(intermediate, 2.0);
   }

   public static double easeInCirc(double progress) {
      double computedValue = 1.0 - Math.pow(progress, 2.0);
      return 1.0 - Math.sqrt(computedValue);
   }
}
