package org.eclipse.paho.android

import kotlin.Throws
import java.lang.InterruptedException

internal class TestCaseNotifier {
    private var exception: Throwable? = null
    fun storeException(exception: Throwable?) {
        this.exception = exception
    }

    @Synchronized
    @Throws(Throwable::class)
    fun waitForCompletion(timeout: Long) {
        try {
            Thread.sleep(timeout) // ugly !
        } catch (ignored: InterruptedException) {
        }
    }
}