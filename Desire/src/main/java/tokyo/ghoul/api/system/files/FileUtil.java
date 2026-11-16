package tokyo.ghoul.api.system.files;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@UtilityClass
public class FileUtil {
    private final Gson GSON = new Gson();

    public InputStream getFromAssets(String input) {
        return FileUtil.class.getResourceAsStream("/assets/" + ClientInfo.NAME.toLowerCase() + "/" + input);
    }

    public Identifier getImage(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), "images/" + path + ".png");
    }

    public Identifier getShader(String name) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), "core/" + name);
    }

    public <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
        return GSON.fromJson(toString(identifier), clazz);
    }

    public String toString(Identifier identifier) {
        return toString(identifier, "\n");
    }

    public String toString(Identifier identifier, String delimiter) {
        try(InputStream inputStream = Minecraft.getInstance().getResourceManager().open(identifier);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining(delimiter));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
