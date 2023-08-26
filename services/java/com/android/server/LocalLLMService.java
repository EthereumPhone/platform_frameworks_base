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
import android.content.Intent;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import android.annotation.SystemService;
import android.os.ParcelFileDescriptor;

@SystemService(Context.LOCALLLM_SERVICE)
public class LocalLLMService extends ILLMService.Stub {

  private static final String TAG = "LocalLLMService";
  private static LocalLLMService instance;
  private Context context;
  private boolean isRunning;
  private Object mLock = new Object();

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
  
  private String runLlama(String prompt, ParcelFileDescriptor realOutput) {
    /*
    String[] cmd = {"llama", "-m", "/system/media/llmmodel/ggml-model-q4_0.bin", "-c", "512", "-b", "1024", "-n", "256", "--keep", "48", "--repeat_penalty", "1.0", "--color", "-i", "-r", "User:", "-p", "Transscript of an interactive search engine. The search engine never fails to deliver an answer immediately and with precision.\n\nUser: What is the capital of Russia?\nSearch Engine: The capital of Russia is Moscow.\nUser:" + prompt + "\n"};
    Process process = null;
	FileOutputStream fos = new FileOutputStream(realOutput.getFileDescriptor());
    try {
        int charNum = 0;
        process = Runtime.getRuntime().exec(cmd);
        java.io.InputStream inputStream = process.getInputStream();
        java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("");
        StringBuilder output = new StringBuilder();
        String newestLine = "";
        // Beginning of text
        int beginningOfText = 225 + prompt.length() + 20;
        while (s.hasNext()) {
            String character = s.next();
            output.append(character);
            if (charNum >= beginningOfText && !character.equals("\n")) {
                // Write character to ParcelFileDescriptor
				byte[] bytes = character.getBytes();
				try {
					fos.write(bytes);
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
            charNum++;
            if (character.equals("\n")) {
                String line = output.toString().trim();
                if (line.contains("Search Engine:") && !line.contains("The capital of Russia is Moscow")) {
                    newestLine = line.substring(line.indexOf("Search Engine:") + 15);
                    return newestLine;
                }
                output.setLength(0); // Reset the StringBuilder
            }
        }
        process.destroy(); // Stop the process after reading all the input
        return newestLine;
    } catch (Exception e) {
        e.printStackTrace();
        return "error";
    } finally {
        if (process != null) {
            process.destroy();
        }
		try {
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    */
    return "Not yet implemented";
  }

  public String executePrompt(String prompt, ParcelFileDescriptor realOutput) {
    synchronized(mLock) {
      isRunning = true;
      String output = runLlama(prompt, realOutput);
      isRunning = false;
      return output;
    }
  }
}
