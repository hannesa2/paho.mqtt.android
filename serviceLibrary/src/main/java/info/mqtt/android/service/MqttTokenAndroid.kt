package info.mqtt.android.service

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage
import java.util.concurrent.locks.ReentrantLock

internal open class MqttTokenAndroid constructor(
    private val client: MqttAndroidClient,
    private var userContext: Any?,
    private var listener: IMqttActionListener?,
    private val topics: Array<String>? = null
) : IMqttToken {
    @Volatile
    private var isComplete = false

    @Volatile
    private var lastException: MqttException? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var delegate: IMqttToken? = null
    private var pendingException: Throwable? = null

    @Throws(MqttException::class)
    override fun waitForCompletion() {
        lock.lock()
        try {
            condition.await()
        } catch (_: InterruptedException) {
        } finally {
            lock.unlock()
        }
        pendingException?.let { throw it }
    }

    @Throws(MqttException::class)
    override fun waitForCompletion(timeout: Long) {
        lock.lock()
        try {
            condition.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
        } finally {
            lock.unlock()
        }
        if (!isComplete) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt(), Throwable("After $timeout ms"))
        }
        pendingException?.let { throw it }
    }

    fun notifyComplete() {
        lock.lock()
        try {
            isComplete = true
            condition.signalAll()
            listener?.onSuccess(this)
        } finally {
            lock.unlock()
        }
    }

    fun notifyFailure(throwable: Throwable) {
        lock.lock()
        try {
            isComplete = true
            pendingException = throwable
            condition.signalAll()
            if (throwable is MqttException) {
                lastException = throwable
            }
            listener?.onFailure(this, throwable)
        } finally {
            lock.unlock()
        }
    }

    override fun isComplete(): Boolean {
        return isComplete
    }

    override fun getException(): MqttException? {
        return lastException
    }

    override fun getClient(): IMqttAsyncClient {
        return client
    }

    override fun getActionCallback(): IMqttActionListener? {
        return listener
    }

    override fun setActionCallback(listener: IMqttActionListener) {
        this.listener = listener
    }

    override fun getTopics(): Array<String>? {
        return topics
    }

    override fun getUserContext(): Any? {
        return userContext
    }

    override fun setUserContext(userContext: Any) {
        this.userContext = userContext
    }

    fun setDelegate(delegate: IMqttToken?) {
        this.delegate = delegate
    }

    override fun getMessageId(): Int {
        return if (delegate != null) delegate!!.messageId else 0
    }

    override fun getResponse(): MqttWireMessage {
        return delegate!!.response
    }

    override fun getSessionPresent(): Boolean {
        return delegate!!.sessionPresent
    }

    override fun getGrantedQos(): IntArray {
        return delegate!!.grantedQos
    }

}
