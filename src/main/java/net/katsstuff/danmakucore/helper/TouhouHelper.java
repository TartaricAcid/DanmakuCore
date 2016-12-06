/*
 * This class was created by <Katrix>. It's distributed as
 * part of the DanmakuCore Mod. Get the Source Code in github:
 * https://github.com/Katrix-/DanmakuCore
 *
 * DanmakuCore is Open Source and distributed under the
 * the DanmakuCore license: https://github.com/Katrix-/DanmakuCore/blob/master/LICENSE.md
 */
package net.katsstuff.danmakucore.helper;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.katsstuff.danmakucore.capability.CapabilityDanmakuCoreData;
import net.katsstuff.danmakucore.capability.IDanmakuCoreData;
import net.katsstuff.danmakucore.data.Vector3;
import net.katsstuff.danmakucore.entity.EntityFallingData;
import net.katsstuff.danmakucore.entity.spellcard.EntitySpellcard;
import net.katsstuff.danmakucore.entity.spellcard.Spellcard;
import net.katsstuff.danmakucore.misc.LogicalSideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;

public class TouhouHelper {

	/**
	 * Checks if a player can declare a given spellcard.
	 *
	 * @return The target for the spellcard, if a spellcard can be declared.
	 * Note that this can return some and the declaration can still fail, if
	 * it does, it's because the spellcard denied it.
	 */
	public static Optional<EntityLivingBase> canPlayerDeclareSpellcard(EntityPlayer player, Spellcard spellcard) {
		World world = player.worldObj;

		//Only allow spellcard if use user isn't already using a spellcard
		@SuppressWarnings("ConstantConditions") List<EntitySpellcard> spellcardList = world.getEntitiesWithinAABB(EntitySpellcard.class,
				player.getEntityBoundingBox().expandXyz(32D), entitySpellcard -> player == entitySpellcard.getUser());

		if(spellcardList.isEmpty()) {
			Optional<Entity> optTarget = Vector3.getEntityLookedAt(player,
					targetEntity -> targetEntity instanceof EntityLivingBase && !(targetEntity instanceof EntityAgeable), 32D);

			if(optTarget.isPresent()) {
				EntityLivingBase target = (EntityLivingBase)optTarget.get();

				if(!player.capabilities.isCreativeMode) {

					int neededBombs = spellcard.getLevel();
					int currentBombs = getDanmakuCoreData(player).map(IDanmakuCoreData::getBombs).orElse(0);

					if(currentBombs >= neededBombs) {
						return Optional.of(target);
					}
				}
				else return Optional.of(target);
			}
		}

		return Optional.empty();
	}

	/**
	 * Declares a spellcard as a player, doing the necessary checks and
	 * changes according to the player.
	 *
	 * @return The Spellcard if it was spawned.
	 */
	@SuppressWarnings("SameParameterValue")
	public static Optional<EntitySpellcard> declareSpellcardPlayer(EntityPlayer player, Spellcard spellcard, boolean firstAttack) {
		Optional<EntityLivingBase> canDeclareSpellcard = canPlayerDeclareSpellcard(player, spellcard);

		if(canDeclareSpellcard.isPresent()) {
			if(!player.capabilities.isCreativeMode) {
				changeAndSyncPlayerData(data -> data.addBombs(-spellcard.getLevel()), player);
			}

			return declareSpellcard(player, canDeclareSpellcard.get(), spellcard, firstAttack);
		}

		return Optional.empty();
	}

