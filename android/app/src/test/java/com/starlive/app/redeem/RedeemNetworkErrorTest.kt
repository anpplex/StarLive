package com.starlive.app.redeem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RedeemNetworkErrorTest {
    @Test
    fun unknown_host_maps_offline() {
        val e = RedeemClient.mapNetworkError(UnknownHostException("buy.998618.xyz"), "兑换")
        assertTrue(e.message!!.contains("网络"))
    }

    @Test
    fun timeout_maps_offline() {
        val e = RedeemClient.mapNetworkError(SocketTimeoutException("timeout"), "兑换")
        assertTrue(e.message!!.contains("网络"))
    }

    @Test
    fun keeps_illegal_state() {
        val e = RedeemClient.mapNetworkError(IllegalStateException("兑换码无效"), "兑换")
        assertTrue(e.message!!.contains("兑换码无效"))
    }
}
