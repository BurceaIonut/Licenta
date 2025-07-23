#include <jni.h>
#include <string>
#include <android/log.h>
#include <oqs/oqs.h>

#define LOG_TAG "JNI-OQS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define KEM_ALG "Kyber1024"
#define SIG_ALG "Dilithium2"

extern "C" {

JNIEXPORT jobjectArray JNICALL
Java_atm_licenta_crypto_1engine_Utils_PQ_generateKemKeypair(JNIEnv *env, jobject /* this */) {
    OQS_KEM *kem = OQS_KEM_new(KEM_ALG);
    if (!kem) {
        LOGI("Failed to initialize KEM");
        return nullptr;
    }

    uint8_t *public_key = new uint8_t[kem->length_public_key];
    uint8_t *secret_key = new uint8_t[kem->length_secret_key];

    if (OQS_KEM_keypair(kem, public_key, secret_key) != OQS_SUCCESS) {
        LOGI("KEM keypair generation failed");
        OQS_KEM_free(kem);
        return nullptr;
    }

    jobjectArray result = env->NewObjectArray(2, env->FindClass("[B"), nullptr);
    jbyteArray jpub = env->NewByteArray(kem->length_public_key);
    jbyteArray jsec = env->NewByteArray(kem->length_secret_key);

    env->SetByteArrayRegion(jpub, 0, kem->length_public_key, (jbyte *)public_key);
    env->SetByteArrayRegion(jsec, 0, kem->length_secret_key, (jbyte *)secret_key);

    env->SetObjectArrayElement(result, 0, jpub);
    env->SetObjectArrayElement(result, 1, jsec);

    delete[] public_key;
    delete[] secret_key;
    OQS_KEM_free(kem);

    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_atm_licenta_crypto_1engine_Utils_PQ_encapsulate(JNIEnv *env, jobject /* this */, jbyteArray jpubKey) {
OQS_KEM *kem = OQS_KEM_new(KEM_ALG);
if (!kem) return nullptr;

uint8_t *pubKey = (uint8_t *)env->GetByteArrayElements(jpubKey, nullptr);

uint8_t *ciphertext = new uint8_t[kem->length_ciphertext];
uint8_t *shared_secret = new uint8_t[kem->length_shared_secret];

OQS_KEM_encaps(kem, ciphertext, shared_secret, pubKey);

jbyteArray jct = env->NewByteArray(kem->length_ciphertext);
jbyteArray jss = env->NewByteArray(kem->length_shared_secret);

env->SetByteArrayRegion(jct, 0, kem->length_ciphertext, (jbyte *)ciphertext);
env->SetByteArrayRegion(jss, 0, kem->length_shared_secret, (jbyte *)shared_secret);

jobjectArray result = env->NewObjectArray(2, env->FindClass("[B"), nullptr);
env->SetObjectArrayElement(result, 0, jct);
env->SetObjectArrayElement(result, 1, jss);

delete[] ciphertext;
delete[] shared_secret;
OQS_KEM_free(kem);

return result;
}

JNIEXPORT jbyteArray JNICALL
Java_atm_licenta_crypto_1engine_Utils_PQ_decapsulate(JNIEnv *env, jobject /* this */,
                                                            jbyteArray jciphertext, jbyteArray jsecretKey) {
OQS_KEM *kem = OQS_KEM_new(KEM_ALG);
if (!kem) return nullptr;

uint8_t *ct = (uint8_t *)env->GetByteArrayElements(jciphertext, nullptr);
uint8_t *sk = (uint8_t *)env->GetByteArrayElements(jsecretKey, nullptr);

uint8_t *shared_secret = new uint8_t[kem->length_shared_secret];
OQS_KEM_decaps(kem, shared_secret, ct, sk);

jbyteArray jss = env->NewByteArray(kem->length_shared_secret);
env->SetByteArrayRegion(jss, 0, kem->length_shared_secret, (jbyte *)shared_secret);

delete[] shared_secret;
OQS_KEM_free(kem);
return jss;
}

JNIEXPORT jobjectArray JNICALL
Java_atm_licenta_crypto_1engine_Utils_PQ_generateSigKeypair(JNIEnv *env, jobject /* this */) {
    OQS_SIG *sig = OQS_SIG_new(SIG_ALG);
    if (!sig) return nullptr;

    uint8_t *public_key = new uint8_t[sig->length_public_key];
    uint8_t *secret_key = new uint8_t[sig->length_secret_key];

    OQS_SIG_keypair(sig, public_key, secret_key);

    jbyteArray jpub = env->NewByteArray(sig->length_public_key);
    jbyteArray jsec = env->NewByteArray(sig->length_secret_key);

    env->SetByteArrayRegion(jpub, 0, sig->length_public_key, (jbyte *)public_key);
    env->SetByteArrayRegion(jsec, 0, sig->length_secret_key, (jbyte *)secret_key);

    jobjectArray result = env->NewObjectArray(2, env->FindClass("[B"), nullptr);
    env->SetObjectArrayElement(result, 0, jpub);
    env->SetObjectArrayElement(result, 1, jsec);

    delete[] public_key;
    delete[] secret_key;
    OQS_SIG_free(sig);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_atm_licenta_crypto_1engine_Utils_PQ_sign(JNIEnv *env, jobject /* this */,
                                                     jbyteArray jmessage, jbyteArray jsecretKey) {
OQS_SIG *sig = OQS_SIG_new(SIG_ALG);
if (!sig) return nullptr;

jsize msg_len = env->GetArrayLength(jmessage);
uint8_t *msg = (uint8_t *)env->GetByteArrayElements(jmessage, nullptr);
uint8_t *sk = (uint8_t *)env->GetByteArrayElements(jsecretKey, nullptr);

size_t sig_len = sig->length_signature;
uint8_t *signature = new uint8_t[sig_len];

OQS_SIG_sign(sig, signature, &sig_len, msg, msg_len, sk);

jbyteArray jsig = env->NewByteArray(sig_len);
env->SetByteArrayRegion(jsig, 0, sig_len, (jbyte *)signature);

delete[] signature;
OQS_SIG_free(sig);
return jsig;
}

JNIEXPORT jboolean JNICALL
Java_atm_licenta_crypto_1engine_Utils_PQ_verify(JNIEnv *env, jobject /* this */,
                                                       jbyteArray jmessage, jbyteArray jsignature, jbyteArray jpublicKey) {
OQS_SIG *sig = OQS_SIG_new(SIG_ALG);
if (!sig) return JNI_FALSE;

jsize msg_len = env->GetArrayLength(jmessage);
jsize sig_len = env->GetArrayLength(jsignature);

uint8_t *msg = (uint8_t *)env->GetByteArrayElements(jmessage, nullptr);
uint8_t *signature = (uint8_t *)env->GetByteArrayElements(jsignature, nullptr);
uint8_t *pk = (uint8_t *)env->GetByteArrayElements(jpublicKey, nullptr);

OQS_STATUS status = OQS_SIG_verify(sig, msg, msg_len, signature, sig_len, pk);

OQS_SIG_free(sig);
return status == OQS_SUCCESS ? JNI_TRUE : JNI_FALSE;
}
}