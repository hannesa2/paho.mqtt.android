package info.mqtt.android.service.extension

import android.content.Intent
import android.os.Bundle
import java.io.Serializable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> getParcelable(key) as? T
}

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> getSerializable(key) as? T
}

@Suppress("DEPRECATION")
inline fun <reified T> Intent.parcelableExtra(key: String): T? = when {
    SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> getParcelableExtra(key) as? T
}
