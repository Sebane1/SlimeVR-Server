package dev.slimevr.plugin

import dev.slimevr.plugin.bone.PluginBone
import dev.slimevr.skeleton.ComputedSkeleton
import io.github.axisangles.ktmath.Quaternion
import io.github.axisangles.ktmath.Vector3
import solarxr_protocol.datatypes.BodyPart

class PluginBoneManager {
	private val registeredBones = mutableListOf<PluginBone>()

	fun registerBones(bones: List<PluginBone>) {
		registeredBones.addAll(bones)
	}

	fun getBones(): List<PluginBone> = registeredBones

	fun updateWorldTransforms(computedSkeleton: ComputedSkeleton) {
		val boneMap = registeredBones.associateBy { it.id }

		for (bone in registeredBones) {
			updateBoneTransform(bone, boneMap, computedSkeleton)
		}
	}

	private fun updateBoneTransform(
		bone: PluginBone,
		boneMap: Map<String, PluginBone>,
		computedSkeleton: ComputedSkeleton,
	) {
		// Try parsing as standard BodyPart first
		val bodyPart = try {
			BodyPart.valueOf(bone.parentBoneId.uppercase())
		} catch (_: IllegalArgumentException) {
			null
		}

		if (bodyPart != null && bodyPart != BodyPart.NONE) {
			val parentState = computedSkeleton[bodyPart]
			if (parentState != null) {
				val parentPos = parentState.tailPosition
				val parentRot = parentState.rotation
				bone.worldPosition = parentPos + parentRot.sandwich(bone.localPosition)
				bone.worldRotation = parentRot * bone.localRotation
				return
			}
		}

		// Otherwise check if parent is another PluginBone
		val parentPluginBone = boneMap[bone.parentBoneId]
		if (parentPluginBone != null) {
			val parentPos = parentPluginBone.worldPosition
			val parentRot = parentPluginBone.worldRotation
			bone.worldPosition = parentPos + parentRot.sandwich(bone.localPosition)
			bone.worldRotation = parentRot * bone.localRotation
		} else {
			// If no parent found, treat local as world
			bone.worldPosition = bone.localPosition
			bone.worldRotation = bone.localRotation
		}
	}
}
