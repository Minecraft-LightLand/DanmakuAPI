package dev.xkmc.danmakuapi.content.entity;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.fastprojectileapi.collision.LaserHitHelper;
import dev.xkmc.fastprojectileapi.entity.BaseLaser;
import dev.xkmc.l2serial.serialization.codec.PacketCodec;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.Objects;

@SerialClass
public class DanmakuLaserEntity extends BaseLaser implements IEntityWithComplexSpawn, IDanmakuEntity {

	@SerialField
	protected int life = 0, prepare, start, end;
	@SerialField
	private boolean bypassWall = false;
	@SerialField
	public float damage = 0, length = 0;
	@SerialField
	public boolean setupLength;

	/**
	 * Client-side only, see {@link IDanmakuEntity#setClientFriendly}.
	 */
	public boolean clientFriendly = false;

	@Override
	public void setClientFriendly(boolean friendly) {
		this.clientFriendly = friendly;
	}

	@Override
	public boolean isClientFriendly() {
		return clientFriendly;
	}

	public double earlyTerminate = -1;

	protected DanmakuLaserEntity(EntityType<? extends DanmakuLaserEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	protected DanmakuLaserEntity(EntityType<? extends DanmakuLaserEntity> pEntityType, double pX, double pY, double pZ, Level pLevel) {
		this(pEntityType, pLevel);
		this.setPos(pX, pY, pZ);
	}

	protected DanmakuLaserEntity(EntityType<? extends DanmakuLaserEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		this(pEntityType, pShooter.getX(), pShooter.getEyeY() - (double) 0.1F, pShooter.getZ(), pLevel);
		this.setOwner(pShooter);
	}

	public void setup(float damage, int life, float length, boolean bypassWall, Vec3 vec3) {
		double d0 = vec3.horizontalDistance();
		setup(damage, life, length, bypassWall,
				(float) (-Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG),
				(float) (-Mth.atan2(vec3.y, d0) * Mth.RAD_TO_DEG));
	}


	public void setupTime(int prepare, int start, int life, int end) {
		this.prepare = prepare;
		this.start = this.prepare + start;
		this.end = this.start + life;
		this.life = this.end + end;
	}

	public void setup(float damage, int life, float length, boolean bypassWall, float rY, float rX) {
		this.damage = damage;
		this.bypassWall = bypassWall;
		this.length = length;
		setupTime(20, 20, life, 20);
		setYRot(rY);
		setXRot(rX);
	}

	@Override
	public double getLength() {
		return length;
	}

	@Override
	public boolean checkBlockHit() {
		return !bypassWall;
	}

	@Override
	public float getEffectiveHitRadius() {
		return getBbWidth() / 4f;
	}

	@Override
	public boolean checkEntityHit() {
		return tickCount > start && tickCount < end;
	}

	@Override
	public float damage(Entity target) {
		return damage;
	}

	public float percentOpen(float pTick) {
		return setupLength ? 1 : percentLoad(pTick);
	}

	public float percentLoad(float pTick) {
		pTick += tickCount;
		if (pTick < prepare) return 0.1f;
		else if (pTick < start)
			return (pTick - prepare) / (start - prepare) * 0.9f + 0.1f;
		else if (pTick < end) return 1;
		else if (pTick < life)
			return (pTick - end) / (life - end) * -0.9f + 1f;
		else return 0;
	}

	public float effectiveLength(float pTick) {
		if (setupLength) {
			return percentLoad(pTick) * length;
		}
		if (earlyTerminate >= 0)
			return (float) earlyTerminate;
		return length;
	}

	@Override
	public void tick() {
		super.tick();
		danmakuMove();
		if (!level().isClientSide() && tickCount > life) {
			discard();
		}
	}

	protected void danmakuMove() {

	}

	@Override
	public boolean canHitEntity(Entity target) {
		return super.canHitEntity(target) && IDanmakuEntity.canHurt(getOwner(), target);
	}

	@Override
	protected void onHit(LaserHitHelper.LaserHitResult hit) {
		if (level().isClientSide()) {
			if (hit.bhit() != null && hit.bhit().getType() != HitResult.Type.MISS) {
				earlyTerminate = position().add(0, getBbHeight() / 2f, 0).distanceTo(hit.bhit().getLocation());
			} else earlyTerminate = -1;
		}
		for (var e : hit.ehit()) {
			hurtTarget(e);
		}
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		var src = position().add(0, getBbHeight() / 2f, 0);
		return new AABB(src, src.add(getForward().scale(length))).inflate(getBbWidth() / 2f);
	}

	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.put("auto-serial", Objects.requireNonNull(new TagCodec(level().registryAccess()).toTag(new CompoundTag(), this)));
	}

	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("auto-serial")) {
			Wrappers.run(() -> new TagCodec(level().registryAccess()).fromTag(nbt.getCompound("auto-serial"), getClass(), this));
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		PacketCodec.to(buffer, this);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
		super.readSpawnData(additionalData);
		PacketCodec.from(additionalData, getClass(), Wrappers.cast(this));
	}

	@Override
	public boolean isValid() {
		return tickCount < life;
	}

	@Override
	public TraceableEntity asTraceable() {
		return this;
	}

}
