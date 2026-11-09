package live.kadalyn.unifiedbrewing.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import live.kadalyn.unifiedbrewing.registries.UBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CauldronSwirlParticleOptions(BlockPos pos, float quadSize, ParticleType<CauldronSwirlParticleOptions> getType) implements ParticleOptions {
	public static MapCodec<CauldronSwirlParticleOptions> codec(ParticleType<CauldronSwirlParticleOptions> type) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(CauldronSwirlParticleOptions::pos),
			Codec.FLOAT.fieldOf("size").forGetter(CauldronSwirlParticleOptions::quadSize)
		).apply(instance, (pos, size) -> new CauldronSwirlParticleOptions(pos, size, type)));
	}

	public static StreamCodec<RegistryFriendlyByteBuf, CauldronSwirlParticleOptions> streamCodec(ParticleType<CauldronSwirlParticleOptions> type) {
		return StreamCodec.composite(
			BlockPos.STREAM_CODEC, CauldronSwirlParticleOptions::pos,
			ByteBufCodecs.FLOAT, CauldronSwirlParticleOptions::quadSize,
			(pos, size) -> new CauldronSwirlParticleOptions(pos, size, type)
		);
	}

	public static CauldronSwirlParticleOptions create(BlockPos pos, float quadSize) {
		return new CauldronSwirlParticleOptions(pos, quadSize, UBParticles.CAULDRON_SWIRL_PARTICLE.get());
	}
}
