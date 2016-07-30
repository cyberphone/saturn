/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.merchant;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Date;
import java.util.Vector;

import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.RequestHash;
import org.webpki.saturn.common.ServerAsymKeySigner;

public class WalletRequest implements BaseProperties, MerchantProperties {

    boolean debugMode;
    SavedShoppingCart savedShoppingCart;
    JSONObjectWriter requestObject;
    
    private Date timeStamp;
    private Date expires;
    private Vector<byte[]> hashes = new Vector<byte[]>();
    private JSONArrayWriter paymentNetworks;
    private String currentReferenceId;
    
    void addPaymentNetwork(String merchantId, ServerAsymKeySigner signer, String[] acceptedAccountTypes) throws IOException {
        JSONObjectWriter paymentRequest =
                PaymentRequest.encode(Payee.init(MerchantService.merchantCommonName, merchantId),
                                      new BigDecimal(BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount), 2),
                                      MerchantService.currency,
                                      currentReferenceId,
                                      timeStamp,
                                      expires,
                                      signer);
        paymentNetworks.setObject()
            .setStringArray(ACCEPTED_ACCOUNT_TYPES_JSON, acceptedAccountTypes)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
        hashes.add(RequestHash.getRequestHash(paymentRequest));
    }

    WalletRequest(HttpSession session, 
                  String androidTransactionUrl,
                  String androidCancelUrl,
                  String androidSuccessUrl) throws IOException {
        debugMode = W2NBWalletServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        savedShoppingCart = (SavedShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);
        requestObject = Messages.createBaseMessage(Messages.PAYMENT_CLIENT_REQUEST);
        paymentNetworks = requestObject.setArray(PAYMENT_NETWORKS_JSON);
        timeStamp = new Date();
        expires = Expires.inMinutes(30);
        currentReferenceId = MerchantService.getReferenceId();

        // Optional merchant network
        if (MerchantService.otherNetworkKey != null) {
            addPaymentNetwork(MerchantService.otherNetworkId, MerchantService.otherNetworkKey, new String[]{"http://othernetworkpay"});
        }
       
        // The standard payment network supported by the Saturn demo
        Vector<String> acceptedAccountTypes = new Vector<String>();
        for (PayerAccountTypes account : MerchantService.acceptedAccountTypes) {
            acceptedAccountTypes.add(account.getTypeUri());
        }
        addPaymentNetwork(MerchantService.merchantId, MerchantService.merchantKey, acceptedAccountTypes.toArray(new String[0]));
        
        // For checking the wallet return
        session.setAttribute(REQUEST_HASH_SESSION_ATTR, hashes);

        // Android and QR wallets need special arrangements...
        if (androidCancelUrl != null) {
            requestObject.setString(ANDROID_CANCEL_URL_JSON, androidCancelUrl)
                         .setString(ANDROID_SUCCESS_URL_JSON, androidSuccessUrl)
                         .setString(ANDROID_TRANSACTION_URL_JSON, androidTransactionUrl);
        }

        if (debugMode) {
            debugData.InvokeWallet = requestObject;
        }
    }

    WalletRequest(HttpSession session) throws IOException {
        this(session, null, null, null);
    }
}
