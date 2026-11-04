package dev.xkmc.danmakuapi.init.registrate;

import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class DanmakuSounds {

	public static final SimpleEntry<SoundEvent> GRAZE = reg("graze");
	public static final SimpleEntry<SoundEvent> LASER = reg("laser");
	public static final SimpleEntry<SoundEvent> LASER_2 = reg("laser_2");
	public static final SimpleEntry<SoundEvent> SLASH = reg("slash");
	public static final SimpleEntry<SoundEvent> SHOOT = reg("shoot");
	public static final SimpleEntry<SoundEvent> SHOOT_2 = reg("shoot_2");
	public static final SimpleEntry<SoundEvent> SHOOT_3 = reg("shoot_3");
	public static final SimpleEntry<SoundEvent> SHOOT_MINI = reg("mini_shoot");

	private static SimpleEntry<SoundEvent> reg(String id) {
		ResourceLocation rl = DanmakuAPI.loc(id);
		return new SimpleEntry<>(DanmakuAPI.REGISTRATE.simple(id, Registries.SOUND_EVENT, () -> SoundEvent.createVariableRangeEvent(rl)));
	}

	public static void register() {
	}

}
