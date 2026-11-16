package tokyo.ghoul.client.features.modules.autobuy.engine;

import tokyo.ghoul.client.features.modules.autobuy.engine.items.EmbeddedParsedItemTemplates;

import java.util.List;

public final class ParsedItemRegistry {
   public static final List<ParsedItemTemplate> ALL = EmbeddedParsedItemTemplates.ALL;

   private ParsedItemRegistry() {
   }
}
