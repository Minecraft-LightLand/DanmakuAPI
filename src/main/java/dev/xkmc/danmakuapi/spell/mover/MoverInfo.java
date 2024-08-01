package dev.xkmc.danmakuapi.spell.mover;

import dev.xkmc.danmakuapi.entity.danmaku.IYHDanmaku;
import net.minecraft.world.phys.Vec3;

public record MoverInfo(int tick, Vec3 prevPos, Vec3 prevVel, IYHDanmaku self) {

	public MoverInfo offsetTime(int i) {
		return new MoverInfo(tick + i, prevPos, prevVel, self);
	}

}
