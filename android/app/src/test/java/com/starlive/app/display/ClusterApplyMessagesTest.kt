package com.starlive.app.display

import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterApplyMessagesTest {
    @Test
    fun c4_no_cluster_copy() {
        val msg = ClusterApplyMessages.noCluster("id=0 name=control_panel")
        assertTrue(msg.contains("星环屏不可达"))
        assertTrue(msg.contains("模拟器"))
        assertTrue(ClusterApplyMessages.isNoClusterHint(msg))
    }
}
