package dev.slimevr.osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortOut
import com.jme3.math.FastMath
import dev.slimevr.VRServer
import dev.slimevr.config.SpatialHeadphonesOSCConfig
import dev.slimevr.tracking.trackers.Tracker
import dev.slimevr.tracking.trackers.TrackerPosition
import io.eiren.util.collections.FastList
import io.eiren.util.logging.LogManager
import io.github.axisangles.ktmath.EulerOrder
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress

class SpatialHeadphonesOSCHandler(
	private val server: VRServer,
	private val config: SpatialHeadphonesOSCConfig,
) : OSCHandler {
	private var oscSender: OSCPortOut? = null
	private var headTracker: Tracker? = null
	private val oscArgs = FastList<Float>(3)
	private var oscPortOut = 0
	private var oscIp: InetAddress? = null
	private var timeAtLastError: Long = 0

	init {
		refreshSettings(false)
	}

	override fun refreshSettings(refreshRouterSettings: Boolean) {
		updateOscSender(config.portOut, config.address)

		if (refreshRouterSettings) {
			server.oSCRouter.refreshSettings(false)
		}
	}

	override fun updateOscReceiver(portIn: Int, args: Array<String>) {
		// Rime doesn't need to receive OSC messages (at least for now)
	}

	override fun updateOscSender(portOut: Int, ip: String) {
		val wasConnected = oscSender != null && oscSender!!.isConnected
		if (wasConnected) {
			try {
				oscSender!!.close()
			} catch (e: IOException) {
				LogManager.severe("[SpatialHeadphonesOSCHandler] Error closing the OSC sender: $e")
			}
		}

		if (config.enabled) {
			try {
				val addr = InetAddress.getByName(ip)
				oscSender = OSCPortOut(InetSocketAddress(addr, portOut))
				if (oscPortOut != portOut && oscIp != addr || !wasConnected) {
					LogManager.info("[SpatialHeadphonesOSCHandler] Sending to port $portOut at address $ip")
				}
				oscPortOut = portOut
				oscIp = addr
				oscSender?.connect()
			} catch (e: IOException) {
				LogManager.severe("[SpatialHeadphonesOSCHandler] Error connecting to port $portOut at the address $ip: $e")
			}
		}
	}

	override fun update() {
		if (oscSender == null || !oscSender!!.isConnected || !config.enabled) {
			return
		}

		if (headTracker == null) {
			headTracker = server.allTrackers.find { it.trackerPosition == TrackerPosition.HEAD }
			if (headTracker == null) return
		}

		val rotation = headTracker!!.getRotation()
		// YXZ order results in:
		// x = pitch
		// y = yaw
		// z = roll
		val angles = rotation.toEulerAngles(EulerOrder.YXZ)

		oscArgs.clear()
		// Neumann RIME and similar software expect /ypr <yaw> <pitch> <roll>
		oscArgs.add(angles.y * FastMath.RAD_TO_DEG * -1)
		oscArgs.add(angles.x * FastMath.RAD_TO_DEG * 1)
		oscArgs.add(angles.z * FastMath.RAD_TO_DEG * 1)

		val message = OSCMessage("/ypr", oscArgs.clone())

		try {
			oscSender?.send(message)
		} catch (e: Exception) {
			val currentTime = System.currentTimeMillis()
			if (currentTime - timeAtLastError > 1000) {
				timeAtLastError = currentTime
				LogManager.warning("[SpatialHeadphonesOSCHandler] Error sending OSC message: $e")
			}
		}
	}

	override fun getOscSender(): OSCPortOut? = oscSender

	override fun getPortOut(): Int = oscPortOut

	override fun getAddress(): InetAddress? = oscIp

	override fun getOscReceiver(): OSCPortIn? = null

	override fun getPortIn(): Int = 0
}
