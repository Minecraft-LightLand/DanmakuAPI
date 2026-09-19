package dev.xkmc.danmakuapi.content.entity;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.fastprojectileapi.entity.BaseProjectile;
import dev.xkmc.fastprojectileapi.entity.ProjectileMovement;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.Objects;

@SerialClass
public class DanmakuBulletEntity extends BaseProjectile implements IDanmakuEntity, IEntityWithComplexSpawn {

	@SerialField
	private int life = 0;
	@SerialField
	private boolean bypassWall = false, bypassEntity = false;
	@SerialField
	public float damage = 0;

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

	protected DanmakuBulletEntity(EntityType<? extends DanmakuBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	protected DanmakuBulletEntity(EntityType<? extends DanmakuBulletEntity> pEntityType, double pX, double pY, double pZ, Level pLevel) {
		this(pEntityType, pLevel);
		this.setPos(pX, pY, pZ);
	}

	protected DanmakuBulletEntity(EntityType<? extends DanmakuBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		this(pEntityType, pShooter.getX(), pShooter.getEyeY() - (double) 0.1F, pShooter.getZ(), pLevel);
		this.setOwner(pShooter);
	}

	public void setup(float damage, int life, boolean bypassWall, boolean bypassEntity, Vec3 initVec) {
		this.damage = damage;
		this.life = life;
		this.bypassWall = bypassWall;
		this.bypassEntity = bypassEntity;
		setDeltaMovement(initVec);
		var rot = ProjectileMovement.of(initVec).rot();
		this.setXRot((float) rot.x * Mth.RAD_TO_DEG);
		this.setYRot((float) rot.y * Mth.RAD_TO_DEG);
	}

	@Override
	public boolean checkBlockHit() {
		return !bypassWall;
	}

	@Override
	public int lifetime() {
		return life;
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
	protected void onHitBlock(BlockHitResult pResult) {
		super.onHitBlock(pResult);
		if (!level().isClientSide) {
			discard();
		}
	}

	@Override
	public float damage(Entity target) {
		return damage;
	}

	@Override
	public boolean canHitEntity(Entity target) {
		return super.canHitEntity(target) && IDanmakuEntity.canHurt(getOwner(), target);
	}

	@Override
	public void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		hurtTarget(result);
		if (!bypassEntity) {
			discard();
		}
	}

	@Override
	public TraceableEntity asTraceable() {
		return this;
	}


}
