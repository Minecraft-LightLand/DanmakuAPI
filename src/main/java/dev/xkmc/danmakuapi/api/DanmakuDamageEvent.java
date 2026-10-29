package dev.xkmc.danmakuapi.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/** Modify danmaku damage source */
public class DanmakuDamageEvent extends Event {

	private final LivingEntity user;
	private final IDanmakuEntity bullet;

	private DamageSource source;

	public DanmakuDamageEvent(LivingEntity user, DamageSource source, IDanmakuEntity bullet) {
		this.user = user;
		this.source = source;
		this.bullet = bullet;
	}

	public LivingEntity getUser() {
		return user;
	}

	public IDanmakuEntity getBullet() {
		return bullet;
	}

	public DamageSource getSource() {
		return source;
	}

	public void setSource(DamageSource source) {
		this.source = source;
	}

}
