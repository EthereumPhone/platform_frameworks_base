package android.app;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.annotation.NonNull;

@SuppressLint("AcronymName")
public class WalletSDK {
    private static final String SYS_SERVICE_CLASS = "android.os.WalletProxy";
    private static final String SYS_SERVICE = "wallet";
    public static final String DECLINE = "decline";
    public static final String NOTFULFILLED = "notfulfilled";

    private Class<?> cls;
    private Method createSession;
    private Method getUserDecision;
    private Method hasBeenFulfilled;
    private Method sendTransaction;
    private Method signMessageSys;
    private Method getAddress;
    private Method getChainId;
    private Method changeChainId;
    private String address;
    private Object proxy;
    private String web3RPC;
    private String sysSession;

    @SuppressLint({"WrongConstant", "GenericException", "MissingNullability"})
    public WalletSDK(@NonNull Context context, @NonNull String newWeb3RPC) throws Exception {
        cls = Class.forName(SYS_SERVICE_CLASS);
        Method[] declaredMethods = cls.getDeclaredMethods();
        createSession = declaredMethods[2];
        getUserDecision = declaredMethods[5];
        hasBeenFulfilled = declaredMethods[6];
        sendTransaction = declaredMethods[7];
        signMessageSys = declaredMethods[8];
        getAddress = declaredMethods[3];
        getChainId = declaredMethods[4];
        changeChainId = declaredMethods[1];
        web3RPC = newWeb3RPC;
        proxy = context.getSystemService(SYS_SERVICE);

        if (proxy == null) {
            throw new Exception("No system wallet found");
        } else {
            try {
                sysSession = (String) createSession.invoke(proxy);
                String reqID = (String) getAddress.invoke(proxy, sysSession);
                while (NOTFULFILLED.equals(hasBeenFulfilled.invoke(proxy, reqID))) {
                    Thread.sleep(10);
                }
                address = (String) hasBeenFulfilled.invoke(proxy, reqID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize WalletSDK", e);
            }
        }
    }

    @NonNull
    public String addZerosAndConvertToHex(@NonNull String numberStr) {
        // Convert the input string to a BigDecimal
        BigInteger number = new BigInteger(numberStr);

        // Convert the integer result to a hexadecimal string and prefix it with "0x"
        return "0x" + number.toString(16);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint({"GenericException", "BadFuture"})
    @NonNull
    public CompletableFuture<String> sendTransaction(@NonNull String to, @NonNull String value, @NonNull String data, @NonNull String gasAmount, @NonNull int chainId) throws Exception {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        if (proxy != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                try {

                    String gasAmountReal;
                    if (gasAmount.equals("0")) {
                        gasAmountReal = estimateGas(to, addZerosAndConvertToHex(value), data);
                    } else {
                        gasAmountReal = gasAmount;
                    }

                    String gasPriceVAL = getCurrentGasPrice().get();
                    String count = hexToBase10String(getTransactionCount(address).get());

                    String reqID = (String) sendTransaction.invoke(proxy, sysSession, to, value, data, count, gasPriceVAL, gasAmountReal , chainId);

                    String result = NOTFULFILLED;

                    while (true) {
                        Object tempResult = hasBeenFulfilled.invoke(proxy, reqID);
                        if (tempResult != null) {
                            result = (String) tempResult;
                            if (!NOTFULFILLED.equals(result)) {
                                break;
                            }
                        }
                        Thread.sleep(100);
                    }
                    if (DECLINE.equals(result)) {
                        completableFuture.complete(DECLINE);
                    } else {
                        JSONArray params = new JSONArray();
                        params.put(result); // The signed transaction hex string is directly used here

                        String response = sendJsonRpcRequest("eth_sendRawTransaction", params);
                        JSONObject jsonResponse = new JSONObject(response);
                        String txHash = jsonResponse.getString("result");
                        completableFuture.complete(txHash);
                    }
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            });
            return completableFuture;
        } else {
            throw new Exception("No system wallet found");
        }
    }

    @NonNull
    public String hexToBase10String(@NonNull String hexString) {
        // Remove the "0x" prefix if present
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        // Parse the hex string to an integer
        int decimalValue = Integer.parseInt(hexString, 16);

        // Convert the integer to a base 10 string
        return Integer.toString(decimalValue);
    }

    @NonNull
    @SuppressLint("BadFuture")
    public CompletableFuture<String> getCurrentGasPrice() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(() -> {
            try {
                JSONArray params = new JSONArray(); // No parameters needed for eth_gasPrice
                String response = sendJsonRpcRequest("eth_gasPrice", params);
                JSONObject jsonResponse = new JSONObject(response);
                String gasPriceHex = jsonResponse.getString("result");

                // Convert from hex to BigInteger for calculation
                BigInteger gasPrice = new BigInteger(gasPriceHex.substring(2), 16);

                // Calculate 5% increase
                BigInteger fivePercentIncrease = gasPrice.divide(BigInteger.valueOf(20)); // equivalent to gasPrice * 5 / 100
                BigInteger increasedGasPrice = gasPrice.add(fivePercentIncrease);

                // Convert back to hex string if necessary
                String increasedGasPriceHex = increasedGasPrice.toString();

                completableFuture.complete(increasedGasPriceHex); // Hexadecimal value of the increased gas price in wei
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });
        return completableFuture;
    }

    @NonNull
    @SuppressLint("BadFuture")
    public CompletableFuture<String> getTransactionCount(@NonNull String address) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(() -> {
            try {
                JSONArray params = new JSONArray();
                params.put(address);
                params.put("latest"); // Use "latest" to get the count for the latest block
                String response = sendJsonRpcRequest("eth_getTransactionCount", params);
                JSONObject jsonResponse = new JSONObject(response);
                String transactionCount = jsonResponse.getString("result");
                completableFuture.complete(transactionCount); // Hexadecimal value of the count
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });
        return completableFuture;
    }

    @NonNull
    @SuppressLint("GenericException")
    public String estimateGas(@NonNull String to, @NonNull String value, @NonNull String data) throws Exception {
        JSONObject transactionObject = new JSONObject();
        transactionObject.put("from", getAddress()); // Assuming getAddress() returns the wallet address
        transactionObject.put("to", to);

        transactionObject.put("value", value);

        if (data != null) {
            // Ensure data starts with "0x"
            if (!data.startsWith("0x")) {
                data = "0x" + data;
            }
            // Ensure data length is even, if not prepend a "0" after "0x"
            if ((data.length() % 2) != 0) {
                data = "0x0" + data.substring(2);
            }
            transactionObject.put("data", data);
        }
        // Other fields like gas, gasPrice can be omitted or set as null if not needed

        JSONArray params = new JSONArray();
        params.put(transactionObject);

        String response = sendJsonRpcRequest("eth_estimateGas", params);

        // Parse the JSON response to extract the estimated gas amount
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.has("result")) {
            // The result field contains the estimated gas amount in hex format
            String estimatedGasHex = jsonResponse.getString("result");
            // Convert from hex to BigInteger for easy manipulation or return as is if you prefer hex
            BigInteger estimatedGasWei = new BigInteger(estimatedGasHex.substring(2), 16);
            return estimatedGasWei.toString(); // Return as string in decimal format
        } else {
            // Handle error or no result scenario
            throw new Exception("Failed to estimate gas: " + jsonResponse.optString("error"));
        }
    }

