package info.mqtt.android.extsample.internal;

import info.mqtt.android.extsample.model.ReceivedMessage;

public interface IReceivedMessageListener {

    void onMessageReceived(ReceivedMessage message);
}