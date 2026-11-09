package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.Tags;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/satisfy/herbalbrews/client/gui/handler/CauldronGuiHandler$FuelSlot")
public class FuelSlotMixin {
	@WrapMethod(method = "mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z")
	private static boolean mayPlaceItem(ItemStack itemStack, Operation<Boolean> original) {
		return (
			itemStack.is(ObjectRegistry.HERBAL_INFUSION)
			|| IronsCompat.isScroll(itemStack)
			|| itemStack.is(UBTags.BLAZING_PROVIDER)
			|| Util.isPotionIngredient(itemStack)  // blaze powder in here
			|| itemStack.is(Tags.Items.POTIONS)
			&& Util.oneOf(itemStack, Potions.MUNDANE)
		);
	}
}