	/**
	 * Declares a spellcard. If you are declaring a spellcard for a player,
	 * user {@link TouhouHelper#declareSpellcardPlayer(EntityPlayer, Spellcard, boolean)}.
	 *
	 * @return The Spellcard if it was spawned.
	 */
	public static Optional<EntitySpellcard> declareSpellcard(EntityLivingBase user, @Nullable EntityLivingBase target, Spellcard spellCard,
			boolean firstAttack) {
		if(spellCard.onDeclare(user, target, firstAttack)) {
			EntitySpellcard entitySpellCard = new EntitySpellcard(user, target, spellCard);
			user.worldObj.spawnEntityInWorld(entitySpellCard);

			//Does chat
			if(user instanceof EntityPlayer) {
				user.addChatMessage(new TextComponentTranslation(spellCard.getUnlocalizedName()));
			}

			if(firstAttack) {
				if(target instanceof EntityPlayer) {
					target.addChatMessage(new TextComponentTranslation(spellCard.getUnlocalizedName()));
				}

				DanmakuHelper.danmakuRemove(user, 40.0F, DanmakuHelper.DanmakuRemoveMode.ENEMY, true);
			}

			return Optional.of(entitySpellCard);
		}

		return Optional.empty();
	}

	public static Optional<IDanmakuCoreData> getDanmakuCoreData(ICapabilityProvider provider) {
		if(provider.hasCapability(CapabilityDanmakuCoreData.DANMAKUCORE_DATA_CAPABILITY, null)) {
			return Optional.of(provider.getCapability(CapabilityDanmakuCoreData.DANMAKUCORE_DATA_CAPABILITY, null));
		}
		else return Optional.empty();
	}

	@SuppressWarnings("unused")
	@LogicalSideOnly(Side.SERVER)
	public static void changeAndSyncEntityData(Consumer<IDanmakuCoreData> dataRunnable, Entity target, double radius) {
		getDanmakuCoreData(target).ifPresent(data -> {
			dataRunnable.accept(data);

			NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(target.dimension, target.posX, target.posY, target.posZ, radius);
			data.syncToClose(point, target);
		});
	}

	@LogicalSideOnly(Side.SERVER)
	public static void changeAndSyncPlayerData(Consumer<IDanmakuCoreData> dataRunnable, EntityPlayer player) {
		getDanmakuCoreData(player).ifPresent(data -> {
			dataRunnable.accept(data);

			if(player instanceof EntityPlayerMP) {
				data.syncTo((EntityPlayerMP)player, player);
			}
		});
	}

	public static Vector3 fuzzPosition(Vector3 pos) {
		return pos.add(Vector3.randomVector());
	}

	public static EntityFallingData createScoreGreen(World world, @Nullable Entity target, Vector3 pos, Vector3 angle) {
		return new EntityFallingData(world, EntityFallingData.DataType.SCORE_GREEN,
				fuzzPosition(pos), Vector3.angleLimitRandom(angle, 7.5F), target, 10);
	}

	public static EntityFallingData createScoreBlue(World world, @Nullable Entity target, Vector3 pos, Vector3 angle) {
		return new EntityFallingData(world, EntityFallingData.DataType.SCORE_BLUE,
				fuzzPosition(pos), Vector3.angleLimitRandom(angle, 7.5F), target, 100);
	}

	public static EntityFallingData createPower(World world, Vector3 pos, Vector3 angle) {
		return new EntityFallingData(world, EntityFallingData.DataType.POWER,
				fuzzPosition(pos), Vector3.angleLimitRandom(angle, 7.5F), null, 0.05F);
	}

	public static EntityFallingData createBigPower(World world, Vector3 pos, Vector3 angle) {
		return new EntityFallingData(world, EntityFallingData.DataType.BIG_POWER,
				fuzzPosition(pos), Vector3.angleLimitRandom(angle, 7.5F), null, 1F);
	}

	public static EntityFallingData createLife(World world, Vector3 pos, Vector3 angle) {
		return new EntityFallingData(world, EntityFallingData.DataType.LIFE,
				fuzzPosition(pos), Vector3.angleLimitRandom(angle, 7.5F), null, 1);
	}

	public static EntityFallingData createBomb(World world, Vector3 pos, Vector3 angle) {
		return new EntityFallingData(world, EntityFallingData.DataType.BOMB,
				fuzzPosition(pos), Vector3.angleLimitRandom(angle, 7.5F), null, 1);
	}
}
