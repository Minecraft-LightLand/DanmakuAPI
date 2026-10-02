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
import dev.xkmc.fastprojectileapi.render.ProjTypeHolder;
import dev.xkmc.fastprojectileapi.render.RenderableProjectileType;
import dev.xkmc.l2serial.util.Wrappers;
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

	/**
	 * Fires one laser.
	 * <p>
	 * As with {@link DanmakuItem#use}, the throw itself is fixed and everything a laser item may
	 * want to change about it is a hook below, so a subclass customises a shot rather than copying
 * this method.
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (GrazeHelper.forbidDanmaku(player))
			return InteractionResultHolder.fail(stack);

		var event = new DanmakuUseEvent(player, stack, cooldown());
		NeoForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			return InteractionResultHolder.fail(stack);
		}
		playThrowSound(level, player);
		if (!level.isClientSide) {
			spawnLaser(newLaser(player, level), player, level, event);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, event.getCooldown());
		if (consume() && event.consume()) {
			stack.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/**
	 * The cooldown in ticks this item proposes to the {@link DanmakuUseEvent} it posts on use,
	 * which listeners may still raise or lower.
	 * <p>
	 * A laser that draws itself out over time is a shot rather than a beam, and is paced like a
	 * danmaku throw; one that appears at full length is paced like a beam.
	 */
	protected int cooldown() {
		return type.setupLength() ? DanmakuConfig.SERVER.playerDanmakuCooldown.get()
				: DanmakuConfig.SERVER.playerLaserCooldown.get();
	}

	/**
	 * Whether using this item costs one off the stack. A reusable shooter returns false, and is
	 * then also free of the {@link DanmakuUseEvent}'s own consume flag, since it has no stack to
	 * pay from however that flag is set.
	 */
	protected boolean consume() {
		return true;
	}

	/** How long the laser lives, in ticks, counted from the shot. */
	protected int life() {
		return DanmakuConfig.SERVER.playerLaserDuration.get();
	}

	/** The visual length of a laser that appears at full length, used for its hitbox. */
	protected float length() {
		return 40;
	}

	/**
	 * The laser this item fires. Override to fire a subclass of the shared danmaku laser;
	 * {@link #spawnLaser} is then the place to configure it further.
	 */
	protected ItemLaserEntity newLaser(Player player, Level level) {
		return new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), player, level);
	}

	/**
	 * The stack the laser carries, which is the beam it is drawn as and the laser type and colour
	 * read from. Usually the stack shot; override to fire one laser item out of another, as a
	 * reusable shooter does.
	 */
	protected ItemStack laserStack(Player player, DanmakuUseEvent event) {
		return event.getStack();
	}

	/**
	 * Fires the laser: gives it its stack, aims it down the player's view, adds it to the level and
	 * hands it to the player's spell tracking.
	 * <p>
	 * A laser whose {@link DanmakuLaser#setupLength()} is set draws itself out over a few ticks
	 * rather than appearing whole, and its length is worked out from {@link DanmakuLaser#visualLength()}
	 * so that the beam is the same length whichever item fired it; the delayed mover is what extends
	 * it, and {@link #length()} does not apply to that case.
	 */
	protected void spawnLaser(ItemLaserEntity danmaku, Player player, Level level, DanmakuUseEvent event) {
		danmaku.setItem(laserStack(player, event));
		int dur = life();
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
			danmaku.setup(type.damage(), dur, length(), false, player.getYRot(), player.getXRot());
		}
		level.addFreshEntity(danmaku);
		if (player instanceof ServerPlayer sp)
			SpellContainer.track(sp, danmaku);
	}

	/** The sound this item makes when fired. */
	protected void playThrowSound(Level level, Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	/**
	 * The dye colour a laser is drawn in. Layer 0 is the coloured core, layer 1 the untinted outer
	 * glow, so this is the item model's own tint hook as well as the beam's.
	 */
	public int getDanmakuColor(ItemStack stack, int i) {
		return i == 0 ? 0xff000000 | color.getFireworkColor() : 0xffffffff;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		list.add(DanmakuLang.DANMAKU_DAMAGE.get(type.damage()));
	}

	/**
	 * How this item's laser is drawn. The default is this mod's two-layer beam art in the item's
	 * colour, drawn as a pointed pencil for {@link DanmakuLaser#setupLength()} lasers and as a
	 * plain beam otherwise; override to draw something else.
	 * <p>
	 * Only ever called once per item, lazily, from the client; see {@link #getTypeForRender()}.
	 */
	protected RenderableProjectileType<?, ?> buildRenderer() {
		int col = 0xff000000 | color.getFireworkColor();
		var inner = DanmakuAPI.loc("textures/entity/laser/laser_inner.png");
		var outer = DanmakuAPI.loc("textures/entity/laser/laser_outer.png");
		return type.setupLength() ? new PencilLayerLaserType(inner, outer, col)
				: new DoubleLayerLaserType(inner, outer, col);
	}

	private ProjTypeHolder<? extends RenderableProjectileType<?, ?>, ?> render;

	/**
	 * The laser's render type, built on first use and then kept, because the bulk renderer groups
	 * everything on screen by this value and has to hold on to it between frames. Client only.
	 */
	public ProjTypeHolder<? extends RenderableProjectileType<?, ?>, ?> getTypeForRender() {
		if (render == null) {
			render = ProjTypeHolder.wrap(Wrappers.cast(buildRenderer()));
		}
		return render;
	}
}
