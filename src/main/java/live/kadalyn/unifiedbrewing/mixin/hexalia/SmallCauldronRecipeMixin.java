package live.kadalyn.unifiedbrewing.mixin.hexalia;

import live.kadalyn.unifiedbrewing.mixin.Util;
import net.astralya.hexalia.recipe.SmallCauldronRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SmallCauldronRecipe.class)
public class SmallCauldronRecipeMixin implements Util.ISmallCauldronRecipeExtras {
	@Unique
	private int unifiedbrewing$blazeCharges = 0;

	@Unique
	public int unifiedbrewing$getBlazeCharges() {
		return unifiedbrewing$blazeCharges;
	}

	@Unique
	public void unifiedbrewing$setBlazeCharges(int blazeCharges) {
		this.unifiedbrewing$blazeCharges = blazeCharges;
	}
}
