package com.android.server;

import android.content.Context;
import android.util.Log;
import android.os.Environment;
import android.os.IPrivateWalletService;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.UUID;
import org.web3j.crypto.*;
import android.content.Intent;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import org.bouncycastle.asn1.x9.X9ECParameters;
import java.io.ObjectOutputStream;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import java.math.BigInteger;
import java.io.FileInputStream;
import java.util.ArrayList;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.web3j.protocol.Web3j;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.web3j.protocol.http.HttpService;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.utils.Numeric;
import java.security.Provider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import java.nio.ByteBuffer;
import javax.crypto.SecretKey;
import java.security.KeyStore;
import android.annotation.SystemService;
import org.web3j.abi.datatypes.Address;

import java.nio.file.Files;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyGenParameterSpec;
import java.security.SecureRandom;
import android.util.Base64;
import javax.crypto.spec.GCMParameterSpec;
import com.android.server.EnCryptor;
import com.android.server.DeCryptor;
import com.android.server.MyOwnKeyPairGeneratorSpi;
import org.bouncycastle.crypto.generators.SCrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;

@SystemService(Context.PRIVATEWALLET_SERVICE)
public class PrivateWalletService extends IPrivateWalletService.Stub {
    private static final String TAG = "PrivateWalletService";
    private static WalletService instance;
    private String walletPath;
    private String dataDir;
    private Credentials credentials;
    private Web3j web3j;
    private int chainId = 1;
    private SharedState sharedState;
    private Context mContext;
    private KeyStore keyStore;

