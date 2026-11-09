package dev.xkmc.danmakuapi.content.custom.forms;

import dev.xkmc.danmakuapi.content.custom.data.RingSpellFormData;
import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.spellcard.ActualSpellCard;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.util.Mth;

@SerialClass
public class RingSpellAct<C extends ActualSpellCard> extends ISpellAct<RingSpellFormData, C> {

	@SerialClass.SerialField
	private RingSpellFormData data;

	@SerialClass.SerialField
	private int step, tick;

	@Override
	public void init(RingSpellFormData data) {
		super.init(data);
		this.data = data;
	}

	@Override
	public boolean tick(CardHolder holder, C card) {
		var o = DanmakuHelper.getOrientation(holder.forward());
		var form = data.form();
		int n = form.branches();
		var rand = holder.random();
		while (tick >= step * form.delay() && step < form.steps()) {
			double s0 = form.steps() <= 1 ? data.speedFirst() : Mth.lerp(1d * step / (form.steps() - 1), data.speedFirst(), data.speedLast());
			for (int i = 0; i < n; i++) {
				double ax = (i - (n - 1) * 0.5) * form.branchAngle() +
						(step - (form.steps() - 1) * 0.5) * form.stepAngle();
				double ay = (step - (form.steps() - 1) * 0.5) * form.stepVerticalAngle();
				double rx = (rand.nextDouble() * 2 - 1) * form.randomizedAngle();
				double ry = (rand.nextDouble() * 2 - 1) * form.randomizedAngle();
				double s1 = s0 * (1 + (rand.nextDouble() * 2 - 1) * data.randomizedSpeed());
				var dir = o.rotateDegrees(ax + rx, ay + ry);
				var e = data.base().prepare(holder, dir, s1);
				holder.shoot(e);
			}
			step++;
		}
		tick++;
		return step >= form.steps() & super.tick(holder, card);
	}

}
