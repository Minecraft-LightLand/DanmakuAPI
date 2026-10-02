package dev.xkmc.danmakuapi.content.item;

import dev.xkmc.danmakuapi.api.DanmakuBullet;
import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.render.ButterflyProjectileType;
import dev.xkmc.danmakuapi.content.render.FlatProjectileType;
import dev.xkmc.danmakuapi.content.render.ItemModelProjectileType;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.content.render.RotatingProjectileType;
import dev.xkmc.danmakuapi.content.render.SimpleProjectileType;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.danmakuapi.init.data.DanmakuLang;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuSounds;
import dev.xkmc.fastprojectileapi.render.ProjTypeHolder;
import dev.xkmc.fastprojectileapi.render.RenderableProjectileType;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

import static dev.xkmc.danmakuapi.init.registrate.DanmakuItems.Bullet.*;

public class DanmakuItem extends Item {

	public final DanmakuBullet type;
	public final DyeColor color;
	public final float size;

	public DanmakuItem(Properties pProperties, DanmakuBullet type, DyeColor color, float size) {
		super(pProperties);
		this.type = type;
		this.color = color;
		this.size = size;
	}

	/**
	 * Throws one danmaku.
	 * <p>
	 * The whole throw is here, and everything an item might want to change about it is pulled out
	 * into the hooks below, so that a subclass customises a throw rather than copying this method.
	 * The throw is deliberately split into those steps: a reusable shooter changes
	 * {@link #consume()} and {@link #bulletStack}, a thrown weapon that comes back changes
	 * {@link #newBullet} and {@link #spawnBullet}, and neither has to restate the rest of a throw
	 * that happens to be identical.
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
			spawnBullet(newBullet(player, level), player, level, event);
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
	 * which listeners may still raise or lower. Only reached on the server's config value.
	 */
	protected int cooldown() {
		return DanmakuConfig.SERVER.playerDanmakuCooldown.get();
	}

	/**
	 * Whether using this item costs one off the stack. A reusable shooter returns false, and is
	 * then also free of the {@link DanmakuUseEvent}'s own consume flag, since it has no stack to
	 * pay from however that flag is set.
	 */
	protected boolean consume() {
		return true;
	}

	/** How long the thrown danmaku lives, in ticks, counted from the throw. */
	protected int life() {
		return 40;
	}

	/**
	 * The bullet this item throws. Override to throw a subclass of the shared danmaku bullet;
	 * {@link #spawnBullet} is then the place to configure it further.
	 */
	protected ItemBulletEntity newBullet(Player player, Level level) {
		return new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), player, level);
	}

	/**
	 * The stack the bullet carries, which is the danmaku it is drawn as, the item it is worth when
	 * it lands, and what its own {@link DanmakuItem} type and colour are read from. Usually the
	 * stack thrown; override to fire one danmaku item out of another, as a reusable shooter does.
	 */
	protected ItemStack bulletStack(Player player, DanmakuUseEvent event) {
		return event.getStack();
	}

	/**
	 * Throws the bullet: gives it its stack, aims it down the player's view, adds it to the level
	 * and hands it to the player's spell tracking.
	 * <p>
	 * The event is passed as well as the stack because only the throw knows whether it cost its
	 * owner anything, which is not something the bullet can work out for itself once it is in
	 * flight: a throw made in creative, or one a listener cleared {@link DanmakuUseEvent#consume()}
	 * on, paid for nothing and must not hand anything back.
	 */
	protected void spawnBullet(ItemBulletEntity danmaku, Player player, Level level, DanmakuUseEvent event) {
		danmaku.setItem(bulletStack(player, event));
		danmaku.setup(type.damage(), life(), false, type.bypass(),
				RayTraceUtil.getRayTerm(Vec3.ZERO, player.getXRot(), player.getYRot(), 2));
		danmaku.moveTo(RayTraceUtil.getRayTerm(player.getEyePosition(), player.getXRot(), player.getYRot(), 2));
		level.addFreshEntity(danmaku);
		if (player instanceof ServerPlayer sp)
			SpellContainer.track(sp, danmaku);
	}

	/** The sound this item makes when thrown. */
	protected void playThrowSound(Level level, Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), DanmakuSounds.SHOOT.get(), SoundSource.PLAYERS,
				0.5F, 1.0F);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		list.add(DanmakuLang.DANMAKU_DAMAGE.get(type.damage()));
		if (type.bypass())
			list.add(DanmakuLang.DANMAKU_BYPASS.get());
	}

	/**
	 * How this item's danmaku is drawn. The default is this mod's art for {@link #type}, so an item
	 * that is just a recoloured or resized danmaku needs no renderer of its own; override to draw
	 * something else, in particular to draw from the item's own model via
	 * {@link ItemModelProjectileType} instead of a texture.
	 * <p>
	 * Only ever called once per item, lazily, from the client; see {@link #getTypeForRender()}.
	 */
	protected RenderableDanmakuType<?, ?> buildRenderer() {
		var loc = DanmakuAPI.loc("textures/entity/bullet/" + type.getName() + "/" + color.getName() + ".png");
		return switch (type) {
			case BUTTERFLY -> new ButterflyProjectileType(loc, type.display(), 20);
			case SPARK -> new RotatingProjectileType(loc, type.display(), 20);
			case STAR -> new RotatingProjectileType(loc, type.display(), 40);
			case CARD -> new FlatProjectileType(loc, type.display(), 60);
			case DAGGER -> new ItemModelProjectileType(this, type.display(), 0);
			default -> new SimpleProjectileType(loc, type.display());
		};
	}

	private ProjTypeHolder<? extends RenderableProjectileType<?, ?>, ?> render;

	/**
	 * The danmaku's render type, built on first use and then kept, because the bulk renderer groups
	 * every danmaku in the world by this value and has to hold on to it between frames. Client
	 * only.
	 */
	public ProjTypeHolder<? extends RenderableProjectileType<?, ?>, ?> getTypeForRender() {
		if (render == null) {
			render = ProjTypeHolder.wrap(Wrappers.cast(buildRenderer()));
		}
		return render;
	}

	/**
	 * How much of the owner's own fading this item's danmaku keeps, so that a throw which is hard
	 * to see from behind the thrower can stay visible to them without staying visible to everyone.
	 * Called by {@link dev.xkmc.danmakuapi.content.entity.ItemBulletRenderer} for danmaku whose
	 * owner is the camera.
	 */
	public double modifyFading(double selfFading) {
		return selfFading;
	}

}
