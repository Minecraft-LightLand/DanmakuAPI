package dev.xkmc.danmakuapi.content.custom.forms;

import dev.xkmc.danmakuapi.content.spell.spellcard.ActualSpellCard;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.Ticker;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.util.Wrappers;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.List;

@SerialClass
public abstract class ISpellAct<D, C extends ActualSpellCard> extends Ticker<C> {


	@SerialClass.SerialField
	private final ArrayList<Ticker<?>> tickers = new ArrayList<>();

	private List<Ticker<?>> temp;

	@OverridingMethodsMustInvokeSuper
	public void init(D data) {
		tickers.clear();
	}

	@OverridingMethodsMustInvokeSuper
	public boolean tick(CardHolder holder, C card) {
		tick++;
		temp = new ArrayList<>();
		tickers.removeIf(t -> t.tick(holder, Wrappers.cast(this)));
		tickers.addAll(temp);
		temp = null;
		return tickers.isEmpty();
	}

	public <T extends ISpellAct<?, ?>> void addTicker(Ticker<T> tick) {
		if (temp != null) temp.add(tick);
		else tickers.add(tick);
	}

}
