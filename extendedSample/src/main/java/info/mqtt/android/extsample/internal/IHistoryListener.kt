package info.mqtt.android.extsample.internal

interface IHistoryListener {
    var identifer : String
    fun onHistoryReceived(history: String)
}
