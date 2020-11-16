[![](https://jitpack.io/v/hannesa2/paho.mqtt.android.svg)](https://jitpack.io/#hannesa2/paho.mqtt.android)
# Paho Android Service


The Paho Android Service is an MQTT client library written in Java for developing applications on Android.
It has been created to provide reliable open-source implementations of open and standard messaging protocols aimed at new, existing, and emerging
applications for Machine-to-Machine (M2M) and Internet of Things (IoT).
Paho reflects the inherent physical and cost constraints of device connectivity. Its objectives include effective levels of decoupling between devices and applications, designed to keep markets open and encourage the rapid growth of scalable Web and Enterprise middleware and applications.


## Features
|                     |                    |   |                      |                    |
|---------------------|--------------------|---|----------------------|--------------------|
| MQTT 3.1            | :heavy_check_mark: |   | Automatic Reconnect  | :heavy_check_mark: |
| MQTT 3.1.1          | :heavy_check_mark: |   | Offline Buffering    | :heavy_check_mark: |
| LWT                 | :heavy_check_mark: |   | WebSocket Support    | :heavy_check_mark: |
| SSL / TLS           | :heavy_check_mark: |   | Standard TCP Support | :heavy_check_mark: |
| Message Persistence | :heavy_check_mark: |   |

## Links

- Project Website: [https://www.eclipse.org/paho](https://www.eclipse.org/paho)
- Eclipse Project Information: [https://projects.eclipse.org/projects/iot.paho](https://projects.eclipse.org/projects/iot.paho)
- Paho Android Client Page: [https://www.eclipse.org/paho/clients/android/](https://www.eclipse.org/paho/clients/android/)
- GitHub: [https://github.com/eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android)
- Twitter: [@eclipsepaho](https://twitter.com/eclipsepaho)
- Issues: [https://github.com/eclipse/paho.mqtt.android/issues](https://github.com/eclipse/paho.mqtt.android/issues)
- Mailing-list: [https://dev.eclipse.org/mailman/listinfo/paho-dev](https://dev.eclipse.org/mailman/listinfo/paho-dev)


## Using the Paho Android Client

#### Jitpack.io

If you are using Android Studio and / or Gradle to manage your application dependencies and build then you can use the same repository to get the Paho Android Service. Add the Eclipse Maven repository to your `build.gradle` file and then add the Paho dependency to the `dependencies` section.

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
```
dependencies {
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    implementation 'com.github.hannesa2:paho.mqtt.android:$latestVersion'
}
```
__Note:__ currently you have to include the `org.eclipse.paho:org.eclipse.paho.client.mqttv3` dependency as well. We are attempting to get the build to produce an Android `AAR` file that contains both the Android service as well as it's dependencies, however this is still experimental. If you wish to try it, remove the `org.eclipse.paho:org.eclipse.paho.client.mqttv3` dependency and append `@aar` to the end of the Android Service dependency. E.g. `org.eclipse.paho:org.eclipse.paho.android.service:1.1.1@aar`

If you're using `androidx` dependency, include `androidx.legacy:legacy-support-v4:1.0.0`

If you find that there is functionality missing or bugs in the release version, you may want to try using the snapshot version to see if this helps before raising a feature request or an issue.

### Running the Sample App:

 * Open the this current directory in Android Studio (org.eclipse.paho.android.service).
 * In the toolbar along the top, there should be a dropdown menu. Make sure that it contains 'org.eclipse.android.sample' then click the Green 'Run' Triangle. It should now build and launch an Virtual Android Device to run the App. If you have an Android device with developer mode turned on plugged in, you will have the oppertunity to run it directly on that.
 * If you have any problems, check out the Android Developer Documentation for help: https://developer.android.com
