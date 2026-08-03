package com.starlive.app.upgrade

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun newer_tag() {
        assertTrue(UpdateChecker.isNewerThan("0.1.8-polish", "v0.1.15-quality"))
        assertTrue(UpdateChecker.isNewerThan("0.1.14-car", "v0.1.15-quality"))
    }

    @Test
    fun same_or_older() {
        assertFalse(UpdateChecker.isNewerThan("0.1.15-quality", "v0.1.15-quality"))
        assertFalse(UpdateChecker.isNewerThan("0.1.16-polish", "v0.1.15-quality"))
        assertFalse(UpdateChecker.isNewerThan("0.2.0", "v0.1.99"))
    }

    @Test
    fun messy_tags() {
        assertTrue(UpdateChecker.isNewerThan("0.1.0", "v0.1.1-car"))
        assertFalse(UpdateChecker.isNewerThan("1.0.0", "v0.9.9-beta"))
    }
}
