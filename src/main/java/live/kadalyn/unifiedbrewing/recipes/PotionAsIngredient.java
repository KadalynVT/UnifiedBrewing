package live.kadalyn.unifiedbrewing.recipes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class PotionAsIngredient implements Predicate<ItemStack> {
	ItemStack item;

	public PotionAsIngredient(ItemStack item) {
		this.item = item;
	}

	@Override
	public boolean test(ItemStack itemStack) {
		var myPotionContents = item.get(DataComponents.POTION_CONTENTS);
		var theirPotionContents = itemStack.get(DataComponents.POTION_CONTENTS);
		return itemStack.is(item.getItem())
			&& myPotionContents != null && theirPotionContents != null
			&& myPotionContents.potion().isPresent()
			&& theirPotionContents.is(myPotionContents.potion().get());
	}
}
