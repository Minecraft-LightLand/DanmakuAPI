package dev.xkmc.danmakuapi.content.custom.screen;

import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraftforge.network.NetworkEvent;

@SerialClass
public class SpellSetToServer extends SerialPacketBase {

	@SerialClass.SerialField
	public Record data;

	public SpellSetToServer() {
	}

	public SpellSetToServer(Record data) {
		this.data = data;
	}

	@Override
	public void handle(NetworkEvent.Context ctx) {
		ServerCustomSpellHandler.handle(ctx.getSender(), data);
	}

}
