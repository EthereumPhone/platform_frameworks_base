/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import static android.app.admin.DevicePolicyResources.Strings.SystemUi.STATUS_BAR_WORK_ICON_ACCESSIBILITY;

import android.annotation.Nullable;
import com.android.internal.statusbar.StatusBarIcon;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import androidx.constraintlayout.widget.ConstraintLayout;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.core.content.ContextCompat;
import androidx.biometric.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import javax.crypto.Cipher;
import android.os.CancellationSignal;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.security.keystore.KeyGenParameterSpec;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import java.util.concurrent.atomic.AtomicInteger;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import androidx.lifecycle.Observer;
import android.widget.ProgressBar;

import com.android.systemui.res.R;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.qualifiers.DisplayId;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.qualifiers.UiBackground;
import com.android.systemui.display.domain.interactor.ConnectedDisplayInteractor;
import com.android.systemui.privacy.PrivacyItem;
import com.android.systemui.privacy.PrivacyItemController;
import com.android.systemui.privacy.PrivacyType;
import com.android.systemui.privacy.logging.PrivacyLogger;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.screenrecord.RecordingController;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastController.CastDevice;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DataSaverController.Listener;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback;
import com.android.systemui.statusbar.policy.SensorPrivacyController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.RingerModeTracker;
import com.android.systemui.util.kotlin.JavaAdapter;
import com.android.systemui.util.time.DateFormatUtil;
import android.os.StrictMode;
import java.io.BufferedReader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import android.widget.Toast;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;
import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.animation.ValueAnimator;

import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.math.BigDecimal;
import android.app.Activity;
import android.os.Looper;

import android.view.animation.AnimationUtils;

/**
 * This class contains all of the policy about which icons are installed in the status bar at boot
 * time. It goes through the normal API for icons, even though it probably strictly doesn't need to.
 */
