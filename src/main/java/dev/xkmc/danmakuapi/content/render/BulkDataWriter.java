package dev.xkmc.danmakuapi.content.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class BulkDataWriter {

	private final VertexConsumer vc;
	private final BufferBuilder direct;

	public BulkDataWriter(VertexConsumer vc, int size) {
		this.vc = vc;
		direct = vc instanceof BufferBuilder buf ? buf : null;
	}

	public void addVertex(Matrix4f m4, float x, float y, float z, float u, float v, int col) {
		var vec = new Vector4f(x, y, z, 1).mul(m4);
		addVertex(vec.x, vec.y, vec.z, u, v, col);
	}

	public void addVertex(float x, float y, float z, float u, float v, int col) {
		vc.vertex(x, y, z).uv(u, v).color(col).endVertex();
	}

	public void flush() {
	}

}
