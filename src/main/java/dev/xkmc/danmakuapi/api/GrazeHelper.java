package dev.xkmc.danmakuapi.api;

import dev.xkmc.danmakuapi.init.registrate.DanmakuSounds;
import dev.xkmc.fastprojectileapi.entity.GrazingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class GrazeHelper {

	public static int globalInvulTime = 0;
	public static int globalForbidTime = 0;

	public static void graze(Player entity, GrazingEntity e) {
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), DanmakuSounds.GRAZE.get(), SoundSource.PLAYERS,
		0.5F, 1.0F);
	}

	public static float getHitBoxShrink(Player player) {
		return 0;
	}

	public static boolean shouldPlayerHurt(Player player, LivingEntity le) {
		return true;
	}

	public static boolean forbidDanmaku(Player player) {
		return false;
	}

}
