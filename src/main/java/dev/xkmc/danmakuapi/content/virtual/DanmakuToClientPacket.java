package dev.xkmc.danmakuapi.content.virtual;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.serialization.SerialClass;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.List;

@SerialClass
public class DanmakuToClientPacket extends SerialPacketBase {

	@SerialClass
	public static class Data {
		@SerialClass.SerialField
		private int typeId;
		@SerialClass.SerialField
		private int entityId;
		@SerialClass.SerialField
		private double posX, posY, posZ;
		@SerialClass.SerialField
		private float pitch, yaw;
		@SerialClass.SerialField
		private double velX, velY, velZ;

		public void restore(Entity e) {
			e.syncPacketPositionCodec(posX, posY, posZ);
			e.absMoveTo(posX, posY, posZ, yaw, pitch);
			e.setId(entityId);
			e.lerpMotion(velX, velY, velZ);
		}
	}

	@SerialClass.SerialField
	private Data[] entities;

	@SerialClass.SerialField
	private byte[] data;

	@SerialClass.SerialField
	private boolean friendly;

	public DanmakuToClientPacket() {
	}

	public DanmakuToClientPacket(Data[] entities, byte[] data, boolean friendly) {
		this.entities = entities;
		this.data = data;
		this.friendly = friendly;
	}

	public static DanmakuToClientPacket of(List<SimplifiedProjectile> list) {
		return of(list, false);
	}

	/**
	 * {@code friendly} applies to the whole batch: it is true when these danmaku
	 * cannot hurt the receiving player, so they render as if shot by the player.
	 * Targeting state is owner-level ({@link IDanmakuEntity#canHurt}), so callers
	 * evaluate it once per player.
	 */
	public static DanmakuToClientPacket of(List<SimplifiedProjectile> list, boolean friendly) {
		var data = new FriendlyByteBuf(Unpooled.buffer());
		var entities = new Data[list.size()];
		for (int i = 0; i < list.size(); i++) {
			var e = list.get(i);
			var dat = new Data();
			entities[i] = dat;
			dat.typeId = BuiltInRegistries.ENTITY_TYPE.getId(e.getType());
			dat.entityId = e.getId();
			dat.posX = e.getX();
			dat.posY = e.getY();
			dat.posZ = e.getZ();
			dat.pitch = e.getXRot();
			dat.yaw = e.getYRot();
			Vec3 vec3d = e.getDeltaMovement();
			dat.velX = vec3d.x;
			dat.velY = vec3d.y;
			dat.velZ = vec3d.z;
			e.writeSpawnData(data);
		}
		var arr = Arrays.copyOfRange(data.array(), 0, data.writerIndex());
		data.release();
		return new DanmakuToClientPacket(entities, arr, friendly);
	}

	@Override
	public void handle(NetworkEvent.Context ctx) {
		var buffer = Unpooled.wrappedBuffer(data);
		var buf = new FriendlyByteBuf(buffer);
		for (var dat : entities) {
			var type = BuiltInRegistries.ENTITY_TYPE.getHolder(dat.typeId);
			if (type.isEmpty()) break;
			Entity e = DanmakuClientHandler.create(type.get().value());
			if (!(e instanceof SimplifiedProjectile sp)) break;
			dat.restore(e);
			sp.readSpawnData(buf);
			if (sp instanceof IDanmakuEntity dan) {
				dan.setClientFriendly(friendly);
			}
			DanmakuClientHandler.add(sp);
		}
		buffer.release();
	}

}
