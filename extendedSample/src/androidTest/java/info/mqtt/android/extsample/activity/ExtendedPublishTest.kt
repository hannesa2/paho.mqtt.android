package info.mqtt.android.extsample.activity

import android.Manifest
import android.view.Gravity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.moka.lib.assertions.WaitingAssertion
import com.moka.utils.Screenshot
import com.moka.utils.ScreenshotActivityRule
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ExtendedPublishTest {

    @get:Rule
    var mActivityTestRule = ScreenshotActivityRule(MainActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun mainSmokeTest() {
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open())
        onView(withId(R.id.action_add_connection)).perform(click())
        onView(withId(R.id.action_save_connection)).perform(click())

        Screenshot.takeScreenshot("AddConnect")

        onView(withId(R.id.switchForActionBar)).perform(click())
        onView(withId(3)).perform(click())
        //onView(withTagValue(`is`("Subscribe" as Any))).perform(click())

        onView(withId(R.id.subscribe_button)).perform(click())
        onView(withId(R.id.subscription_topic_edit_text)).perform(typeText(TOPIC))
        Screenshot.takeScreenshot("Subscribe")
        onView(withText("OK")).perform(click())

        onView(withId(2)).perform(click())
        onView(withId(R.id.topic)).perform(replaceText(TOPIC))
        onView(withId(R.id.message)).perform(replaceText("msg"))
        Screenshot.takeScreenshot("publish")
        onView(withId(R.id.publish_button)).perform(click())

        onView(withId(1)).perform(click())

        WaitingAssertion.checkAssertion(R.id.history_list_view, Matchers.withListSizeBigger(0), 2500)
        Screenshot.takeScreenshot("End")
    }

    companion object {
        private const val TOPIC = "AnotherTest"
    }
}
