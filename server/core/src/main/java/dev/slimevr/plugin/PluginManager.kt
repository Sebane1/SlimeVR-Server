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
	val boneManager = PluginBoneManager()

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
				boneManager.registerBones(plugin.getPluginBones())
				println("[PluginManager] Successfully loaded plugin: ${plugin.name} v${plugin.version} by ${plugin.author}")
				logger.info("Successfully loaded plugin: ${plugin.name} v${plugin.version} by ${plugin.author}")
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
			boneManager.registerBones(plugin.getPluginBones())
			logger.info("Manually registered plugin: ${plugin.name} v${plugin.version}")
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

		if (computed != null) {
			boneManager.updateWorldTransforms(computed)
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
		val pluginBones = boneManager.getBones()
		for (plugin in loadedPlugins) {
			for (ext in plugin.getInputProcessorExtensions()) {
				try {
					ext.process(mutableInputSkeleton, skeletonHeight, pluginBones)
				} catch (e: Exception) {
					logger.error("Error running input processor extension for ${plugin.name}", e)
				}
			}
		}
	}

	fun dispatchVmc(sendVmc: (boneName: String, position: FloatArray, rotation: FloatArray) -> Unit) {
		val pluginBones = boneManager.getBones()
		for (plugin in loadedPlugins) {
			for (ext in plugin.getVmcExtensions()) {
				try {
					ext.onVmcFrame(pluginBones, sendVmc)
				} catch (e: Exception) {
					logger.error("Error executing VMC extension for ${plugin.name}", e)
				}
			}
		}
	}

	fun buildVrcOscMessages(): List<OscMessage> {
		val result = mutableListOf<OscMessage>()
		val pluginBones = boneManager.getBones()
		for (plugin in loadedPlugins) {
			for (ext in plugin.getVrcOscExtensions()) {
				try {
					result.addAll(ext.buildOscMessages(pluginBones))
				} catch (e: Exception) {
					logger.error("Error executing VRCOSC extension for ${plugin.name}", e)
				}
			}
		}
		return result
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
