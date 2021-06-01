package info.mqtt.android.extsample.activity

import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import info.mqtt.android.extsample.R
import timber.log.Timber
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import java.lang.StringBuilder

class HelpFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_help, container, false)
        val websiteButton = rootView.findViewById<Button>(R.id.websiteButton)
        websiteButton.setOnClickListener {
            Timber.i("Opening Web Browser to Paho Website")
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PAHO_WEBSITE))
            startActivity(browserIntent)
        }
        val feedbackButton = rootView.findViewById<Button>(R.id.feedbackButton)
        feedbackButton.setOnClickListener {
            Timber.i("Preparing Feedback Email.")
            val data = Uri.parse("mailto:$FEEDBACK_EMAIL?subject=$FEEDBACK_SUBJECT&body=$debugInfoForEmail")
            val feedbackIntent = Intent(Intent.ACTION_VIEW, data)
            startActivity(feedbackIntent)
        }
        val enableLoggingSwitch = rootView.findViewById<SwitchCompat>(R.id.enable_logging_switch)
        enableLoggingSwitch.setOnCheckedChangeListener { _, isChecked ->
            val connections: Map<String, Connection> = getInstance(rootView.context).connections
            if (connections.isNotEmpty()) {
                val entry = connections.entries.iterator().next()
                val connection = entry.value
                connection.client.setTraceEnabled(isChecked)
                if (isChecked) {
                    connection.client.setTraceCallback(MqttTraceCallback())
                }
                Timber.i("Trace was set to: $isChecked")
            } else {
                Timber.i("No Connection available to enable / disable trace on.")
            }
        }

        // Inflate the layout for this fragment
        return rootView
    }

    private val debugInfoForEmail: String
        get() {
            val sb = StringBuilder()
            try {
                val pInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
                sb.append(FEEDBACK_VERSION + pInfo.versionName + FEEDBACK_NEW_LINE)
            } catch (ex: PackageManager.NameNotFoundException) {
                sb.append(FEEDBACK_VERSION + FEEDBACK_UNKNOWN + FEEDBACK_NEW_LINE)
            }
            sb.append(FEEDBACK_PHONE_MODEL + Build.MANUFACTURER + " " + Build.MODEL + FEEDBACK_NEW_LINE)
            sb.append(FEEDBACK_ANDROID_VERSION + Build.VERSION.SDK_INT)
            return sb.toString()
        }

    companion object {
        private const val FEEDBACK_EMAIL = "paho-dev@eclipse.org"
        private const val FEEDBACK_SUBJECT = "Eclipse Paho Android Sample Feedback"
        private const val FEEDBACK_VERSION = "App Version: "
        private const val FEEDBACK_PHONE_MODEL = "Phone Model: "
        private const val FEEDBACK_ANDROID_VERSION = "Android SDK Version: "
        private const val FEEDBACK_UNKNOWN = "Unknown"
        private const val FEEDBACK_NEW_LINE = "\r\n"
        private const val PAHO_WEBSITE = "http://www.eclipse.org/paho/"
    }
}