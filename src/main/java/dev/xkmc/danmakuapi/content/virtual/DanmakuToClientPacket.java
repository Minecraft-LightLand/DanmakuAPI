package dev.xkmc.danmakuapi.content.virtual;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@SerialClass
public record DanmakuToClientPacket(Data[] entities, byte[] data) implements SerialPacketBase<DanmakuToClientPacket> {

	@SerialClass
	public static class Data {
		@SerialField
		private int typeId;
		@SerialField
		private int entityId;
		@SerialField
		private double posX, posY, posZ;
		@SerialField
		private float pitch, yaw;
		@SerialField
		private double velX, velY, velZ;
		@SerialField
		private boolean friendly;

		public void restore(Entity e) {
			e.syncPacketPositionCodec(posX, posY, posZ);
			e.absMoveTo(posX, posY, posZ, yaw, pitch);
			e.setId(entityId);
			e.lerpMotion(velX, velY, velZ);
		}
	}


	public static DanmakuToClientPacket of(RegistryAccess access, List<SimplifiedProjectile> list) {
		return of(access, list, null);
	}

	/**
	 * {@code friendly} is true when the danmaku cannot hurt {@code target},
	 * so it renders as if shot by the player on their client.
	 */
	public static DanmakuToClientPacket of(RegistryAccess access, List<SimplifiedProjectile> list, @Nullable ServerPlayer target) {
		var data = new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE);
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
			dat.friendly = target != null && !IDanmakuEntity.mayHurtTarget(e, target);
			e.writeSpawnData(data);
		}
		var arr = Arrays.copyOfRange(data.array(), 0, data.writerIndex());
		data.release();
		return new DanmakuToClientPacket(entities, arr);
	}

	@Override
	public void handle(Player player) {
		var buffer = Unpooled.wrappedBuffer(data);
		var buf = new RegistryFriendlyByteBuf(buffer, player.registryAccess(), ConnectionType.NEOFORGE);
		for (var dat : entities) {
			var type = BuiltInRegistries.ENTITY_TYPE.getHolder(dat.typeId);
			if (type.isEmpty()) break;
			Entity e = DanmakuClientHandler.create(type.get().value());
			if (!(e instanceof SimplifiedProjectile sp)) break;
			dat.restore(e);
			sp.readSpawnData(buf);
			if (sp instanceof IDanmakuEntity dan) {
				dan.setClientFriendly(dat.friendly);
			}
			DanmakuClientHandler.add(sp);
		}
		buffer.release();
	}

}
