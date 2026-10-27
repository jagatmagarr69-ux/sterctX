package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DisplayConfig
import com.example.model.ProjectionEngine
import com.example.model.ScalingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StretchX", appName)
    }

    @Test
    fun testProjectionEngineCalculations() {
        // Native panel 1080 x 2340
        val native = DisplayConfig(1080, 2340, 420)
        
        // At 1.11x projection multiplier:
        // Projected long edge = round(2340 * 1.11) = 2597
        // Projected short edge = 1080 (preserves native short edge, 0 black bars!)
        val projected = ProjectionEngine.calculateProjection(native, 1.11f, ScalingMode.STRETCH)
        
        assertEquals(1080, projected.shortEdge)
        assertEquals(2597, projected.longEdge)
        assertTrue(projected.densityDpi > 420)
    }
}
