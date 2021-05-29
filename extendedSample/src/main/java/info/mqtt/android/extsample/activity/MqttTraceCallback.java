/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
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
package info.mqtt.android.extsample.activity;

import info.mqtt.android.service.MqttTraceHandler;
import org.jetbrains.annotations.Nullable;

import timber.log.Timber;

class MqttTraceCallback implements MqttTraceHandler {

    @Override
    public void traceDebug(@Nullable String message) {
        Timber.d(message);
    }

    @Override
    public void traceError(@Nullable String message) {
        Timber.e(message);
    }

    @Override
    public void traceException(@Nullable String message, @Nullable Exception e) {
        Timber.e(e, message);
    }
}
