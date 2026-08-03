package com.starlive.app.runtime

/**
 * Pure delay tables for boot / process-start recover (unit-testable).
 */
object BootRecoverDelays {
    /** Cold boot / USER_PRESENT: cluster may not be ready immediately. */
    val BOOT: LongArray = longArrayOf(2_500L, 8_000L, 20_000L)

    /** User opened app after force-stop / blocked BOOT: first tick sooner. */
    val PROCESS_START: LongArray = longArrayOf(400L, 2_500L, 8_000L)

    const val DEBOUNCE_MS = 45_000L

    fun forReason(reason: String): LongArray =
        if (reason.startsWith("process-start")) PROCESS_START else BOOT
}
