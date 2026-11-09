package live.kadalyn.unifiedbrewing.mixin.hexalia;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.Config;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.registries.UBItems;
import net.astralya.hexalia.block.entity.custom.SmallCauldronBlockEntity;
import net.astralya.hexalia.gameplay.smallcauldron.SmallCauldronContents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SmallCauldronBlockEntity.class)
public abstract class SmallCauldronBlockEntityMixin extends BlockEntity implements Util.ISmallCauldronBlockEntityExtras {
	@Shadow @Final
	private SmallCauldronContents contents;

	@Shadow
	protected abstract double centerX();

	@Shadow
	protected abstract double centerY();

	@Shadow
	protected abstract double centerZ();

	@Shadow
	protected abstract void syncIfNeeded();

	public SmallCauldronBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	@WrapMethod(method = "canStir(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Z")
	public boolean canStir(BlockState state, Player player, Operation<Boolean> original) {
		var result = original.call(state, player);
		Util.ISmallCauldronContentsExtras c = (Util.ISmallCauldronContentsExtras) (Object) contents;
		if (level == null || c == null || !result) return result;

		if (Config.PREVENT_SPOILED.getAsBoolean()) {
			var recipe = c.unifiedbrewing$findRecipeMatch(level);
			if (recipe.isPresent()) {
				var r = (Util.ISmallCauldronRecipeExtras) (Object) recipe.get().value();
				if (r.unifiedbrewing$getBlazeCharges() > 0) {
					var held = player.getMainHandItem();
					return !Config.REQUIRE_BLAZING.getAsBoolean()
						|| held.is(UBItems.BLAZING_LADLE)
						&& held.getDamageValue() + r.unifiedbrewing$getBlazeCharges() - c.getStirsDone() < held.getMaxDamage();
				}
				return true;
			}
			return false;
		}
		return true;
	}

	@WrapMethod(method = "tryStir(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Z")
	public boolean tryStir(BlockState state, Player player, Operation<Boolean> original) {
		Util.ISmallCauldronContentsExtras c = (Util.ISmallCauldronContentsExtras) (Object) contents;
		if (c == null) return false;  // not possible
		var stirsDone = c.getStirsDone();

		var recipe = c.unifiedbrewing$findRecipeMatch(level);
		var held = player.getMainHandItem();
		boolean doDamage = false;
		if (recipe.isPresent()) {
			var r = (Util.ISmallCauldronRecipeExtras) (Object) recipe.get().value();
			if (Config.REQUIRE_BLAZING.getAsBoolean() && stirsDone < r.unifiedbrewing$getBlazeCharges()) {
				if (!held.is(UBItems.BLAZING_LADLE))
					return false;
				doDamage = true;
			}
		}
		else if (Config.PREVENT_SPOILED.getAsBoolean())
			return false;

		if (original.call(state, player)) {
			if (doDamage) {
				held.hurtAndBreak(1, player, held.getEquipmentSlot());
			}
			return true;
		}

		return false;
	}

	@WrapMethod(method = "applySpoiledAura(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V")
	private void applySpoiledAura(ServerLevel server, BlockPos pos, Operation<Void> original) {
		if (Config.DISABLE_SPOILED_POISON.getAsBoolean())
			return;
		original.call(server, pos);
	}

	@Override
	public ResourceLocation unifiedbrewing$getMixtureTexture() {
		return ((Util.ISmallCauldronContentsExtras) (Object) contents).unifiedbrewing$getMixtureTexture();
	}

	@Unique
	public boolean unifiedbrewing$isPotionLikeContainer(ItemStack held) {
		return ((Util.ISmallCauldronContentsExtras) (Object) contents).unifiedbrewing$isPotionLikeContainer(held);
	}

	@Unique
	public boolean unifiedbrewing$canUsePotionLikeContainer(ItemStack held) {
		return ((Util.ISmallCauldronContentsExtras) (Object) contents).unifiedbrewing$canUsePotionLikeContainer(held);
	}

	@Unique
	public boolean unifiedbrewing$tryUsePotionLikeContainer(Player player, ItemStack held) {
		if (level instanceof ServerLevel server) {
			boolean res = ((Util.ISmallCauldronContentsExtras) (Object) contents).unifiedbrewing$tryUsePotionLikeContainer(
				server, centerX(), centerY(), centerZ(), player, held);
			if (!res)
				level.playSound(null, centerX(), centerY(), centerZ(), SoundEvents.BUCKET_FILL_AXOLOTL, SoundSource.BLOCKS, 1.0F, 0.5F);
			syncIfNeeded();
			return res;
		}
		return false;
	}
}
