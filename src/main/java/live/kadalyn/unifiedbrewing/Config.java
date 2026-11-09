package live.kadalyn.unifiedbrewing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_VANILLA = BUILDER
        .comment("Allow vanilla recipes to be brewed in all cauldrons")
        .define("allowVanillaInCauldrons", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_BLAZING = BUILDER
        .comment("Require water used for vanilla potions to be blaze-touched")
        .define("requireBlazeTouched", false);

    // True by default for balance with others <!--
    public static final ModConfigSpec.BooleanValue REQUIRE_INGREDIENTS = BUILDER
        .comment("Ensures everything that can be placed into a Small Cauldron is an ingredient for some potion")
        .define("ingredientsOnly", true);

    public static final ModConfigSpec.BooleanValue PREVENT_SPOILED = BUILDER
        .comment("Prevent using a ladle on Small Cauldrons which contain an invalid recipe")
        .define("preventSpoiled", true);

    public static final ModConfigSpec.BooleanValue DISABLE_SPOILED_POISON = BUILDER
        .comment("Disable the poison effect from spoiled brews")
        .define("disableSpoiledPoison", true);

    public static final ModConfigSpec.BooleanValue PREVENT_OVERCOOKING = BUILDER
        .comment("Prevent Small Cauldrons from overcooking their contents")
        .define("preventOvercooking", true);
    // -->

    // Used via UBConfigCondition
    public static final ModConfigSpec.BooleanValue HARD_FLASKS = BUILDER
        .comment("Make Empty Flask recipes require Blaze Powder")
        .define("difficultFlaskRecipes", false);

    public static final ModConfigSpec.IntValue POTION_STACK = BUILDER
            .comment("Stack size for vanilla potions")
            .defineInRange("potionStack", 4, 0, 64);

    public static final ModConfigSpec.IntValue FLASK_STACK = BUILDER
        .comment("Stack size for Flasks")
        .defineInRange("flaskStack", 4, 0, 64);

    public static final ModConfigSpec.IntValue BREW_STACK = BUILDER
        .comment("Stack size for brews")
        .defineInRange("brewStack", 4, 0, 64);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static <T> T get(String path) {
        return SPEC.getValues().get(path);
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
