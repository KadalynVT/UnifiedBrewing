package live.kadalyn.unifiedbrewing.particles;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
public class SwirlParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	public static final int SWIRL_TIME = 40;

	protected SwirlParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet, float quadSize) {
		super(level, x, y, z, 0.0, 0.0, 0.0);
		this.sprites = spriteSet;
		this.quadSize = quadSize;
		this.lifetime = SWIRL_TIME;
		this.gravity = 0.0F;
		this.setSpriteFromAge(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public FacingCameraMode getFacingCameraMode() {
		return (quaternion, camera, partialTicks) -> {
			quaternion.identity();
			quaternion.rotateX(-1.5707963f);  // Math.toRadians(-90)
		};
	}

	@Override
	public void tick() {
		if (this.lifetime-- <= 0) {
			this.remove();
		}
		else {
			// Show each frame for 2 ticks
			this.setSprite(sprites.get(this.lifetime % 16, 16));
		}
	}

	@OnlyIn(Dist.CLIENT)
	@ParametersAreNonnullByDefault
	public static class Provider implements ParticleProvider<SwirlParticleOptions> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		public Particle createParticle(
			SwirlParticleOptions type,
			ClientLevel level,
			double x,
			double y,
			double z,
			double xSpeed,
			double ySpeed,
			double zSpeed
		) {
			var particle = new SwirlParticle(level, x, y, z, this.sprites, type.quadSize());
			var color = type.colorOption();
			particle.setColor(color.getRed(), color.getGreen(), color.getBlue());
			particle.setAlpha(color.getAlpha());
			return particle;
		}
	}

	@OnlyIn(Dist.CLIENT)
	@ParametersAreNonnullByDefault
	public static class CauldronSwirlProvider implements ParticleProvider<CauldronSwirlParticleOptions> {
		private final SpriteSet sprites;

		public CauldronSwirlProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		public Particle createParticle(
			CauldronSwirlParticleOptions type,
			ClientLevel level,
			double x,
			double y,
			double z,
			double xSpeed,
			double ySpeed,
			double zSpeed
		) {
			var particle = new SwirlParticle(level, x, y, z, this.sprites, type.quadSize());
			if (level.getBlockEntity(type.pos()) instanceof AlchemistCauldronTile cauldron) {
				var color = cauldron.getAverageWaterColor();
				float scale = 2.5f;
				particle.rCol = Mth.clamp(FastColor.ARGB32.red(color) * scale, 0, 255) / 255.0F;
				particle.gCol = Mth.clamp(FastColor.ARGB32.green(color) * scale, 0, 255) / 255.0F;
				particle.bCol = Mth.clamp(FastColor.ARGB32.blue(color) * scale, 0, 255) / 255.0F;
				particle.setAlpha(0.4f);
			}
			return particle;
		}
	}
}
