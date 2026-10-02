package dev.xkmc.danmakuapi.content.item;

import dev.xkmc.danmakuapi.api.DanmakuLaser;
import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.entity.ItemLaserEntity;
import dev.xkmc.danmakuapi.content.render.DoubleLayerLaserType;
import dev.xkmc.danmakuapi.content.render.PencilLayerLaserType;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.danmakuapi.init.data.DanmakuLang;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuSounds;
import dev.xkmc.fastprojectileapi.render.ProjTypeHolder;
import dev.xkmc.fastprojectileapi.render.RenderableProjectileType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

import static dev.xkmc.danmakuapi.init.registrate.DanmakuItems.Laser.LASER;
import static dev.xkmc.danmakuapi.init.registrate.DanmakuItems.Laser.PENCIL;

public class LaserItem extends Item {

	public final DyeColor color;
	public final DanmakuLaser type;
	public final float size;

	public LaserItem(Properties pProperties, DanmakuLaser type, DyeColor color, float size) {
		super(pProperties);
		this.color = color;
		this.type = type;
		this.size = size;
	}

	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (GrazeHelper.forbidDanmaku(player))
			return InteractionResultHolder.fail(stack);

		int cooldown = type.setupLength() ? DanmakuConfig.SERVER.playerDanmakuCooldown.get() : DanmakuConfig.SERVER.playerLaserCooldown.get();
		var event = new DanmakuUseEvent(player, stack, cooldown);
		NeoForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			return InteractionResultHolder.fail(stack);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), DanmakuSounds.LASER.get(), SoundSource.PLAYERS,
				0.5F, 1.0F);
		if (!level.isClientSide) {
			ItemLaserEntity danmaku = new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), player, level);
			danmaku.setItem(stack);
			int dur = DanmakuConfig.SERVER.playerLaserDuration.get();
			if (type.setupLength()) {
				int delay = 4;
				float v = 2f;
				float lenAll = v * delay;
				float vl = type.visualLength();
				float len = lenAll / vl;
				float v0 = (vl - 1) / 2 * v;
				danmaku.setup(type.damage(), dur, len, false, player.getYRot(), player.getXRot());
				danmaku.setupLength = true;
				danmaku.setupTime(1, delay, dur, 1);
				danmaku.setDelayedMover(v0, v, 1, delay);
			} else {
				danmaku.setup(type.damage(), dur, 40, false, player.getYRot(), player.getXRot());
			}
			level.addFreshEntity(danmaku);
			if (player instanceof ServerPlayer sp)
				SpellContainer.track(sp, danmaku);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, event.getCooldown());
		if (event.consume()) {
			stack.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	public int getDanmakuColor(ItemStack stack, int i) {
		return i == 0 ? 0xff000000 | color.getFireworkColor() : 0xffffffff;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		list.add(DanmakuLang.DANMAKU_DAMAGE.get(type.damage()));
	}


	private ProjTypeHolder<? extends RenderableProjectileType<?, ?>, ?> render;

	public ProjTypeHolder<? extends RenderableProjectileType<?, ?>, ?> getTypeForRender() {
		if (render == null) {
			render = switch (type) {
				case LASER -> ProjTypeHolder.wrap(new DoubleLayerLaserType(
						DanmakuAPI.loc("textures/entity/laser/laser_inner.png"),
						DanmakuAPI.loc("textures/entity/laser/laser_outer.png"),
						0xff000000 | color.getFireworkColor()));
				case PENCIL -> ProjTypeHolder.wrap(new PencilLayerLaserType(
						DanmakuAPI.loc("textures/entity/laser/laser_inner.png"),
						DanmakuAPI.loc("textures/entity/laser/laser_outer.png"),
						0xff000000 | color.getFireworkColor()));
				default -> throw new IllegalStateException();//TODO
			};
		}
		return render;
	}
}
