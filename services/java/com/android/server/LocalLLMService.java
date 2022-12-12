package com.android.server;

import android.content.Context;
import android.os.Environment;
import android.os.ILLMService;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import android.annotation.SystemService;
import android.os.ParcelFileDescriptor;

@SystemService(Context.LOCALLLM_SERVICE)
public class LocalLLMService extends ILLMService.Stub {

    private static final String TAG = "LocalLLMService";
    private static LocalLLMService instance;

    public LocalLLMService(Context con) {
        super();
        Log.v(TAG, "LocalLLMService, onCreate");
        instance = this;
    }

    private String executeCommand(String[] command) {
        return "";
    }


    public static LocalLLMService getInstance() {
        return instance;
    }

    public void loadModel() {
        // Load model into ram
        //if (model == null) {
        //    model = new LlamaModel(modelPath, modelParams);
        //}
    }

    public boolean isRunning() {
        return false;
    }
    
    private String runLlama(String prompt, ParcelFileDescriptor realOutput) {
        return "";
    }
    

    public String executePrompt(String prompt, ParcelFileDescriptor realOutput) {
        return prompt;
    }
}

