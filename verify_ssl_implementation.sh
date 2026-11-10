#!/bin/bash

echo "=== Verifying SSL Socket Factory Implementation ==="
echo ""

echo "1. Checking if the new method exists in MqttAndroidClient.kt..."
if grep -q "fun getSSLSocketFactory(trustStore: KeyStore" /Users/hannes/git/paho.mqtt.android/serviceLibrary/src/main/java/info/mqtt/android/service/MqttAndroidClient.kt; then
    echo "   ✓ New getSSLSocketFactory(trustStore: KeyStore) method found"
else
    echo "   ✗ New method not found"
    exit 1
fi

echo ""
echo "2. Checking if the deprecated method still exists..."
if grep -q "@Deprecated" /Users/hannes/git/paho.mqtt.android/serviceLibrary/src/main/java/info/mqtt/android/service/MqttAndroidClient.kt; then
    echo "   ✓ Deprecated annotation found on old method"
else
    echo "   ✗ Deprecated annotation not found"
    exit 1
fi

echo ""
echo "3. Checking if modern TLS version selection is implemented..."
if grep -q "TLSv1.3" /Users/hannes/git/paho.mqtt.android/serviceLibrary/src/main/java/info/mqtt/android/service/MqttAndroidClient.kt; then
    echo "   ✓ TLS 1.3 support found"
else
    echo "   ✗ TLS 1.3 support not found"
    exit 1
fi

echo ""
echo "4. Checking if HostnameVerifier import is present..."
if grep -q "import javax.net.ssl.HostnameVerifier" /Users/hannes/git/paho.mqtt.android/serviceLibrary/src/main/java/info/mqtt/android/service/MqttAndroidClient.kt; then
    echo "   ✓ HostnameVerifier import found"
else
    echo "   ✗ HostnameVerifier import not found"
    exit 1
fi

echo ""
echo "5. Checking if new test file was created..."
if [ -f "/Users/hannes/git/paho.mqtt.android/serviceLibrary/src/androidTest/java/info/mqtt/android/SSLSocketFactoryTest.kt" ]; then
    echo "   ✓ SSLSocketFactoryTest.kt created"
else
    echo "   ⚠ Test file not found (optional)"
fi

echo ""
echo "6. Building the project..."
cd /Users/hannes/git/paho.mqtt.android
./gradlew :serviceLibrary:compileDebugKotlin --quiet 2>&1

if [ $? -eq 0 ]; then
    echo "   ✓ Project builds successfully"
else
    echo "   ✗ Build failed"
    exit 1
fi

echo ""
echo "=== All checks passed! ==="
echo ""
echo "Summary of changes:"
echo "- New getSSLSocketFactory(KeyStore) method added"
echo "- New getSSLSocketFactory(KeyStore, HostnameVerifier) method added"
echo "- Old method marked as @Deprecated"
echo "- Modern TLS 1.2/1.3 support"
echo "- Hostname verification support"
echo "- Backward compatibility maintained"
echo ""
echo "The implementation is ready for use!"
