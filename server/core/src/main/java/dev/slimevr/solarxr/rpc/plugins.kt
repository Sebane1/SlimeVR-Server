package dev.slimevr.solarxr.rpc

import dev.slimevr.plugin.PluginBone
import dev.slimevr.plugin.PluginManager
import dev.slimevr.solarxr.SolarXRBridge
import dev.slimevr.solarxr.SolarXRBridgeBehaviour
import solarxr_protocol.rpc.PluginBonesUpdateResponse
import solarxr_protocol.rpc.PluginBone

class PluginBonesBehaviour(
	private val pluginManager: PluginManager,
) : SolarXRBridgeBehaviour {
	override fun observe(receiver: SolarXRBridge) {
		receiver.onRpc<RpcMessage.PLUGIN_BONES_UPDATE_RESPONSE>(PLUGIN_BONES_UPDATE_REQUEST) { _, _ ->
			val bones = pluginManager.boneManager.getBones()
				.map { PluginBone(
					id = it.id,
					name = it.name ?: "",
					parentBoneId = it.parentBoneId ?: "",
					localPositionX = it.localPosition.x,
					localPositionY = it.localPosition.y,
					localPositionZ = it.localPosition.z,
					localRotationX = it.localRotation.x,
					localRotationY = it.localRotation.y,
					localRotationZ = it.localRotation.z,
					localRotationW = it.localRotation.w,
				) }
			val response = PluginBonesUpdateResponse(bones = bones)
			receiver.sendRpc(response, replyTo = null)
		}
	}

	companion object {
		private const val PLUGIN_BONES_UPDATE_REQUEST = "PluginBonesUpdate"
	}
}
