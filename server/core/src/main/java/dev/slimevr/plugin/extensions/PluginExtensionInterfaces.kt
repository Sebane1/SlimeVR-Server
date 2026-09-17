package dev.slimevr.plugin.extensions

import dev.slimevr.osc.OscMessage
import dev.slimevr.plugin.bone.PluginBone
import dev.slimevr.skeleton.InputSkeleton
import dev.slimevr.skeleton.Skeleton

fun interface InputProcessorExtension {
	fun process(mutableInputSkeleton: InputSkeleton, skeletonHeight: Float, pluginBones: List<PluginBone>)
}

fun interface VMCExtension {
	fun onVmcFrame(pluginBones: List<PluginBone>, sendVmc: (boneName: String, position: FloatArray, rotation: FloatArray) -> Unit)
}

fun interface VRCOSCExtension {
	fun buildOscMessages(pluginBones: List<PluginBone>): List<OscMessage>
}
