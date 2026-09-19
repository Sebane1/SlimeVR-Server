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

	fun getPluginBoneRegistrations(): List<solarxr_protocol.rpc.PluginBoneRegistration> = emptyList()
	fun getInputProcessorExtensions(): List<InputProcessorExtension>? = null
	fun getVmcExtensions(): List<VMCExtension>? = null
	fun getVrcOscExtensions(): List<VRCOSCExtension>? = null
}
