package dev.aitalkingavatar.avatar

import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.io.File
import java.nio.ByteBuffer

class FilamentRendererManager(private val surfaceView: SurfaceView) {
    companion object { init { Utils.init() } }

    private val modelViewer: ModelViewer = ModelViewer(surfaceView)
    private var isRunning = false
    private var isDisposed = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning || isDisposed) return
            try {
                modelViewer.render(frameTimeNanos)
            } catch (_: IllegalStateException) {
                return
            } catch (_: RuntimeException) {
                // Safe fallback on render glitches
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        surfaceView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)
            true
        }
    }

    fun start() {
        if (isDisposed || isRunning) return
        isRunning = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun pause() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun loadModel(file: File): Boolean {
        if (isDisposed || !file.exists()) return false
        return try {
            val bytes = file.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            modelViewer.loadModelGlb(buffer)
            modelViewer.transformToUnitCube()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun destroy() {
        if (isDisposed) return
        pause()
        isDisposed = true
        surfaceView.setOnTouchListener(null)
        runCatching {
            modelViewer.destroyModel()
        }
    }
}
