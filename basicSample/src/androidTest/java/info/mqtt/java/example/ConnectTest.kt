package info.mqtt.java.example

import android.Manifest
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.WaitingAssertion
import com.moka.utils.Screenshot
import com.moka.utils.ScreenshotActivityRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectTest {

    @get:Rule
    var mActivityTestRule = ScreenshotActivityRule(MQTTExampleActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    @Ignore("On CI it doesn't work anymore")
    fun basicSmokeTest() {
        WaitingAssertion.checkAssertion(R.id.history_recycler_view, isDisplayed(), 1500)
        Screenshot.takeScreenshot("Step1")
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.history_recycler_view, 3, MatchOperator.GREATER_EQUAL, 5500)
        Screenshot.takeScreenshot("End")
    }

}
