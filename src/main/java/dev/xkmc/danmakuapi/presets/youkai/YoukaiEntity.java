package dev.xkmc.danmakuapi.presets.youkai;

import dev.xkmc.danmakuapi.api.DanmakuBullet;
import dev.xkmc.danmakuapi.api.DanmakuLaser;
import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.api.IYoukaiEntity;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.entity.ItemLaserEntity;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.SpellCardWrapper;
import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.spellcircle.SpellCircleHolder;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SerialClass
public abstract class YoukaiEntity extends PathfinderMob implements IYoukaiEntity, SpellCircleHolder, CardHolder {

	private static final int GROUND_HEIGHT = 5, ATTEMPT_ABOVE = 3;

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(YoukaiEntity.class, ser);
	}

	protected static final SyncedData YOUKAI_DATA = new SyncedData(YoukaiEntity::defineId);

	protected static final EntityDataAccessor<Integer> DATA_FLAGS_ID = YOUKAI_DATA.define(SyncedData.INT, 0, "youkai_flags");

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.4)
				.add(Attributes.FOLLOW_RANGE, 48);
	}

	public final MoveControl walkCtrl, flyCtrl;
	public final PathNavigation walkNav, fltNav;

	@SerialField
	public final YoukaiTargetContainer targets;

	@SerialField
	public SpellCardWrapper spellCard;

	public YoukaiEntity(EntityType<? extends YoukaiEntity> pEntityType, Level pLevel) {
		this(pEntityType, pLevel, 10);
	}

	public YoukaiEntity(EntityType<? extends YoukaiEntity> pEntityType, Level pLevel, int maxSize) {
		super(pEntityType, pLevel);
		walkCtrl = moveControl;
		walkNav = navigation;
		flyCtrl = new FlyingMoveControl(this, 10, false);
		fltNav = new FlyingPathNavigation(this, level());
		targets = new YoukaiTargetContainer(this, maxSize);
	}

	protected SoundEvent getAmbientSound() {
		return SoundEvents.EMPTY;
	}

	protected SoundEvent getHurtSound(DamageSource pDamageSource) {
		return SoundEvents.EMPTY;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.EMPTY;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	// base

	protected SyncedData data() {
		return YOUKAI_DATA;
	}

	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		data().register(builder);
	}

	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Age", tickCount);
		tag.put("auto-serial", Objects.requireNonNull(new TagCodec(level().registryAccess()).toTag(new CompoundTag(), this)));
		data().write(level().registryAccess(), tag, entityData);
	}

	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		tickCount = tag.getInt("Age");
		if (tag.contains("auto-serial")) {
			Wrappers.run(() -> new TagCodec(level().registryAccess()).fromTag(tag.getCompound("auto-serial"), getClass(), this));
		}
		data().read(level().registryAccess(), tag, entityData);
		if (getTarget() == null) {
			setWalking();
		}
	}

	public boolean getFlag(int flag) {
		return (entityData.get(DATA_FLAGS_ID) & flag) != 0;
	}

	public void setFlag(int flag, boolean enable) {
		int b0 = entityData.get(DATA_FLAGS_ID);
		if (enable) {
			b0 = (byte) (b0 | flag);
		} else {
			b0 = (byte) (b0 & (-1 - flag));
		}
		entityData.set(DATA_FLAGS_ID, b0);
	}

	// features

	public boolean shouldIgnore(LivingEntity e) {
		var event = new YoukaiFightEvent(this, e);
		return NeoForge.EVENT_BUS.post(event).isCanceled();
	}

	@Override
	public boolean isInvulnerableTo(DamageSource pSource) {
		if (pSource.getEntity() instanceof LivingEntity le && shouldIgnore(le)) {
			return true;
		}
		return pSource.is(DamageTypeTags.IS_FALL);
	}

	@Override
	protected boolean canRide(Entity pVehicle) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public Vec3 center() {
		return position().add(0, getBbHeight() / 2, 0);
	}

	@Override
	public Vec3 forward() {
		var target = target();
		if (target != null) {
			return target.subtract(center()).normalize();
		}
		return getForward();
	}

	@Override
	public @Nullable Vec3 target() {
		var le = getTarget();
		if (le == null) return null;
		return le.position().add(0, le.getBbHeight() / 2, 0);
	}

	@Override
	public @Nullable Vec3 targetVelocity() {
		var le = getTarget();
		if (le == null) return null;
		return le.getDeltaMovement();
	}

	@Override
	public RandomSource random() {
		return random;
	}

	@Override
	public ItemBulletEntity prepareDanmaku(int life, Vec3 vec, DanmakuBullet type, DyeColor color) {
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), this, level());
		danmaku.setPos(center());
		danmaku.setItem(type.get(color).asStack());
		danmaku.setup((float) getAttributeValue(Attributes.ATTACK_DAMAGE),
				life, true, true, vec);
		return danmaku;
	}

	@Override
	public ItemLaserEntity prepareLaser(int life, Vec3 pos, Vec3 vec, int len, DanmakuLaser type, DyeColor color) {
		ItemLaserEntity danmaku = new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), this, level());
		danmaku.setItem(type.get(color).asStack());
		danmaku.setup((float) getAttributeValue(Attributes.ATTACK_DAMAGE),
				life, len, true, vec);
		danmaku.setPos(pos);
		return danmaku;
	}

	@Override
	public void shoot(SimplifiedProjectile danmaku) {
		level().addFreshEntity(danmaku);
	}

	public void shoot(float dmg, int life, Vec3 vec, DyeColor color) {
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), this, level());
		danmaku.setItem(DanmakuItems.Bullet.CIRCLE.get(color).asStack());
		danmaku.setup(dmg, life, true, true, vec);
		level().addFreshEntity(danmaku);
	}

	// flying

	public final void setFlying() {
		setNoGravity(true);
		if (moveControl == flyCtrl) return;
		getNavigation().stop();
		moveControl = flyCtrl;
		navigation = fltNav;
	}

	public final void setWalking() {
		setNoGravity(false);
		if (moveControl == walkCtrl) return;
		getNavigation().stop();
		setYya(0);
		setXxa(0);
		setZza(0);
		moveControl = walkCtrl;
		navigation = walkNav;
	}

	public final boolean isFlying() {
		return moveControl == flyCtrl;
	}

	public void aiStep() {
		if (!level().isClientSide()) {
			if (!onGround() && getDeltaMovement().y < 0.0D) {
				double fall = isAggressive() ? 0.6 : 0.8;
				setDeltaMovement(getDeltaMovement().multiply(1.0D, fall, 1.0D));
			}
			targets.tick();
			if (spellCard != null) {
				if (isAggressive() && shouldShowSpellCircle()) {
					spellCard.tick(this);
				} else {
					spellCard.reset();
				}
			}
		}
		super.aiStep();
	}

	@Override
	protected void actuallyHurt(DamageSource source, float amount) {
		if (spellCard != null) spellCard.hurt(this, source, amount);
		actuallyHurtImpl(source, amount);
	}

	protected final void actuallyHurtImpl(DamageSource source, float amount) {
		if (!isInvulnerableTo(source)) {
			assert damageContainers != null;
			damageContainers.peek().setReduction(DamageContainer.Reduction.ARMOR, damageContainers.peek().getNewDamage() - getDamageAfterArmorAbsorb(source, (damageContainers.peek()).getNewDamage()));
			getDamageAfterMagicAbsorb(source, (damageContainers.peek()).getNewDamage());
			float damage = CommonHooks.onLivingDamagePre(this, damageContainers.peek());
			damageContainers.peek().setReduction(DamageContainer.Reduction.ABSORPTION, Math.min(getAbsorptionAmount(), damage));
			float absorbed = Math.min(damage, (damageContainers.peek()).getReduction(DamageContainer.Reduction.ABSORPTION));
			setAbsorptionAmount(Math.max(0.0F, getAbsorptionAmount() - absorbed));
			if (absorbed > 0.0F && absorbed < 3.4028235E37F) {
				if (source.getEntity() instanceof ServerPlayer serverplayer) {
					serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbed * 10.0F));
				}
			}
			hurtFinal(source, damageContainers.peek().getNewDamage());
		}
	}

	protected void hurtFinal(DamageSource source, float amount) {
		if (amount != 0.0F) {
			getCombatTracker().recordDamage(source, amount);
			hurtFinalImpl(source, getHealth() - amount);
			gameEvent(GameEvent.ENTITY_DAMAGE);
			onDamageTaken(damageContainers.peek());
		}
		CommonHooks.onLivingDamagePost(this, damageContainers.peek());
	}

	protected void hurtFinalImpl(DamageSource source, float amount) {
		super.setHealth(amount);
	}

	protected void customServerAiStep() {
		LivingEntity target = getTarget();
		if (target != null && canAttack(target) && moveControl == flyCtrl) {
			boolean tooHigh = tooHigh();
			int expectedHeight = tooHigh ? 0 : ATTEMPT_ABOVE;
			double low = -0.5;
			double high = tooHigh ? 0 : 0.5;
			double diff = target.getEyeY() + expectedHeight - getEyeY();
			Vec3 vec3 = getDeltaMovement();
			double moveY = vec3.y * 0.5 + diff * 0.02;
			if (getEyeY() < target.getEyeY() + expectedHeight + low) {
				setY(Math.max(vec3.y, moveY));
			}
			if (getEyeY() > target.getEyeY() + expectedHeight + high) {
				if (diff < -1) {
					setY(Math.min(vec3.y, moveY));
				} else if (tooHigh) {
					setY(Math.min(vec3.y, -0.01));
				}
			}
		}

		super.customServerAiStep();
	}

	private void setY(double vy) {
		Vec3 vec3 = getDeltaMovement();
		setDeltaMovement(vec3.x, vy, vec3.z);
		if (vy > 0 && onGround()) {
			hasImpulse = true;
		}
	}

	private boolean tooHigh() {
		BlockPos pos = getOnPos();
		for (int i = 0; i < GROUND_HEIGHT; i++) {
			BlockPos off = pos.offset(0, -i, 0);
			if (!level().getBlockState(off).isAir()) return false;
		}
		return true;
	}

	public boolean shouldHurt(LivingEntity le) {
		return targets.contains(le);
	}

	@Override
	public LivingEntity self() {
		return super.self();
	}

	@Override
	public DamageSource getDanmakuDamageSource(IDanmakuEntity danmaku) {
		if (spellCard != null) return spellCard.card.getDanmakuDamageSource(danmaku);
		return DanmakuDamageTypes.danmaku(danmaku);
	}

	@Override
	public void onDanmakuHit(LivingEntity target, IDanmakuEntity bullet) {

	}

}