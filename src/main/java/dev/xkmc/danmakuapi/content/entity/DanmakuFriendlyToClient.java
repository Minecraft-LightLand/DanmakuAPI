package dev.xkmc.danmakuapi.content.entity;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

@SerialClass
public class DanmakuFriendlyToClient extends SerialPacketBase {

	@SerialClass.SerialField
	private int id;
	@SerialClass.SerialField
	private boolean friendly;

	public DanmakuFriendlyToClient() {
	}

	public DanmakuFriendlyToClient(int id, boolean friendly) {
		this.id = id;
		this.friendly = friendly;
	}

	@Override
	public void handle(NetworkEvent.Context ctx) {
		var level = Minecraft.getInstance().level;
		if (level != null && level.getEntity(id) instanceof IDanmakuEntity dan) {
			dan.setClientFriendly(friendly);
		}
	}

}
