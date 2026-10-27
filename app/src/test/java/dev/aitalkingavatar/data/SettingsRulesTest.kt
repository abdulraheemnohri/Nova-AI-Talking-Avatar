package dev.aitalkingavatar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {
    @Test fun defaultsPointToSameDeviceLoopbackAndRequireExplicitHttpOptIn() {
        assertEquals("http://localhost:11434", AvatarPreferences().endpoint)
        assertTrue(EndpointRules.validate(DEFAULT_ENDPOINT, false) is EndpointValidation.Invalid)
        assertTrue(EndpointRules.validate(DEFAULT_ENDPOINT, true) is EndpointValidation.Valid)
    }

    @Test fun acceptsHttpsWithoutHttpOptIn() {
        assertTrue(EndpointRules.validate("https://model.local:11434", false) is EndpointValidation.Valid)
    }

    @Test fun disablingHttpReplacesHttpEndpointsWithSecureFallbacks() {
        val normalized = SettingsRules.normalize(
            AvatarPreferences(
                allowInsecureHttp = false,
                ollamaEndpoint = "http://model.local:11434",
                liteRtEndpoint = "http://model.local:8080"
            )
        )
        assertEquals("https://localhost:11434", normalized.ollamaEndpoint)
        assertEquals("https://localhost:8080", normalized.liteRtEndpoint)
        assertTrue(EndpointRules.validate(normalized.ollamaEndpoint, false) is EndpointValidation.Valid)
        assertTrue(EndpointRules.validate(normalized.liteRtEndpoint, false) is EndpointValidation.Valid)
    }

    @Test fun rejectsCredentialsAndUnsupportedSchemes() {
        assertTrue(EndpointRules.validate("https://user:secret@model.local", true) is EndpointValidation.Invalid)
        assertTrue(EndpointRules.validate("file:///tmp/model", true) is EndpointValidation.Invalid)
        assertTrue(EndpointRules.validate("https://model.local/api?token=secret", true) is EndpointValidation.Invalid)
    }

    @Test fun redactionRemovesQueryAndUserInformation() {
        assertEquals("https://example.test:443/api", EndpointRules.redacted("https://example.test:443/api?token=hidden"))
    }

    @Test fun normalizesTextAndBoundsVoiceAndCameraPreferences() {
        val normalized = SettingsRules.normalize(AvatarPreferences(providerType = AiProviderType.OLLAMA, ollamaEndpoint = " https://localhost:8443/ ", ollamaModel = " model ", voiceLocale = " ", speechRate = 3f, pitch = 0.2f, cameraZoom = 9f))
        assertEquals("https://localhost:8443/", normalized.endpoint)
        assertEquals("model", normalized.modelName)

        assertEquals("en-US", normalized.voiceLocale)
        assertEquals(2f, normalized.speechRate, 0.001f)
        assertEquals(0.5f, normalized.pitch, 0.001f)
        assertEquals(2f, normalized.cameraZoom, 0.001f)
    }

    @Test fun replacesNonFiniteVoiceAndCameraValuesWithSafeDefaults() {
        val normalized = SettingsRules.normalize(
            AvatarPreferences(
                speechRate = Float.NaN,
                pitch = Float.POSITIVE_INFINITY,
                cameraZoom = Float.NEGATIVE_INFINITY
            )
        )
        assertEquals(1f, normalized.speechRate, 0.001f)
        assertEquals(1f, normalized.pitch, 0.001f)
        assertEquals(1f, normalized.cameraZoom, 0.001f)
    }

    @Test fun neverPersistsQueryCredentialsOrUserInfo() {
        assertEquals(DEFAULT_ENDPOINT, SettingsRules.normalize(AvatarPreferences(ollamaEndpoint = "https://model.local/api?token=secret")).endpoint)
        assertEquals(DEFAULT_ENDPOINT, SettingsRules.normalize(AvatarPreferences(ollamaEndpoint = "https://user:secret@model.local")).endpoint)
    }
}
