package info.mqtt.android.extsample.activity

import android.app.UiAutomation
import android.os.Build
import android.view.Gravity
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moka.lib.assertions.WaitingAssertion
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Thread.sleep
import java.util.Locale


@RunWith(AndroidJUnit4::class)
class LongRunningSleepMode {

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

    @get:Rule
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Before
    fun setUp() {
    }

    @Test
    fun connectWaitAndPublish() {
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open())
        onView(withId(R.id.action_add_connection)).perform(click())
        onView(withId(R.id.action_save_connection)).perform(click())

        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-AddConnect")

        onView(withId(R.id.disConnectSwitch)).perform(click())
        onView(withId(3)).perform(click())
        // onView(withTagValue(`is`("Subscribe" as Any))).perform(click())

        onView(withId(R.id.subscribe_button)).perform(click())
        onView(withId(R.id.subscription_topic_edit_text)).perform(typeText(TOPIC))
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-Subscribe")
        onView(withText("OK")).perform(click())

        // Now send device to sleep
        Timber.i("Send device to sleep")
        sendKeyEvent(KeyEvent.SLEEP)
        Timber.i("wait ${WAIT_SECONDS}")
        sleep(1000 * WAIT_SECONDS)
        sendKeyEvent(KeyEvent.AWAKE)
        Timber.i("Awake device")

        onView(withId(2)).perform(click())
        onView(withId(R.id.topic)).perform(replaceText(TOPIC))
        onView(withId(R.id.message)).perform(replaceText("msg"))
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-publish")
        onView(withId(R.id.publish_button)).perform(click())

        onView(withId(1)).perform(click())

        WaitingAssertion.checkAssertion(R.id.history_list_view, Matchers.withListSizeBigger(0), 2500)
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-End")
    }

    // Source:
    // https://github.com/facebook/screenshot-tests-for-android/blob/main/core/src/main/java/com/facebook/testing/screenshot/internal/Registry.java
    private fun sendKeyEvent(event: KeyEvent) {
        if (Build.VERSION.SDK_INT < 23) {
            return
        }
        val command = String.format(Locale.ENGLISH, "adb shell input keyevent %s", event.eventKey)

//        Timber.d("event=${event.name} cmd='$command'")
//        try {
//            val proc = Runtime.getRuntime().exec(arrayOf(command))
//            var line: String?
//
//            val stderr = proc.errorStream
//            val esr = InputStreamReader(stderr)
//            val ebr = BufferedReader(esr)
//            while ((ebr.readLine().also { line = it }) != null) Timber.e("FXN-BOOTCLASSPATH", line!!)
//
//            val stdout = proc.inputStream
//            val osr = InputStreamReader(stdout)
//            val obr = BufferedReader(osr)
//            while ((obr.readLine().also { line = it }) != null) Timber.i("FXN-BOOTCLASSPATH", line!!)
//
//            val exitVal = proc.waitFor()
//            Timber.d("FXN-BOOTCLASSPATH", "getprop exitValue: $exitVal")
//        } catch (e: Exception) {
//            Timber.e(e)
//        }

        Timber.d("event=${event.name} cmd='$command'")
        val automation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val fileDescriptor = automation.executeShellCommand(command)
        val stream: InputStream = FileInputStream(fileDescriptor.fileDescriptor)
        try {
            val buffer = ByteArray(1024)
            Timber.d("start")
            while (stream.read(buffer) != -1) {
                Timber.d("while")
                // Consume stdout to ensure the command completes
                Timber.v(buffer.toString())
            }
            Timber.d("done")
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                Timber.e(e)
            }
            try {
                fileDescriptor.close()
            } catch (e: IOException) {
                Timber.e(e)
            }
            Timber.d("finished")
        }
    }

    companion object {
        private const val TOPIC = "AnotherTest"
        private const val WAIT_SECONDS = 60L
    }
}
