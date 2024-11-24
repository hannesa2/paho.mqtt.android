package info.mqtt.android.extsample.activity

import android.graphics.Bitmap
import android.view.Gravity
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.moka.lib.assertions.WaitingAssertion
import info.hannes.timber.DebugFormatTree
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import org.hamcrest.CoreMatchers.not
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import timber.log.Timber
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
class ExtendedPublishTest {

    private lateinit var device: UiDevice

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

    @get:Rule
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Before
    fun setUp() {
        Timber.plant(DebugFormatTree())
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun connectAndPublish() {
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open())
        onView(withId(R.id.action_add_connection)).perform(click())
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-0ShowConnection")
            })

        onView(withId(R.id.action_save_connection)).perform(click())

        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-1AddConnect")
            })

        onView(withId(R.id.disConnectSwitch)).perform(click())
        onView(withId(3)).perform(click())
        // onView(withTagValue(`is`("Subscribe" as Any))).perform(click())

        onView(withId(R.id.subscribe_button)).perform(click())
        onView(withId(R.id.subscription_topic_edit_text)).perform(typeText(TOPIC))
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-2Subscribe")
            })
        onView(withText("OK")).perform(click())

        Assert.assertTrue(device.isScreenOn)

        onView(withId(2)).perform(click())
        onView(withId(R.id.topic)).perform(replaceText(TOPIC))
        onView(withId(R.id.message)).perform(replaceText("Typed message"))
        sleep(400)
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-4publish")
            })
        onView(withId(R.id.publish_button)).perform(click())

        onView(withId(1)).perform(click())

        WaitingAssertion.checkAssertion(R.id.history_list_view, Matchers.withListSizeBigger(0), 2500)
        sleep(400)
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-6End")
            })
    }

    @Test
    fun disconnect() {
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-Before")
            })
        // it should be checked on previous test
        onView(withId(R.id.disConnectSwitch)).check(matches(isChecked()))

        onView(withId(R.id.disConnectSwitch)).perform(click())
        sleep(400)
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-isDisConnected")
            })

        onView(withId(R.id.disConnectSwitch)).check(matches(not(isChecked())))

        onView(withId(R.id.disConnectSwitch)).perform(click())
        sleep(400)
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap ->
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-isConnectedAgain")
            })

        onView(withId(R.id.disConnectSwitch)).check(matches(isChecked()))
    }

    companion object {
        private const val TOPIC = "AnotherTest"
    }
}
