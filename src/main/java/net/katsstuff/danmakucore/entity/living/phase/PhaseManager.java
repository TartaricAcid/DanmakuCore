/*
 * This class was created by <Katrix>. It's distributed as
 * part of the DanmakuCore Mod. Get the Source Code in github:
 * https://github.com/Katrix-/DanmakuCore
 *
 * DanmakuCore is Open Source and distributed under the
 * the DanmakuCore license: https://github.com/Katrix-/DanmakuCore/blob/master/LICENSE.md
 */
package net.katsstuff.danmakucore.entity.living.phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import net.katsstuff.danmakucore.entity.living.EntityDanmakuMob;
import net.katsstuff.danmakucore.lib.data.LibPhases;
import net.katsstuff.danmakucore.registry.DanmakuRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

@SuppressWarnings("unused")
public class PhaseManager implements INBTSerializable<NBTTagCompound> {

	private static final String NBT_PHASES = "phases";
	private static final String NBT_INDEX = "index";

	private final List<Phase> phaseList = new ArrayList<>();
	private int currentPhaseIndex = 0;

	public final EntityDanmakuMob entity;

	public PhaseManager(EntityDanmakuMob entity) {
		this.entity = entity;
	}

	/**
	 * Ticks the current phase.
	 */
	public void tick() {
		Phase phase = getCurrentPhase();
		if(phase.isActive()) {
			if(!entity.world.isRemote) {
				phase.serverUpdate();
			}
			else {
				phase.clientUpdate();
			}
		}
	}

	/**
	 * The phase that is currently in use.
	 */
	public Phase getCurrentPhase() {
		return phaseList.get(currentPhaseIndex);
	}

	/**
	 * Sets a phase as the one in use, overriding the previous one. This also initiates the new phase.
	 */
	public void setCurrentPhase(Phase phase) {
		getCurrentPhase().deconstruct();
		phaseList.set(currentPhaseIndex, phase);
		phase.init();
	}

	/**
	 * Sets the current phase to the next one and initiates it.
	 */
	public void nextPhase() {
		getCurrentPhase().deconstruct();
		currentPhaseIndex++;
		getCurrentPhase().init();
	}

	/**
	 * Sets the current phase to the previous one and initiates it.
	 */
	public void previousPhase() {
		getCurrentPhase().deconstruct();
		currentPhaseIndex--;
		getCurrentPhase().init();
	}

	/**
	 * Adds a new phase.
	 */
	public void addPhase(Phase phase) {
		phaseList.add(phase);
	}

	/**
	 * Adds a new phase at a specific index.
	 */
	public void addPhase(Phase phase, int index) {
		phaseList.add(index, phase);
	}


	/**
	 * Adds new phases.
	 */
	public void addPhases(List<Phase> phases) {
		phaseList.addAll(phases);
	}

	/**
	 * Adds new phases.
	 */
	public void addPhases(Phase... phases) {
		Collections.addAll(phaseList, phases);
	}

	/**
	 * Sets a phase at the specific index.
	 */
	public void setPhase(Phase phase, int index) {
		phaseList.set(index, phase);
	}

	/**
	 * Removes a phase.
	 */
	public void removePhase(Phase phase) {
		phaseList.remove(phase);
	}

	/**
	 * Removes a phase.
	 */
	public void removePhase(int index) {
		phaseList.remove(index);
	}

	/**
	 * Gets the index of a phase.
	 */
	public int getPhaseIndex(Phase phase) {
		return phaseList.indexOf(phase);
	}

	/**
	 * Changes the current active phase without throwing away the previous one.
	 */
	public void changePhase(int newPhase) {
		currentPhaseIndex = newPhase;
	}

	public boolean hasNextPhase() {
		return currentPhaseIndex < phaseList.size() - 1;
	}

	public List<Phase> getPhaseList() {
		return ImmutableList.copyOf(phaseList);
	}

	public int getCurrentPhaseIndex() {
		return currentPhaseIndex;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger(NBT_INDEX, currentPhaseIndex);

		NBTTagList list = new NBTTagList();
		for(Phase phase : phaseList) {
			list.appendTag(phase.serializeNBT());
		}
		tag.setTag(NBT_PHASES, list);
		return tag;
	}

	@Override
	public void deserializeNBT(NBTTagCompound tag) {
		currentPhaseIndex = tag.getInteger(NBT_INDEX);

		NBTTagList list = tag.getTagList(NBT_PHASES, Constants.NBT.TAG_COMPOUND);
		int size = list.tagCount();

		List<Phase> deseralized = new ArrayList<>();
		ListMultimap<PhaseType, Phase> multimap = MultimapBuilder.hashKeys().linkedListValues().build();

		phaseList.stream().collect(Collectors.groupingBy(Phase::getType)).forEach(multimap::putAll);

		for(int i = 0; i < size; i++) {
			NBTTagCompound tagPhase = list.getCompoundTagAt(i);
			PhaseType type = DanmakuRegistry.PHASE.getObject(new ResourceLocation(tagPhase.getString(Phase.NBT_NAME)));
			//noinspection ConstantConditions
			if(type == null) {
				type = LibPhases.FALLBACK;
			}

			Phase phase;
			if(multimap.containsKey(type)) {
				phase = multimap.get(type).get(0);
				multimap.remove(type, phase);
			}
			else {
				phase = type.instantiate(this);
			}

			phase.deserializeNBT(tagPhase);
			deseralized.add(phase);
		}

		phaseList.clear();
		phaseList.addAll(deseralized);

		if(currentPhaseIndex != 0 && phaseList.get(0).isActive()) {
			phaseList.get(0).deconstruct();
		}

		getCurrentPhase().init();
	}
}
