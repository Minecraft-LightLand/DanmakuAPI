package dev.xkmc.danmakuapi.content.spell.spellcard;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.fastprojectileapi.entity.ProjectileMovement;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@SerialClass
public class SpellCardWrapper extends SpellCard {

	@SerialField
	public String modelId;

	@SerialField
	public SpellCard card;

	@Override
	public void tick(CardHolder holder) {
		super.tick(holder);
		if (card != null) card.tick(holder);
	}

	@Override
	public ProjectileMovement move(int code, int tickCount, Vec3 vec) {
		if (card != null) return card.move(code, tickCount, vec);
		return super.move(code, tickCount, vec);
	}

	@Override
	public void reset() {
		if (card != null) card.reset();
	}

	@Nullable
	public String getModelId() {
		return modelId;
	}

	public void hurt(CardHolder holder, DamageSource source, float amount) {
		if (card != null) card.hurt(holder, source, amount);
	}

	@Override
	public DamageSource getDanmakuDamageSource(IDanmakuEntity danmaku) {
		if (card != null) return card.getDanmakuDamageSource(danmaku);
		return super.getDanmakuDamageSource(danmaku);
	}
}
