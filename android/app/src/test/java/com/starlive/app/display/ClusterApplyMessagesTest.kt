package com.starlive.app.display

import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterApplyMessagesTest {
    @Test
    fun c4_no_cluster_copy() {
        val msg = ClusterApplyMessages.noCluster("id=0 name=control_panel")
        assertTrue(msg.contains("未检测到星环屏"))
        // Must not leak developer probe strings into user copy.
        assertTrue(!msg.contains("control_panel"))
        assertTrue(!msg.contains("cluster", ignoreCase = true))
        assertTrue(ClusterApplyMessages.isNoClusterHint(msg))
    }
}
