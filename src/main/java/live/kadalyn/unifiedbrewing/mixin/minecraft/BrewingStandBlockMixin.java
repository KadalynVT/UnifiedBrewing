package live.kadalyn.unifiedbrewing.mixin.minecraft;

import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(BrewingStandBlock.class)
public class BrewingStandBlockMixin {
	@Shadow @Final @Mutable
	public static BooleanProperty[] HAS_BOTTLE;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void addHasBottle(CallbackInfo ci) {
		int idx = HAS_BOTTLE.length;
		HAS_BOTTLE = Arrays.copyOf(HAS_BOTTLE, idx + 1);
		HAS_BOTTLE[idx] = BooleanProperty.create("has_bottle_3");
	}

	@ModifyArg(
		method = "<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BrewingStandBlock;registerDefaultState(Lnet/minecraft/world/level/block/state/BlockState;)V")
	)
	public BlockState init$addSlot5(BlockState state) {
		return state.setValue(HAS_BOTTLE[3], false);
	}

	@ModifyArg(
		method = "createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/StateDefinition$Builder;add([Lnet/minecraft/world/level/block/state/properties/Property;)Lnet/minecraft/world/level/block/state/StateDefinition$Builder;"
		)
	)
	protected Property<?>[] createBlockStateDefinition(Property<?>[] properties) {
		int idx = properties.length;
		properties = Arrays.copyOf(properties, idx + 1);
		properties[idx] = HAS_BOTTLE[3];
		return properties;
	}
}
