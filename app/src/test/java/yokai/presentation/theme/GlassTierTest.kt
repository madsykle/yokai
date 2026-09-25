package yokai.presentation.theme

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tier boundaries decide which rendering path every glass surface takes; a regression here is
 * silent until someone runs the app on an older device, so they are pinned explicitly.
 * DESIGN.md §3: refraction needs AGSL (API 33), blur needs RenderEffect (API 31), and below
 * that the scrim fallback is the only honest option.
 */
class GlassTierTest {

    @Test
    fun `api 33 and up get the full refraction tier`() {
        glassTierFor(33) shouldBe GlassTier.Full
        glassTierFor(34) shouldBe GlassTier.Full
        glassTierFor(36) shouldBe GlassTier.Full
    }

    @Test
    fun `api 31 and 32 get the blur tier`() {
        glassTierFor(31) shouldBe GlassTier.Blur
        glassTierFor(32) shouldBe GlassTier.Blur
    }

    @Test
    fun `api 30 and below get the scrim fallback`() {
        glassTierFor(30) shouldBe GlassTier.Scrim
        glassTierFor(29) shouldBe GlassTier.Scrim
        glassTierFor(21) shouldBe GlassTier.Scrim
    }

    @Test
    fun `the refraction tier starts exactly one level above the blur tier`() {
        glassTierFor(32) shouldBe GlassTier.Blur
        glassTierFor(33) shouldBe GlassTier.Full
    }
}
