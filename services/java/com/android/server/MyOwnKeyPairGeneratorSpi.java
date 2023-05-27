package com.android.server;

import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;

public class MyOwnKeyPairGeneratorSpi extends KeyPairGeneratorSpi {
    public MyOwnKeyPairGeneratorSpi(String algorithmName) {
        super(algorithmName);
    }
}
