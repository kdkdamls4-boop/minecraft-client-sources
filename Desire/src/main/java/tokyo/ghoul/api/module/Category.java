package tokyo.ghoul.api.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    OTHER("Display"),
    AUTOBUY("AutoBuy");

    private final String label;
}
