/*
 * This class was created by <Katrix>. It's distributed as
 * part of the DanmakuCore Mod. Get the Source Code in github:
 * https://github.com/Katrix-/DanmakuCore
 *
 * DanmakuCore is Open Source and distributed under the
 * the DanmakuCore license: https://github.com/Katrix-/DanmakuCore/blob/master/LICENSE.md
 */
package net.katsstuff.danmakucore.impl.phase;

import java.util.Optional;

import net.katsstuff.danmakucore.entity.living.EntityDanmakuMob;
import net.katsstuff.danmakucore.entity.living.phase.Phase;
import net.katsstuff.danmakucore.entity.living.phase.PhaseManager;
import net.katsstuff.danmakucore.entity.living.phase.PhaseType;
import net.katsstuff.danmakucore.entity.spellcard.EntitySpellcard;
import net.katsstuff.danmakucore.entity.spellcard.Spellcard;
import net.katsstuff.danmakucore.helper.TouhouHelper;
import net.katsstuff.danmakucore.lib.data.LibItems;
import net.katsstuff.danmakucore.registry.DanmakuRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class PhaseTypeSpellcard extends PhaseType {

	@Override
	public Phase instantiate(PhaseManager manager) {
		return new PhaseSpellcard(manager, this, DanmakuRegistry.SPELLCARD.getRandomObject(manager.entity.getRNG()));
	}

	public PhaseSpellcard instantiate(PhaseManager manager, Spellcard spellcard) {
		return new PhaseSpellcard(manager, this, spellcard);
	}

	public static class PhaseSpellcard extends Phase {

		public static final String NBT_SPELLCARD = "spellcard";
		public static final String NBT_FIRST_ATTACK = "firstAttack";

		private Spellcard spellcard;
		private final PhaseTypeSpellcard type;
		private boolean firstAttack;

		public PhaseSpellcard(PhaseManager manager, PhaseTypeSpellcard type, Spellcard spellcard) {
			super(manager);
			this.type = type;
			this.spellcard = spellcard;
		}

		@Override
		public void init() {
			if(spellcard == null) {
				getEntity().setDead();
				return;
			}

			super.init();
			interval = spellcard.getEndTime();
			firstAttack = true;

			getEntity().hurtResistantTime = 40;
		}

		@Override
		public void serverUpdate() {
			super.serverUpdate();

			if(spellcard == null) {
				getEntity().setDead();
				return;
			}

			EntityDanmakuMob entity = getEntity();
			EntityLivingBase target = entity.getAttackTarget();

			if(!isFrozen() && (isCounterStart() || firstAttack) && target != null && entity.getEntitySenses().canSee(target)) {
				Optional<EntitySpellcard> optSpellcard = TouhouHelper.declareSpellcard(entity, target, spellcard, firstAttack, false);
				if(optSpellcard.isPresent()) {
					EntitySpellcard spellcard = optSpellcard.get();
					firstAttack = false;
					spellcard.getSpellCard().setDanmakuLevel(level);
				}
			}
		}

		@Override
		public PhaseType getType() {
			return type;
		}

		public Optional<Spellcard> getSpellcard() {
			return Optional.of(spellcard);
		}

		@Override
		public boolean isSpellcard() {
			return true;
		}

		@Override
		public Optional<ITextComponent> getSpellcardName() {
			return Optional.of(new TextComponentTranslation(spellcard.getUnlocalizedName()));
		}

		@Override
		public NBTTagCompound serializeNBT() {
			NBTTagCompound compound = super.serializeNBT();
			compound.setString(NBT_SPELLCARD, spellcard.getFullName().toString());
			compound.setBoolean(NBT_FIRST_ATTACK, firstAttack);
			return compound;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			super.deserializeNBT(nbt);
			spellcard = DanmakuRegistry.SPELLCARD.getObject(new ResourceLocation(nbt.getString(NBT_SPELLCARD)));
			//noinspection ConstantConditions
			if(spellcard == null) {
				spellcard = DanmakuRegistry.SPELLCARD.getRandomObject(getEntity().getRNG());
			}
			firstAttack = nbt.getBoolean(NBT_FIRST_ATTACK);
		}

		@Override
		public void dropLoot(DamageSource source) {
			getEntity().entityDropItem(new ItemStack(LibItems.SPELLCARD, 1, DanmakuRegistry.SPELLCARD.getId(spellcard)), 0F);
		}
	}
}
