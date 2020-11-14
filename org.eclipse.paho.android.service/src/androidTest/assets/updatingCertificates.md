# How to Add certificates to the BKS keystore

## Getting the certificates

```openssl s_client -connect mqtt.eclipse.org:8883 -showcerts```

Notes: you need all certificates in chain (Copy each to a .crt file)

## Adding to keystore

```keytool -importcert -v -trustcacerts -file "mqtt.eclipse.org.crt" -alias mqtt.eclipse.org -keystore "test.bks" -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath "/home/james/Downloads/bcprov-jdk15on-154.jar" -storetype BKS -storepass mqtttest```
