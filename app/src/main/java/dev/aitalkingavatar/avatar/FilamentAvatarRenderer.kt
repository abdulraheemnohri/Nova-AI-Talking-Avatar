package dev.aitalkingavatar.avatar

import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import com.google.android.filament.RenderableManager
import com.google.android.filament.TransformManager
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import dev.aitalkingavatar.data.GraphicsQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.cos
import kotlin.math.sin

class FilamentAvatarRenderer(private val surface: SurfaceView) {
    companion object { init { Utils.init() } }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val viewer = ModelViewer(surface)
    private var running = false
    private var disposed = false
    private var avatarState = AvatarState.IDLE
    private var previousState = avatarState
    private var stateStartNanos = System.nanoTime()
    private var activeClip: ClipSelection = ClipSelection(null, null, false)
    private var clipNames: List<String> = emptyList()
    private var morphBindings: List<Pair<Int, Int>> = emptyList()
    private var talkNodes: List<Int> = emptyList()
    private var baseTalkTransforms: Map<Int, FloatArray> = emptyMap()
    private var modelFile: File? = null
    private var quality = GraphicsQuality.BALANCED
    private var reducedMotion = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running || disposed) return
            try {
                if (avatarState != previousState) {
                    previousState = avatarState
                    stateStartNanos = frameTimeNanos
                    activeClip = AnimationClipMapper.select(avatarState, clipNames)
                }
                resetTalkTransforms()
                val animator = viewer.animator
                val clipIndex = activeClip.index
                if (animator != null && animator.animationCount > 0 && clipIndex != null) {
                    val index = clipIndex.coerceIn(0, animator.animationCount - 1)
                    val duration = animator.getAnimationDuration(index).coerceAtLeast(0.05f)
                    val seconds = if (reducedMotion) 0f else ((frameTimeNanos - stateStartNanos).coerceAtLeast(0) / 1_000_000_000f) % duration
                    animator.applyAnimation(index, seconds)
                    animator.updateBoneMatrices()
                }
                applySpeechMotion(frameTimeNanos)
                viewer.render(frameTimeNanos)
            } catch (_: IllegalStateException) {
                // The SurfaceView can detach during a configuration or lifecycle transition.
                return
            } catch (_: RuntimeException) {
                // A model may omit optional rig features; the viewer continues with safe fallbacks.
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        surface.contentDescription = "Interactive 3D character preview. Drag to orbit and pinch to zoom."
        surface.setOnTouchListener { _, event: MotionEvent -> viewer.onTouchEvent(event); true }
        applyQuality(quality)
    }

    fun start() {
        if (disposed || running) return
        running = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun pause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun setState(state: AvatarState) { avatarState = state }

    fun setQuality(value: GraphicsQuality) { quality = value; applyQuality(value) }
    fun setReducedMotion(value: Boolean) { reducedMotion = value }

    fun setCameraZoom(value: Float) {
        if (!disposed) runCatching { viewer.cameraFocalLength = 28f * value.coerceIn(0.5f, 2f) }
    }

    fun resetCamera() {
        if (!disposed) runCatching { viewer.transformToUnitCube() }
    }

    fun loadModel(file: File, onLoaded: (Boolean, String) -> Unit) {
        if (disposed) {
            onLoaded(false, "Avatar renderer is no longer active.")
            return
        }
        val preflight = GlbPreflight.validate(file)
        if (!preflight.valid) {
            onLoaded(false, preflight.message)
            return
        }
        modelFile = file
        scope.launch {
            try {
                val buffer = withContext(Dispatchers.IO) {
                    FileInputStream(file).channel.use { channel -> channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()) }
                }
                if (disposed) return@launch
                viewer.loadModelGlb(buffer)
                viewer.transformToUnitCube()
                collectRigInfo()
                onLoaded(viewer.asset != null, if (viewer.asset != null) "3D character loaded" else "This GLB could not be loaded. Try another rigged glTF 2.0 binary asset.")
            } catch (_: OutOfMemoryError) {
                onLoaded(false, "This character is too large for the available memory. Try a smaller mobile-optimized GLB.")
            } catch (error: Exception) {
                onLoaded(false, "Could not load this GLB. Check that it is a valid glTF 2.0 binary model.")
            }
        }
    }

    private fun collectRigInfo() {
        val asset = viewer.asset ?: return
        val animator = viewer.animator
        clipNames = if (animator == null) emptyList() else (0 until animator.animationCount).map { animator.getAnimationName(it) }
        activeClip = AnimationClipMapper.select(avatarState, clipNames)
        morphBindings = asset.renderableEntities.flatMap { entity ->
            val names = runCatching { asset.getMorphTargetNames(entity).toList() }.getOrDefault(emptyList())
            val mouthIndex = names.indexOfFirst { name ->
                val normalized = name.lowercase().replace(Regex("[^a-z0-9]"), "")
                normalized.contains("jawopen") || normalized.contains("mouthopen") || normalized.contains("visemeaa") || normalized == "aa"
            }
            if (mouthIndex < 0) emptyList() else listOf(entity to mouthIndex)
        }
        val namedNodes = asset.entities.filter { entity ->
            val name = runCatching { asset.getName(entity) }.getOrNull()?.lowercase().orEmpty()
            name.contains("jaw") || name.contains("head") || name.contains("neck")
        }
        talkNodes = if (namedNodes.isNotEmpty()) namedNodes else listOf(asset.root)
        val transforms = viewer.engine.transformManager
        baseTalkTransforms = talkNodes.associateWith { entity ->
            if (transforms.hasComponent(entity)) {
                val mat = FloatArray(16)
                transforms.getTransform(transforms.getInstance(entity), mat)
                mat
            } else FloatArray(16)
        }
    }

    private fun applySpeechMotion(frameTimeNanos: Long) {
        val speaking = avatarState == AvatarState.SPEAKING
        val phase = (frameTimeNanos - stateStartNanos).coerceAtLeast(0).toDouble() / 1_000_000_000.0
        val wave = if (speaking && !reducedMotion) (0.5 + 0.5 * sin(phase * 9.0)).toFloat() else 0f
        if (morphBindings.isNotEmpty()) {
            val manager = viewer.engine.renderableManager
            morphBindings.forEach { (entity, targetIndex) ->
                val instance = manager.getInstance(entity)
                if (instance != 0) {
                    val count = manager.getMorphTargetCount(instance)
                    if (targetIndex in 0 until count) {
                        val weights = FloatArray(count)
                        weights[targetIndex] = wave * 0.72f
                        manager.setMorphWeights(instance, weights, 0)
                    }
                }
            }
        } else if (speaking && !reducedMotion) {
            // Approximate speech motion only; this is not phoneme-accurate lip-sync.
            val transforms: TransformManager = viewer.engine.transformManager
            val angle = wave * 0.045f
            val c = cos(angle)
            val s = sin(angle)
            talkNodes.forEach { entity ->
                if (transforms.hasComponent(entity)) {
                    val instance = transforms.getInstance(entity)
                    val base = FloatArray(16)
                    transforms.getTransform(instance, base)
                    val localX = floatArrayOf(1f, 0f, 0f, 0f, 0f, c, s, 0f, 0f, -s, c, 0f, 0f, 0f, 0f, 1f)
                    transforms.setTransform(instance, multiply4x4(base, localX))
                }
            }
        }
    }


    private fun resetTalkTransforms() {
        val transforms = viewer.engine.transformManager
        baseTalkTransforms.forEach { (entity, matrix) ->
            if (transforms.hasComponent(entity)) transforms.setTransform(transforms.getInstance(entity), matrix)
        }
    }

    private fun multiply4x4(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (column in 0..3) for (row in 0..3) {
            var sum = 0f
            for (k in 0..3) sum += a[k * 4 + row] * b[column * 4 + k]
            out[column * 4 + row] = sum
        }
        return out
    }

    private fun applyQuality(value: GraphicsQuality) {
        if (disposed) return
        runCatching {
            viewer.view.dynamicResolutionOptions = viewer.view.dynamicResolutionOptions.apply {
                enabled = value != GraphicsQuality.HIGH
                quality = when (value) {
                    GraphicsQuality.LOW -> com.google.android.filament.View.QualityLevel.LOW
                    GraphicsQuality.BALANCED -> com.google.android.filament.View.QualityLevel.MEDIUM
                    GraphicsQuality.HIGH -> com.google.android.filament.View.QualityLevel.HIGH
                }
            }
            viewer.view.antiAliasing = if (value == GraphicsQuality.LOW) com.google.android.filament.View.AntiAliasing.NONE else com.google.android.filament.View.AntiAliasing.FXAA
        }
    }

    fun dispose() {
        if (disposed) return
        pause()
        disposed = true
        scope.cancel()
        surface.setOnTouchListener(null)
        runCatching { viewer.destroyModel() }
        // ModelViewer owns the engine and destroys it when its SurfaceView detaches.
    }
}
