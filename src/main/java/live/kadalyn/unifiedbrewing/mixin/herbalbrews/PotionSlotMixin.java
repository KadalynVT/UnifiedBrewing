package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/satisfy/herbalbrews/client/gui/handler/CauldronGuiHandler$PotionSlot")
public abstract class PotionSlotMixin extends Slot {
	public PotionSlotMixin(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@WrapMethod(method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z")
	private boolean mayPlace(ItemStack itemStack, Operation<Boolean> original) {
		var fuel = container.getItem(4);
		if (fuel.is(ObjectRegistry.HERBAL_INFUSION) || fuel.isEmpty())
			return itemStack.is(Tags.Items.POTIONS);
		return Util.isPotionIngredient(itemStack)
			|| IronsCompat.isScroll(itemStack);
	}

	@WrapMethod(method = "mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z")
	private static boolean mayPlaceItem(ItemStack itemStack, Operation<Boolean> original) {
		return itemStack.is(Tags.Items.POTIONS)
			|| Util.isPotionIngredient(itemStack)
			|| IronsCompat.isScroll(itemStack);
	}

	@WrapMethod(method = "getMaxStackSize()I")
	private int getMaxStackSize(Operation<Integer> original) {
		return 99;
	}
}
