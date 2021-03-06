/*
 * This class was created by <Katrix>. It's distributed as
 * part of the DanmakuCore Mod. Get the Source Code in github:
 * https://github.com/Katrix-/DanmakuCore
 *
 * DanmakuCore is Open Source and distributed under the
 * the DanmakuCore license: https://github.com/Katrix-/DanmakuCore/blob/master/LICENSE.md
 */
package net.katsstuff.danmakucore.capability;

import java.util.Optional;

import javax.annotation.Nullable;

import net.katsstuff.danmakucore.entity.danmaku.DamageSourceDanmaku;
import net.katsstuff.danmakucore.handler.ConfigHandler;
import net.katsstuff.danmakucore.helper.TouhouHelper;
import net.katsstuff.danmakucore.lib.LibMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class DanmakuCoreDataHandler {

	@SubscribeEvent
	public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		TouhouHelper.getDanmakuCoreData(event.player).ifPresent(data -> {
			if(event.player instanceof EntityPlayerMP) {
				data.syncTo((EntityPlayerMP)event.player, event.player);
			}
		});
	}

	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		TouhouHelper.getDanmakuCoreData(event.player).ifPresent(data -> {
			if(event.player instanceof EntityPlayerMP) {
				data.syncTo((EntityPlayerMP)event.player, event.player);
			}
		});
	}

	@SubscribeEvent
	public void attachPlayer(AttachCapabilitiesEvent<Entity> event) {
		if(event.getObject() instanceof EntityPlayer) {
			event.addCapability(new ResourceLocation(LibMod.MODID, "DanmakuCoreData"), new DanmakuCoreDataProvider());
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		EntityLivingBase living = event.getEntityLiving();
		if(!living.world.isRemote && event.getSource() instanceof DamageSourceDanmaku && living instanceof EntityPlayer
				&& TouhouHelper.getDanmakuCoreData(living).map(IDanmakuCoreData::getLives).orElse(0) >= 1) {
			EntityPlayer player = (EntityPlayer)living;
			TouhouHelper.changeAndSyncPlayerData(data -> {
				if(ConfigHandler.gameplay.resetBombsOnDeath) {
					data.setBombs(ConfigHandler.gameplay.defaultBombsAmount);
				}
				data.removeLife();
			}, player);
			player.isDead = false;
			player.setHealth(player.getMaxHealth());
			player.hurtResistantTime = 50;
			//TODO: Fancy sound and animation
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2F, 1F);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
		if(!event.isWasDeath()) {
			EntityPlayer oldPlayer = event.getOriginal();
			EntityPlayer newPlayer = event.getEntityPlayer();

			Optional<IDanmakuCoreData> optOldData = TouhouHelper.getDanmakuCoreData(oldPlayer);
			if(optOldData.isPresent()) {
				IDanmakuCoreData oldData = optOldData.get();
				TouhouHelper.changeAndSyncPlayerData(newData -> {
					newData.setPower(oldData.getPower());
					newData.setScore(oldData.getScore());
					newData.setBombs(oldData.getBombs());
					newData.setLives(oldData.getLives());
				}, newPlayer);
			}
		}
	}

	public static class DanmakuCoreDataProvider implements ICapabilitySerializable<NBTTagCompound> {

		@CapabilityInject(IDanmakuCoreData.class)
		private static Capability<IDanmakuCoreData> CORE_DATA = null;


		private final IDanmakuCoreData data = new BoundedDanmakuCoreData(0F, 0, ConfigHandler.gameplay.defaultLivesAmount,
				ConfigHandler.gameplay.defaultBombsAmount, 4F, 9);

		@Override
		public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
			return capability == CORE_DATA;
		}

		@Override
		public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
			return capability == CORE_DATA ? CORE_DATA.cast(data) : null;
		}

		@Override
		public NBTTagCompound serializeNBT() {
			return (NBTTagCompound)CORE_DATA.writeNBT(data, null);
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			CORE_DATA.readNBT(data, null, nbt);
		}
	}
}
