package live.kadalyn.unifiedbrewing.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {
	@WrapMethod(method = "mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;")
	private ItemStack mix(
		ItemStack inputItem,
		ItemStack outputPotion,
		Operation<ItemStack> original
	) {
		ItemStack mixed = original.call(inputItem, outputPotion);

		if (Util.oneOf(mixed, Potions.MUNDANE)) {
			mixed.set(
				UBDataComponents.MUNDANE_INGREDIENT.get(),
				(inputItem.is(Tags.Items.GUNPOWDERS) || inputItem.is(Items.DRAGON_BREATH))
					? outputPotion.get(UBDataComponents.MUNDANE_INGREDIENT.get())
					: UBDataComponents.SingleItemComponent.of(inputItem)
			);
		}
		return mixed;
	}
}
