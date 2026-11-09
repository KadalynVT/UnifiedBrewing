package live.kadalyn.unifiedbrewing.registries;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public class UBTagsProvider extends TagsProvider<Item> {
	public UBTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper fileHelper) {
		super(output, Registries.ITEM, registries, UnifiedBrewing.MODID, fileHelper);
	}

	private Function<String, ResourceLocation> location(String namespace) {
		return x -> ResourceLocation.fromNamespaceAndPath(namespace, x);
	}

	private ResourceLocation location(String namespace, String name) {
		return ResourceLocation.fromNamespaceAndPath(namespace, name);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		var hexalia = location("hexalia");
		this.tag(UBTags.LADLE)
			.add(UBItems.BLAZING_LADLE.getKey())
			.addOptional(hexalia.apply("ladle"))
			.addOptional(location("farmers_spell", "cherry_spoon"));

		var irons = location("irons_spellbooks");
		for (var tag : List.of(Tags.Items.POTIONS, Tags.Items.POTION_BOTTLE)) {
			this.tag(tag);
		}

		this.tag(UBTags.NON_POTION_BREWABLE)
			.addOptional(irons.apply("oakskin_elixir"))
			.addOptional(irons.apply("greater_oakskin_elixir"))
			.addOptional(irons.apply("greater_healing_potion"))
			.addOptional(irons.apply("invisibility_elixir"))
			.addOptional(irons.apply("greater_invisibility_elixir"))
			.addOptional(irons.apply("evasion_elixir"))
			.addOptional(irons.apply("greater_evasion_elixir"))
			.addOptional(irons.apply("ice_venom"))
			.addOptional(irons.apply("timeless_slurry"))
			.addOptional(irons.apply("common_ink"))
			.addOptional(irons.apply("uncommon_ink"))
			.addOptional(irons.apply("rare_ink"))
			.addOptional(irons.apply("epic_ink"))
			.addOptional(irons.apply("legendary_ink"));

		this.tag(UBTags.EMPTY_POTION_BOTTLE)
			.add(Items.GLASS_BOTTLE.builtInRegistryHolder().getKey())
			.add(UBItems.EMPTY_FLASK.getKey())
			.addOptional(hexalia.apply("rustic_bottle"));

		this.tag(UBTags.INK_BOTTLE)
			.addOptional(irons.apply("common_ink"))
			.addOptional(irons.apply("uncommon_ink"))
			.addOptional(irons.apply("rare_ink"))
			.addOptional(irons.apply("epic_ink"))
			.addOptional(irons.apply("legendary_ink"));

		this.tag(UBTags.BLAZING_PROVIDER)
			.add(UBItems.DEPLETED_BLAZE_POWDER.getKey());

		this.tag(UBTags.BLOOD_BOTTLE)
			.addOptional(irons.apply("blood_vial"));

		this.tag(UBTags.BLOOD_BUCKET)
			.addOptional(location("biomesoplenty", "blood_bucket"));
	}
}
