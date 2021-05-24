package info.mqtt.java.example

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.RecyclerViewItemCountAssertion
import com.moka.utils.Screenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResumeTest {

    @get:Rule
    var mActivityTestRule = ActivityScenarioRule(PahoExampleActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun checkForDuplicateAfterPressRecentApps() {
        val recycler = Espresso.onView(ViewMatchers.withId(R.id.history_recycler_view))
        recycler.check(RecyclerViewItemCountAssertion(3, MatchOperator.GREATER_EQUAL))
        Screenshot.takeScreenshot("BeforeResume")

        // Might be a good idea to initialize it somewhere else
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.pressRecentApps()
        Screenshot.takeScreenshot("HomeButton")
        Thread.sleep(WAIT_LONG)

        uiDevice.pressRecentApps()
        Thread.sleep(WAIT)

        recycler.check(RecyclerViewItemCountAssertion(3, MatchOperator.GREATER_EQUAL))
        Screenshot.takeScreenshot("AfterResume")
    }

    companion object {
        const val WAIT_LONG = 6 * 60 * 1000L
        const val WAIT = 1000L
    }
}
