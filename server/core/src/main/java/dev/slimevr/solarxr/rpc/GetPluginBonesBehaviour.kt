package dev.slimevr.solarxr.rpc

import dev.slimevr.plugin.PluginManager
import dev.slimevr.solarxr.SolarXRBridgeBehaviour
import io.klogging.noCoLogger
import solarxr_protocol.rpc.GetPluginBonesRequest
import solarxr_protocol.rpc.GetPluginBonesResponse

/**
 * Handles GetPluginBones RPC requests and sends plugin bone registrations to the client.
 */
class GetPluginBonesBehaviour(private val pluginManager: PluginManager) : SolarXRBridgeBehaviour {
	private val logger = noCoLogger("GetPluginBonesBehaviour")

	override fun observe(receiver: dev.slimevr.solarxr.SolarXRBridge) {
		receiver.onRpc<GetPluginBonesRequest> { request, txId ->
			logger.info("Received GetPluginBonesRequest (txId=$txId)")
			
			val plugins = pluginManager.getPlugins()

			val pluginBones = plugins.flatMap { it.getPluginBoneRegistrations() }
			if (pluginBones.isEmpty()) {
				logger.warn("No plugin bones registered. Plugins returned empty lists.")
			} else {
				logger.info("Found ${pluginBones.size} plugin bone(s) from loaded plugins")
			}

			val response = GetPluginBonesResponse(registeredpluginbones = pluginBones)
			
			receiver.sendRpc(response, txId = txId)
		}.launchIn(receiver.context.scope)
	}
}
