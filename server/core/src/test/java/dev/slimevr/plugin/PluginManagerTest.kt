package dev.slimevr.plugin

import dev.slimevr.osc.OscArg
import dev.slimevr.osc.OscMessage
import dev.slimevr.plugin.bone.PluginBone
import dev.slimevr.plugin.extensions.VMCExtension
import dev.slimevr.plugin.extensions.VRCOSCExtension
import dev.slimevr.skeleton.BoneState
import dev.slimevr.skeleton.ComputedSkeleton
import dev.slimevr.skeleton.ZERO_VELOCITY
import dev.slimevr.skeleton.bodyPartMap
import dev.slimevr.skeleton.mutateCopy
import io.github.axisangles.ktmath.Quaternion
import io.github.axisangles.ktmath.Vector3
import solarxr_protocol.datatypes.BodyPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginManagerTest {

	class EarRigPlugin : SlimePlugin {
		override val id = "ear-rig-plugin"
		override val name = "Ear Rig Plugin"
		override val version = "1.0.0"
		override val author = "Test Author"

		var vmcFrameCalled = false
		var vrcOscFrameCalled = false

		private val leftEar = PluginBone(
			id = "LEFT_EAR",
			parentBoneId = "HEAD",
			localPosition = Vector3(0.1f, 0.2f, 0.0f),
			localRotation = Quaternion.IDENTITY,
			vmcBoneName = "LeftEar",
			vrcOscParamName = "EarLeftPitch",
		)

		private val rightEar = PluginBone(
			id = "RIGHT_EAR",
			parentBoneId = "HEAD",
			localPosition = Vector3(-0.1f, 0.2f, 0.0f),
			localRotation = Quaternion.IDENTITY,
			vmcBoneName = "RightEar",
			vrcOscParamName = "EarRightPitch",
		)

		override fun getPluginBones(): List<PluginBone> = listOf(leftEar, rightEar)

		override fun getVmcExtensions(): List<VMCExtension> = listOf(
			VMCExtension { bones, sendVmc ->
				vmcFrameCalled = true
				for (b in bones) {
					b.vmcBoneName?.let { name ->
						sendVmc(name, floatArrayOf(b.worldPosition.x, b.worldPosition.y, b.worldPosition.z), floatArrayOf(0f, 0f, 0f, 1f))
					}
				}
			},
		)

		override fun getVrcOscExtensions(): List<VRCOSCExtension> = listOf(
			VRCOSCExtension { bones ->
				vrcOscFrameCalled = true
				bones.mapNotNull { b ->
					b.vrcOscParamName?.let { param ->
						OscMessage("/avatar/parameters/$param", listOf(OscArg.Float(0.5f)))
					}
				}
			},
		)
	}

	@Test
	fun testPluginRegistrationAndStackedBones() {
		val manager = PluginManager()
		val earPlugin = EarRigPlugin()
		manager.registerPlugin(earPlugin)

		assertEquals(1, manager.getPlugins().size)
		assertEquals(2, manager.boneManager.getBones().size)

		val headBoneState = BoneState(
			parentBone = null,
			bodyPart = BodyPart.HEAD,
			headOffset = Vector3.ZERO,
			offset = Vector3.ZERO,
			rotation = Quaternion.IDENTITY,
			acceleration = Vector3.ZERO,
			headPosition = Vector3(0f, 1.7f, 0f),
			tailPosition = Vector3(0f, 1.8f, 0f),
			velocity = ZERO_VELOCITY,
		)

		val mockComputed: ComputedSkeleton = bodyPartMap<BoneState>().mutateCopy {
			it[BodyPart.HEAD] = headBoneState
		}

		manager.boneManager.updateWorldTransforms(mockComputed)

		val leftEar = manager.boneManager.getBones().find { it.id == "LEFT_EAR" }!!
		val rightEar = manager.boneManager.getBones().find { it.id == "RIGHT_EAR" }!!

		assertEquals(0.1f, leftEar.worldPosition.x, 0.001f)
		assertEquals(2.0f, leftEar.worldPosition.y, 0.001f)
		assertEquals(0.0f, leftEar.worldPosition.z, 0.001f)

		assertEquals(-0.1f, rightEar.worldPosition.x, 0.001f)
		assertEquals(2.0f, rightEar.worldPosition.y, 0.001f)

		var vmcCount = 0
		manager.dispatchVmc { name, _, _ ->
			vmcCount++
		}
		assertTrue(earPlugin.vmcFrameCalled)
		assertEquals(2, vmcCount)

		val oscMessages = manager.buildVrcOscMessages()
		assertTrue(earPlugin.vrcOscFrameCalled)
		assertEquals(2, oscMessages.size)
	}
}
