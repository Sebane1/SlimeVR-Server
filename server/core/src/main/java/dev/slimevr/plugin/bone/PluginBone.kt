package dev.slimevr.plugin.bone

import io.github.axisangles.ktmath.Quaternion
import io.github.axisangles.ktmath.Vector3

/**
 * Represents a generic stacked overlay bone registered by a plugin.
 * Attached relative to [parentBoneId], which can be a standard BodyPart name (e.g. "HEAD", "HIP")
 * or another [PluginBone] id.
 */
data class PluginBone(
	val id: String,
	val parentBoneId: String,
	var localPosition: Vector3 = Vector3.ZERO,
	var localRotation: Quaternion = Quaternion.IDENTITY,
	var worldPosition: Vector3 = Vector3.ZERO,
	var worldRotation: Quaternion = Quaternion.IDENTITY,
	val vmcBoneName: String? = null,
	val vrcOscParamName: String? = null,
	val modelUrl: String? = null,
)
