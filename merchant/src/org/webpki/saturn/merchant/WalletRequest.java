/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.NonDirectPayments;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.TimeUtil;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;

public class WalletRequest implements BaseProperties, MerchantProperties {

    boolean debugMode;
    SavedShoppingCart savedShoppingCart;
    JSONObjectWriter requestObject;
    
    WalletRequest(HttpSession session,
                  NonDirectPayments optionalNonDirectPayment,
                  String androidTransactionUrl,
                  String androidCancelUrl,
                  String androidSuccessUrl) throws IOException {
        debugMode = HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        savedShoppingCart = (SavedShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);
        requestObject = Messages.PAYMENT_CLIENT_REQUEST.createBaseMessage();
        JSONArrayWriter paymentNetworksArray = requestObject.setArray(PAYMENT_NETWORKS_JSON);
        GregorianCalendar timeStamp = new GregorianCalendar();
        GregorianCalendar expires = TimeUtil.inMinutes(30);
        String currentReferenceId = MerchantService.getReferenceId();
        LinkedHashMap<String,JSONObjectWriter> requests = new LinkedHashMap<String,JSONObjectWriter>();

        // Create a signed payment request for each payment network
        for (PaymentNetwork paymentNetwork : MerchantService.paymentNetworks.values()) {
            JSONObjectWriter paymentRequest =
                PaymentRequest.encode(new Payee(MerchantService.merchantCommonName, paymentNetwork.merchantId),
                                      new BigDecimal(BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount),
                                                     MerchantService.currency.getDecimals()),
                                      MerchantService.currency,
                                      optionalNonDirectPayment,
                                      currentReferenceId,
                                      timeStamp,
                                      expires,
                                      paymentNetwork.signer);
            for (String accountType : paymentNetwork.acceptedAccountTypes) {
                if (requests.put(accountType, paymentRequest) != null) {
                    throw new IOException("Duplicate: " + accountType);
                }
            }
            paymentNetworksArray.setObject()
                .setStringArray(PAYMENT_METHODS_JSON, paymentNetwork.acceptedAccountTypes)
                .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
        }
        
        // Android and QR wallets need special arrangements...
        if (androidCancelUrl != null) {
            requestObject.setString(ANDROID_CANCEL_URL_JSON, androidCancelUrl)
                         .setString(ANDROID_SUCCESS_URL_JSON, androidSuccessUrl)
                         .setString(ANDROID_TRANSACTION_URL_JSON, androidTransactionUrl + "/authorize");
        }

        if (debugMode) {
            debugData.InvokeWallet = ProcessingBaseServlet.makeReader(requestObject);
        }

        // Must keep
        session.setAttribute(WALLET_REQUEST_SESSION_ATTR, requests);
    }

    WalletRequest(HttpSession session, NonDirectPayments optionalNonDirectPayment) throws IOException {
        this(session, optionalNonDirectPayment, null, null, null);
    }
}
