#ifndef __PJSIP_JNI_H__
#define __PJSIP_JNI_H__


#include <jni.h>
#include "pj_loader.h"


PJ_BEGIN_DECL

typedef struct
{
    char serverip[64];

    char port[8];

}sip_server_t;

typedef struct
{
    char username[32];

    char passwd[32];

}sip_user_t;


typedef enum
{
    DIGIT,

    SIPURI,

}NUMBER_TYPE;

typedef struct
{
    NUMBER_TYPE number_type;

    char number[64];

    char sip_uri[64];

}number_info_t;

/*
 * jni native mehtods.
 */
JNIEXPORT jint JNICALL init(JNIEnv* env, jclass cls);

JNIEXPORT jint JNICALL login(JNIEnv* env, jclass cls, jobject server, jobject user);

JNIEXPORT jint JNICALL make_call(JNIEnv* env, jclass cls, jobject number_info);

JNIEXPORT jint JNICALL answer(JNIEnv* env, jclass cls, jint call_id);

JNIEXPORT jint JNICALL hangup(JNIEnv* env, jclass cls, jint call_id);

JNIEXPORT jint JNICALL destroy(JNIEnv* env, jclass cls);

JNIEXPORT jint JNICALL send_message(JNIEnv* env, jclass cls, jstring num, jstring text);

JNIEXPORT jint JNICALL get_group_member(JNIEnv* env, jclass cls, jstring num);

JNIEXPORT jint JNICALL cancel_ptt_right(JNIEnv* env, jclass cls, jstring num);

JNIEXPORT jint JNICALL apply_ptt_right(JNIEnv* env, jclass cls, jstring num);

JNIEXPORT jint JNICALL get_group_number(JNIEnv* env, jclass cls, jstring num);

JNIEXPORT jint JNICALL open_ptt(JNIEnv* env, jclass cls);

JNIEXPORT jint JNICALL ho_ind(JNIEnv* env, jclass cls, jstring ip);

PJ_END_DECL


#endif	/* __PJSIP_JNI_H__ */
