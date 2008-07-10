/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_openqa_selenium_ie_InternetExplorerDriver */

#ifndef _Included_org_openqa_selenium_ie_InternetExplorerDriver
#define _Included_org_openqa_selenium_ie_InternetExplorerDriver
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_close
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    doExecuteScript
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doExecuteScript
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    get
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_get
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    getCurrentUrl
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_getCurrentUrl
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    getTitle
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_getTitle
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    getVisible
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_getVisible
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    setVisible
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_setVisible
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementById
 * Signature: (Ljava/lang/String;)Lorg/openqa/selenium/WebElement;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementById
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementsById
 * Signature: (Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementsById
  (JNIEnv *, jobject, jstring, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementByName
 * Signature: (Ljava/lang/String;)Lorg/openqa/selenium/WebElement;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementByName
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementsByName
 * Signature: (Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementsByName
  (JNIEnv *, jobject, jstring, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementByClassName
 * Signature: (Ljava/lang/String;)Lorg/openqa/selenium/WebElement;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementByClassName
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementsByClassName
 * Signature: (Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementsByClassName
  (JNIEnv *, jobject, jstring, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementByXPath
 * Signature: (Ljava/lang/String;)Lorg/openqa/selenium/WebElement;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementByXPath
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementsByXPath
 * Signature: (Ljava/lang/String;Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementsByXPath
  (JNIEnv *, jobject, jstring, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementByLink
 * Signature: (Ljava/lang/String;)Lorg/openqa/selenium/WebElement;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementByLink
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    selectElementsByLink
 * Signature: (Ljava/lang/String;Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_selectElementsByLink
  (JNIEnv *, jobject, jstring, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    waitForLoadToComplete
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_waitForLoadToComplete
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    startComNatively
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_startComNatively
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    openIe
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_openIe
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    deleteStoredObject
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_deleteStoredObject
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    setFrameIndex
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_setFrameIndex
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    goBack
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_goBack
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    goForward
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_goForward
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    doAddCookie
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doAddCookie
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    doGetCookies
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doGetCookies
  (JNIEnv *, jobject);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    doSetMouseSpeed
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doSetMouseSpeed
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_openqa_selenium_ie_InternetExplorerDriver
 * Method:    doSwitchToActiveElement
 * Signature: ()Lorg/openqa/selenium/WebElement;
 */
JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doSwitchToActiveElement
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