    private EnCryptor enCryptor;
    private DeCryptor deCryptor;
    public PrivateWalletService(SharedState sharedState, Context context) {
        super();
        Log.v(TAG, "PrivateWalletService, onCreate");
        System.out.println("PrivateWalletService, onCreate");
        dataDir = "/data/wallet_files/";
        this.sharedState = sharedState;
        this.mContext = context;
        //Provider provider = setupBouncyCastle();
        Provider provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        System.out.println("PrivateWalletService, setupBouncyCastle done");
        // So that class gets included
        Class clazz = MyOwnKeyPairGeneratorSpi.ECDSA.class;
        System.out.println("PrivateWalletService, MyOwnKeyPairGeneratorSpi.ECDSA done: " + clazz.getName());

        Address address = new Address(BigInteger.ZERO);


        Class clazz2 = SCrypt.class;
        System.out.println("PrivateWalletService, SCrypt done: " + clazz2.getName());
        try {
            // If folder "wallet_files" in /data doesn't exist yet create it
            File dir = new File(dataDir);
            if (!dir.exists()) {
                try {
                    Files.createDirectory(dir.toPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            enCryptor = new EnCryptor();
            deCryptor = new DeCryptor();

            if (doesWalletExist()) {
                String encryptedKey = loadEncryptedKeyFromFile();
                // Decrypt encrypted key using cipher and secret key
                String keyString = decrypt(encryptedKey);
            
                loadWalletPath();
                credentials = WalletUtils.loadCredentials(
                        keyString,
                        walletPath);
            } else {
                // Generate wallet on first boot
                importNewWallet(createEthereumPrivateKey());
            }
            
        } catch (Exception exception) {
            System.out.println("PrivateWalletService error: " + exception.getMessage());
            exception.printStackTrace();
        }
        web3j = Web3j.build(new HttpService());

        //removeBouncyCastle(provider);

    }

    private String createEthereumPrivateKey() {
        // Ensure BouncyCastle is initialized
        Security.addProvider(new BouncyCastleProvider());

        // Create a secure random number generator
        SecureRandom secureRandom = new SecureRandom();

        // Specify the curve parameters for Ethereum (secp256k1)
        X9ECParameters curveParams = org.bouncycastle.asn1.sec.SECNamedCurves.getByName("secp256k1");
        ECDomainParameters domainParameters = new ECDomainParameters(curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH());

        // Generate the key pair
        ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
        keyGen.init(new ECKeyGenerationParameters(domainParameters, secureRandom));
        AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

        // Extract the private key and return it as a hex string
        ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters) keyPair.getPrivate();
        return Hex.toHexString(privateKeyParams.getD().toByteArray());
    }

    private void saveEncryptedKeyToFile(String encryptedKey) throws Exception {
        // Save encrypted key to file
        FileWriter myWriter = new FileWriter(new File(dataDir, "/encrypted_key.txt"));
        myWriter.write(encryptedKey);
        myWriter.close();
    }

    private Boolean doesEncryptedKeyFileExist() {
        return doesFileExist(new File(dataDir, "encrypted_key.txt").getAbsolutePath());
    }

    public void createNewWallet() {
        long startTime = System.currentTimeMillis();
        System.out.println(TAG + ": Got request to create a new wallet");
        try {
            if (!doesWalletExist()) {
                // Generate new random key using SecureRandom
                //SecureRandom secureRandom = new SecureRandom();
                //byte[] randomBytes = new byte[64];
                //secureRandom.nextBytes(randomBytes);
                System.out.println(TAG + ": Starting to create a very new wallet");
                String randomUnencryptedKey = UUID.randomUUID().toString();
                System.out.println(TAG + ": Created a random UUID");
                long beforeWalletCreate = System.currentTimeMillis();
                createWallet(randomUnencryptedKey);
                System.out.println(TAG + ": Created a new wallet and it took: "+(System.currentTimeMillis() - beforeWalletCreate) + "ms");
                long beforeLoadCred = System.currentTimeMillis();
                credentials = WalletUtils.loadCredentials(
                    randomUnencryptedKey,
                        walletPath);
                System.out.println(TAG + ": Loaded the new Wallet and it took: "+(System.currentTimeMillis() - beforeLoadCred) + "ms");
                saveEncryptedKeyToFile(encrypt(randomUnencryptedKey));
                System.out.println(TAG + ": Saved the encrypted key.");
            } else {
                if (!doesEncryptedKeyFileExist()) {
                    // Old wallet with password "password", change it to new random key
                    // Generate new random key using SecureRandom
                    //SecureRandom secureRandom = new SecureRandom();
                    //byte[] randomBytes = new byte[64];
                    //secureRandom.nextBytes(randomBytes);
                    String randomUnencryptedKey = UUID.randomUUID().toString();
                    loadWalletPath();
                    credentials = WalletUtils.loadCredentials(
                            "password",
                            walletPath);
                    File oldWallet = new File(walletPath);
                    encryptCreateNewWallet(randomUnencryptedKey, credentials);
                    oldWallet.delete();
                    saveEncryptedKeyToFile(encrypt(randomUnencryptedKey));
                } else {
                    String encryptedKey = loadEncryptedKeyFromFile();
                    // Decrypt encrypted key using cipher and secret key
                    String keyString = decrypt(encryptedKey);
                
                    loadWalletPath();
                    credentials = WalletUtils.loadCredentials(
                            keyString,
                            walletPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long entireTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Took "+entireTime+"ms to execute");
    }

    public void importNewWallet(String privateKey) {
        if (!doesWalletExist()) {
            try {
                String randomUnencryptedKey = UUID.randomUUID().toString();
                credentials = Credentials.create(privateKey);
                encryptCreateNewWallet(randomUnencryptedKey, credentials);
                saveEncryptedKeyToFile(encrypt(randomUnencryptedKey));
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
    }

    private String loadEncryptedKeyFromFile() throws Exception {
        // Load encrypted key from file
        File file = new File(dataDir, "encrypted_key.txt");
        Scanner scanner = new Scanner(file);
        String encryptedKey = scanner.nextLine();
        scanner.close();
        return encryptedKey;
    }

    private String encrypt(String plaintext) throws Exception {
        byte[] encryptedText = enCryptor.encryptText("private_wallet_key", plaintext);
        return Base64.encodeToString(encryptedText, Base64.DEFAULT);
    }    
    

    private String decrypt(String encryptedResult) throws Exception {
        return deCryptor.decryptData("private_wallet_key", Base64.decode(encryptedResult, Base64.DEFAULT), enCryptor.getIv());
    }


    private boolean doesFileExist(String filePathString) {
        File f = new File(filePathString);
        return f.exists() && !f.isDirectory();
    }

    private Provider setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return provider;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return provider;
        }
        // Android registers its own BC provider. As it might be outdated and might not
        // include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and
        // because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        return provider;
    }

    private void removeBouncyCastle(Provider provider) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(provider, 1);
    }

    private boolean doesWalletExist() {
        File file = new File(dataDir, "wallet_path.txt");
        if (!file.exists()) {
            return false;
        }
        return true;
    }

    private void loadWalletPath() {
        try {
            File file = new File(dataDir, "wallet_path.txt");
            Scanner scanner = new Scanner(file);
            walletPath = scanner.useDelimiter("\\Z").next();
            scanner.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void encryptCreateNewWallet(String passwordString, Credentials credentials) {
        try {
            String fileName = WalletUtils.generateWalletFile(
                    passwordString,
                    credentials.getEcKeyPair(),
                    new File(dataDir),
                    true);
            System.out.println(fileName);
            File file = new File(dataDir, "wallet_path.txt");
            FileWriter myWriter = new FileWriter(file);
            myWriter.write(new File(dataDir, fileName).getAbsolutePath());
            myWriter.close();
            walletPath = new File(dataDir, fileName).getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createWallet(String passwordString) {
        Log.d(TAG, "Running create Wallet");
        try {
            String fileName = WalletUtils.generateNewWalletFile(
                    passwordString,
                    new File(dataDir));
            Log.d(TAG, "Created the wallet");
            System.out.println(fileName);
            File file = new File(dataDir, "wallet_path.txt");
            FileWriter myWriter = new FileWriter(file);
            myWriter.write(new File(dataDir, fileName).getAbsolutePath());
            myWriter.close();
            Log.d(TAG, "Wrote the walletPath to file");
            walletPath = new File(dataDir, fileName).getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPrivateKey() {
        return credentials.getEcKeyPair().getPrivateKey().toString(16);
    }

    public void getChainId(String requestId) {
        sharedState.fulfillRequest(requestId, Integer.toString(chainId));
    }

    public int directGetChainId() {
        return chainId;
    }

    public void changeChainId(int chainId) {
        this.chainId = chainId;
        // Save chainId to file
        savePreference(dataDir+"savedChainId.txt", chainId);
        Intent intent = new Intent("changeChain");
        mContext.sendBroadcast(intent);
    }

    public void savePreference(String filename, int saveChainId) {
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(Integer.toString(saveChainId));
            myWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void pushDecision(String requestId, String response) {
        sharedState.fulfillRequest(requestId, response);
    }

    public void sendTransaction(String requestId, String to, String value, String data, String nonce, String gasPrice,
            String gasAmount) {
        try {
            // Take of 2% of gas price
            BigInteger gasPriceBigInteger = new BigInteger(gasPrice);
            BigInteger maxPrioFee = gasPriceBigInteger.divide(new BigInteger("50"));
            gasPriceBigInteger = gasPriceBigInteger.subtract(maxPrioFee);
            RawTransaction transaction1559 = RawTransaction.createTransaction(
                    (long) chainId, new BigInteger(nonce), new BigInteger(gasAmount), to,
                    new BigInteger(value), data, maxPrioFee, gasPriceBigInteger);
            byte[] signedMessage1559 = TransactionEncoder.signMessage(transaction1559, chainId, credentials);
            String hexValue1559 = Numeric.toHexString(signedMessage1559);
            sharedState.fulfillRequest(requestId, hexValue1559);
        } catch (Exception exception) {
            exception.printStackTrace();
            sharedState.fulfillRequest(requestId, "Error: " + exception.getMessage());
        }
    }

    public void signMessage(String requestId, String message, String type) { // type = true for personal sign
        Log.v(TAG, "PrivateWalletService, signMessage. message: " + message + ", type: " + type + ", requestId: "
                + requestId);
        try {
            if (type.equals("personal_sign_hex")) {
                Log.v(TAG, "PrivateWalletService, signMessage, personal_sign_hex");
                // Use personal_sign
                byte[] messageBytes = hexToString(message.substring(2)).getBytes(StandardCharsets.ISO_8859_1);
                Sign.SignatureData signature = Sign.signPrefixedMessage(messageBytes, credentials.getEcKeyPair());
                String r = Numeric.toHexString(signature.getR());
                String s = Numeric.toHexString(signature.getS()).substring(2);
                String v = Numeric.toHexString(signature.getV()).substring(2);
                String hexValue = new StringBuilder(r).append(s).append(v).toString();
                sharedState.fulfillRequest(requestId, hexValue);
                Log.v(TAG, "PrivateWalletService, signMessage, personal_sign_hex, hexValue: " + hexValue);
            } else if(type.equals("personal_sign")) {
                // Sign using personal_sign
    
                byte[] messageBytes = message.getBytes(StandardCharsets.ISO_8859_1);
    
                Sign.SignatureData signature = Sign.signPrefixedMessage(messageBytes, credentials.getEcKeyPair());
    
                byte[] retval = new byte[65];
                System.arraycopy(signature.getR(), 0, retval, 0, 32);
                System.arraycopy(signature.getS(), 0, retval, 32, 32);
                System.arraycopy(signature.getV(), 0, retval, 64, 1);
    
                String signedMessage = Numeric.toHexString(retval);
                sharedState.fulfillRequest(requestId, signedMessage);
            } else if (type.equals("eth_signTypedData")) {
                try {
                    StructuredDataEncoder dataEncoder = new com.android.server.StructuredDataEncoder(message);
                    byte[] hashStructuredData = dataEncoder.hashStructuredData();
                    
                    Sign.SignatureData signature = Sign.signMessage(hashStructuredData, credentials.getEcKeyPair(), false);
    
                    ByteBuffer sigBuffer = ByteBuffer.allocate(signature.getR().length + signature.getS().length + 1);
                    sigBuffer.put(signature.getR());
                    sigBuffer.put(signature.getS());
                    sigBuffer.put(signature.getV());
    
                    String signedMessage = Numeric.toHexString(sigBuffer.array());
    
                    sharedState.fulfillRequest(requestId, signedMessage);
                }catch (Exception e) {
                    e.printStackTrace();
                    sharedState.fulfillRequest(requestId, "error");
                }
                
            }
        } catch(Exception exception) {
            exception.printStackTrace();
            sharedState.fulfillRequest(requestId, "error");
        }
    }

    public String signTypedData(String message) {
        byte[] messageBytes = message.getBytes(StandardCharsets.ISO_8859_1);
        Sign.SignatureData signature = Sign.signPrefixedMessage(messageBytes, credentials.getEcKeyPair());
        byte[] retval = new byte[65];
        System.arraycopy(signature.getR(), 0, retval, 0, 32);
        System.arraycopy(signature.getS(), 0, retval, 32, 32);
        System.arraycopy(signature.getV(), 0, retval, 64, 1);
        String signedMessage = Numeric.toHexString(retval);
        return signedMessage;
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

    public void getAddress(String requestId) {
        if (credentials == null) {
            try {
                String encryptedKey = loadEncryptedKeyFromFile();
                // Decrypt encrypted key using cipher and secret key
                String keyString = decrypt(encryptedKey);
            
                loadWalletPath();
                credentials = WalletUtils.loadCredentials(
                        keyString,
                        walletPath);
                sharedState.fulfillRequest(requestId, Keys.toChecksumAddress(credentials.getAddress()));
            } catch (Exception e) {
                System.out.println("Error loading creds:");
                e.printStackTrace();
                sharedState.fulfillRequest(requestId, e.getMessage());
            }
        } else {
            sharedState.fulfillRequest(requestId, Keys.toChecksumAddress(credentials.getAddress()));
        }
    }

}
