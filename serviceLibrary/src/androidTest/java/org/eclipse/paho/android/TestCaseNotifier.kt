package org.eclipse.paho.android

import java.lang.InterruptedException

internal class TestCaseNotifier {

    var start: Int = 0

    @Synchronized
    fun waitForCompletion(timeout: Long) {
        try {
            Thread.sleep(timeout) // ugly !
        } catch (ignored: InterruptedException) {
        }
    }
}