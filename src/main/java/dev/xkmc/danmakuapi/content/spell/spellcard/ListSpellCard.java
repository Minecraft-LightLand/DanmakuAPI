package dev.xkmc.danmakuapi.content.spell.spellcard;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.fastprojectileapi.entity.ProjectileMovement;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;

@SerialClass
public class ListSpellCard extends SpellCard {

	public static ListSpellCard of(ActualSpellCard... cards) {
		ListSpellCard ans = new ListSpellCard();
		ans.list.addAll(Arrays.asList(cards));
		return ans;
	}

	@SerialField
	public final ArrayList<ActualSpellCard> list = new ArrayList<>();

	@SerialField
	public int index, health = 20;

	public ActualSpellCard card() {
		return list.get(index);
	}

	@Override
	public void tick(CardHolder holder) {
		super.tick(holder);
		if (card().hit >= health) {
			index++;
			if (index == list.size()) {
				index = 0;
			}
			card().reset();
		}
		card().tick(holder);
	}

	@Override
	public ProjectileMovement move(int code, int tickCount, Vec3 vec) {
		return card().move(code, tickCount, vec);
	}

	public void hurt(CardHolder holder, DamageSource source, float amount) {
		super.hurt(holder, source, amount);
		card().hurt(holder, source, amount);
	}

	@Override
	public DamageSource getDanmakuDamageSource(IDanmakuEntity danmaku) {
		return card().getDanmakuDamageSource(danmaku);
	}

	@Override
	public void reset() {
		super.reset();
		for (var e : list) {
			e.reset();
		}
		index = 0;
	}

}
