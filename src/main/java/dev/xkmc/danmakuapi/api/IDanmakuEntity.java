package dev.xkmc.danmakuapi.api;

import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.fastprojectileapi.entity.GrazingEntity;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

public interface IDanmakuEntity extends GrazingEntity {

	float GRAZE_RANGE = 1.5f;

	float damage(Entity target);

	default SimplifiedProjectile self() {
		return (SimplifiedProjectile) this;
	}

	default boolean shouldHurt(@Nullable Entity owner, Entity e) {
		if (owner == null) return false;
		if (owner instanceof IYoukaiEntity youkai) {
			if (e instanceof LivingEntity le) {
				return youkai.shouldHurt(le);
			}
			return false;
		}
		return true;
	}

	default DamageSource source() {
		DamageSource dmgType = DanmakuDamageTypes.danmaku(this);
		if (self().getOwner() instanceof CardHolder youkai) {
			dmgType = youkai.getDanmakuDamageSource(this);
		}
		if (self().getOwner() instanceof LivingEntity le) {
			var event = new DanmakuDamageEvent(le, dmgType, this);
			NeoForge.EVENT_BUS.post(event);
			dmgType = event.getSource();
		}
		return dmgType;
	}

	default void hurtTarget(EntityHitResult result) {
		if (self().level().isClientSide) return;
		var e = result.getEntity();
		DamageSource source = source();
		LivingEntity target = null;
		while (e instanceof PartEntity<?> pe) {
			e = pe.getParent();
		}
		if (e instanceof LivingEntity le) target = le;
		if (target != null) {
			DamageSource last = target.getLastDamageSource();
			int time = target.getLastHurtByMobTimestamp();
			if (last != null && last.getDirectEntity() instanceof IDanmakuEntity && time + 5 > target.tickCount) {
				return;
			}
		}
		var owner = self().getOwner();
		if (target != null && owner instanceof IYoukaiEntity youkai) {
			youkai.danmakuHitTarget(this, source, target);
			return;
		}
		if (owner instanceof Player player) {
			if (e instanceof LivingEntity le) {
				if (!GrazeHelper.shouldPlayerHurt(player, le)) return;
			}
		}
		if (target != null)
			target.hurt(source, damage(target));
		else e.hurt(source, damage(e));
	}

	@Override
	default float grazeRange() {
		return GRAZE_RANGE;
	}

	@Override
	default AABB alterHitBox(Entity x, float radius, float graze) {
		if (self().getOwner() instanceof Player player &&
				x instanceof IYoukaiEntity youkai &&
				youkai.isTarget(player)) {
			return youkai.getBoundingBoxForDanmaku().inflate(GRAZE_RANGE);
		}
		return alterEntityHitBox(x, radius, graze);
	}

	static AABB alterEntityHitBox(Entity x, float radius, float graze) {
		var box = x.getBoundingBox();
		if (graze > 0) return box.inflate(radius + graze);
		float shrink = x instanceof Player player ? GrazeHelper.getHitBoxShrink(player) : 0;
		return new AABB(
				box.minX + shrink - radius, box.minY + shrink * 2 - radius, box.minZ + shrink - radius,
				box.maxX - shrink + radius, box.maxY + radius, box.maxZ - shrink + radius
		);
	}

	TraceableEntity asTraceable();

}