    @NonNull
    @SuppressLint("GenericException")
    private String sendJsonRpcRequest(@NonNull String method, @NonNull JSONArray params) throws Exception {
        URL url = new URL(web3RPC);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "2.0");
        jsonRequest.put("method", method);
        jsonRequest.put("params", params);
        jsonRequest.put("id", 1);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonRequest.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

    @SuppressLint({"GenericException", "BadFuture"})
    @RequiresApi(Build.VERSION_CODES.N)
    @NonNull
    public CompletableFuture<String> signMessage(@NonNull String message, @NonNull String type) throws Exception {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        if (proxy != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                try {
                    String reqID = (String) signMessageSys.invoke(proxy, sysSession, message, type);
                    String result = NOTFULFILLED;
                    while (true) {
                        Object tempResult = hasBeenFulfilled.invoke(proxy, reqID);
                        if (tempResult != null) {
                            result = (String) tempResult;
                            if (!NOTFULFILLED.equals(result)) {
                                break;
                            }
                        }
                        Thread.sleep(100);
                    }
                    completableFuture.complete(result);
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            });
            return completableFuture;
        } else {
            throw new Exception("No system wallet found");
        }
    }

    @SuppressLint("GenericException")
    @NonNull
    public String createSession() throws Exception {
        if (proxy != null) {
            return sysSession;
        } else {
            throw new Exception("No system wallet found");
        }
    }

    @SuppressLint("GenericException")
    @NonNull
    public String getAddress() throws Exception {
        if (proxy != null) {
            return address;
        } else {
            throw new Exception("No system wallet found");
        }
    }

    @SuppressLint("GenericException")
    @RequiresApi(Build.VERSION_CODES.N)
    public int getChainId() throws Exception {
        if (proxy != null) {
            CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                try {
                    String reqId = (String) getChainId.invoke(proxy, sysSession);
                    while (NOTFULFILLED.equals(hasBeenFulfilled.invoke(proxy, reqId))) {
                        Thread.sleep(10);
                    }
                    completableFuture.complete(Integer.parseInt((String) hasBeenFulfilled.invoke(proxy, reqId)));
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            });
            return completableFuture.get();
        } else {
            throw new Exception("No system wallet found");
        }
    }

    @SuppressLint({"GenericException", "BadFuture"})
    @RequiresApi(Build.VERSION_CODES.N)
    @NonNull
    public CompletableFuture<String> changeChainId(@NonNull int chainId, @NonNull String newWeb3RPC) throws Exception {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        if (proxy != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                try {
                    String reqID = (String) changeChainId.invoke(proxy, sysSession, chainId);
                    String result = NOTFULFILLED;
                    while (true) {
                        Object tempResult = hasBeenFulfilled.invoke(proxy, reqID);
                        if (tempResult != null) {
                            result = (String) tempResult;
                            if (!NOTFULFILLED.equals(result)) {
                                break;
                            }
                        }
                        Thread.sleep(100);
                    }
                    if(!result.equals(DECLINE)) {
                        web3RPC = newWeb3RPC;
                    }
                    completableFuture.complete(result);
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            });
            return completableFuture;
        } else {
            throw new Exception("No system wallet found");
        }
    }

    @SuppressLint("AcronymName")
    public boolean isEthOS() {
        return proxy != null;
    }
}