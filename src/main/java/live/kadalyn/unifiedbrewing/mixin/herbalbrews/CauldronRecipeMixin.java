package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat.CauldronRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.satisfy.herbalbrews.core.recipe.CauldronRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@Mixin(CauldronRecipe.class)
public abstract class CauldronRecipeMixin {
	@Shadow
	public abstract boolean matches(RecipeInput recipeInput, Level level);

	@WrapMethod(method = "matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z")
	public boolean matches(RecipeInput recipeInput, Level level, Operation<Boolean> original) {
		CauldronRecipe self = (CauldronRecipe) (Object) this;
		if (recipeInput instanceof CauldronRecipeInput(List<ItemStack> items)) {
			var ingredients = self.getIngredients();
			if (ingredients.size() != items.size())
				return false;
			var size = ingredients.size();

			// List.of(ingredient => int[]{matching inputs})
			var valid = ingredients.stream().map(
				ingredient -> IntStream.range(0, size).filter(
					i -> ingredient.test(items.get(i))).toArray()).toList();

			// If any ingredients have no matches, fail
			if (valid.stream().anyMatch(i -> i.length == 0))
				return false;

			// Permutate matches looking for a full straight
			int i;
			int[] pos = new int[size], match = IntStream.range(0, size).toArray();
			do {
				var test = IntStream.range(0, size).map(j -> valid.get(j)[pos[j]]).sorted().toArray();
				if (Arrays.equals(test, match))
					return true;
				for (i = 0; i < size && ++pos[i] >= valid.get(i).length; i++) {
					pos[i] = 0;
				}
			} while (i < size);
		}
		return false;
	}

	@WrapMethod(method = "assemble(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;")
	public ItemStack assemble(
		RecipeInput recipeInput,
		HolderLookup.Provider provider,
		Operation<ItemStack> original
	) {
		CauldronRecipe self = (CauldronRecipe) (Object) this;
		return self.getResultItem(provider);  // already copied
	}
}
