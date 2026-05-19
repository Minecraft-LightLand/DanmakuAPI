package dev.xkmc.danmakuapi.content.spell.item;

import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.Ticker;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@SerialClass
public class ItemSpell {

	@SerialField
	private final ArrayList<Ticker<?>> tickers = new ArrayList<>();
	@SerialField
	private UUID targetId;
	@SerialField
	public Vec3 dir = new Vec3(1, 0, 0), targetPos;

	List<SimplifiedProjectile> cache = new LinkedList<>();

	private LivingEntity targetCache;
	protected CardHolder holder;

	public void start(LivingEntity entity, @Nullable LivingEntity target) {
		this.dir = RayTraceUtil.getRayTerm(Vec3.ZERO, entity.getXRot(), entity.getYRot(), 1);
		if (target != null) {
			targetId = target.getUUID();
			targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
			targetCache = target;
		}
	}

	@Nullable
	LivingEntity getTarget(ServerLevel sl) {
		if (targetCache != null) {
			if (targetCache.isAlive()) {
				return targetCache;
			} else {
				targetId = null;
			}
		}
		if (targetId == null) return null;
		var e = sl.getEntity(targetId);
		if (e instanceof LivingEntity le && le.isAlive()) {
			targetCache = le;
			return le;
		} else {
			targetId = null;
			return null;
		}
	}

	public boolean tick(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel sl)) return true;
		var target = getTarget(sl);
		if (target != null) targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
		holder = new PlayerHolder(entity, dir, this, target);
		tickers.removeIf(e -> e.tick(holder, Wrappers.cast(this)));
		cache.removeIf(e -> !e.isValid());
		return tickers.isEmpty();
	}

	protected <T extends ItemSpell> void addTicker(Ticker<T> tick) {
		tickers.add(tick);
	}

}
