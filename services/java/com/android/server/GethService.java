package com.android.server;

import android.content.Context;
import android.util.Log;
import android.os.Environment;
import android.os.IGethService;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.content.Intent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.annotation.SystemService;

@SystemService(Context.GETH_SERVICE)
public class GethService extends IGethService.Stub {
    private static final String TAG = "GethService";
    private static GethService instance;
    private final String dataDir = "/data/lightclient/";
    private Process mainProcess;
    private ProcessBuilder builder;
    private final String checkPoint = "0xb2fbee34f6ec8e93d7d1e1be870fe721f99e0bb74e449805b571fdd11d653bc2";
    private final String web3URL = "https://eth-mainnet.g.alchemy.com/v2/Ka357dlw4WBBevyJtDENSs2b0ZjKiDia";
    private final String[] nimbusCommand = {"/system/bin/nimbus_verified_proxy", "--trusted-block-root="+checkPoint, "--web3-url="+web3URL};
    private final String[] heliosCommand = {"/system/bin/helios", "--execution-rpc", web3URL, "--data-dir", dataDir, "--checkpoint", checkPoint};
    private String currentCommand = "helios";
    private Context context;
    private Thread standardOutputThread;
    private Thread errorOutputThread;

    public GethService(Context con) {
        super();
        Log.v(TAG, "GethNode, onCreate: " + dataDir);
        this.context = con;

        builder = new ProcessBuilder(heliosCommand);
    }

    public void redirectProcessOutput(Process process) {
        InputStream inputStream = process.getInputStream();
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader buffer = new BufferedReader(reader);
      
        standardOutputThread = new Thread(() -> {
          try {
            String line;
            while ((line = buffer.readLine()) != null) {
              System.out.println(TAG +": " +line);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
        standardOutputThread.start();
      
        InputStream errorStream = process.getErrorStream();
        InputStreamReader errorReader = new InputStreamReader(errorStream);
        BufferedReader errorBuffer = new BufferedReader(errorReader);
      
        errorOutputThread = new Thread(() -> {
          try {
            String line;
            while ((line = errorBuffer.readLine()) != null) {
                System.out.println(TAG +": " +line);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
        errorOutputThread.start();
    }
    

    public String getEnodeURL() {
        return "";
    }

    public void shutdownGeth() {
        if (mainProcess != null) {
            mainProcess.destroy();
            mainProcess = null;
            updateViews();
        }
        Log.v(TAG, "GethNode, successfully stopped :)");
    }

    public void shutdownWithoutPreference() {
        if (mainProcess != null) {
            mainProcess.destroy();
            mainProcess = null;
            updateViews();
        }
        Log.v(TAG, "GethNode, successfully stopped. Without preferences :)");
    }

    public void startGeth() {
        try {
            if (mainProcess == null) {
                mainProcess = builder.start();
                redirectProcessOutput(mainProcess);
                updateViews();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v(TAG, "GethNode, successfully started :)");
    }

    public boolean doesFileExist(String filePathString) {
        File f = new File(filePathString);
        return f.exists() && !f.isDirectory();
    }

    public void changeClient(String client) {
        if (mainProcess != null) {
            mainProcess.destroy();
            mainProcess = null;
            updateViews();
        }
        if (client.equals("Nimbus")) {
            builder = new ProcessBuilder(nimbusCommand);
            currentCommand = "Nimbus";
        } else if (client.equals("Helios")) {
            builder = new ProcessBuilder(heliosCommand);
            currentCommand = "Helios";
        }
    }

    public String getCurrentClient() {
        return currentCommand;
    }

    public boolean isRunning() {
        if (mainProcess != null) {
            return mainProcess.isAlive();
        }
        return false;
    }

    private void updateViews() {
        Intent intent = new Intent("NODE_UPDATE");
        context.sendBroadcast(intent);
    }

}
