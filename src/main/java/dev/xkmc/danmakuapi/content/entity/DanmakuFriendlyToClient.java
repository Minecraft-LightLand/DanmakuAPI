package dev.xkmc.danmakuapi.content.entity;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

public record DanmakuFriendlyToClient(int id, boolean friendly) implements SerialPacketBase<DanmakuFriendlyToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().getEntity(id) instanceof IDanmakuEntity dan) {
			dan.setClientFriendly(friendly);
		}
	}

}
