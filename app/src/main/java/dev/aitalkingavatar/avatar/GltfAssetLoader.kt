package dev.aitalkingavatar.avatar

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Loads glTF 2.0 binary assets through Filament.
 *
 * ResourceLoader owns native-side loading resources and must be destroyed even
 * when texture/material loading throws. The returned FilamentAsset remains
 * owned by the caller and must be destroyed before its AssetLoader/Engine.
 */
class GltfAssetLoader(private val engine: Engine, private val assetLoader: AssetLoader) {

    fun loadGlbFromFile(file: File): FilamentAsset? {
        if (!file.isFile || !file.canRead() || file.length() == 0L) return null
        return try {
            val buffer = FileInputStream(file).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
            loadAsset(buffer)
        } catch (_: Exception) {
            null
        }
    }

    fun loadGlbFromAssets(context: Context, assetPath: String): FilamentAsset? {
        return try {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            if (bytes.isEmpty()) return null
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            loadAsset(buffer)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAsset(buffer: ByteBuffer): FilamentAsset? {
        val asset = assetLoader.createAsset(buffer) ?: return null
        val resourceLoader = ResourceLoader(engine)
        return try {
            resourceLoader.loadResources(asset)
            asset.releaseSourceData()
            asset
        } catch (_: Exception) {
            // The caller receives null on failure; do not return a half-loaded asset.
            runCatching { assetLoader.destroyAsset(asset) }
            null
        } finally {
            runCatching { resourceLoader.destroy() }
        }
    }
}
