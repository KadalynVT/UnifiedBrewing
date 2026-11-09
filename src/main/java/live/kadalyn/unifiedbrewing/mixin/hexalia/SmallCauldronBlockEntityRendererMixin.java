package live.kadalyn.unifiedbrewing.mixin.hexalia;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.astralya.hexalia.block.entity.custom.SmallCauldronBlockEntity;
import net.astralya.hexalia.client.renderer.blockentity.SmallCauldronBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Collectors;

@Mixin(SmallCauldronBlockEntityRenderer.class)
public class SmallCauldronBlockEntityRendererMixin {
	@Shadow @Final
	private static ResourceLocation WATER_STILL;

	@Inject(method = "renderLiquid(Lnet/astralya/hexalia/block/entity/custom/SmallCauldronBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
	private void renderLiquid(
		SmallCauldronBlockEntity blockEntity,
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		CallbackInfo ci,
		@Share("blockEntity") LocalRef<SmallCauldronBlockEntity> blockEntityRef
	) {
		blockEntityRef.set(blockEntity);
	}

	@Redirect(
		method = "renderLiquid(Lnet/astralya/hexalia/block/entity/custom/SmallCauldronBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(
			value = "FIELD",
			target = "Lnet/astralya/hexalia/client/renderer/blockentity/SmallCauldronBlockEntityRenderer;WATER_STILL:Lnet/minecraft/resources/ResourceLocation;",
			opcode = Opcodes.GETSTATIC
		)
	)
	private ResourceLocation getWATER_STILL(@Share("blockEntity") LocalRef<SmallCauldronBlockEntity> blockEntityRef) {
		var blockEntity = blockEntityRef.get();
		if (blockEntity != null) {
			return ((Util.ISmallCauldronBlockEntityExtras) (Object) blockEntity).unifiedbrewing$getMixtureTexture();
		}
		return WATER_STILL;
	}
}
