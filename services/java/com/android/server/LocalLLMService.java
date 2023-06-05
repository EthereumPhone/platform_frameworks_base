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
  }

  public boolean isRunning() {
    return isRunning;
  }

  public String executePrompt(String prompt) {
    // TODO: Implement with actual model
    String[] cmd = {"llama", "-m", "/system/media/ggml-model-q4_0.bin", "-c", "512", "-b", "1024", "-n", "256", "--keep", "48", "--repeat_penalty", "1.0", "--color", "-i", "-r", "User:", "-p", "Transscript of an interactive search engine. The search engine never fails to deliver an answer immediately and with precision.\n\nUser: What is the capital of Russia?\nSearch Engine: The capital of Russia is Moscow.\nUser:" + prompt + "\n"};
    try {
        Process process = Runtime.getRuntime().exec(cmd);
        java.io.InputStream inputStream = process.getInputStream();
        java.util.Scanner s = new java.util.Scanner(inputStream);
        String output = "";
        while (s.hasNextLine()) {
            String line = s.nextLine();
            output += line;
            if (line.contains("Search Engine:") && !line.contains("The capital of Russia is Moscow")) {
                process.destroy(); // Stop the process if an empty line is encountered
                // Remove the beginning which is "Search Engine: " from line
                line = line.substring(19);
                return line;
            }
        }
        process.waitFor();
        return output;
    } catch (Exception e) {
        return e.getMessage();
    }
  }
}
