package info.mqtt.android.service.extension

import android.os.Bundle
import java.io.Serializable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> {
        @Suppress("DEPRECATION")
        getParcelable(key) as? T
    }
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> {
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}
