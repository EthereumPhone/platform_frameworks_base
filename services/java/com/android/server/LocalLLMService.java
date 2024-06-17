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
import de.kherud.llama.LlamaModel;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel.Output;

@SystemService(Context.LOCALLLM_SERVICE)
public class LocalLLMService extends ILLMService.Stub {

    private static final String TAG = "LocalLLMService";
    private static LocalLLMService instance;
    private Context context;
    private boolean isRunning;
    private Object mLock = new Object();
    private String modelPath = "/data/gpt2medium.gguf";
    private LlamaModel model = null;
    private ModelParameters modelParams;
    private InferenceParameters inferenceParams;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LocalLLMService(Context con) {
        super();
        Log.v(TAG, "LocalLLMService, onCreate");
        try {
            LlamaModel.setLogger((level, message) -> System.out.println(TAG + message));
        } catch (Exception e) {
            e.printStackTrace();
        }
        context = con;
        instance = this;
        isRunning = false;
        modelParams = new ModelParameters();
        modelParams.setNBbatch(128); // Adjusted for potential memory constraints
        modelParams.setUseMmap(true); // Use mmap if available for faster model loading
        modelParams.setUseMLock(true); // Use mmap if available for faster model loading
        //modelParams.setMemoryF16(true);

        inferenceParams = new InferenceParameters();
        inferenceParams.setNPredict(256);
        inferenceParams.setTemperature(0.8f);
        inferenceParams.setNKeep(48);
        inferenceParams.setRepeatPenalty(1.0f);
        inferenceParams.setAntiPrompt("User:");
    }

    private String executeCommand(String[] command) {
        /**
        StringBuffer output = new StringBuffer();
        StringBuffer errorOutput = new StringBuffer();
        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Process process = null;

        try {
            process = Runtime.getRuntime().exec(command);
            final Process processCopy = process;
            
            // Submit stdout reading to executor
            Future<?> futureOut = executor.submit(() -> {
                try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(processCopy.getInputStream()))) {
                    String line;
                    while ((line = stdInput.readLine()) != null) {
                        output.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Submit stderr reading to executor
            Future<?> futureErr = executor.submit(() -> {
                try (BufferedReader stdError = new BufferedReader(new InputStreamReader(processCopy.getErrorStream()))) {
                    String line;
                    while ((line = stdError.readLine()) != null) {
                        errorOutput.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // Wait for both readers to complete
            futureOut.get();
            futureErr.get();

            process.waitFor();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Error executing command: " + e.getMessage();
        } finally {
            if (process != null) {
                process.destroy();
            }
            executor.shutdown();
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        String result = "Output:\n" + output.toString();
        if (errorOutput.length() > 0) {
            result += "Error Output:\n" + errorOutput.toString();
        }
        result += "Time taken: " + duration + " ms\n";
         */
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
        return model != null;
    }
    
    private String runLlama(String prompt, ParcelFileDescriptor realOutput) {
        FileOutputStream fos = new FileOutputStream(realOutput.getFileDescriptor());
 /* 
        for (Output output : model.generate(prompt)) {
            System.out.println(TAG + ": " + output.text);
            byte[] bytes = output.text.getBytes(StandardCharsets.UTF_8);
            try {
                fos.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
       
        String[] cmd = {"llama", "-m", modelPath, "-c", "512", "-b", "1024", "-n", "256", "--keep", "48", "--repeat_penalty", "1.0", "--color", "-r", "User:", "-p", prompt};        
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            java.io.InputStream inputStream = process.getInputStream();
            StringBuilder buffer = new StringBuilder();
            boolean startPrinting = false;
    
            int character;
            while ((character = inputStream.read()) != -1) {
                char ch = (char) character;
                
                byte[] bytes = String.valueOf(ch).getBytes(StandardCharsets.UTF_8);
                fos.write(bytes);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during execution.");
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
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

