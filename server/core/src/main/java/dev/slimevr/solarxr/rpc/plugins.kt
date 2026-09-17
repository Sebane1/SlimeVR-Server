package dev.slimevr.solarxr.rpc

import dev.slimevr.plugin.PluginManager
import dev.slimevr.solarxr.SolarXRBridge
import dev.slimevr.solarxr.SolarXRBridgeBehaviour

class PluginBonesBehaviour(
	private val pluginManager: PluginManager,
) : SolarXRBridgeBehaviour {
	override fun observe(receiver: SolarXRBridge) {
		// Exposed for plugin bones synchronization
	}
}
