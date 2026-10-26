package dev.aitalkingavatar.avatar

import java.io.File

data class AssetValidationReport(
    val isValid: Boolean,
    val fileSize: Long,
    val message: String
)

object AvatarAssetValidator {
    fun validateGlbFile(file: File?): AssetValidationReport {
        if (file == null || !file.exists()) {
            return AssetValidationReport(false, 0L, "Asset file does not exist.")
        }
        if (!file.canRead()) {
            return AssetValidationReport(false, file.length(), "Asset file is not readable.")
        }
        val size = file.length()
        if (size < 100) {
            return AssetValidationReport(false, size, "Asset file is too small to be a valid GLB.")
        }
        return AssetValidationReport(true, size, "GLB asset successfully validated and readable.")
    }
}
