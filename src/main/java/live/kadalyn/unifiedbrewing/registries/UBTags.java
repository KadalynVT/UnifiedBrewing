package live.kadalyn.unifiedbrewing.registries;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class UBTags {
    public static final TagKey<Item> LADLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "ladle"));
    public static final TagKey<Item> NON_POTION_BREWABLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "non_potion_brewable"));
    public static final TagKey<Item> EMPTY_POTION_BOTTLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "empty_potion_bottle"));
    public static final TagKey<Item> INK_BOTTLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "ink_bottle"));
    public static final TagKey<Item> BLAZING_PROVIDER = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "blazing_provider"));
    public static final TagKey<Item> BLOOD_BOTTLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "blood_bottle"));
    public static final TagKey<Item> BLOOD_BUCKET = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, "blood_bucket"));
}
