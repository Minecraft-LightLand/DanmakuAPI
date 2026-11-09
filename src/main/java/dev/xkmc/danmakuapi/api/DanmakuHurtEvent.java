package dev.xkmc.danmakuapi.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/** Predicate danmaku damage */
public class DanmakuHurtEvent extends Event {

	private final Player user;

	private final LivingEntity target;

	private boolean ans;

	public DanmakuHurtEvent(Player user, LivingEntity target, boolean ans) {
		this.user = user;
		this.target = target;
		this.ans = ans;
	}

	public boolean getAnswer() {
		return ans;
	}

	public void setAnswer(boolean ans) {
		this.ans = ans;
	}

	public Player getUser() {
		return user;
	}

	public LivingEntity getTarget() {
		return target;
	}

}
