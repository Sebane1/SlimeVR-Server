package dev.slimevr.config

class SpatialHeadphonesOSCConfig : OSCConfig() {
	init {
		enabled = false
		portOut = 7001
		address = "127.0.0.1"
	}
}
