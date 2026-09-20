package dev.slimevr.plugin

import dev.slimevr.plugin.extensions.InputProcessorExtension
import dev.slimevr.plugin.extensions.VMCExtension
import dev.slimevr.plugin.extensions.VRCOSCExtension
import solarxr_protocol.rpc.PluginBoneRegistration

interface SlimePlugin {
	val id: String
	val name: String
	val version: String
	val author: String get() = "Unknown"

	fun onEnable() {}
	fun onDisable() {}
	fun onTick() {}

	/** 
	 * Override this method to register plugin bones for tracker assignment and GUI display.
	 * Example implementation showing how to populate bone registrations:
	 * ```kotlin
	 * override fun getPluginBoneRegistrations(): List<PluginBoneRegistration> = listOf(
	 *   PluginBoneRegistration(
	 *     id = "plugin_head",
	 *     name = "Custom Head Bone",
	 *     parent_bone_id = "HEAD" // or null for independent bone
	 *   )
	 * )
	 * ```
	 */
	fun getPluginBoneRegistrations(): List<PluginBoneRegistration> = emptyList()

	fun getInputProcessorExtensions(): List<InputProcessorExtension>? = null
	fun getVmcExtensions(): List<VMCExtension>? = null
	fun getVrcOscExtensions(): List<VRCOSCExtension>? = null
}
