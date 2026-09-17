package dev.slimevr.skeleton.inputprocessors

import dev.slimevr.plugin.PluginManager
import dev.slimevr.skeleton.InputSkeleton
import dev.slimevr.skeleton.SkeletonInputProcessor

/**
 * Executes plugin input processor extensions directly inside the Skeleton solver's input processing pipeline chain.
 */
class PluginInputProcessor(private val pluginManager: PluginManager? = null) : SkeletonInputProcessor {
	override fun process(mutableInputSkeleton: InputSkeleton, skeletonHeight: Float) {
		pluginManager?.processInputProcessorExtensions(mutableInputSkeleton, skeletonHeight)
	}
}
