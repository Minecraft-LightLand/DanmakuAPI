package dev.xkmc.danmakuapi.content.item;

import dev.xkmc.danmakuapi.content.custom.data.ISpellFormData;
import dev.xkmc.danmakuapi.content.custom.screen.ClientCustomSpellHandler;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.data.DanmakuLang;
import dev.xkmc.l2library.util.raytrace.IGlowingTarget;
import dev.xkmc.l2library.util.raytrace.RayTraceUtil;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomSpellItem extends Item implements IGlowingTarget {

	private final ISpellFormData<?> def;
	private final boolean requireTarget;

	public CustomSpellItem(Properties properties, boolean requireTarget, ISpellFormData<?> def) {
		super(properties);
		this.requireTarget = requireTarget;
		this.def = def;
	}

	private ISpellFormData<?> getData(ItemStack stack) {
		var tag = stack.getTag();
		if (tag != null && tag.contains("SpellData")) {
			var obj = TagCodec.valueFromTag(tag.getCompound("SpellData"), Record.class);
			if (obj instanceof ISpellFormData<?> dat)
				return dat;
		}
		return def;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(stack);
		ISpellFormData<?> data = getData(stack);
		if (player.isShiftKeyDown()) {
			if (level.isClientSide()) {
				ClientCustomSpellHandler.open(stack.getHoverName(), data);
			}
		} else {
			LivingEntity target = RayTraceUtil.serverGetTarget(player);
			if (requireTarget && target == null)
				return InteractionResultHolder.fail(stack);
			if (!player.getAbilities().instabuild) {
				Item ammo = data.getAmmoCost();
				int toCost = data.cost();
				if (!consumeAmmo(ammo, toCost, player, false))
					return InteractionResultHolder.fail(stack);
				if (player instanceof ServerPlayer)
					consumeAmmo(ammo, toCost, player, true);
			}
			if (player instanceof ServerPlayer sp) {
				SpellContainer.castSpell(sp, data::createInstance, target);
				player.getCooldowns().addCooldown(this, data.getDuration());
			}
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
		ISpellFormData<?> data = getData(stack);
		list.add(DanmakuLang.SPELL_COST.get(data.cost(), data.getAmmoCost().getDefaultInstance().getHoverName()));
		if (requireTarget) {
			list.add(DanmakuLang.SPELL_TARGET.get());
		}
	}

	private static boolean consumeAmmo(Item ammo, int toCost, Player player, boolean execute) {
		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (toCost <= 0) break;
			ItemStack item = inv.getItem(i);
			if (item.is(ammo)) {
				int consume = Math.min(toCost, item.getCount());
				if (execute) item.shrink(consume);
				toCost -= consume;
			}
		}
		return toCost == 0;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity user, int slot, boolean sel) {
		if (user instanceof Player player && level.isClientSide && sel) {
			RayTraceUtil.clientUpdateTarget(player, 64);
		}
	}

	@Override
	public int getDistance(ItemStack stack) {
		return 64;
	}

}
