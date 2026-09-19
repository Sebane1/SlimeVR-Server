package dev.slimevr.plugin.extensions

import dev.slimevr.osc.OscMessage
import dev.slimevr.skeleton.InputSkeleton
import dev.slimevr.skeleton.Skeleton
import solarxr_protocol.rpc.PluginBoneRegistration

fun interface InputProcessorExtension {
	fun process(mutableInputSkeleton: InputSkeleton, skeletonHeight: Float, pluginBones: List<PluginBoneRegistration>)
}

fun interface VMCExtension {
	fun onVmcFrame(pluginBones: List<PluginBoneRegistration>, sendVmc: (boneName: String, position: FloatArray, rotation: FloatArray) -> Unit)
}

fun interface VRCOSCExtension {
	fun buildOscMessages(pluginBones: List<PluginBoneRegistration>): List<OscMessage>
}
