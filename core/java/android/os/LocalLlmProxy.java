package android.os;
import java.lang.annotation.Native;
import android.annotation.SystemService;
import android.content.Context;

/**
 * /framework/base/core/java/android/os/LocalLlmProxy.java
 * This will be the proxy for the local llm.
 * Use this Singleton class to call the private functionality of LocalLlmProxy
 * @author mhaas.eth
 */
import android.annotation.NonNull;
import android.content.Context;
import android.util.Log;
@SystemService(Context.LOCALLLM_SERVICE)
public class LocalLlmProxy {
    private static final String TAG = "LocalLlmProxy";
    private static LocalLlmProxy myProxy;
    private final ILLMService mIMyService;
    /**
     * Use {@link #getmyProxy} to get the myProxy instance.
     */
    LocalLlmProxy(ILLMService myService) {
        if (myService == null) {
            throw new IllegalArgumentException("my service is null");
        }
        mIMyService = myService;
    }
    /** Get a handle to the MyService.
     * @return the MyService, or null.
     */
    @NonNull
    public static LocalLlmProxy getLlmProxy() {
        Object mThingLock = new Object();
        synchronized (mThingLock) {
            if (myProxy == null) {
                IBinder binder = android.os.ServiceManager.getService("localllm");
                if (binder != null) {
                    ILLMService managerService = ILLMService.Stub.asInterface(binder);
                    myProxy = new LocalLlmProxy(managerService);
                } else {
                    Log.e(TAG, "Service binder is null");
                }
            }
            return myProxy;
        }
        
    }

    @NonNull
    public String executePrompt(@NonNull String prompt) {
        try {
            return mIMyService.executePrompt(prompt);
        } catch(Exception e) {
            e.printStackTrace();
        }
	return "failed";
    }

    @NonNull
    public boolean isRunning() {
        try {
            return mIMyService.isRunning();
        } catch(Exception e) {
            e.printStackTrace();
        }
	return false;
    }

    @NonNull
    public void loadModel() {
        try {
            mIMyService.loadModel();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
   
}
