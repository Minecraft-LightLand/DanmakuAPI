package dev.xkmc.danmakuapi.content.spell.spellcard;

import dev.xkmc.danmakuapi.api.DanmakuBullet;
import dev.xkmc.danmakuapi.api.DanmakuLaser;
import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.entity.ItemLaserEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.item.LaserItem;
import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface CardHolder {

	Vec3 center();

	Vec3 forward();

	@Nullable
	Vec3 target();

	RandomSource random();

	default ItemBulletEntity prepareDanmaku(int life, Vec3 vec, DanmakuItem item) {
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), self(), self().level());
		danmaku.setItem(new ItemStack(item));
		danmaku.setup(item.type.damage(), life, true, true, vec);
		danmaku.setPos(center());
		return danmaku;
	}

	default ItemBulletEntity prepareDanmaku(int life, Vec3 vec, DanmakuBullet type, DyeColor color) {
		return prepareDanmaku(life, vec, type.get(color).get());
	}

	default ItemLaserEntity prepareLaser(int life, Vec3 pos, Vec3 vec, float len, LaserItem item) {
		ItemLaserEntity danmaku = new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), self(), self().level());
		danmaku.setItem(new ItemStack(item));
		danmaku.setup(item.type.damage(), life, len, true, vec);
		danmaku.setPos(pos);
		danmaku.setupLength = item.type.setupLength();
		return danmaku;
	}

	default ItemLaserEntity prepareLaser(int life, Vec3 pos, Vec3 vec, float len, DanmakuLaser type, DyeColor color) {
		return prepareLaser(life, pos, vec, len, type.get(color).get());
	}

	void shoot(SimplifiedProjectile danmaku);

	LivingEntity self();

	default DamageSource getDanmakuDamageSource(IDanmakuEntity danmaku) {
		return DanmakuDamageTypes.danmaku(danmaku);
	}

	@Nullable
	Vec3 targetVelocity();

}
