package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueLMProviderResolverTest {

    @Test
    fun defaultBundleIsMissingConfig() {
        val resolution = BlueLMProviderResolver.resolve(ProviderConfigBundle.defaults())

        assertSame(BlueLMProviderResolution.MissingConfig, resolution)
    }

    @Test
    fun incompleteCredentialIsNotReady() {
        val bundle = bundleWith(Credential.BlueLm(unitAppId, ""))

        val resolution = BlueLMProviderResolver.resolve(bundle)

        assertSame(BlueLMProviderResolution.IncompleteConfig, resolution)
    }

    @Test
    fun maskedCredentialIsInvalid() {
        val bundle = bundleWith(Credential.BlueLm(unitAppId, "sk-****"))

        val resolution = BlueLMProviderResolver.resolve(bundle)

        assertSame(BlueLMProviderResolution.InvalidConfig, resolution)
    }

    @Test
    fun realCredentialIsReadyAndReturnsBlueLmConfig() {
        val bundle = bundleWith(Credential.BlueLm(unitAppId, unitCredential))

        val resolution = BlueLMProviderResolver.resolve(bundle)

        assertTrue(resolution is BlueLMProviderResolution.Ready)
        assertSame(bundle.configOf(ProviderKind.BLUELM), (resolution as BlueLMProviderResolution.Ready).config)
        assertTrue(BlueLMProviderResolver.isReady(bundle))
    }

    private val unitAppId = "official-app-id"
    private val unitCredential = "unit-test-credential"

    private fun bundleWith(credential: Credential): ProviderConfigBundle {
        val base = ProviderConfigBundle.defaults()
        val blueLm = base.configOf(ProviderKind.BLUELM)!!.copy(credential = credential)
        return ProviderConfigBundle.forProfile(
            profile = LearnerProfile.OFFICIAL_BLUELM,
            configs = base.configs + (ProviderKind.BLUELM to blueLm),
        )
    }
}
