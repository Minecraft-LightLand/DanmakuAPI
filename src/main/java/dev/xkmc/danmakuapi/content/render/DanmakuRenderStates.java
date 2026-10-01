package dev.xkmc.danmakuapi.content.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderer;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class DanmakuRenderStates extends RenderType {


	public DanmakuRenderStates(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
		super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
	}

	protected static final ShaderStateShard DANMAKU_SHADER = new ShaderStateShard(GameRenderer::getPositionTexColorShader);

	private static RenderType create(String name, ResourceLocation tex, boolean cull, DisplayType type) {
		return create(name,
				DefaultVertexFormat.POSITION_TEX_COLOR,
				VertexFormat.Mode.QUADS,
				256, true, type != DisplayType.SOLID,
				CompositeState.builder()
						.setShaderState(DANMAKU_SHADER)
						.setTextureState(new TextureStateShard(tex, false, false))
						.setTransparencyState(switch (type) {
							case SOLID -> NO_TRANSPARENCY;
							case TRANSPARENT -> TRANSLUCENT_TRANSPARENCY;
							case ADDITIVE -> ADDITIVE_TRANSPARENCY;
						})
						.setCullState(cull ? CULL : NO_CULL)
						.createCompositeState(false));
	}

	private static final BiFunction<ResourceLocation, DisplayType, RenderType> DANMAKU =
			Util.memoize((rl, type) -> create("danmaku_" + type.getName(), rl, false, type));
	private static final BiFunction<ResourceLocation, DisplayType, RenderType> LASER =
			Util.memoize((rl, type) -> create("laser_" + type.getName(), rl, true, type));
	private static final Function<DisplayType, RenderType> ITEM_MODEL =
			Util.memoize(type -> create("item_model_" + type.getName(), TextureAtlas.LOCATION_BLOCKS, false, type));

	public static RenderType danmaku(ResourceLocation rl, DisplayType type) {
		if (type == DisplayType.SOLID) type = DisplayType.TRANSPARENT;
		return DANMAKU.apply(rl, type);
	}

	public static RenderType laser(ResourceLocation rl, DisplayType type) {
		return LASER.apply(rl, type);
	}

	/**
	 * Render state for danmaku drawn as a baked item model. Such a model samples the item
	 * atlas, which in this version is the block atlas, so that is the bound texture.
	 * <p>
	 * It keeps {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#POSITION_TEX_COLOR} rather
	 * than the {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#NEW_ENTITY} vanilla item
	 * rendering uses: danmaku are always full bright and are shaded per quad into the vertex
	 * color, so the lightmap, overlay (no glint) and normal attributes can all be dropped,
	 * cutting a vertex from 40 to 24 bytes.
	 */
	public static RenderType itemModel(DisplayType type) {
		if (type == DisplayType.SOLID) type = DisplayType.TRANSPARENT;
		return ITEM_MODEL.apply(type);
	}

	public static int fading(DisplayType display, int col, ProjectileRenderer<?> r, SimplifiedProjectile e) {
		double perc = r.fading(e);
		if (perc == 0) return col;
		int alpha = (int) ((col >>> 24) * perc);
		if (display == DisplayType.ADDITIVE) {
			return 0xff000000 | alpha << 16 | alpha << 8 | alpha;
		}
		return (alpha << 24) | col & 0xffffff;
	}

}
