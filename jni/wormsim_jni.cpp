#include <jni.h>
#include <math.h>
#include "wormsim.h"

extern "C" 
{
    JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_init(JNIEnv * env, jobject obj);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_setSteeringSynapseWeights(JNIEnv * env, jobject obj, jdoubleArray weights);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_step(JNIEnv * env, jobject obj, jdouble salt_stimulus);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_stepback(JNIEnv * env, jobject obj);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getSteeringActivations(JNIEnv * env, jobject obj, jdoubleArray activations);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getDorsalMotorActivations(JNIEnv * env, jobject obj, jdoubleArray activations);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getVentralMotorActivations(JNIEnv * env, jobject obj, jdoubleArray activations);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getDorsalMuscleActivations(JNIEnv * env, jobject obj, jdoubleArray activations);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getVentralMuscleActivations(JNIEnv * env, jobject obj, jdoubleArray activations);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getBody(JNIEnv * env, jobject obj, jdoubleArray body);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getSegmentAngles(JNIEnv * env, jobject obj, jdoubleArray angles);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_terminate(JNIEnv * env, jobject obj);
	JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_overrideSMBmuscleAmplifiers(JNIEnv * env, jobject obj, jdouble dorsal, jdouble ventral);
};

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_init(JNIEnv * env, jobject obj)
{
    init();
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_setSteeringSynapseWeights(JNIEnv * env, jobject obj, jdoubleArray weights)
{
    jsize len = env->GetArrayLength(weights);
    jdouble *weightsf = env->GetDoubleArrayElements(weights, 0);
    for (int i = 0; i < len; i++)
    {
        set_steering_synapse_weight(i, weightsf[i]);
    }
    env->ReleaseDoubleArrayElements(weights, weightsf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_step(JNIEnv * env, jobject obj, jdouble salt_stimulus)
{
    step(salt_stimulus);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_stepback(JNIEnv * env, jobject obj)
{
    stepback();
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getSteeringActivations(JNIEnv * env, jobject obj, jdoubleArray activations)
{
    jsize len = env->GetArrayLength(activations);
    jdouble *activationsf = env->GetDoubleArrayElements(activations, 0);
    for (int i = 0; i < len; i++)
    {
        activationsf[i] = get_steering_activation(i);
    }
    env->ReleaseDoubleArrayElements(activations, activationsf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getDorsalMotorActivations(JNIEnv * env, jobject obj, jdoubleArray activations)
{
    jsize len = env->GetArrayLength(activations);
    jdouble *activationsf = env->GetDoubleArrayElements(activations, 0);
    for (int i = 0; i < len; i++)
    {
        activationsf[i] = get_dorsal_motor_activation(i);
    }
    env->ReleaseDoubleArrayElements(activations, activationsf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getVentralMotorActivations(JNIEnv * env, jobject obj, jdoubleArray activations)
{
    jsize len = env->GetArrayLength(activations);
    jdouble *activationsf = env->GetDoubleArrayElements(activations, 0);
    for (int i = 0; i < len; i++)
    {
        activationsf[i] = get_ventral_motor_activation(i);
    }
    env->ReleaseDoubleArrayElements(activations, activationsf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getDorsalMuscleActivations(JNIEnv * env, jobject obj, jdoubleArray activations)
{
    jsize len = env->GetArrayLength(activations);
    jdouble *activationsf = env->GetDoubleArrayElements(activations, 0);
    for (int i = 0; i < len; i++)
    {
        activationsf[i] = get_dorsal_muscle_activation(i);
    }
    env->ReleaseDoubleArrayElements(activations, activationsf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getVentralMuscleActivations(JNIEnv * env, jobject obj, jdoubleArray activations)
{
    jsize len = env->GetArrayLength(activations);
    jdouble *activationsf = env->GetDoubleArrayElements(activations, 0);
    for (int i = 0; i < len; i++)
    {
        activationsf[i] = get_ventral_muscle_activation(i);
    }
    env->ReleaseDoubleArrayElements(activations, activationsf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getBody(JNIEnv * env, jobject obj, jdoubleArray body)
{
    jdouble *bodyf = env->GetDoubleArrayElements(body, 0);
    for (int i = 0; i < NBAR; ++i)
    {
        bodyf[i * 3] = get_body_point(i * 3);
        bodyf[i * 3 + 1] = get_body_point(i * 3 + 1);
        bodyf[i * 3 + 2] = get_body_point(i * 3 + 2);
    }
    env->ReleaseDoubleArrayElements(body, bodyf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_getSegmentAngles(JNIEnv * env, jobject obj, jdoubleArray angles)
{
    jdouble *anglesf = env->GetDoubleArrayElements(angles, 0);
    for (int i = 0; i < 12; ++i)
    {
        anglesf[i] = get_segment_angle(i);
    }
    env->ReleaseDoubleArrayElements(angles, anglesf, 0);
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_terminate(JNIEnv * env, jobject obj)
{
    term();
}

JNIEXPORT void JNICALL Java_openworm_morphognosis_wormworx_Wormsim_overrideSMBmuscleAmplifiers(JNIEnv * env, jobject obj, jdouble dorsal, jdouble ventral)
{
	override_smb_muscle_amplifiers(dorsal, ventral);
}
