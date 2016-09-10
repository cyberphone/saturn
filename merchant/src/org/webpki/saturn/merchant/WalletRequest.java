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
import org.webpki.saturn.common.NonDirectPayments;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.RequestHash;

public class WalletRequest implements BaseProperties, MerchantProperties {

    boolean debugMode;
    SavedShoppingCart savedShoppingCart;
    JSONObjectWriter requestObject;
    
    WalletRequest(HttpSession session,
                  NonDirectPayments optionalNonDirectPayment,
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
        JSONArrayWriter paymentNetworksArray = requestObject.setArray(PAYMENT_NETWORKS_JSON);
        Date timeStamp = new Date();
        Date expires = Expires.inMinutes(30);
        String currentReferenceId = MerchantService.getReferenceId();
        Vector<byte[]> hashes = new Vector<byte[]>();

        // Create a signed payment request for each payment network
         for (PaymentNetwork paymentNetwork : MerchantService.paymentNetworks.values()) {
            JSONObjectWriter paymentRequest =
                    PaymentRequest.encode(Payee.init(MerchantService.merchantCommonName, paymentNetwork.merchantId),
                                          new BigDecimal(BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount), 2),
                                          MerchantService.currency,
                                          optionalNonDirectPayment,
                                          currentReferenceId,
                                          timeStamp,
                                          expires,
                                          paymentNetwork.signer);
            paymentNetworksArray.setObject()
                .setStringArray(ACCEPTED_ACCOUNT_TYPES_JSON, paymentNetwork.acceptedAccountTypes)
                .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
            hashes.add(RequestHash.getRequestHash(paymentRequest));
        }
        
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

    WalletRequest(HttpSession session, NonDirectPayments optionalNonDirectPayment) throws IOException {
        this(session, optionalNonDirectPayment, null, null, null);
    }
}
