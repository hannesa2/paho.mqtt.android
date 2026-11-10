# Certificate Pinning Implementation for MQTT-Android

## Overview

The `getSSLSocketFactory` method has been enhanced to support modern SSL/TLS security features including:

- **Modern TLS versions** (TLS 1.2/1.3)
- **Hostname verification** support
- **Custom trust stores**
- **Backward compatibility** with existing code

## Implementation Details

### Old Method (Deprecated)

```kotlin
@Deprecated("Use getSSLSocketFactory(KeyStore) for better security")
fun getSSLSocketFactory(keyStore: InputStream?, password: String): SSLSocketFactory
```

**Issues with the old implementation:**
- Uses outdated `TLSv1` protocol
- No hostname verification support
- Limited security features

### New Methods

#### Basic SSL with Custom Trust Store

```kotlin
fun getSSLSocketFactory(trustStore: KeyStore): SSLSocketFactory
```

This method provides:
- Automatic TLS version selection (TLS 1.3 on Android 10+, TLS 1.2 on Android 5+)
- Custom trust store validation
- Modern cipher suites

#### SSL with Hostname Verification

```kotlin
fun getSSLSocketFactory(
    trustStore: KeyStore, 
    hostnameVerifier: HostnameVerifier? = null
): SSLSocketFactory
```

This method provides all features above plus:
- Custom hostname verification
- Additional security layer for MITM protection

## Usage Examples

### Example 1: Basic SSL Connection with Custom Trust Store

```kotlin
val mqttClient = MqttAndroidClient(context, serverUri, clientId)

// Load your custom trust store (BKS format)
val trustStoreInputStream = context.assets.open("truststore.bks")
val trustStore = KeyStore.getInstance("BKS")
trustStore.load(trustStoreInputStream, "password".toCharArray())

// Get SSL socket factory with modern TLS
val sslSocketFactory = mqttClient.getSSLSocketFactory(trustStore)

// Use with MQTT connection options
val options = MqttConnectOptions()
options.socketFactory = sslSocketFactory
mqttClient.connect(options)
```

### Example 2: SSL with Custom Hostname Verification

```kotlin
val mqttClient = MqttAndroidClient(context, serverUri, clientId)

// Load trust store
val trustStoreInputStream = context.assets.open("truststore.bks")
val trustStore = KeyStore.getInstance("BKS")
trustStore.load(trustStoreInputStream, "password".toCharArray())

// Create custom hostname verifier
val hostnameVerifier = HostnameVerifier { hostname, session ->
    // Custom verification logic
    val peerCertificate = session.peerCertificates.firstOrNull()
    // Add your pinning logic here
    true // or your custom verification
}

// Get SSL socket factory with hostname verification
val sslSocketFactory = mqttClient.getSSLSocketFactory(trustStore, hostnameVerifier)

// Use with MQTT connection options
val options = MqttConnectOptions()
options.socketFactory = sslSocketFactory
mqttClient.connect(options)
```

### Example 3: Certificate Pinning with OkHttp (Recommended)

For production applications, consider using OkHttp's `CertificatePinner` for robust certificate pinning:

```kotlin
// First, get the server's certificate hash
val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(CertificatePinner.Builder()
        .add("mqtt.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .build())
    .build()

// Extract the SSLSocketFactory from OkHttp
val sslSocketFactory = okHttpClient.sslSocketFactory

// Use with MQTT
val options = MqttConnectOptions()
options.socketFactory = sslSocketFactory
mqttClient.connect(options)
```

### Example 4: Loading Certificate from Server

```kotlin
// Get certificate from server (one-time setup)
fun getServerCertificate(host: String, port: Int = 443): X509Certificate? {
    val socketFactory = SSLSocketFactory.getDefault()
    val socket = socketFactory.createSocket(host, port) as SSLSocket
    try {
        socket.startHandshake()
        val session = socket.session
        return session.peerCertificates.firstOrNull() as? X509Certificate
    } finally {
        socket.close()
    }
}

// Extract public key hash for pinning
fun getPublicKeyHash certificate: X509Certificate): String {
    val publicKey = certificate.publicKey
    val sha256 = MessageDigest.getInstance("SHA-256")
    val hash = sha256.digest(publicKey.encoded)
    return Base64.encodeToString(hash, Base64.NO_WRAP)
}
```

## Migration Guide

### From Old Implementation

**Before:**
```kotlin
val keyStoreInputStream = context.assets.open("keystore.bks")
val sslSocketFactory = mqttClient.getSSLSocketFactory(keyStoreInputStream, "password")
```

**After:**
```kotlin
val keyStoreInputStream = context.assets.open("keystore.bks")
val trustStore = KeyStore.getInstance("BKS")
trustStore.load(keyStoreInputStream, "password".toCharArray())
val sslSocketFactory = mqttClient.getSSLSocketFactory(trustStore)
```

## Security Best Practices

1. **Always use certificate pinning** in production to prevent MITM attacks
2. **Use TLS 1.2 or higher** - the new implementation automatically selects the best version
3. **Keep backup certificates** in case your primary certificate expires
4. **Use OkHttp's CertificatePinner** for production apps - it provides:
   - Multiple pin support for certificate rotation
   - Automatic validation
   - Better error handling
   - Well-tested implementation

## API Reference

### getSSLSocketFactory(trustStore: KeyStore)

Creates an SSL socket factory using the provided trust store.

**Parameters:**
- `trustStore`: The trusted certificates keystore

**Returns:**
- `SSLSocketFactory`: Configured with modern TLS versions

**Throws:**
- `MqttSecurityException`: If there was an error getting the SSLSocketFactory

### getSSLSocketFactory(trustStore: KeyStore, hostnameVerifier: HostnameVerifier?)

Creates an SSL socket factory with custom hostname verification.

**Parameters:**
- `trustStore`: The trusted certificates keystore
- `hostnameVerifier`: Optional hostname verifier for additional security

**Returns:**
- `SSLSocketFactory`: Configured with custom hostname verification

**Throws:**
- `MqttSecurityException`: If there was an error getting the SSLSocketFactory

## Supported Android Versions

- **Minimum**: Android 5.0 (API 21) - TLS 1.2
- **Recommended**: Android 10+ (API 29) - TLS 1.3 support

## Troubleshooting

### common Issues

1. **SSL Handshake Failed**
   - Ensure the server certificate is in your trust store
   - Check TLS version compatibility
   - Verify hostname matches certificate

2. **Certificate Not Trusted**
   - Include all intermediate certificates in your trust store
   - Check certificate chain validity
   - Verify certificate expiration

3. **Hostname Verification Failed**
   - Ensure your hostname verifier correctly validates the host
   - Check certificate Common Name (CN) or Subject Alternative Names (SANs)

## Future Enhancements

Potential improvements for future versions:
- Built-in OkHttp CertificatePinner integration
- Automatic certificate rotation support
- Certificate transparency checks
- Stapled OCSP response validation
