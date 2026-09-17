package dev.slimevr.plugin

import dev.slimevr.plugin.bone.PluginBone
import dev.slimevr.plugin.extensions.InputProcessorExtension
import dev.slimevr.plugin.extensions.VMCExtension
import dev.slimevr.plugin.extensions.VRCOSCExtension

interface SlimePlugin {
	val id: String
	val name: String
	val version: String
	val author: String get() = "Unknown"

	fun onEnable() {}
	fun onDisable() {}
	fun onTick() {}

	fun getPluginBones(): List<PluginBone> = emptyList()
	fun getInputProcessorExtensions(): List<InputProcessorExtension> = emptyList()
	fun getVmcExtensions(): List<VMCExtension> = emptyList()
	fun getVrcOscExtensions(): List<VRCOSCExtension> = emptyList()
}
