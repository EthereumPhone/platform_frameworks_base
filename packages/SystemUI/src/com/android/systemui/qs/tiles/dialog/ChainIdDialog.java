package com.android.systemui.qs.tiles.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import java.lang.reflect.*;
import java.util.Arrays;
import android.widget.AdapterView;
import java.util.List;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.annotation.NonNull;
import android.widget.TextView;
import android.annotation.NonNull;
import android.widget.RadioGroup;

import com.android.internal.app.AlertController;
import com.android.systemui.R;

public class ChainIdDialog extends SystemUIDialog implements Window.Callback {

    private static final String TAG = "ChainIdDialog";

    private Spinner mSpinner;
    protected View mDialogView;
    private ChainIdDialogFactory mChainIdDialogFactory;
    private Context mContext;

    public ChainIdDialog(Context context, ChainIdDialogFactory chainIdDialogFactory) {
        super(context);
        mChainIdDialogFactory = chainIdDialogFactory;
        mContext = context;
    }

    public boolean isDarkModeEnabled() {
        // Check if the system is in dark mode
        int nightModeFlags = mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    
        // Inflate the new layout with the RadioGroup
        mDialogView = LayoutInflater.from(mContext).inflate(R.layout.chain_id_dialog, null);
    
        // Find the RadioGroup from the layout
        RadioGroup chainRadioGroup = mDialogView.findViewById(R.id.chain_radio_group);
    
        // Set up the initial state of the radio buttons based on the current chain ID
        int chainId = getChainId();
        switch (chainId) {
            case 1:
                chainRadioGroup.check(R.id.radio_ethereum_mainnet);
                break;
            case 10:
                chainRadioGroup.check(R.id.radio_optimism);
                break;
            case 42161:
                chainRadioGroup.check(R.id.radio_arbitrum_one);
                break;
            case 84531:
                chainRadioGroup.check(R.id.radio_base_testnet);
                break;
            case 5:
                chainRadioGroup.check(R.id.radio_goerli_testnet);
                break;
            case 8453:
                chainRadioGroup.check(R.id.radio_base_mainnet);
                break;
            case 7777777:
                chainRadioGroup.check(R.id.radio_zora_l2);
                break;
        }
    
        // Set OnCheckedChangeListener
        chainRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Get the selected chain id
                int newChainId = 1; // Default to some chain id
                if (checkedId == R.id.radio_ethereum_mainnet) {
                    newChainId = 1;
                } else if (checkedId == R.id.radio_optimism) {
                    newChainId = 10;
                } else if (checkedId == R.id.radio_arbitrum_one) {
                    newChainId = 42161;
                } else if (checkedId == R.id.radio_base_mainnet) {
                    newChainId = 8453;
                } else if (checkedId == R.id.radio_zora_l2) {
                    newChainId = 7777777;
                } else if (checkedId == R.id.radio_base_testnet) {
                    newChainId = 84531;
                } else if (checkedId == R.id.radio_goerli_testnet) {
                    newChainId = 5;
                }
    
                try {
                    Class<?> walletProxy = Class.forName("android.os.PrivateWalletProxy");
                    Method changeChainid = walletProxy.getMethod("changeChainId", int.class);
                    Object walletManager = mContext.getSystemService("privatewallet");
                    changeChainid.invoke(walletManager, newChainId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
    
                mChainIdDialogFactory.destroyDialog();
            }
        });
    
        final Window window = getWindow();
        window.setContentView(mDialogView);
    }


    @Override
    public void dismiss() {
        super.dismiss();
        Log.d(TAG, "dismissDialog");
        mChainIdDialogFactory.destroyDialog();
    }

}
