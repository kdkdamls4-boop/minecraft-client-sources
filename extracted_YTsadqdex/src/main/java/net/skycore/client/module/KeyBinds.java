package net.skycore.client.module;

import java.util.Locale;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {
   public static final int MOUSE_BASE = 1000;

   public static boolean isPressed(long timestamp, int keyCode) {
      if (keyCode < 0) {
         return false;
      } else if (keyCode >= 1000) {
         int intermediate = keyCode - 1000;
         return GLFW.glfwGetMouseButton(timestamp, intermediate) == 1;
      } else {
         return GLFW.glfwGetKey(timestamp, keyCode) == 1;
      }
   }

   public static boolean isPrimaryMouseButton(int value) {
      return value == 1000 || value == 1001;
   }

   private KeyBinds() {
   }

   public static String name(int value) {
      if (value < 0) {
         return "None";
      }

      if (value >= 1000) {
         return "M" + (value - 1000 + 1);
      }

      if (value >= 290 && value <= 314) {
         return "F" + (value - 290 + 1);
      }

      return switch (value) {
         case 32 -> "Space";
         case 256 -> "Esc";
         case 257 -> "Enter";
         case 258 -> "Tab";
         case 259 -> "Backspace";
         case 260 -> "Insert";
         case 261 -> "Delete";
         case 262 -> "Right";
         case 263 -> "Left";
         case 264 -> "Down";
         case 265 -> "Up";
         case 266 -> "PageUp";
         case 267 -> "PageDown";
         case 268 -> "Home";
         case 269 -> "End";
         case 280 -> "CapsLock";
         case 281 -> "ScrollLock";
         case 282 -> "NumLock";
         case 283 -> "PrintScreen";
         case 284 -> "Pause";
         case 340 -> "LShift";
         case 341 -> "LCtrl";
         case 342 -> "LAlt";
         case 344 -> "RShift";
         case 345 -> "RCtrl";
         case 346 -> "RAlt";
         default -> {
            if (value >= 65 && value <= 90) {
               yield Character.toString((char)(65 + value - 65));
            } else if (value >= 48 && value <= 57) {
               yield Character.toString((char)(48 + value - 48));
            } else {
               yield switch (value) {
                  case 39 -> "'";
                  case 44 -> ",";
                  case 45 -> "-";
                  case 46 -> ".";
                  case 47 -> "/";
                  case 59 -> ";";
                  case 61 -> "=";
                  case 91 -> "[";
                  case 92 -> "\\";
                  case 93 -> "]";
                  case 96 -> "`";
                  default -> {
                     String text = GLFW.glfwGetKeyName(value, 0);
                     yield text == null ? "Key " + value : text.toUpperCase(Locale.ROOT);
                  }
               };
            }
         }
      };
   }

   public static int parse(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = text.trim();
         String text3 = text2.toUpperCase(Locale.ROOT);
         if (text3.matches("M\\d+")) {
            try {
               int computedValue = Integer.parseInt(text3.substring(1)) - 1;
               if (computedValue >= 0 && computedValue <= 7) {
                  return 1000 + computedValue;
               }
            } catch (NumberFormatException failure) {
            }
         }

         if (text3.matches("F\\d+")) {
            try {
               int value = Integer.parseInt(text3.substring(1));
               if (value >= 1 && value <= 25) {
                  return 290 + (value - 1);
               }
            } catch (NumberFormatException failure2) {
            }
         }
         return switch (text3) {
            case "RSHIFT", "RIGHT_SHIFT" -> 344;
            case "LSHIFT", "LEFT_SHIFT", "SHIFT" -> 340;
            case "RCTRL", "RCONTROL", "RIGHT_CONTROL" -> 345;
            case "LCTRL", "LCONTROL", "LEFT_CONTROL", "CTRL", "CONTROL" -> 341;
            case "RALT", "RIGHT_ALT" -> 346;
            case "LALT", "LEFT_ALT", "ALT" -> 342;
            case "SPACE" -> 32;
            case "ENTER", "RETURN" -> 257;
            case "TAB" -> 258;
            case "ESC", "ESCAPE" -> 256;
            case "BACKSPACE", "BACK" -> 259;
            case "DELETE", "DEL" -> 261;
            case "INSERT", "INS" -> 260;
            case "HOME" -> 268;
            case "END" -> 269;
            case "PAGEUP", "PGUP" -> 266;
            case "PAGEDOWN", "PGDN" -> 267;
            case "UP" -> 265;
            case "DOWN" -> 264;
            case "LEFT" -> 263;
            case "RIGHT" -> 262;
            default -> {
               keyNameLookup: {
                  if (text3.length() == 1) {
                     char character = text3.charAt(0);
                     if (character >= 'A' && character <= 'Z') {
                        yield 65 + (character - 'A');
                     }

                     if (character >= '0' && character <= '9') {
                        yield 48 + (character - '0');
                     }
                  }

                  int value2 = 32;

                  while (true) {
                     if (value2 > 348) {
                        yield -1;
                     }

                     if (name(value2).equalsIgnoreCase(text2)) {
                        yield value2;
                     }

                     value2++;
                  }
               }
            }
         };
      } else {
         return -1;
      }
   }
}
