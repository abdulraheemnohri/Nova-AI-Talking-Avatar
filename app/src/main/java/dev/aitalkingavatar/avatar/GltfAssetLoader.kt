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

class GltfAssetLoader(private val engine: Engine, private val assetLoader: AssetLoader) {

    fun loadGlbFromFile(file: File): FilamentAsset? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            val buffer = FileInputStream(file).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
            val asset = assetLoader.createAsset(buffer) ?: return null
            val resourceLoader = ResourceLoader(engine)
            resourceLoader.loadResources(asset)
            asset.releaseSourceData()
            resourceLoader.destroy()
            asset
        } catch (e: Exception) {
            null
        }
    }

    fun loadGlbFromAssets(context: Context, assetPath: String): FilamentAsset? {
        return try {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            val asset = assetLoader.createAsset(buffer) ?: return null
            val resourceLoader = ResourceLoader(engine)
            resourceLoader.loadResources(asset)
            asset.releaseSourceData()
            resourceLoader.destroy()
            asset
        } catch (e: Exception) {
            null
        }
    }
}
