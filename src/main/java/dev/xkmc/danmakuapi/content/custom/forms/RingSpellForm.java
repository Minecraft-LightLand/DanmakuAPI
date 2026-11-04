package dev.xkmc.danmakuapi.content.custom.forms;

import dev.xkmc.danmakuapi.content.custom.data.RingSpellFormData;
import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.item.PlayerHolder;
import dev.xkmc.danmakuapi.init.registrate.DanmakuSounds;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

@SerialClass
public class RingSpellForm extends ISpellForm<RingSpellFormData> {

	@SerialField
	private RingSpellFormData data;

	@SerialField
	private int step, tick;

	@Override
	public void init(RingSpellFormData data) {
		this.data = data;
	}

	@Override
	public boolean tick(LivingEntity player) {
		if (holder == null)
			holder = new PlayerHolder(player, dir, this, null);
		var o = DanmakuHelper.getOrientation(dir);
		var form = data.form();
		int n = form.branches();
		var rand = holder.random();
		while (tick >= step * form.delay() && step < form.steps()) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DanmakuSounds.SHOOT_3.get(), SoundSource.PLAYERS,
				0.5F, 1.0F);
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
		return step >= form.steps() & super.tick(player);
	}

}
