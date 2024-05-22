package info.mqtt.android.extsample.activity

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
import androidx.test.uiautomator.UiDevice
import com.moka.lib.assertions.WaitingAssertion
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import timber.log.Timber
import java.lang.Thread.sleep


@RunWith(AndroidJUnit4::class)
class ExtendedPublishSleepTest {

    private lateinit var device: UiDevice

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

    @get:Rule
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-1AddConnect")

        onView(withId(R.id.disConnectSwitch)).perform(click())
        onView(withId(3)).perform(click())
        // onView(withTagValue(`is`("Subscribe" as Any))).perform(click())

        onView(withId(R.id.subscribe_button)).perform(click())
        onView(withId(R.id.subscription_topic_edit_text)).perform(typeText(TOPIC))
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-2Subscribe")
        onView(withText("OK")).perform(click())

        Assert.assertTrue("Device is in sleep mode", device.isScreenOn)

        // Now send device to sleep
        Timber.i("Send device to sleep")
        device.sleep()
        sleep(1000)
        Assert.assertFalse("Device is not in sleep mode", device.isScreenOn)

//        onView(ViewMatchers.isRoot())
//            .captureToBitmap()
//            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-sleep")
        Timber.i("wait $WAIT_SECONDS seconds")
        sleep(1000 * WAIT_SECONDS)
        device.wakeUp()
        sleep(1000)

        Timber.i("Wakeup device")
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-3afterWakeUp")

        onView(withId(2)).perform(click())
        onView(withId(R.id.topic)).perform(replaceText(TOPIC))
        onView(withId(R.id.message)).perform(replaceText("Typed message"))
        sleep(200)
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-4publish")
        onView(withId(R.id.publish_button)).perform(click())

        onView(withId(1)).perform(click())

        sleep(200)
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-5publish")

        WaitingAssertion.checkAssertion(R.id.history_list_view, Matchers.withListSizeBigger(0), 2500)
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-6End")
    }

    companion object {
        private const val TOPIC = "AnotherTest"
        private const val WAIT_SECONDS = 310L
    }
}
