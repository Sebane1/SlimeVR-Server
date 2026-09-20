package dev.slimevr.plugin

import dev.slimevr.osc.OscMessage
import dev.slimevr.plugin.extensions.InputProcessorExtension
import dev.slimevr.plugin.extensions.VMCExtension
import dev.slimevr.plugin.extensions.VRCOSCExtension
import dev.slimevr.skeleton.InputSkeleton
import dev.slimevr.skeleton.Skeleton
import io.klogging.noCoLogger
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

class PluginManager {
	private val logger = noCoLogger("PluginManager")
	private val loadedPlugins = mutableListOf<SlimePlugin>()

	fun loadPlugins(pluginsDir: File = File("plugins")) {
		var dir = pluginsDir
		if (!dir.exists() || dir.listFiles { _, name -> name.endsWith(".jar") }?.isEmpty() == true) {
			val desktopDir = File("server/desktop/plugins")
			if (desktopDir.exists() && desktopDir.listFiles { _, name -> name.endsWith(".jar") }?.isNotEmpty() == true) {
				dir = desktopDir
			}
		}

		if (!dir.exists()) {
			dir.mkdirs()
			logger.info("Created plugins directory at ${dir.absolutePath}")
			return
		}

		val jarFiles = dir.listFiles { _, name -> name.endsWith(".jar") } ?: return
		if (jarFiles.isEmpty()) {
			logger.info("No plugin JARs found in ${dir.absolutePath}")
			return
		}

		val urls = jarFiles.map { it.toURI().toURL() }.toTypedArray()
		val classLoader = URLClassLoader(urls, javaClass.classLoader)

		val serviceLoader = ServiceLoader.load(SlimePlugin::class.java, classLoader)
		println("[PluginManager] Scanning ${dir.absolutePath} for plugins...")
		for (plugin in serviceLoader) {
			try {
				loadedPlugins.add(plugin)
				plugin.onEnable()
				val boneCount = plugin.getPluginBoneRegistrations().size
				println("[PluginManager] Successfully loaded plugin: ${plugin.name} v${plugin.version} by ${plugin.author}")
				if (boneCount > 0) {
					logger.info("Plugin '${plugin.name}' registered $boneCount bone(s)")
				} else {
					println("[PluginManager] Plugin '${plugin.name}' has no bones registered")
				}
				logger.info("Successfully loaded plugin: ${plugin.name} v${plugin.version}")
			} catch (e: Exception) {
				println("[PluginManager] Failed to enable plugin ${plugin.name}: ${e.message}")
				logger.error("Failed to enable plugin ${plugin.name}", e)
			}
		}
	}

	fun reloadPlugins(pluginsDir: File = File("plugins")) {
		shutdown()
		loadPlugins(pluginsDir)
	}

		fun registerPlugin(plugin: SlimePlugin) {
			try {
				loadedPlugins.add(plugin)
				plugin.onEnable()
				val boneCount = plugin.getPluginBoneRegistrations().size
				if (boneCount > 0) {
					logger.info("Manually registered plugin '${plugin.name}' v${plugin.version} with $boneCount bone(s)")
				} else {
					println("[PluginManager] Manually registered plugin '${plugin.name}' - no bones")
				}
			} catch (e: Exception) {
			logger.error("Failed to enable manually registered plugin ${plugin.name}", e)
		}
	}

	fun getPlugins(): List<SlimePlugin> = loadedPlugins

	fun tick(skeleton: Skeleton) {
		val computed = try {
			skeleton.currentComputed
		} catch (_: Exception) {
			null
		}

		for (plugin in loadedPlugins) {
			try {
				plugin.onTick()
			} catch (e: Exception) {
				logger.error("Error ticking plugin ${plugin.name}", e)
			}
		}
	}

	fun processInputProcessorExtensions(mutableInputSkeleton: InputSkeleton, skeletonHeight: Float) {
		for (plugin in loadedPlugins) {
			val extensions = plugin.getInputProcessorExtensions() ?: emptyList()
			for (extension in extensions) {
				try {
					extension.process(mutableInputSkeleton, skeletonHeight, plugin.getPluginBoneRegistrations())
				} catch (e: Exception) {
					logger.error("Error processing input extension for plugin ${plugin.name}", e)
				}
			}
		}
	}

	fun dispatchVmc(sendVmc: (boneName: String, position: FloatArray, rotation: FloatArray) -> Unit) {
		val extensions = loadedPlugins.flatMap { it.getVmcExtensions() ?: emptyList() }
		for (extension in extensions) {
			try {
				extension.onVmcFrame(loadedPlugins.flatMap { it.getPluginBoneRegistrations() ?: emptyList() }, sendVmc)
			} catch (e: Exception) {
				logger.error("Error dispatching VMC for plugin", e)
			}
		}
	}

	fun buildVrcOscMessages(): List<dev.slimevr.osc.OscMessage> {
		val messages = mutableListOf<dev.slimevr.osc.OscMessage>()
		for (plugin in loadedPlugins) {
			val extensions = plugin.getVrcOscExtensions() ?: emptyList()
			for (extension in extensions) {
				try {
					messages.addAll(extension.buildOscMessages(loadedPlugins.flatMap { it.getPluginBoneRegistrations() ?: emptyList() }))
				} catch (e: Exception) {
					logger.error("Error building OSC messages for plugin ${plugin.name}", e)
				}
			}
		}
		return messages.distinctBy { it.address }.sortedBy { it.address }
	}

	fun shutdown() {
		for (plugin in loadedPlugins) {
			try {
				plugin.onDisable()
			} catch (e: Exception) {
				logger.error("Error disabling plugin ${plugin.name}", e)
			}
		}
		loadedPlugins.clear()
	}
}
