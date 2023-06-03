/*
* aidl file :
* frameworks/base/core/java/android/os/IWalletService.aidl
* This file contains definitions of functions which are
* exposed by service.
*/
package android.os;

/** @hide */
interface ILLMService {
    boolean isRunning();
    void loadModel();
    String executePrompt(String prompt);
}