public class PhoneStatusBarPolicy
        implements BluetoothController.Callback,
                CommandQueue.Callbacks,
                RotationLockControllerCallback,
                Listener,
                ZenModeController.Callback,
                DeviceProvisionedListener,
                KeyguardStateController.Callback,
                PrivacyItemController.Callback,
                LocationController.LocationChangeCallback,
                RecordingController.RecordingStateChangeCallback {
    private static final String TAG = "PhoneStatusBarPolicy";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    static final int LOCATION_STATUS_ICON_ID = PrivacyType.TYPE_LOCATION.getIconId();

    private final String mSlotCast;
    private final String mSlotHotspot;
    private final String mSlotBluetooth;
    private final String mSlotTty;
    private final String mSlotZen;
    private final String mSlotMute;
    private final String mSlotVibrate;
    private final String mSlotAlarmClock;
    private final String mSlotManagedProfile;
    private final String mSlotRotate;
    private final String mSlotHeadset;
    private final String mSlotDataSaver;
    private final String mSlotLocation;
    private final String mSlotMicrophone;
    private final String mSlotCamera;
    private final String mSlotSensorsOff;
    private final String mSlotScreenRecord;
    private final String mSlotConnectedDisplay;
    private final int mDisplayId;
    private final SharedPreferences mSharedPreferences;
    private final DateFormatUtil mDateFormatUtil;
    private final JavaAdapter mJavaAdapter;
    private final ConnectedDisplayInteractor mConnectedDisplayInteractor;
    private final TelecomManager mTelecomManager;

    private final Handler mHandler;
    private final CastController mCast;
    private final HotspotController mHotspot;
    private final NextAlarmController mNextAlarmController;
    private final AlarmManager mAlarmManager;
    private final UserInfoController mUserInfoController;
    private final UserManager mUserManager;
    private final UserTracker mUserTracker;
    private final DevicePolicyManager mDevicePolicyManager;
    private final StatusBarIconController mIconController;
    private final CommandQueue mCommandQueue;
    private final BroadcastDispatcher mBroadcastDispatcher;
    private final Resources mResources;
    private final RotationLockController mRotationLockController;
    private final DataSaverController mDataSaver;
    private final ZenModeController mZenController;
    private final DeviceProvisionedController mProvisionedController;
    private final KeyguardStateController mKeyguardStateController;
    private final LocationController mLocationController;
    private final PrivacyItemController mPrivacyItemController;
    private final Executor mMainExecutor;
    private final Executor mUiBgExecutor;
    private final SensorPrivacyController mSensorPrivacyController;
    private final RecordingController mRecordingController;
    private final RingerModeTracker mRingerModeTracker;
    private final PrivacyLogger mPrivacyLogger;
    private final Context mContext;

    private boolean mZenVisible;
    private boolean mVibrateVisible;
    private boolean mMuteVisible;
    private boolean mCurrentUserSetup;

    private boolean mProfileIconVisible = false;

    private BluetoothController mBluetooth;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    @Inject
    public PhoneStatusBarPolicy(StatusBarIconController iconController,
            CommandQueue commandQueue, BroadcastDispatcher broadcastDispatcher,
            @Main Executor mainExecutor, @UiBackground Executor uiBgExecutor, @Main Looper looper,
            @Main Resources resources, CastController castController,
            HotspotController hotspotController, BluetoothController bluetoothController,
            NextAlarmController nextAlarmController, UserInfoController userInfoController,
            RotationLockController rotationLockController, DataSaverController dataSaverController,
            ZenModeController zenModeController,
            DeviceProvisionedController deviceProvisionedController,
            KeyguardStateController keyguardStateController,
            LocationController locationController,
            SensorPrivacyController sensorPrivacyController, AlarmManager alarmManager,
            UserManager userManager, UserTracker userTracker,
            DevicePolicyManager devicePolicyManager, RecordingController recordingController,
            @Nullable TelecomManager telecomManager, @DisplayId int displayId,
            @Main SharedPreferences sharedPreferences, DateFormatUtil dateFormatUtil,
            RingerModeTracker ringerModeTracker,
            PrivacyItemController privacyItemController,
            PrivacyLogger privacyLogger,
            ConnectedDisplayInteractor connectedDisplayInteractor,
            JavaAdapter javaAdapter,
            Context context
    ) {
        mContext = context;
        mIconController = iconController;
        mCommandQueue = commandQueue;
        mConnectedDisplayInteractor = connectedDisplayInteractor;
        mBroadcastDispatcher = broadcastDispatcher;
        mHandler = new Handler(looper);
        mResources = resources;
        mCast = castController;
        mHotspot = hotspotController;
        mBluetooth = bluetoothController;
        mNextAlarmController = nextAlarmController;
        mAlarmManager = alarmManager;
        mUserInfoController = userInfoController;
        mUserManager = userManager;
        mUserTracker = userTracker;
        mDevicePolicyManager = devicePolicyManager;
        mRotationLockController = rotationLockController;
        mDataSaver = dataSaverController;
        mZenController = zenModeController;
        mProvisionedController = deviceProvisionedController;
        mKeyguardStateController = keyguardStateController;
        mLocationController = locationController;
        mPrivacyItemController = privacyItemController;
        mSensorPrivacyController = sensorPrivacyController;
        mRecordingController = recordingController;
        mMainExecutor = mainExecutor;
        mUiBgExecutor = uiBgExecutor;
        mTelecomManager = telecomManager;
        mRingerModeTracker = ringerModeTracker;
        mPrivacyLogger = privacyLogger;
        mJavaAdapter = javaAdapter;

        mSlotCast = resources.getString(com.android.internal.R.string.status_bar_cast);
        mSlotConnectedDisplay = resources.getString(
                com.android.internal.R.string.status_bar_connected_display);
        mSlotHotspot = resources.getString(com.android.internal.R.string.status_bar_hotspot);
        mSlotBluetooth = resources.getString(com.android.internal.R.string.status_bar_bluetooth);
        mSlotTty = resources.getString(com.android.internal.R.string.status_bar_tty);
        mSlotZen = resources.getString(com.android.internal.R.string.status_bar_zen);
        mSlotMute = resources.getString(com.android.internal.R.string.status_bar_mute);
        mSlotVibrate = resources.getString(com.android.internal.R.string.status_bar_volume);
        mSlotAlarmClock = resources.getString(com.android.internal.R.string.status_bar_alarm_clock);
        mSlotManagedProfile = resources.getString(
                com.android.internal.R.string.status_bar_managed_profile);
        mSlotRotate = resources.getString(com.android.internal.R.string.status_bar_rotate);
        mSlotHeadset = resources.getString(com.android.internal.R.string.status_bar_headset);
        mSlotDataSaver = resources.getString(com.android.internal.R.string.status_bar_data_saver);
        mSlotLocation = resources.getString(com.android.internal.R.string.status_bar_location);
        mSlotMicrophone = resources.getString(com.android.internal.R.string.status_bar_microphone);
        mSlotCamera = resources.getString(com.android.internal.R.string.status_bar_camera);
        mSlotSensorsOff = resources.getString(com.android.internal.R.string.status_bar_sensors_off);
        mSlotScreenRecord = resources.getString(
                com.android.internal.R.string.status_bar_screen_record);

        mDisplayId = displayId;
        mSharedPreferences = sharedPreferences;
        mDateFormatUtil = dateFormatUtil;
    }

    /** Initialize the object after construction. */
    public void init() {
        // listen for broadcasts
        IntentFilter filter = new IntentFilter();

        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction(Intent.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelecomManager.ACTION_CURRENT_TTY_MODE_CHANGED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_PROFILE_REMOVED);
        filter.addAction(Intent.ACTION_PROFILE_ACCESSIBLE);
        filter.addAction(Intent.ACTION_PROFILE_INACCESSIBLE);
        mBroadcastDispatcher.registerReceiverWithHandler(mIntentReceiver, filter, mHandler);
        Observer<Integer> observer = ringer -> mHandler.post(this::updateVolumeZen);

        mRingerModeTracker.getRingerMode().observeForever(observer);
        mRingerModeTracker.getRingerModeInternal().observeForever(observer);

        // listen for user / profile change.
        mUserTracker.addCallback(mUserSwitchListener, mMainExecutor);

        // TTY status
        updateTTY();

        // bluetooth status
        updateBluetooth();

        // Alarm clock
        mIconController.setIcon(mSlotAlarmClock, R.drawable.stat_sys_alarm, null);
        mIconController.setIconVisibility(mSlotAlarmClock, false);

        // zen
        mIconController.setIcon(mSlotZen, R.drawable.stat_sys_dnd, null);
        mIconController.setIconVisibility(mSlotZen, false);

        // vibrate
        mIconController.setIcon(mSlotVibrate, R.drawable.stat_sys_ringer_vibrate,
                mResources.getString(R.string.accessibility_ringer_vibrate));
        mIconController.setIconVisibility(mSlotVibrate, false);

        // ethosicon
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        mIconController.setIcon("ethosicon", R.drawable.zero_peers_lightnode, "ethOS Light-Client");
        //thread = new Thread(executionerMethod);
        //thread.start();

        // Add listener to Wallet requests
        BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Initialize the biometric prompt
                final Executor executor = ContextCompat.getMainExecutor(context);
                BiometricManager biometricManager = BiometricManager.from(context);
                final boolean hasAFingerprintRegistered  = biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
                try {
                    Bundle extras = intent.getExtras();
                    String method = "method";
                    String toAddr = "address";
                    String value = "value";
                    String data = "data";
                    String message = "message";
                    String requestID = "requestID";
                    String nonce = "1";
                    String gasPrice = "0";
                    String gasAmount = "0";
                    int chainId = 1;
                    String type = "";
                    int changeToChainId = 1;
                    try {
                        method = extras.getString("method");
                        requestID = extras.getString("requestID");
                        if (method.equals("sendTransaction")) {
                            toAddr = extras.getString("to");
                            value = extras.getString("value");
                            data = extras.getString("data");
                            nonce = extras.getString("nonce");
                            gasPrice = extras.getString("gasPrice");
                            gasAmount = extras.getString("gasAmount");
                            chainId = extras.getInt("chainId");
                        } else if (method.equals("signMessage")) {
                            message = extras.getString("message");
                            type = extras.getString("type");
                        } else if (method.equals("changeChainId")) {
                            changeToChainId = extras.getInt("chainId");
                        }
                    } catch (NullPointerException exception) {
                        exception.printStackTrace();
                    }

                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.RGBA_8888);

                    params.gravity = Gravity.BOTTOM;

                    params.setTitle("Load Average");
                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

                    // Create view
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    final Context contextF = context;

                    if (method.equals("getDecision")) {
                        ConstraintLayout mainView = (ConstraintLayout) inflater.inflate(R.layout.wallet_decision_view,
                                null);
                        Button systemWallet = (Button) mainView.findViewById(R.id.systemwallet);
                        Button otherWallet = (Button) mainView.findViewById(R.id.others);
                        final String requestIDf = requestID;
                        systemWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // System Wallet
                                try {
                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                    Object obj = context.getSystemService("privatewallet");
                                    Method method = cls.getDeclaredMethods()[9];
                                    method.invoke(obj, requestIDf, "system_wallet");
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }

                                removeMainView(wm, mainView);
                            }
                        });
                        otherWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Other Wallet
                                try {
                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                    Object obj = context.getSystemService("privatewallet");
                                    Method method = cls.getDeclaredMethods()[9];
                                    method.invoke(obj, requestIDf, "other_wallet");
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }

                                removeMainView(wm, mainView);
                            }
                        });
                        wm.addView(mainView, params);

                        // Set the initial y position below the screen
                        params.y = wm.getDefaultDisplay().getHeight();
                        wm.updateViewLayout(mainView, params);

                        // Calculate the final y position of the view (top of the screen)
                        int finalY = wm.getDefaultDisplay().getHeight() - mainView.getHeight();

                        // Create a translation animation to move the view from the initial to final y position
                        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(
                                mainView,
                                "translationY",
                                wm.getDefaultDisplay().getHeight(),
                                finalY
                        );
                        yAnimator.setDuration(900);


                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.play(yAnimator);
                        animatorSet.start();

                    } else if (method.equals("sendTransaction")) {
                        final ConstraintLayout mainView = (ConstraintLayout) inflater.inflate(R.layout.wallet_accept_transaction,
                                null);

                        final Button acceptWallet = (Button) mainView.findViewById(R.id.acceptbtn);
                        final Button declineWallet = (Button) mainView.findViewById(R.id.declinebtn);
                        
                        final TextView messagetitle = (TextView) mainView.findViewById(R.id.choose);
                        TextView toAddrView = (TextView) mainView.findViewById(R.id.message);
                        TextView ethAmount = (TextView) mainView.findViewById(R.id.ethamount);
                        TextView gasAmountView = (TextView) mainView.findViewById(R.id.textView9);
                        TextView totalAmount = (TextView) mainView.findViewById(R.id.ethamount_sign);
                        TextView chainIdTextView = (TextView) mainView.findViewById(R.id.chainIdTextView);
                        
                        // Change to actual chainId
                        chainId = getChainId();
                        if (chainId == 137) {
                            // Change eth to matic
                            TextView amountEthextension = (TextView) mainView.findViewById(R.id.amountethextension);
                            TextView gasEthextention = (TextView) mainView.findViewById(R.id.gasethextention);
                            TextView totalAmountExtension = (TextView) mainView.findViewById(R.id.textView13);
                            amountEthextension.setText("MATIC");
                            gasEthextention.setText("MATIC");
                            totalAmountExtension.setText("MATIC");
                        }
                        chainIdTextView.setText(chainIdToChainName(chainId));

                        toAddrView.setText(toAddr);

                        BigDecimal amountB = new BigDecimal(value);
                        amountB = amountB.divide(BigDecimal.TEN.pow(18));

                        BigDecimal gasAmountB = new BigDecimal(gasPrice).multiply(new BigDecimal(gasAmount));
                        gasAmountB = gasAmountB.divide(BigDecimal.TEN.pow(18));

                        ethAmount.setText(beautifyDouble(amountB.doubleValue()));
                        gasAmountView.setText(beautifyDouble(gasAmountB.doubleValue()));

                        totalAmount.setText(beautifyDouble(amountB.add(gasAmountB).doubleValue()));

                        final String requestIDf = requestID;
                        final String toF = toAddr;
                        final String valueF = new BigDecimal(value).toBigInteger().toString();
                        final String dataF = data;
                        final String nonceF = new BigDecimal(nonce).toBigInteger().toString();
                        final String gasPriceF = new BigDecimal(gasPrice).toBigInteger().toString();
                        final String gasAmountF = new BigDecimal(gasAmount).toBigInteger().toString();
                        final int chainIdF = chainId;
                        final Handler handler = new Handler(Looper.getMainLooper());

                        
                        acceptWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Accept send transaction
                                if (hasAFingerprintRegistered) {
                                    final int maxAttempts = 4;
                                    final AtomicInteger attempts = new AtomicInteger(0);
                                    final CancellationSignal cancellationSignal = new CancellationSignal();
                                    BiometricPrompt mBiometricPrompt = new BiometricPrompt.Builder(context)
                                        .setTitle("Authenticate")
                                        .setSubtitle("Authenticate to send transaction")
                                        .setNegativeButton("Cancel", context.getMainExecutor(), (dialogInterface, i) -> {
                                            removeMainView(wm, mainView);
                                        })
                                        .setAllowBackgroundAuthentication(true)
                                        .build();
                                    mBiometricPrompt.authenticateWallet(cancellationSignal, context.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                                        @Override
                                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                                            // Handle authentication error
                                            super.onAuthenticationError(errorCode, errString);
                                            System.out.println("Fingerprint error: " + errString);
                                            cancellationSignal.cancel();
                                            try {
                                                Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                Object obj = context.getSystemService("privatewallet");
                                                Method method = cls.getMethod("pushDecision", String.class, String.class);
                                                method.invoke(obj, requestIDf, "decline");
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                            try {
                                                removeMainView(wm, mainView);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                            // Handle successful authentication
                                            super.onAuthenticationSucceeded(result);
                                            try {
                                                Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                Object obj = context.getSystemService("privatewallet");
                                                Method method = cls.getDeclaredMethods()[10];
                                                method.invoke(obj, requestIDf, toF, valueF, dataF, nonceF, gasPriceF, gasAmountF);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                            try {
                                                removeMainView(wm, mainView);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onAuthenticationFailed() {
                                            // Handle authentication failure
                                            super.onAuthenticationFailed();
                                            if (attempts.incrementAndGet() == maxAttempts) {
                                                cancellationSignal.cancel();
                                                try {
                                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                    Object obj = context.getSystemService("privatewallet");
                                                    Method method = cls.getDeclaredMethods()[9];
                                                    method.invoke(obj, requestIDf, "decline");
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                                try {
                                                    removeMainView(wm, mainView);
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    try {
                                        Class cls = Class.forName("android.os.PrivateWalletProxy");
                                        Object obj = context.getSystemService("privatewallet");
                                        Method method = cls.getDeclaredMethods()[10];
                                        method.invoke(obj, requestIDf, toF, valueF, dataF, nonceF, gasPriceF, gasAmountF);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    removeMainView(wm, mainView);
                                }
                            }
                        });

                        declineWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Decline send transaction
                                try {
                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                    Object obj = context.getSystemService("privatewallet");
                                    Method method = cls.getDeclaredMethods()[9];
                                    method.invoke(obj, requestIDf, "decline");
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                removeMainView(wm, mainView);
                            }
                        });

                        wm.addView(mainView, params);

                        int screenHeight = wm.getDefaultDisplay().getHeight();
                        mainView.setTranslationY(screenHeight);
                        mainView.setVisibility(View.VISIBLE);
                        mainView.animate().translationY(0).setDuration(900).start();
                    } else if (method.equals("signMessage")) {
                        ConstraintLayout mainView = (ConstraintLayout) inflater.inflate(R.layout.wallet_sign_transaction,
                                null);

                        TextView messageView = (TextView) mainView.findViewById(R.id.message);
                        final TextView messagetitle = (TextView) mainView.findViewById(R.id.choose);

                        if (type.equals("personal_sign_hex")) {
                            if (message.startsWith("0x")) {
                                String strippedMess = message.substring(2);
                                messageView.setText(hexToString(strippedMess));
                            } else {
                                messageView.setText(hexToString(message));
                            }
                        } else  {
                            messageView.setText(message);
                        }


                        final Button acceptWallet = (Button) mainView.findViewById(R.id.acceptbtn);
                        final Button declineWallet = (Button) mainView.findViewById(R.id.declinebtn);

                        final String requestIDf = requestID;
                        final String messageF = message;
                        final String typeF = type;

                        acceptWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Accept sign message
                                if (hasAFingerprintRegistered) {
                                    final int maxAttempts = 4;
                                    final AtomicInteger attempts = new AtomicInteger(0);
                                    final CancellationSignal cancellationSignal = new CancellationSignal();
                                    BiometricPrompt mBiometricPrompt = new BiometricPrompt.Builder(context)
                                        .setTitle("Authenticate")
                                        .setSubtitle("Authenticate to sign Message")
                                        .setNegativeButton("Cancel", context.getMainExecutor(), (dialogInterface, i) -> {
                                            removeMainView(wm, mainView);
                                        })
                                        .setAllowBackgroundAuthentication(true)
                                        .build();
                                    mBiometricPrompt.authenticateWallet(cancellationSignal, context.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                                        @Override
                                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                                            super.onAuthenticationError(errorCode, errString);
                                            System.out.println("Fingerprint error: " + errString);
                                            cancellationSignal.cancel();
                                            try {
                                                Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                Object obj = context.getSystemService("privatewallet");
                                                Method method = cls.getDeclaredMethods()[9];
                                                method.invoke(obj, requestIDf, "decline");
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                            try {
                                                removeMainView(wm, mainView);
                                            }catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                            super.onAuthenticationSucceeded(result);
                                            try {
                                                Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                Object obj = context.getSystemService("privatewallet");
                                                Method method = cls.getDeclaredMethods()[11];
                                                method.invoke(obj, requestIDf, messageF, typeF);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                            try {
                                                removeMainView(wm, mainView);
                                            }catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onAuthenticationFailed() {
                                            super.onAuthenticationFailed();
                                            if (attempts.incrementAndGet() == maxAttempts) {
                                                cancellationSignal.cancel();
                                                try {
                                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                    Object obj = context.getSystemService("privatewallet");
                                                    Method method = cls.getDeclaredMethods()[9];
                                                    method.invoke(obj, requestIDf, "decline");
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                                try {
                                                    removeMainView(wm, mainView);
                                                }catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    try {
                                        Class cls = Class.forName("android.os.PrivateWalletProxy");
                                        Object obj = context.getSystemService("privatewallet");
                                        Method method = cls.getDeclaredMethods()[11];
                                        method.invoke(obj, requestIDf, messageF, typeF);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    try {
                                        removeMainView(wm, mainView);
                                    }catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }
                            }
                        });

                        declineWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Decline sign transaction
                                try {
                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                    Object obj = context.getSystemService("privatewallet");
                                    Method method = cls.getDeclaredMethods()[9];
                                    method.invoke(obj, requestIDf, "decline");
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                removeMainView(wm, mainView);
                            }
                        });

                        wm.addView(mainView, params);

                        int screenHeight = wm.getDefaultDisplay().getHeight();
                        mainView.setTranslationY(screenHeight);
                        mainView.setVisibility(View.VISIBLE);
                        mainView.animate().translationY(0).setDuration(900).start();

                    } else if (method.equals("getAddress")) {
                        final String requestIDf = requestID;
                        try {
                            Class cls = Class.forName("android.os.PrivateWalletProxy");
                            Object obj = context.getSystemService("privatewallet");
                            Method getAddressMethod = cls.getDeclaredMethods()[5];
                            getAddressMethod.invoke(obj, requestIDf);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }

                    } else if (method.equals("getChainId")) {
                        final String requestIDf = requestID;
                        try {
                            Class cls = Class.forName("android.os.PrivateWalletProxy");
                            Object obj = context.getSystemService("privatewallet");
                            Method getChainIdMethod = cls.getDeclaredMethods()[6];
                            getChainIdMethod.invoke(obj, requestIDf);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    } else if (method.equals("changeChainId")) {
                        ConstraintLayout mainView = (ConstraintLayout) inflater.inflate(R.layout.wallet_change_network,
                                null);

                        final Button acceptWallet = (Button) mainView.findViewById(R.id.acceptbtn);
                        final Button declineWallet = (Button) mainView.findViewById(R.id.declinebtn);
                        
                        final TextView change_network_text = (TextView) mainView.findViewById(R.id.choosemethod);
                        final TextView messagetitle = (TextView) mainView.findViewById(R.id.choose);

                        final String requestIDf = requestID;
                        String networkName = "Ethereum Mainnet";
                        final int changeToChainIdF = changeToChainId;
                        if (changeToChainId == 1) {
                            networkName = "Ethereum Mainnet";
                        } else if (changeToChainId == 10) {
                            networkName = "Optimism";
                        } else if (changeToChainId == 42161) {
                            networkName = "Arbitrum";
                        } else if (changeToChainId == 11155111) {
                            networkName = "Sepolia Testnet";
                        } else if (chainId == 84531){
                            networkName = "Base Testnet";
                        } else {
                            networkName = "Chain ID: " + changeToChainId + "";
                        }
                        change_network_text.setText("Do you want to change network to "+ networkName +"?");
                        
                        acceptWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Accept send transaction
                                if (hasAFingerprintRegistered) {
                                    final int maxAttempts = 4;
                                    final AtomicInteger attempts = new AtomicInteger(0);
                                    final CancellationSignal cancellationSignal = new CancellationSignal();
                                    BiometricPrompt mBiometricPrompt = new BiometricPrompt.Builder(context)
                                        .setTitle("Authenticate")
                                        .setSubtitle("Authenticate to change network")
                                        .setNegativeButton("Cancel", context.getMainExecutor(), (dialogInterface, i) -> {
                                            removeMainView(wm, mainView);
                                        })
                                        .setAllowBackgroundAuthentication(true)
                                        .build();
                                    mBiometricPrompt.authenticateWallet(cancellationSignal, context.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                                        @Override
                                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                                            // Handle authentication error
                                            super.onAuthenticationError(errorCode, errString);
                                            System.out.println("Fingerprint error: " + errString);
                                            cancellationSignal.cancel();
                                            try {
                                                Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                Object obj = context.getSystemService("privatewallet");
                                                Method method = cls.getDeclaredMethods()[9];
                                                method.invoke(obj, requestIDf, "decline");
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                            try {
                                                removeMainView(wm, mainView);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                            // Handle successful authentication
                                            super.onAuthenticationSucceeded(result);
                                            try {
                                                Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                Object obj = context.getSystemService("privatewallet");
                                                Method method = cls.getDeclaredMethods()[1];
                                                method.invoke(obj, changeToChainIdF);
                                                Method pushDecision = cls.getDeclaredMethods()[9];
                                                pushDecision.invoke(obj, requestIDf, "done");
                                                context.sendBroadcast(new Intent("changeChain"));
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                            try {
                                                removeMainView(wm, mainView);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onAuthenticationFailed() {
                                            // Handle authentication failure
                                            super.onAuthenticationFailed();
                                            if (attempts.incrementAndGet() == maxAttempts) {
                                                cancellationSignal.cancel();
                                                try {
                                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                                    Object obj = context.getSystemService("privatewallet");
                                                    Method method = cls.getDeclaredMethods()[9];
                                                    method.invoke(obj, requestIDf, "decline");
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                                try {
                                                    removeMainView(wm, mainView);
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    try {
                                        Class cls = Class.forName("android.os.PrivateWalletProxy");
                                        Object obj = context.getSystemService("privatewallet");
                                        Method method = cls.getDeclaredMethods()[1];
                                        method.invoke(obj, changeToChainIdF);
                                        Method pushDecision = cls.getDeclaredMethods()[9];
                                        pushDecision.invoke(obj, requestIDf, "done");
                                        context.sendBroadcast(new Intent("changeChain"));
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    try {
                                        removeMainView(wm, mainView);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }
                            }
                        });

                        declineWallet.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Decline send transaction
                                try {
                                    Class cls = Class.forName("android.os.PrivateWalletProxy");
                                    Object obj = context.getSystemService("privatewallet");
                                    Method method = cls.getDeclaredMethods()[9];
                                    method.invoke(obj, requestIDf, "decline");
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                removeMainView(wm, mainView);
                            }
                        });

                        wm.addView(mainView, params);

                        int screenHeight = wm.getDefaultDisplay().getHeight();
                        mainView.setTranslationY(screenHeight);
                        mainView.setVisibility(View.VISIBLE);
                        mainView.animate().translationY(0).setDuration(900).start();
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to invoke system wallet");
                }

            }

        };

        IntentFilter requestFilter = new IntentFilter("requestToSystemUI");
        mBroadcastDispatcher.registerReceiverWithHandler(mBroadcastReceiver, requestFilter, new Handler());

        
        BroadcastReceiver mNodeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    System.out.println("Statusbar: Running background thread. Got Intent");;
                    // Check if node is on right now
                    Class cls = Class.forName("android.os.GethProxy");
        
                    Method isRunningMethod = cls.getDeclaredMethod("isRunning");
        
                    Object gethservice = context.getSystemService("geth");
        
                    boolean isRunning = (boolean) isRunningMethod.invoke(gethservice);
                    
                    if (isRunning) {
                        mIconController.setIcon("ethosicon", R.drawable.three_peers_lightnode, "ethOS Light-Client");
                    } else {
                        mIconController.setIcon("ethosicon", R.drawable.zero_peers_lightnode, "ethOS Light-Client");
                    }
                    
                } catch (Exception e) {
                    System.out.println("Deskclock: Error running background thread. Got Intent");
                    e.printStackTrace();
                }
            }
        };

        IntentFilter requestFilter2 = new IntentFilter("NODE_UPDATE");
        mBroadcastDispatcher.registerReceiverWithHandler(mNodeReceiver, requestFilter2, new Handler());


        try {
            int chainId = getChainId();
            switch(chainId) {
                case 1:
                    mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_ethereum, "ethOS System Wallet chain-id: " + chainId);
                    break;
                case 10:
                    mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_optimism, "ethOS System Wallet chain-id: " + chainId);
                    break;
                case 5:
                    mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_goerli, "ethOS System Wallet chain-id: " + chainId);
                    break;
                case 42161:
                    mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_arbitrum, "ethOS System Wallet chain-id: " + chainId);
                    break;
                default:
                    mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_unknown, "ethOS System Wallet chain-id: " + chainId);
                    break;
            }
            mIconController.setIconVisibility("ethoschainicon", true);
        } catch (Exception e) {
            System.out.println("Error during chain-id icon creation: " + e.getMessage());
            e.printStackTrace();
        }

        BroadcastReceiver mChainChangeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                
                try {
                    int chainId = getChainId();
                    switch(chainId) {
                        case 1:
                            mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_ethereum, "ethOS System Wallet chain-id: " + chainId);
                            break;
                        case 10:
                            mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_optimism, "ethOS System Wallet chain-id: " + chainId);
                            break;
                        case 5:
                            mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_goerli, "ethOS System Wallet chain-id: " + chainId);
                            break;
                        case 42161:
                            mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_arbitrum, "ethOS System Wallet chain-id: " + chainId);
                            break;
                        default:
                            mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_unknown, "ethOS System Wallet chain-id: " + chainId);
                            break;
                    }
                    mIconController.setIconVisibility("ethoschainicon", true);
                } catch (Exception e) {
                    System.out.println("Error during chain-id icon creation: " + e.getMessage());
                    e.printStackTrace();
                }
                
            }
        };

        mIconController.setIcon("ethoschainicon", R.drawable.ethoschainicon_ethereum, "ethOS System Wallet chain");
        mIconController.setIconVisibility("ethoschainicon", true);

        // mChainChangeReceiver
        IntentFilter changeChainFilter = new IntentFilter("changeChain");
        mBroadcastDispatcher.registerReceiverWithHandler(mChainChangeReceiver, changeChainFilter, new Handler());


        // mute
        mIconController.setIcon(mSlotMute, R.drawable.stat_sys_ringer_silent,
                mResources.getString(R.string.accessibility_ringer_silent));
        mIconController.setIconVisibility(mSlotMute, false);
        updateVolumeZen();

        // cast
        mIconController.setIcon(mSlotCast, R.drawable.stat_sys_cast, null);
        mIconController.setIconVisibility(mSlotCast, false);

        // connected display
        mIconController.setIcon(mSlotConnectedDisplay, R.drawable.stat_sys_connected_display,
                mResources.getString(R.string.connected_display_icon_desc));
        mIconController.setIconVisibility(mSlotConnectedDisplay, false);

        // hotspot
        mIconController.setIcon(mSlotHotspot, R.drawable.stat_sys_hotspot,
                mResources.getString(R.string.accessibility_status_bar_hotspot));
        mIconController.setIconVisibility(mSlotHotspot, mHotspot.isHotspotEnabled());

        // profile
        updateProfileIcon();

        // data saver
        mIconController.setIcon(mSlotDataSaver, R.drawable.stat_sys_data_saver,
                mResources.getString(R.string.accessibility_data_saver_on));
        mIconController.setIconVisibility(mSlotDataSaver, false);


        // privacy items
        String microphoneString = mResources.getString(PrivacyType.TYPE_MICROPHONE.getNameId());
        String microphoneDesc = mResources.getString(
                R.string.ongoing_privacy_chip_content_multiple_apps, microphoneString);
        mIconController.setIcon(mSlotMicrophone, PrivacyType.TYPE_MICROPHONE.getIconId(),
                microphoneDesc);
        mIconController.setIconVisibility(mSlotMicrophone, false);

        String cameraString = mResources.getString(PrivacyType.TYPE_CAMERA.getNameId());
        String cameraDesc = mResources.getString(
                R.string.ongoing_privacy_chip_content_multiple_apps, cameraString);
        mIconController.setIcon(mSlotCamera, PrivacyType.TYPE_CAMERA.getIconId(),
                cameraDesc);
        mIconController.setIconVisibility(mSlotCamera, false);

        mIconController.setIcon(mSlotLocation, LOCATION_STATUS_ICON_ID,
                mResources.getString(R.string.accessibility_location_active));
        mIconController.setIconVisibility(mSlotLocation, false);

        // sensors off
        mIconController.setIcon(mSlotSensorsOff, R.drawable.stat_sys_sensors_off,
                mResources.getString(R.string.accessibility_sensors_off_active));
        mIconController.setIconVisibility(mSlotSensorsOff,
                mSensorPrivacyController.isSensorPrivacyEnabled());

        // screen record
        mIconController.setIcon(mSlotScreenRecord, R.drawable.stat_sys_screen_record, null);
        mIconController.setIconVisibility(mSlotScreenRecord, false);

        mRotationLockController.addCallback(this);
        mBluetooth.addCallback(this);
        mProvisionedController.addCallback(this);
        mCurrentUserSetup = mProvisionedController.isCurrentUserSetup();
        mZenController.addCallback(this);
        mCast.addCallback(mCastCallback);
        mHotspot.addCallback(mHotspotCallback);
        mNextAlarmController.addCallback(mNextAlarmCallback);
        mDataSaver.addCallback(this);
        mKeyguardStateController.addCallback(this);
        mPrivacyItemController.addCallback(this);
        mSensorPrivacyController.addCallback(mSensorPrivacyListener);
        mLocationController.addCallback(this);
        mRecordingController.addCallback(this);
        mJavaAdapter.alwaysCollectFlow(mConnectedDisplayInteractor.getConnectedDisplayState(),
                this::onConnectedDisplayAvailabilityChanged);

        mCommandQueue.addCallback(this);
    }

    private String getManagedProfileAccessibilityString() {
        return mDevicePolicyManager.getResources().getString(
                STATUS_BAR_WORK_ICON_ACCESSIBILITY,
                () -> mResources.getString(R.string.accessibility_managed_profile));
    }

    private void removeMainView(WindowManager wm, View mainView) {
        int screenHeight = wm.getDefaultDisplay().getHeight();
        mainView.animate()
                .translationY(screenHeight+1)
                .setDuration(900)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mainView.setVisibility(View.GONE);
                            wm.removeView(mainView);
                        } catch (Exception e) {
                            // Was already removed
                        }
                    }
                })
                .start();
    }

    @Override
    public void onZenChanged(int zen) {
        updateVolumeZen();
    }

    public String chainIdToChainName(int chainId) {
        switch (chainId) {
            case 1:
                return "Ethereum";
            case 137:
                return "Polygon";
            case 42161:
                return "Arbitrum";
            case 10:
                return "Optimism";
            case 5:
                return "Goerli";
            case 84531:
                return "Base Testnet";
            case 56:
                return "Binance Smart Chain";
            default:
                return String.valueOf(chainId);
        }
    }

    @Override
    public void onConsolidatedPolicyChanged(NotificationManager.Policy policy) {
        updateVolumeZen();
    }

    public int getChainId() {
        try {
            System.out.println("Getting chain id");
            Object privateWalletManager = mContext.getSystemService("privatewallet");
            Class privateWalletProxy = Class.forName("android.os.PrivateWalletProxy");
            Method directGetChainId = privateWalletProxy.getDeclaredMethod("directGetChainId");
            Object out = directGetChainId.invoke(privateWalletManager);
            return (int) out;
        }catch (Exception e) {
            System.out.println("Failed to get chain id");
            e.printStackTrace();
            return 0;
        }
    }

    public String beautifyDouble(double input) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#.######", symbols);
    
        double threshold = 0.000001;
        if (input > 0 && input < threshold) {
            return decimalFormat.format(threshold);
        }
    
        return decimalFormat.format(input);
    }
    

    public void updateLightClientLogo() {
        String out = executeCommand(
                "curl -X POST -H \"Content-Type: application/json\" --data '{\"jsonrpc\":\"2.0\",\"method\":\"net_peerCount\",\"params\":[],\"id\":67}' http://127.0.0.1:8545");
        try {
            System.out.println("STATUSBAR: (out)->" + out + "<-");
            JSONObject jsonObject = new JSONObject(out);
            if (jsonObject.getJSONObject("error").getString("message").contains("found")) {
                mIconController.setIcon("ethosicon", R.drawable.three_peers_lightnode, "ethOS Light-Client");
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mIconController.setIcon("ethosicon", R.drawable.zero_peers_lightnode, "ethOS Light-Client");
            System.out.println("Failed to set ethOS statusbar");
        }
    }

    public static String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        // 49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            // grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            // convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            // convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }
        return sb.toString();
    }

    public static String executeCommand(String command) {
        /*
         * StrictMode.ThreadPolicy policy = new
         * StrictMode.ThreadPolicy.Builder().permitAll().build();
         * StrictMode.setThreadPolicy(policy);
         * 
         * URL url = new URL("http://127.0.0.1:8545");
         * HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
         * httpConn.setRequestMethod("POST");
         * 
         * httpConn.setRequestProperty("Content-Type", "application/json");
         * 
         * httpConn.setDoOutput(true);
         * OutputStreamWriter writer = new
         * OutputStreamWriter(httpConn.getOutputStream());
         * writer.write(
         * "{\"jsonrpc\":\"2.0\",\"method\":\"net_peerCount\",\"params\":[],\"id\":67}")
         * ;
         * writer.flush();
         * writer.close();
         * httpConn.getOutputStream().close();
         * 
         * InputStream responseStream = httpConn.getResponseCode() / 100 == 2
         * ? httpConn.getInputStream()
         * : httpConn.getErrorStream();
         * Scanner s = new Scanner(responseStream).useDelimiter("\\A");
         * String response = s.hasNext() ? s.next() : "";
         * return response;
         */
        StringBuilder output = new StringBuilder();
        try {
            Process proc = Runtime.getRuntime().exec(new String[] { "sh", "-c", command });
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    private void updateAlarm() {
        final AlarmClockInfo alarm = mAlarmManager.getNextAlarmClock(mUserTracker.getUserId());
        final boolean hasAlarm = alarm != null && alarm.getTriggerTime() > 0;
        int zen = mZenController.getZen();
        final boolean zenNone = zen == Global.ZEN_MODE_NO_INTERRUPTIONS;
        mIconController.setIcon(mSlotAlarmClock, zenNone ? R.drawable.stat_sys_alarm_dim
                : R.drawable.stat_sys_alarm, buildAlarmContentDescription());
        mIconController.setIconVisibility(mSlotAlarmClock, mCurrentUserSetup && hasAlarm);
    }

    private String buildAlarmContentDescription() {
        if (mNextAlarm == null) {
            return mResources.getString(R.string.status_bar_alarm);
        }

        String skeleton = mDateFormatUtil.is24HourFormat() ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        String dateString = DateFormat.format(pattern, mNextAlarm.getTriggerTime()).toString();

        return mResources.getString(R.string.accessibility_quick_settings_alarm, dateString);
    }

    private final void updateVolumeZen() {
        boolean zenVisible = false;
        int zenIconId = 0;
        String zenDescription = null;

        boolean vibrateVisible = false;
        boolean muteVisible = false;
        int zen = mZenController.getZen();

        if (DndTile.isVisible(mSharedPreferences) || DndTile.isCombinedIcon(mSharedPreferences)) {
            zenVisible = zen != Global.ZEN_MODE_OFF;
            zenIconId = R.drawable.stat_sys_dnd;
            zenDescription = mResources.getString(R.string.quick_settings_dnd_label);
        } else if (zen == Global.ZEN_MODE_NO_INTERRUPTIONS) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_dnd;
            zenDescription = mResources.getString(R.string.interruption_level_none);
        } else if (zen == Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_dnd;
            zenDescription = mResources.getString(R.string.interruption_level_priority);
        }

        if (!ZenModeConfig.isZenOverridingRinger(zen, mZenController.getConsolidatedPolicy())) {
            final Integer ringerModeInternal =
                    mRingerModeTracker.getRingerModeInternal().getValue();
            if (ringerModeInternal != null) {
                if (ringerModeInternal == AudioManager.RINGER_MODE_VIBRATE) {
                    vibrateVisible = true;
                } else if (ringerModeInternal == AudioManager.RINGER_MODE_SILENT) {
                    muteVisible = true;
                }
            }
        }

        if (zenVisible) {
            mIconController.setIcon(mSlotZen, zenIconId, zenDescription);
        }
        if (zenVisible != mZenVisible) {
            mIconController.setIconVisibility(mSlotZen, zenVisible);
            mZenVisible = zenVisible;
        }

        if (vibrateVisible != mVibrateVisible) {
            mIconController.setIconVisibility(mSlotVibrate, vibrateVisible);
            mVibrateVisible = vibrateVisible;
        }

        if (muteVisible != mMuteVisible) {
            mIconController.setIconVisibility(mSlotMute, muteVisible);
            mMuteVisible = muteVisible;
        }

        updateAlarm();
    }

    @Override
    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    @Override
    public void onBluetoothStateChange(boolean enabled) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        int iconId = R.drawable.stat_sys_data_bluetooth_connected;
        String contentDescription =
                mResources.getString(R.string.accessibility_quick_settings_bluetooth_on);
        boolean bluetoothVisible = false;
        if (mBluetooth != null) {
            if (mBluetooth.isBluetoothConnected()
                    && (mBluetooth.isBluetoothAudioActive()
                    || !mBluetooth.isBluetoothAudioProfileOnly())) {
                contentDescription = mResources.getString(
                        R.string.accessibility_bluetooth_connected);
                bluetoothVisible = mBluetooth.isBluetoothEnabled();
            }
        }

        mIconController.setIcon(mSlotBluetooth, iconId, contentDescription);
        mIconController.setIconVisibility(mSlotBluetooth, bluetoothVisible);
    }

    private final void updateTTY() {
        if (mTelecomManager == null) {
            updateTTY(TelecomManager.TTY_MODE_OFF);
        } else {
            updateTTY(mTelecomManager.getCurrentTtyMode());
        }
    }

    private final void updateTTY(int currentTtyMode) {
        boolean enabled = currentTtyMode != TelecomManager.TTY_MODE_OFF;

        if (DEBUG) Log.v(TAG, "updateTTY: enabled: " + enabled);

        if (enabled) {
            // TTY is on
            if (DEBUG) Log.v(TAG, "updateTTY: set TTY on");
            mIconController.setIcon(mSlotTty, R.drawable.stat_sys_tty_mode,
                    mResources.getString(R.string.accessibility_tty_enabled));
            mIconController.setIconVisibility(mSlotTty, true);
        } else {
            // TTY is off
            if (DEBUG) Log.v(TAG, "updateTTY: set TTY off");
            mIconController.setIconVisibility(mSlotTty, false);
        }
    }

    private void updateCast() {
        boolean isCasting = false;
        for (CastDevice device : mCast.getCastDevices()) {
            if (device.state == CastDevice.STATE_CONNECTING
                    || device.state == CastDevice.STATE_CONNECTED) {
                isCasting = true;
                break;
            }
        }
        if (DEBUG) Log.v(TAG, "updateCast: isCasting: " + isCasting);
        mHandler.removeCallbacks(mRemoveCastIconRunnable);
        if (isCasting && !mRecordingController.isRecording()) { // screen record has its own icon
            mIconController.setIcon(mSlotCast, R.drawable.stat_sys_cast,
                    mResources.getString(R.string.accessibility_casting));
            mIconController.setIconVisibility(mSlotCast, true);
        } else {
            // don't turn off the screen-record icon for a few seconds, just to make sure the user
            // has seen it
            if (DEBUG) Log.v(TAG, "updateCast: hiding icon in 3 sec...");
            mHandler.postDelayed(mRemoveCastIconRunnable, 3000);
        }
    }

    private void updateProfileIcon() {
        // getLastResumedActivityUserId needs to acquire the AM lock, which may be contended in
        // some cases. Since it doesn't really matter here whether it's updated in this frame
        // or in the next one, we call this method from our UI offload thread.
        mUiBgExecutor.execute(() -> {
            try {
                final int userId = ActivityTaskManager.getService().getLastResumedActivityUserId();
                final int iconResId = mUserManager.getUserStatusBarIconResId(userId);
                // TODO(b/170249807, b/230779281): Handle non-managed-profile String
                String accessibilityString = getManagedProfileAccessibilityString();
                mMainExecutor.execute(() -> {
                    final boolean showIcon;
                    if (iconResId != Resources.ID_NULL && (!mKeyguardStateController.isShowing()
                            || mKeyguardStateController.isOccluded())) {
                        showIcon = true;
                        mIconController.setIcon(mSlotManagedProfile,
                                iconResId,
                                accessibilityString);
                    } else {
                        showIcon = false;
                    }
                    if (mProfileIconVisible != showIcon) {
                        mIconController.setIconVisibility(mSlotManagedProfile, showIcon);
                        mProfileIconVisible = showIcon;
                    }
                });
            } catch (RemoteException e) {
                Log.w(TAG, "updateProfileIcon: ", e);
            }
        });
    }

    private final UserTracker.Callback mUserSwitchListener =
            new UserTracker.Callback() {
                @Override
                public void onUserChanging(int newUser, Context userContext) {
                    mHandler.post(() -> mUserInfoController.reloadUserInfo());
                }

                @Override
                public void onUserChanged(int newUser, Context userContext) {
                    mHandler.post(() -> {
                        updateAlarm();
                        updateProfileIcon();
                        onUserSetupChanged();
                    });
                }
            };

    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        @Override
        public void onHotspotChanged(boolean enabled, int numDevices) {
            mIconController.setIconVisibility(mSlotHotspot, enabled);
        }
    };

    private final CastController.Callback mCastCallback = new CastController.Callback() {
        @Override
        public void onCastDevicesChanged() {
            updateCast();
        }
    };

    private final NextAlarmController.NextAlarmChangeCallback mNextAlarmCallback =
            new NextAlarmController.NextAlarmChangeCallback() {
                @Override
                public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
                    mNextAlarm = nextAlarm;
                    updateAlarm();
                }
            };

    private final SensorPrivacyController.OnSensorPrivacyChangedListener mSensorPrivacyListener =
            new SensorPrivacyController.OnSensorPrivacyChangedListener() {
                @Override
                public void onSensorPrivacyChanged(boolean enabled) {
                    mHandler.post(() -> {
                        mIconController.setIconVisibility(mSlotSensorsOff, enabled);
                    });
                }
            };

    @Override
    public void appTransitionStarting(int displayId, long startTime, long duration,
            boolean forced) {
        if (mDisplayId == displayId) {
            updateProfileIcon();
        }
    }

    @Override
    public void appTransitionFinished(int displayId) {
        if (mDisplayId == displayId) {
            updateProfileIcon();
        }
    }

    @Override
    public void onKeyguardShowingChanged() {
        updateProfileIcon();
    }

    @Override
    public void onUserSetupChanged() {
        boolean userSetup = mProvisionedController.isCurrentUserSetup();
        if (mCurrentUserSetup == userSetup) return;
        mCurrentUserSetup = userSetup;
        updateAlarm();
    }

    @Override
    public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
        boolean portrait = RotationLockTile.isCurrentOrientationLockPortrait(
                mRotationLockController, mResources);
        if (rotationLocked) {
            if (portrait) {
                mIconController.setIcon(mSlotRotate, R.drawable.stat_sys_rotate_portrait,
                        mResources.getString(R.string.accessibility_rotation_lock_on_portrait));
            } else {
                mIconController.setIcon(mSlotRotate, R.drawable.stat_sys_rotate_landscape,
                        mResources.getString(R.string.accessibility_rotation_lock_on_landscape));
            }
            mIconController.setIconVisibility(mSlotRotate, true);
        } else {
            mIconController.setIconVisibility(mSlotRotate, false);
        }
    }

    private void updateHeadsetPlug(Intent intent) {
        boolean connected = intent.getIntExtra("state", 0) != 0;
        boolean hasMic = intent.getIntExtra("microphone", 0) != 0;
        if (connected) {
            String contentDescription = mResources.getString(hasMic
                    ? R.string.accessibility_status_bar_headset
                    : R.string.accessibility_status_bar_headphones);
            mIconController.setIcon(mSlotHeadset, hasMic ? R.drawable.stat_sys_headset_mic
                    : R.drawable.stat_sys_headset, contentDescription);
            mIconController.setIconVisibility(mSlotHeadset, true);
        } else {
            mIconController.setIconVisibility(mSlotHeadset, false);
        }
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mIconController.setIconVisibility(mSlotDataSaver, isDataSaving);
    }

    @Override  // PrivacyItemController.Callback
    public void onPrivacyItemsChanged(List<PrivacyItem> privacyItems) {
        updatePrivacyItems(privacyItems);
    }

    private void updatePrivacyItems(List<PrivacyItem> items) {
        boolean showCamera = false;
        boolean showMicrophone = false;
        boolean showLocation = false;
        for (PrivacyItem item : items) {
            if (item == null /* b/124234367 */) {
                Log.e(TAG, "updatePrivacyItems - null item found");
                StringWriter out = new StringWriter();
                mPrivacyItemController.dump(new PrintWriter(out), null);
                // Throw so we can look into this
                throw new NullPointerException(out.toString());
            }
            switch (item.getPrivacyType()) {
                case TYPE_CAMERA:
                    showCamera = true;
                    break;
                case TYPE_LOCATION:
                    showLocation = true;
                    break;
                case TYPE_MICROPHONE:
                    showMicrophone = true;
                    break;
            }
        }

        // Disabling for now, but keeping the log
        /*
        mIconController.setIconVisibility(mSlotCamera, showCamera);
        mIconController.setIconVisibility(mSlotMicrophone, showMicrophone);
        if (mPrivacyItemController.getLocationAvailable()) {
            mIconController.setIconVisibility(mSlotLocation, showLocation);
        }
         */
        mPrivacyLogger.logStatusBarIconsVisible(showCamera, showMicrophone,  showLocation);
    }

    @Override
    public void onLocationActiveChanged(boolean active) {
        if (!mPrivacyItemController.getLocationAvailable()) {
            updateLocationFromController();
        }
    }

    // Updates the status view based on the current state of location requests.
    private void updateLocationFromController() {
        if (mLocationController.isLocationActive()) {
            mIconController.setIconVisibility(mSlotLocation, true);
        } else {
            mIconController.setIconVisibility(mSlotLocation, false);
        }
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_SIM_STATE_CHANGED:
                    // Avoid rebroadcast because SysUI is direct boot aware.
                    if (intent.getBooleanExtra(Intent.EXTRA_REBROADCAST_ON_UNLOCK, false)) {
                        break;
                    }
                    break;
                case TelecomManager.ACTION_CURRENT_TTY_MODE_CHANGED:
                    updateTTY(intent.getIntExtra(TelecomManager.EXTRA_CURRENT_TTY_MODE,
                            TelecomManager.TTY_MODE_OFF));
                    break;
                case Intent.ACTION_MANAGED_PROFILE_AVAILABLE:
                case Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE:
                case Intent.ACTION_PROFILE_REMOVED:
                case Intent.ACTION_PROFILE_ACCESSIBLE:
                case Intent.ACTION_PROFILE_INACCESSIBLE:
                    updateProfileIcon();
                    break;
                case AudioManager.ACTION_HEADSET_PLUG:
                    updateHeadsetPlug(intent);
                    break;
            }
        }
    };

    private Runnable mRemoveCastIconRunnable = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "updateCast: hiding icon NOW");
            mIconController.setIconVisibility(mSlotCast, false);
        }
    };

    // Screen Recording
    @Override
    public void onCountdown(long millisUntilFinished) {
        if (DEBUG) Log.d(TAG, "screenrecord: countdown " + millisUntilFinished);
        int countdown = (int) Math.floorDiv(millisUntilFinished + 500, 1000);
        int resourceId = R.drawable.stat_sys_screen_record;
        String description = Integer.toString(countdown);
        switch (countdown) {
            case 1:
                resourceId = R.drawable.stat_sys_screen_record_1;
                break;
            case 2:
                resourceId = R.drawable.stat_sys_screen_record_2;
                break;
            case 3:
                resourceId = R.drawable.stat_sys_screen_record_3;
                break;
        }
        mIconController.setIcon(mSlotScreenRecord, resourceId, description);
        mIconController.setIconVisibility(mSlotScreenRecord, true);
        // Set as assertive so talkback will announce the countdown
        mIconController.setIconAccessibilityLiveRegion(mSlotScreenRecord,
                View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
    }

    @Override
    public void onCountdownEnd() {
        if (DEBUG) Log.d(TAG, "screenrecord: hiding icon during countdown");
        mHandler.post(() -> mIconController.setIconVisibility(mSlotScreenRecord, false));
        // Reset talkback priority
        mHandler.post(() -> mIconController.setIconAccessibilityLiveRegion(mSlotScreenRecord,
                View.ACCESSIBILITY_LIVE_REGION_NONE));
    }

    @Override
    public void onRecordingStart() {
        if (DEBUG) Log.d(TAG, "screenrecord: showing icon");
        mIconController.setIcon(mSlotScreenRecord,
                R.drawable.stat_sys_screen_record,
                mResources.getString(R.string.screenrecord_ongoing_screen_only));
        mHandler.post(() -> mIconController.setIconVisibility(mSlotScreenRecord, true));
    }

    @Override
    public void onRecordingEnd() {
        // Ensure this is on the main thread
        if (DEBUG) Log.d(TAG, "screenrecord: hiding icon");
        mHandler.post(() -> mIconController.setIconVisibility(mSlotScreenRecord, false));
    }

    private void onConnectedDisplayAvailabilityChanged(ConnectedDisplayInteractor.State state) {
        boolean visible = state != ConnectedDisplayInteractor.State.DISCONNECTED;

        if (DEBUG) {
            Log.d(TAG, "connected_display: " + (visible ? "showing" : "hiding") + " icon");
        }

        mIconController.setIconVisibility(mSlotConnectedDisplay, visible);
    }
}
