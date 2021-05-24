package paho.mqtt.java.example

import android.Manifest
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.WaitingAssertion
import com.moka.utils.Screenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 18)
class ConnectTest {

    @get:Rule
    var mActivityTestRule = ActivityScenarioRule(PahoExampleActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun basicLogcatTest() {
        WaitingAssertion.checkAssertion(R.id.history_recycler_view, isDisplayed(), 1500)
        Screenshot.takeScreenshot("Step1")
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.history_recycler_view, 4, MatchOperator.GREATER, 2500)
        Screenshot.takeScreenshot("End")
    }

}
