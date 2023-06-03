package com.android.server;

import android.content.Context;
import android.os.Environment;
import android.os.ILLMService;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import org.web3j.crypto.WalletUtils;
import android.content.Intent;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import android.annotation.SystemService;

@SystemService(Context.LOCALLLM_SERVICE)
public class LocalLLMService extends ILLMService.Stub {

  private static final String TAG = "LocalLLMService";
  private static LocalLLMService instance;
  private Context context;
  private boolean isRunning;

  public LocalLLMService(Context con) {
    super();
    Log.v(TAG, "LocalLLMService, onCreate");
    context = con;
    instance = this;
    isRunning = false;
  }

  public static LocalLLMService getInstance() {
    return instance;
  }

  public void loadModel() {
    // TODO: Actually implement loading model into ram
    // Do something for a second
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public boolean isRunning() {
    return isRunning;
  }

  public String executePrompt(String prompt) {
    // TODO: Implement with actual model
    return "Prompt returned: \"" + prompt + "\"";
  }
}
