package com.starlive.app.runtime

/**
 * Pure constants for cluster launch confirm timeout (unit-testable).
 *
 * After [ClusterDisplayController.show] returns true, [showing] is set before the
 * cluster activity lands; confirm within [TIMEOUT_MS] or clear stuck「连接中…」.
 */
object LaunchConfirmPolicy {
    /** Wait for ClusterStripActivity resume on non-DEFAULT display before giving up. */
    const val TIMEOUT_MS = 5_000L
}
