package dev.xkmc.danmakuapi.content.spell.spellcard;

import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.List;

@SerialClass
public class ActualSpellCard extends SpellCard {

	@SerialClass.SerialField
	public int tick, hit;

	@SerialClass.SerialField
	private final ArrayList<Ticker<?>> tickers = new ArrayList<>();

	private List<Ticker<?>> temp;

	@OverridingMethodsMustInvokeSuper
	public void tick(CardHolder holder) {
		tick++;
		temp = new ArrayList<>();
		tickers.removeIf(t -> t.tick(holder, Wrappers.cast(this)));
		tickers.addAll(temp);
		temp = null;
	}

	public void reset() {
		tick = 0;
		hit = 0;
		tickers.clear();
	}

	public void hurt(CardHolder holder, DamageSource source, float amount) {
		if (source.getEntity() instanceof LivingEntity && amount > 1) {
			hit++;
		}
	}

	public <T extends ActualSpellCard> void addTicker(Ticker<T> tick) {
		if (temp != null) temp.add(tick);
		else tickers.add(tick);
	}

}
