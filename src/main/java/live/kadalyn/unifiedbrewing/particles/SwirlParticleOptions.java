package live.kadalyn.unifiedbrewing.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SwirlParticleOptions(ColorParticleOption colorOption, float quadSize, ParticleType<SwirlParticleOptions> getType) implements ParticleOptions {
	public static MapCodec<SwirlParticleOptions> codec(ParticleType<SwirlParticleOptions> type) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
			ColorParticleOption.codec(colorType).forGetter(SwirlParticleOptions::colorOption),
			Codec.FLOAT.fieldOf("size").forGetter(SwirlParticleOptions::quadSize)
		).apply(instance, (color, size) -> new SwirlParticleOptions(color, size, type)));
	}

	public static StreamCodec<RegistryFriendlyByteBuf, SwirlParticleOptions> streamCodec(ParticleType<SwirlParticleOptions> type) {
		return StreamCodec.composite(
			ColorParticleOption.streamCodec(colorType), SwirlParticleOptions::colorOption,
			ByteBufCodecs.FLOAT, SwirlParticleOptions::quadSize,
			(color, size) -> new SwirlParticleOptions(color, size, type)
		);
	}

	@MethodsReturnNonnullByDefault
	static private final ParticleType<ColorParticleOption> colorType = new ParticleType<ColorParticleOption>(false) {
		public MapCodec<ColorParticleOption> codec() {
			return ColorParticleOption.codec(this);
		}

		public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
			return ColorParticleOption.streamCodec(this);
		}
	};
}
