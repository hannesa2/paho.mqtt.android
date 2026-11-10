package info.mqtt.android.service.ping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.mqtt.android.service.ping.AlarmPingSender.Companion.sdf
import info.mqtt.android.service.room.entity.PingEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import timber.log.Timber
import java.util.Date
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class PingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val logging = inputData.getBoolean(LOGGING, false)
        val keepRecords = inputData.getInt(KEEP_RECORDS_COUNT, 1000)
        val key = inputData.getString("id")
        Timber.d("$key Sending Ping at: ${sdf.format(Date(System.currentTimeMillis()))}")

        //check if id is not null
        if (key == null) {
            Timber.e("connection id in ping worker is null!")
            return Result.failure()
        }

        // Drop jobs that don't belong to the current sender for this id, e.g. jobs left by a
        // killed process. Without this, a leftover job would start a second ping chain once
        // the new connection is up, since the client id is the same.
        val session = inputData.getString(SESSION)
        if (session == null || session != AlarmPingSender.activeSessions[key]) {
            Timber.w("$key: dropping ping job from an old session")
            return Result.success()
        }

        //check if there is a clients comm asociated with the key
        val comms = AlarmPingSender.clientCommsMap[key]
        if (comms == null) {
            Timber.e("client comm doesn't exist anymore: $key")
            return Result.failure()
        }

        // Paho has already scheduled the next ping by the time checkForActivity() returns, so
        // waiting here only covers the PINGRESP. Without a limit, a missing PINGRESP keeps this
        // job running until JobScheduler's own timeout, which then calls onStopJob on the main
        // thread. A timeout only ends this job; Paho still detects the dead connection on its
        // next check.
        val result = withTimeoutOrNull(PING_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Timber.d("$key Ping Success ${asyncActionToken?.client?.clientId}")
                        if (logging) recordPing(asyncActionToken, success = true, error = null, keepRecords = keepRecords)
                        if (continuation.isActive) continuation.resume(Result.success())
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Timber.e("$key Ping Failure $exception ${asyncActionToken?.client?.clientId}")
                        if (logging) recordPing(asyncActionToken, success = false, error = exception?.message, keepRecords = keepRecords)
                        if (continuation.isActive) continuation.resume(Result.failure())
                    }
                }

                try {
                    // when token is null doesn't always mean a failure sometimes there wasn't a need
                    // send a ping request yet
                    if (comms.checkForActivity(listener) == null && continuation.isActive) {
                        continuation.resume(Result.success())
                    }
                } catch (e: Exception) {
                    Timber.e(e, "$key checkForActivity threw")
                    if (continuation.isActive) continuation.resume(Result.failure())
                }
            }
        }

        return result ?: run {
            Timber.w("$key Ping response not received within ${PING_TIMEOUT_MS}ms")
            Result.failure()
        }
    }

    // Runs on Paho's callback thread.
    private fun recordPing(token: IMqttToken?, success: Boolean, error: String?, keepRecords: Int) {
        try {
            val dao = AlarmPingSender.messageDatabase?.pingDao() ?: return
            dao.insert(PingEntity(System.currentTimeMillis(), token?.client?.clientId, token?.client?.serverURI, success, error))
            dao.removeOldData(keepRecords)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record ping")
        }
    }

    companion object {
        const val LOGGING = "logging"
        const val KEEP_RECORDS_COUNT = "keepCount"
        const val SESSION = "session"
        const val PING_TIMEOUT_MS = 10_000L
    }
}
