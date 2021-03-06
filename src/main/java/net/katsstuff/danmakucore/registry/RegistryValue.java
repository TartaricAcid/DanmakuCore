/*
 * This class was created by <Katrix>. It's distributed as
 * part of the DanmakuCore Mod. Get the Source Code in github:
 * https://github.com/Katrix-/DanmakuCore
 *
 * DanmakuCore is Open Source and distributed under the
 * the DanmakuCore license: https://github.com/Katrix-/DanmakuCore/blob/master/LICENSE.md
 */
package net.katsstuff.danmakucore.registry;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;

public abstract class RegistryValue<T extends IForgeRegistryEntry<T>> extends IForgeRegistryEntry.Impl<T> {

	/**
	 * The full name as of this value. Both modId and name.
	 */
	public ResourceLocation getFullName() {
		return getRegistryName();
	}

	/**
	 * Get the mod id for this value.
	 */
	@SuppressWarnings("WeakerAccess")
	public String getModId() {
		return getFullName().getResourceDomain();
	}

	/**
	 * Get the short name for this value.
	 */
	public String getName() {
		return getFullName().getResourcePath();
	}
}
