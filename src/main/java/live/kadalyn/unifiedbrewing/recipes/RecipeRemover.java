package live.kadalyn.unifiedbrewing.recipes;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record RecipeRemover(ReloadableServerResources resources) implements ResourceManagerReloadListener {
	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		var recipeManager = resources.getRecipeManager();
		var toRemove = ResourceLocation.fromNamespaceAndPath(
			"herbalbrews", "cauldron_brewing/dummy_recipe");
		recipeManager.replaceRecipes(
			recipeManager.getOrderedRecipes().stream()
				.filter(x -> {
					if (x.id().equals(toRemove))
						UnifiedBrewing.LOGGER.info("removed dummy_recipe");
					return !x.id().equals(toRemove);
				})
				.toList());
	}
}
