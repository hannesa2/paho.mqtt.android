package org.eclipse.paho.android.service

import android.os.Binder


internal class MqttServiceBinder(
    val service: MqttService
) : Binder() {

    /**
     * @return the activityToken provided when the Service was started
     */
    var activityToken: String? = null

}
