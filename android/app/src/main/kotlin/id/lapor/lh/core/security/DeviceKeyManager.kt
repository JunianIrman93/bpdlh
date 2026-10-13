package id.lapor.lh.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "laporlh_device_key"

@Singleton
class DeviceKeyManager
    @Inject
    constructor() {
        private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        fun rotateKeyPair(): String {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
            generator.initialize(
                KeyGenParameterSpec
                    .Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            val keyPair = generator.generateKeyPair()
            return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        }

        fun deleteKey() {
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }

        fun sign(message: String): String {
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey ?: throw KeyStoreException("device key missing")
            val signature =
                Signature.getInstance("SHA256withECDSA").run {
                    initSign(privateKey)
                    update(message.toByteArray(Charsets.UTF_8))
                    sign()
                }
            return Base64.encodeToString(signature, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }
