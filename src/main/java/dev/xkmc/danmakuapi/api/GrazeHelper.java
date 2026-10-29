package dev.xkmc.danmakuapi.api;

import dev.xkmc.fastprojectileapi.entity.GrazingEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public class GrazeHelper {

	public static int globalInvulTime = 0;
	public static int globalForbidTime = 0;

	public static void graze(Player entity, GrazingEntity e) {
	}

	public static float getHitBoxShrink(Player player) {
		return 0;
	}

	public static boolean shouldPlayerHurt(Player player, LivingEntity le) {
		boolean ans = shouldPlayerHurtImpl(player, le);
		return NeoForge.EVENT_BUS.post(new DanmakuHurtEvent(player, le, ans)).getResult();
	}

	private static boolean shouldPlayerHurtImpl(Player player, LivingEntity le) {
		if (le.isAlliedTo(player)) return false;
		if (le instanceof Animal animal && animal.getTarget() == null)
			return false;
		if (le instanceof AbstractVillager villager && villager.getTarget() == null)
			return false;
		if (le instanceof OwnableEntity ownable) {
			var owner = ownable.getOwner();
			if (owner != null && owner.isAlliedTo(player)) return false;
		}
		return true;
	}

	public static boolean forbidDanmaku(Player player) {
		return false;
	}

}
