package com.inapppurchase;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

public class Premium extends AppCompatActivity implements PurchasesUpdatedListener {

    public static final String PREF_FILE = "MyPref";
    public static final String PURCHASE_KEY = "test_premium";
    public static final String PRODUCT_ID = "test_premium";
    TextView purchaseStatus;
    Button purchaseButton;
    String PremiumValue;
    AcknowledgePurchaseResponseListener ackPurchase = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//if purchase is acknowledged
// Grant entitlement to the user. and restart activity
                savePurchaseValueToPref(true);
                Toast.makeText(getApplicationContext(), "Item Purchased", Toast.LENGTH_SHORT).show();
                Premium.this.recreate();
            }
        }
    };
    private BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        // This is for geting premium status and put in Sharedperfreance

        purchaseStatus = (TextView) findViewById(R.id.purchase_status);
        purchaseButton = (Button) findViewById(R.id.purchase_button);
// Establish connection to billing client
//check purchase status from google play store cache
//to check if item already Purchased previously or refunded
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Purchase.PurchasesResult queryPurchase = billingClient.queryPurchases(INAPP);
                    List<Purchase> queryPurchases = queryPurchase.getPurchasesList();
                    if (queryPurchases != null && queryPurchases.size() > 0) {
                        handlePurchases(queryPurchases);
                    }
//if purchase list is empty that means item is not purchased
//Or purchase is refunded or canceled
                    else {
                        savePurchaseValueToPref(false);
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
//item Purchased
        if (getPurchaseValueFromPref()) {
            purchaseButton.setVisibility(View.GONE);
            PremiumValue="Premium";
//if iteam Successfully purchased handle it...
            SharedPreferences sharedPreferences = getSharedPreferences("sPremium", MODE_PRIVATE);
            SharedPreferences.Editor myEdit = sharedPreferences.edit();
            myEdit.putString("PremiumStatus",PremiumValue );
            myEdit.apply();

            purchaseStatus.setText("Purchase Status : Purchased");
        }
//item not Purchased
        else {
            purchaseButton.setVisibility(View.VISIBLE);
            purchaseStatus.setText("Purchase Status : Not Purchased");
        }
    }

    private SharedPreferences getPreferenceObject() {
        return getApplicationContext().getSharedPreferences(PREF_FILE, 0);
    }

    private SharedPreferences.Editor getPreferenceEditObject() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(PREF_FILE, 0);
        return pref.edit();
    }

    private boolean getPurchaseValueFromPref() {
        return getPreferenceObject().getBoolean(PURCHASE_KEY, false);
    }

    private void savePurchaseValueToPref(boolean value) {
        getPreferenceEditObject().putBoolean(PURCHASE_KEY, value).commit();
    }

    //initiate purchase on button click
    public void purchase(View view) {
//check if service is already connected
        if (billingClient.isReady()) {
            initiatePurchase();
        }
//else reconnect service
        else {
            billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        initiatePurchase();
                    } else {
                        Toast.makeText(getApplicationContext(), "Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                }
            });
        }
    }

    private void initiatePurchase() {
        List<String> skuList = new ArrayList<>();
        skuList.add(PRODUCT_ID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                        .setSkuDetails(skuDetailsList.get(0))
                                        .build();
                                billingClient.launchBillingFlow(Premium.this, flowParams);
                            } else {
//try to add item/product id "purchase" inside managed product in google play console
                                Toast.makeText(getApplicationContext(), "Purchase Item not Found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    " Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
//if item newly purchased
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases);
        }
//if item already purchased then check and reflect changes
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Purchase.PurchasesResult queryAlreadyPurchasesResult = billingClient.queryPurchases(INAPP);
            List<Purchase> alreadyPurchases = queryAlreadyPurchasesResult.getPurchasesList();
            if (alreadyPurchases != null) {
                handlePurchases(alreadyPurchases);
            }
        }
//if purchase cancelled
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(getApplicationContext(), "Purchase Canceled", Toast.LENGTH_SHORT).show();
        }
// Handle any other error msgs
        else {
            Toast.makeText(getApplicationContext(), "Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void handlePurchases(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
//if item is purchased
            if (PRODUCT_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
// Invalid purchase
// show error to user
                    Toast.makeText(getApplicationContext(), "Error : Invalid Purchase", Toast.LENGTH_SHORT).show();
                    return;
                }
// else purchase is valid
//if item is purchased and not acknowledged
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, ackPurchase);
                }
//else item is purchased and also acknowledged
                else {
// Grant entitlement to the user on item purchase
// restart activity
                    if (!getPurchaseValueFromPref()) {
                        savePurchaseValueToPref(true);
                        Toast.makeText(getApplicationContext(), "Item Purchased", Toast.LENGTH_SHORT).show();
                        this.recreate();
                    }
                }
            }
//if purchase is pending
            else if (PRODUCT_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Toast.makeText(getApplicationContext(),
                        "Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show();
            }
//if purchase is unknown
            else if (PRODUCT_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                savePurchaseValueToPref(false);
                purchaseStatus.setText("Purchase Status : Not Purchased");
                purchaseButton.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Purchase Status Unknown", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        try {
// To get key go to Developer Console > Select your app > Monetize > Monetization setup.
            String base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCADz3a757GHWP02TDppM0iarT8i0D0K1AopyD+Mxog8ZbQ9gY1+hxSvX9K5ySc2fnAOypW+NISG1L3u9HK8x1P9nyrB0p/TmWc9wcqIh7uF60hvugsEpCdq5xPf76Tk0eDc2yTT1AGbwfWEnX9vnyNZQi16pJQSA6CcjHuZdviiJ1Tce4tSwsxD8tnSyuXZ1aDXOLxcow7nF3cLBbjpUSvTv5IlEHqJC2RcH4bA7D/+XMoDNB3r15nxnKVPnVuQ4na+F4mNp0CZKNuwPYwE7nsUTbysXfg4BPcd/7H7uoo+jA4t7L+CR3mkTx0GOFCnC7NjlhM/96qg9ZMtordwUwIDAQAB";
//                    String.valueOf(R.string.Base64Key);
            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    public void Btn_Premium_Close(View view) {
       onBackPressed();
    }
}