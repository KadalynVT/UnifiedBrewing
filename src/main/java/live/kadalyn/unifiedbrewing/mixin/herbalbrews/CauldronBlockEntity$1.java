package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.satisfy.herbalbrews.core.blocks.entity.CauldronBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/satisfy/herbalbrews/core/blocks/entity/CauldronBlockEntity$1")
public class CauldronBlockEntity$1 {
	@Shadow(aliases = "this$0") @Final
	private CauldronBlockEntity this$0;

	@WrapMethod(method = "get(I)I")
	public int get(int index, Operation<Integer> original) {
		if (index == 2) return ((Util.ICauldronBlockEntityExtras) (Object) this$0).unifiedbrewing$getFuelRemaining();
		return original.call(index);
	}

	@WrapMethod(method = "set(II)V")
	public void set(int index, int value, Operation<Void> original) {
		if (index == 2)
			((Util.ICauldronBlockEntityExtras) (Object) this$0).unifiedbrewing$setFuelRemaining(value);
		else
			original.call(index, value);
	}

	@WrapMethod(method = "getCount()I")
	public int getCount(Operation<Void> original) {
		return 3;
	}
}
