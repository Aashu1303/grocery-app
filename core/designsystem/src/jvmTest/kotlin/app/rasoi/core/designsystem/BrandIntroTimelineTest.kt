package app.rasoi.core.designsystem

import app.rasoi.core.designsystem.brand.BrandGeometry
import app.rasoi.core.designsystem.brand.BrandIntroTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrandIntroTimelineTest {
    private val launch = BrandIntroTimeline.frameAt(0)
    private val zoom = BrandIntroTimeline.frameAt(BrandIntroTimeline.ZOOM_END)
    private val landed = BrandIntroTimeline.frameAt(BrandIntroTimeline.LAND_END)
    private val end = BrandIntroTimeline.frameAt(BrandIntroTimeline.DURATION_MILLIS)

    @Test
    fun firstFrameMatchesThePlatformLaunchScreen() {
        // Android 12+ and iOS draw the 108-unit mark at 240 dp, centred, without the leaf.
        assertEquals(240f / 108f, launch.scale, TOLERANCE)
        assertEquals(BrandGeometry.CANVAS_CENTRE, launch.anchorX, TOLERANCE)
        assertEquals(BrandGeometry.CANVAS_CENTRE, launch.anchorY, TOLERANCE)
        assertEquals(0f, launch.leafAlpha)
        assertEquals(0f, launch.wordReveal)
        assertEquals(0f, launch.taglineAlpha)
    }

    @Test
    fun theMarkZoomsInThenSettlesWhileStayingCentred() {
        assertTrue(zoom.scale > landed.scale && landed.scale > launch.scale)
        assertEquals(BrandGeometry.CANVAS_CENTRE, zoom.anchorX, TOLERANCE)
        assertEquals(BrandGeometry.CANVAS_CENTRE, landed.anchorX, TOLERANCE)
    }

    @Test
    fun theLeafLandsOnTheRaBeforeTheWordAppears() {
        assertEquals(1f, landed.leafAlpha, TOLERANCE)
        assertEquals(0f, landed.leafRotation, TOLERANCE)
        assertEquals(BrandGeometry.LEAF_CENTRE_X, landed.leafX, TOLERANCE)
        assertEquals(BrandGeometry.LEAF_CENTRE_Y, landed.leafY, TOLERANCE)
        assertEquals(0f, landed.wordReveal)
    }

    @Test
    fun lastFrameIsTheCentredLockupWithTheLeafAsTheDot() {
        assertEquals(BrandGeometry.LOCKUP_CENTRE_X, end.anchorX, TOLERANCE)
        assertEquals(BrandGeometry.LOCKUP_CENTRE_Y, end.anchorY, TOLERANCE)
        assertEquals(BrandGeometry.LEAF_DOT_X, end.leafX, TOLERANCE)
        assertEquals(BrandGeometry.LEAF_DOT_Y, end.leafY, TOLERANCE)
        assertEquals(1f, end.wordReveal, TOLERANCE)
        assertEquals(1f, end.wordAlpha, TOLERANCE)
        assertEquals(1f, end.taglineAlpha, TOLERANCE)
        assertTrue(end.scale < launch.scale)
    }

    @Test
    fun timeOutsideTheIntroIsClamped() {
        assertEquals(launch, BrandIntroTimeline.frameAt(-1))
        assertEquals(end, BrandIntroTimeline.frameAt(BrandIntroTimeline.DURATION_MILLIS * 2))
    }

    @Test
    fun revealAndFadesNeverRunBackwards() {
        val frames = (0..BrandIntroTimeline.DURATION_MILLIS).map(BrandIntroTimeline::frameAt)
        frames.zipWithNext().forEach { (a, b) ->
            assertTrue(b.wordReveal >= a.wordReveal && b.leafAlpha >= a.leafAlpha && b.taglineAlpha >= a.taglineAlpha)
        }
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
