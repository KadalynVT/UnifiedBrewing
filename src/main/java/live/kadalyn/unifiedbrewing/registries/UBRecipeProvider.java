package live.kadalyn.unifiedbrewing.registries;

import biomesoplenty.api.BOPAPI;
import biomesoplenty.api.item.BOPItems;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.mojang.datafixers.util.Pair;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.common.conditions.ItemExistsCondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
public class UBRecipeProvider extends RecipeProvider {
	public UBRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void buildRecipes(RecipeOutput recipeOutput) {
		// Hexalia
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, UBItems.BLAZING_LADLE.get())
			.pattern("  B")
			.pattern(" R ")
			.pattern("R  ")
			.define('R', Items.BLAZE_ROD)
			.define('B', Items.GLASS_BOTTLE)
			.unlockedBy("has_blaze_rod", inventoryTrigger(ItemPredicate.Builder.item().of(Items.BLAZE_ROD).build()))
			.save(recipeOutput);

		// Herbal Brews
		List<List<String>> shapes = List.of(
			List.of(),
			List.of(
				" b ",
				"xxx"
			),
			List.of(
				"xb",
				"x ",
				"x "
			),
			List.of(
				"xb",
				"xx"
			),
			List.of(
				" b ",
				" x ",
				"x x"
			),
			List.of(
				" b ",
				"x x",
				" x "
			),
			List.of(
				" x",
				"xb",
				" x"
			)
		);
		for (int i = 1; i <= 6; i++) {
			ItemStack result = new ItemStack(UBItems.EMPTY_FLASK);
			result.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(i));
			var baseId = RecipeBuilder.getDefaultRecipeId(result.getItem());

			var easy = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result);
			var hard = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result);
			for (var s : shapes.get(i)) {
				easy = easy.pattern(s.replace('b', ' '));
				hard = hard.pattern(s);
			}
			easy = easy.unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE));
			hard = hard.define('b', Items.BLAZE_POWDER)
				.unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER));

			for (var r : List.of(
				Pair.of(easy, "false"),
				Pair.of(hard, "true")
			)) {
				var s = r.getSecond();
				var name = s.equals("false") ? "easy" : "hard";
				r.getFirst().define('x', Items.GLASS_BOTTLE)
					.save(recipeOutput.withConditions(
						new ItemExistsCondition("herbalbrews", "flask"),
						new UBConfigCondition("difficultFlaskRecipes", s)
					), baseId.withSuffix("_" + name + "_" + i));
			}
		}

		// Alex's Mobs
		var bloodSac = AMItemRegistry.BLOOD_SAC.get();
		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.BLOOD_VIAL.get(), 3)
			.requires(bloodSac)
			.requires(Items.GLASS_BOTTLE, 3)
			.unlockedBy("has_blood_sac", inventoryTrigger(ItemPredicate.Builder.item().of(bloodSac).build()))
			.save(recipeOutput.withConditions(
				new ItemExistsCondition(AMItemRegistry.BLOOD_SAC.getId()),
				new ItemExistsCondition(ItemRegistry.BLOOD_VIAL.getId())
			));

		// Biomes o' Plenty
		var bloodBucket = BOPItems.BLOOD_BUCKET;
		var bloodStack = new FluidStack(FluidRegistry.BLOOD, 1000);
		new FillAlchemistCauldronRecipe.Builder()
			.withFluid(bloodStack)
			.withInput(bloodBucket)
			.withReturnItem(Items.BUCKET)
			.save(recipeOutput.withConditions(
				new ItemExistsCondition(BOPAPI.MOD_ID, "blood_bucket"),
				new ModLoadedCondition(IronsSpellbooks.MODID)
			));
		new EmptyAlchemistCauldronRecipe.Builder()
			.withFluid(bloodStack)
			.withInput(Items.BUCKET)
			.withReturnItem(bloodBucket)
			.save(recipeOutput.withConditions(
				new ItemExistsCondition(BOPAPI.MOD_ID, "blood_bucket"),
				new ModLoadedCondition(IronsSpellbooks.MODID)
			));
	}
}
