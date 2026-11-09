package live.kadalyn.unifiedbrewing.registries;

import com.mojang.math.MethodsReturnNonnullByDefault;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import live.kadalyn.unifiedbrewing.Config;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;

@MethodsReturnNonnullByDefault
public record UBConfigCondition(String config, String value) implements ICondition {
	public static final MapCodec<UBConfigCondition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		Codec.STRING.fieldOf("config").forGetter(UBConfigCondition::config),
		Codec.STRING.fieldOf("value").forGetter(UBConfigCondition::value)
	).apply(inst, UBConfigCondition::new));

	@Override
	public boolean test(ICondition.IContext context) {
		if (value.equals("true") || value.equals("false")) {
			ModConfigSpec.BooleanValue result = Config.get(config);
			return result.getAsBoolean() == value.equals("true");
		}
		try {
			int result = Integer.parseInt(value);
			return Config.get(config).equals(result);
		} catch (NumberFormatException e) {
			return Config.get(config).equals(value);
		}
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "config(\"" + UnifiedBrewing.MODID + ":" + config + "\" = " + value + ")";
	}
}
