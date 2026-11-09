package dev.xkmc.danmakuapi.content.entity;

import dev.xkmc.danmakuapi.api.DanmakuCommander;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.particle.DanmakuParticleHelper;
import dev.xkmc.danmakuapi.content.spell.mover.DanmakuMover;
import dev.xkmc.danmakuapi.content.spell.mover.MoverInfo;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.TrailAction;
import dev.xkmc.fastprojectileapi.entity.ProjectileMovement;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SerialClass
public class ItemBulletEntity extends DanmakuBulletEntity implements ItemSupplier {

	@SerialClass.SerialField
	public int controlCode = 0;
	@SerialClass.SerialField
	public DanmakuMover mover = null;
	@SerialClass.SerialField
	public TrailAction afterExpiry = null;
	@SerialClass.SerialField
	public ItemStack stack = ItemStack.EMPTY;

	private boolean isErased = false;

	public ItemBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public ItemBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, double pX, double pY, double pZ, Level pLevel) {
		super(pEntityType, pX, pY, pZ, pLevel);
	}

	public ItemBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	public void setItem(ItemStack pStack) {
		stack =  pStack.copyWithCount(1);
		refreshDimensions();
	}

	public void setControlCode(int code) {
		this.controlCode = code;
	}

	@Override
	protected ProjectileMovement updateVelocity(Vec3 vec, Vec3 pos) {
		if (mover != null) {
			return mover.move(new MoverInfo(tickCount, pos, vec, this));
		}
		if (controlCode > 0 && getOwner() instanceof DanmakuCommander commander)
			return commander.move(controlCode, tickCount, vec);
		return super.updateVelocity(vec, pos);
	}

	public ItemStack getItem() {
		return stack;
	}

	protected void defineSynchedData() {
	}

	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (!stack.isEmpty()) {
			nbt.put("Item", stack.save(new CompoundTag()));
		}

	}

	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		ItemStack itemstack = ItemStack.of(nbt.getCompound("Item"));
		this.setItem(itemstack);
	}

	@Override
	public EntityDimensions getDimensions(Pose pPose) {
		return super.getDimensions(pPose).scale(scale());
	}

	public boolean fullBright() {
		return true;
	}

	public void markErased(boolean kill) {
		if (!isErased)
			super.markErased(kill);
		isErased = true;
	}

	@Override
	public boolean isValid() {
		return !isErased && super.isValid();
	}

	private Float sizeCache = null;

	public float scale() {
		if (sizeCache == null) {
			if (getItem().getItem() instanceof DanmakuItem item) {
				sizeCache = item.size;
			}
		}
		return sizeCache == null ? 1 : sizeCache;
	}

	@Override
	protected void terminate() {
		if (afterExpiry == null) return;
		CardHolder holder = null;
		Entity e = getOwner();
		if (e instanceof CardHolder h) holder = h;
		if (holder == null) afterExpiry.execute(position(), getDeltaMovement());
		else afterExpiry.execute(holder, position(), getDeltaMovement());
	}

	private int lastGraze = 0;

	@Override
	public void doGraze(Player entity) {
		if (tickCount < lastGraze) return;
		lastGraze = tickCount + 20;
		GrazeHelper.graze(entity, this);
	}

	@Override
	public void poof() {
		if (!level().isClientSide()) return;
		if (!(getItem().getItem() instanceof DanmakuItem item)) return;
		int col = item.color.getTextColor();
		var pos = position().add(0, getBbHeight() / 2, 0);
		DanmakuParticleHelper.ball(level(), pos, col, getBbWidth() / 2, random);
	}

}