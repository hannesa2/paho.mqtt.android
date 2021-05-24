package info.mqtt.java.example

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.RecyclerViewItemCountAssertion
import com.moka.utils.Screenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AwakeTest {

    @get:Rule
    var mActivityTestRule = ActivityScenarioRule(PahoExampleActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun checkAwake() {
        val recycler = Espresso.onView(ViewMatchers.withId(R.id.history_recycler_view))
        recycler.check(RecyclerViewItemCountAssertion(3, MatchOperator.GREATER_EQUAL))
        Screenshot.takeScreenshot("BeforeAwake")

        // Send device to sleep
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys deviceidle force-idle")
        Thread.sleep(WAIT_LONG)

        // awake the device
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys deviceidle unforce")
        Thread.sleep(WAIT)

        Screenshot.takeScreenshot("AfterAwake")
    }

    companion object {
        const val WAIT = 1000L
        const val WAIT_LONG = 6 * 60 * 1000L
    }
}
