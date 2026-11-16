package brova.toshiba.client.utils.render.font;

import java.awt.Font;
import java.io.InputStream;

public class FontLoader {

    public static Font getFontFromTTF(String text, float floatValue, int intValue) {
        try {
            String textValue = "/assets/toshiba/ttf/" + text;
            InputStream resourceAsStream = FontLoader.class.getResourceAsStream(textValue);
            if (resourceAsStream == null) {
                resourceAsStream = FontLoader.class.getResourceAsStream("/assets/toshiba/fonts/ttf/" + text);
            }
            if (resourceAsStream != null) {
                Font font = Font.createFont(intValue, resourceAsStream);
                font = font.deriveFont(floatValue);
                resourceAsStream.close();
                return font;
            } else {
                return null;
            }
        } catch (Exception failure) {
            failure.printStackTrace();
            return null;
        }
    }
}
