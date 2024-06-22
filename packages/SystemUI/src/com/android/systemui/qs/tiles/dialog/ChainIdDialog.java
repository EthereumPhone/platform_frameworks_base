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
        mDialogView = LayoutInflater.from(mContext).inflate(R.layout.chain_id_dialog,
                null);
        mSpinner = (Spinner) mDialogView.findViewById(R.id.chain_id_spinner);
        /*
	if(isDarkModeEnabled()) {
            mSpinner.setBackgroundResource(R.drawable.layout_dropdown_bg);
        } else {
            mSpinner.setBackgroundResource(R.drawable.layout_dropdown_bg_light);
        }
	*/
        // Add the chain ids to the spinner
        List<String> list = Arrays.asList("Ethereum Mainnet", "Optimism", "Arbitrum One", "Base Mainnet", "Zora L2", "Base Testnet", "Goerli Testnet");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        // Make spinner dropdown background white if dark mode is disabled
        if(!isDarkModeEnabled()) {
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_light);
        }
        mSpinner.setAdapter(adapter);
        int chainId = getChainId();
        switch(chainId) {
            case 1:
                mSpinner.setSelection(0);
                break;
            case 10:
                mSpinner.setSelection(1);
                break;
            case 42161:
                mSpinner.setSelection(2);
                break;
            case 84531:
                mSpinner.setSelection(5);
                break;
            case 5:
                mSpinner.setSelection(6);
                break;
            case 8453:
                mSpinner.setSelection(3);
                break;
            case 7777777:
                mSpinner.setSelection(4);
                break;
        }
        // Set OnItemSelectedListener
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected chain id
                String chainId = parent.getItemAtPosition(position).toString();
                // Set the chain id
                int newChainId = 1;
                switch(chainId) {
                    case "Ethereum Mainnet":
                        newChainId = 1;
                        break;
                    case "Optimism":
                        newChainId = 10;
                        break;
                    case "Arbitrum One":
                        newChainId = 42161;
                        break;
                    case "Goerli Testnet":
                        newChainId = 5;
                        break;
                    case "Base Testnet":
                        newChainId = 84531;
                        break;
                    case "Base Mainnet":
                        newChainId = 8453;
                        break;
                    case "Zora L2":
                        newChainId = 7777777;
			break;
                }
                try {
                    Class walletProxy = Class.forName("android.os.PrivateWalletProxy");
                    Method changeChainid = walletProxy.getMethod("changeChainId", int.class);
                    Object walletManager = mContext.getSystemService("privatewallet");
                    changeChainid.invoke(walletManager, newChainId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mChainIdDialogFactory.destroyDialog();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        final Window window = getWindow();
        window.setContentView(mDialogView);
    }

    /*

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mChainIdDialogFactory.destroyDialog();
    }


    @Override
    public void dismissDialog() {
        Log.d(TAG, "dismissDialog");
        mChainIdDialogFactory.destroyDialog();
        dismiss();
    }
    */
}
