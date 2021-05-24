/*
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import timber.log.Timber;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 * <p>
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {

    private ClientComms comms;
    private final MqttService service;
    private BroadcastReceiver alarmReceiver;
    private final AlarmPingSender that;
    private PendingIntent pendingIntent;
    private volatile boolean hasStarted = false;

    public AlarmPingSender(MqttService service) {
        if (service == null) {
            throw new IllegalArgumentException("Neither service nor client can be null.");
        }
        this.service = service;
        that = this;
    }

    @Override
    public void init(ClientComms comms) {
        this.comms = comms;
        this.alarmReceiver = new AlarmReceiver();
    }

    @Override
    public void start() {
        String action = MqttServiceConstants.PING_SENDER + comms.getClient().getClientId();
        Timber.d("Register alarmreceiver to MqttService" + action);
        service.registerReceiver(alarmReceiver, new IntentFilter(action));

        pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT);

        schedule(comms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {

        Timber.d("Unregister alarmreceiver to MqttService " + comms.getClient().getClientId());
        if (hasStarted) {
            if (pendingIntent != null) {
                // Cancel Alarm.
                AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
            }

            hasStarted = false;
            try {
                service.unregisterReceiver(alarmReceiver);
            } catch (IllegalArgumentException e) {
                //Ignore unregister errors.
            }
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {

        long nextAlarmInMilliseconds = SystemClock.elapsedRealtime() + delayInMilliseconds;
        Timber.d("Schedule next alarm at " + nextAlarmInMilliseconds);
        AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
            // the device to run this task whilst dosing.
            Timber.d("Alarm schedule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            Timber.d("Alarm schedule using setExact, delay: " + delayInMilliseconds);
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        }
    }

    private class PingAsyncTask extends AsyncTask<ClientComms, Void, Boolean> {

        boolean success = false;

        protected Boolean doInBackground(ClientComms... comms) {
            IMqttToken token = comms[0].checkForActivity(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    success = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Timber.d("Ping async task : Failed.");
                    success = false;
                }
            });

            try {
                if (token != null) {
                    token.waitForCompletion();
                } else {
                    Timber.d("Ping async background : Ping command was not sent by the client.");
                }
            } catch (MqttException e) {
                Timber.d("Ping async background : Ignore MQTT exception : " + e.getMessage());
            } catch (Exception ex) {
                Timber.d("Ping async background : Ignore unknown exception : " + ex.getMessage());
            }
            if (!success) {
                Timber.d("Ping async background task completed at " + System.currentTimeMillis() + " Success is " + success);
            }
            return success;
        }

        protected void onPostExecute(Boolean success) {
            if (!success) {
                Timber.d("Ping async task onPostExecute() Success is " + this.success);
            }
        }

        protected void onCancelled(Boolean success) {
            Timber.d("Ping async task onCancelled() Success is " + this.success);
        }

    }

    /*
     * This class sends PingReq packet to MQTT broker
     */
    class AlarmReceiver extends BroadcastReceiver {
        private final String wakeLockTag = MqttServiceConstants.PING_WAKELOCK + that.comms.getClient().getClientId();
        private PingAsyncTask pingRunner = null;

        @Override
        @SuppressLint("Wakelock")
        public void onReceive(Context context, Intent intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.

            PowerManager pm = (PowerManager) service.getSystemService(Service.POWER_SERVICE);
            WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
            wakelock.acquire(10*60*1000L /*10 minutes*/);

            if (pingRunner != null) {
                if (pingRunner.cancel(true)) {
                    Timber.d("Previous ping async task was cancelled at:" + System.currentTimeMillis());
                }
            }

            pingRunner = new PingAsyncTask();
            pingRunner.execute(comms);

            if (wakelock.isHeld()) {
                wakelock.release();
            }
        }
    }
}
