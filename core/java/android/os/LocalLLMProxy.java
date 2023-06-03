package android.os;
import java.lang.annotation.Native;
import android.annotation.SystemService;
import android.content.Context;

/**
 * /framework/base/core/java/android/os/LocalLLMProxy.java
 * This will be the proxy for the local llm.
 * Use this Singleton class to call the private functionality of LocalLLMProxy
 * @author mhaas.eth
 */
import android.annotation.NonNull;
import android.content.Context;
import android.util.Log;
@SystemService(Context.LOCALLLM_SERVICE)
public class LocalLLMProxy {
    private static final String TAG = "LocalLLMProxy";
    private static LocalLLMProxy myProxy;
    private final ILLMService mIMyService;
    /**
     * Use {@link #getmyProxy} to get the myProxy instance.
     */
    LocalLLMProxy(ILLMService myService) {
        if (myService == null) {
            throw new IllegalArgumentException("my service is null");
        }
        mIMyService = myService;
    }
    /** Get a handle to the MyService.
     * @return the MyService, or null.
     */
    @NonNull
    public static LocalLLMProxy getLLMProxy() {
        Object mThingLock = new Object();
        synchronized (mThingLock) {
            if (myProxy == null) {
                IBinder binder = android.os.ServiceManager.getService("localllm");
                if (binder != null) {
                    ILLMService managerService = ILLMService.Stub.asInterface(binder);
                    myProxy = new LocalLLMProxy(managerService);
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
    }

    @NonNull
    public boolean isRunning() {
        try {
            return mIMyService.isRunning();
        } catch(Exception e) {
            e.printStackTrace();
        }
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