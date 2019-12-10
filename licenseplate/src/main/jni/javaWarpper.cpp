#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include "include/Pipeline.h"

extern "C" {
JNIEXPORT jlong JNICALL
Java_com_shouzhong_licenseplate_PlateRecognition_initPlateRecognizer(
        JNIEnv *env, jclass cls,
        jstring detector_filename,
        jstring finemapping_prototxt, jstring finemapping_caffemodel,
        jstring segmentation_prototxt, jstring segmentation_caffemodel,
        jstring charRecognization_proto, jstring charRecognization_caffemodel,
        jstring segmentationfree_proto, jstring segmentationfree_caffemodel) {

    std::string detector_path = env->GetStringUTFChars(detector_filename, JNI_FALSE);
    std::string finemapping_prototxt_path = env->GetStringUTFChars(finemapping_prototxt, JNI_FALSE);
    std::string finemapping_caffemodel_path = env->GetStringUTFChars(finemapping_caffemodel, JNI_FALSE);
    std::string segmentation_prototxt_path = env->GetStringUTFChars(segmentation_prototxt, JNI_FALSE);
    std::string segmentation_caffemodel_path = env->GetStringUTFChars(segmentation_caffemodel, JNI_FALSE);
    std::string charRecognization_proto_path = env->GetStringUTFChars(charRecognization_proto, JNI_FALSE);
    std::string charRecognization_caffemodel_path = env->GetStringUTFChars(charRecognization_caffemodel, JNI_FALSE);
    std::string segmentationfree_proto_path = env->GetStringUTFChars(segmentationfree_proto, JNI_FALSE);
    std::string segmentationfree_caffemodel_path = env->GetStringUTFChars(segmentationfree_caffemodel, JNI_FALSE);

    pr::PipelinePR *PR = new pr::PipelinePR(detector_path,
                                            finemapping_prototxt_path, finemapping_caffemodel_path,
                                            segmentation_prototxt_path,
                                            segmentation_caffemodel_path,
                                            charRecognization_proto_path,
                                            charRecognization_caffemodel_path,
                                            segmentationfree_proto_path,
                                            segmentationfree_caffemodel_path);
    return (jlong) PR;
}

JNIEXPORT void JNICALL
Java_com_shouzhong_licenseplate_PlateRecognition_releasePlateRecognizer(JNIEnv *env, jclass cls, jlong object_re) {
    pr::PipelinePR *PR = (pr::PipelinePR *) object_re;
    delete PR;
}

JNIEXPORT jstring JNICALL
Java_com_shouzhong_licenseplate_PlateRecognition_recognize(JNIEnv *env, jclass cls, jbyteArray yuv, jint width, jint height, jlong object_pr) {
    pr::PipelinePR *PR = (pr::PipelinePR *) object_pr;
    jbyte *pBuf = env->GetByteArrayElements(yuv, 0);
    cv::Mat image(height + height / 2, width, CV_8UC1, (unsigned char *)pBuf);	//注意这里是height+height/2
    cv::Mat mBgr;
    cv::cvtColor(image, mBgr, CV_YUV2BGR_NV21);
    //1表示SEGMENTATION_BASED_METHOD在方法里有说明
    std::vector<pr::PlateInfo> list_res = PR->RunPiplineAsImage(mBgr, pr::SEGMENTATION_FREE_METHOD);
    std::string concat_results;
    for (auto one:list_res) {
        if (one.confidence > 0.9)
            concat_results += one.getPlateName() + ",";
    }
    concat_results = concat_results.substr(0, concat_results.size() - 1);
    env->ReleaseByteArrayElements(yuv, pBuf, 0);
    return env->NewStringUTF(concat_results.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_shouzhong_licenseplate_PlateRecognition_recognizeBmp(JNIEnv *env, jclass cls, jobject bitmap, jlong object_pr) {
    pr::PipelinePR *PR = (pr::PipelinePR *) object_pr;
    cv::Mat mBgr;
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    void * pixels;
    //锁定 bitmap
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    if(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888){ //bitmap
        cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
        cvtColor(tmp, mBgr, CV_RGBA2BGR);
        tmp.release();
    }else if(info.format == ANDROID_BITMAP_FORMAT_RGB_565){
        cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
        cvtColor(tmp, mBgr, CV_BGR5652BGR);
        tmp.release();
    }
    //解锁 bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
    //1表示SEGMENTATION_BASED_METHOD在方法里有说明
    std::vector<pr::PlateInfo> list_res = PR->RunPiplineAsImage(mBgr, pr::SEGMENTATION_FREE_METHOD);
    std::string concat_results;
    for (auto one:list_res) {
        if (one.confidence > 0.9)
            concat_results += one.getPlateName() + ",";
    }
    concat_results = concat_results.substr(0, concat_results.size() - 1);
    env->DeleteLocalRef(bitmap);
    return env->NewStringUTF(concat_results.c_str());
}
}