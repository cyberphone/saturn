/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
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

import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.NonDirectPayments;
import org.webpki.saturn.common.TimeUtils;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;

public class WalletRequest implements BaseProperties, MerchantSessionProperties {

    boolean debugMode;
    SavedShoppingCart savedShoppingCart;
    JSONObjectWriter requestObject;
    
    WalletRequest(HttpSession session,
                  NonDirectPayments optionalNonDirectPayment) throws IOException {
        debugMode = HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        savedShoppingCart = (SavedShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);
        MerchantDescriptor merchant = MerchantService.getMerchant(session);
        requestObject = Messages.PAYMENT_CLIENT_REQUEST.createBaseMessage();

        // Create a payment request
        JSONObjectWriter paymentRequest =
            PaymentRequest.encode(merchant.commonName, 
                                  merchant.homePage,
                                  new BigDecimal(
                                      BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount),
                                      MerchantService.currency.getDecimals()),
                                  MerchantService.currency,
                                  optionalNonDirectPayment,
                                  merchant.getReferenceId(),
                                  new GregorianCalendar(),
                                  TimeUtils.inMinutes(30));

        JSONArrayWriter methodList = requestObject.setArray(SUPPORTED_PAYMENT_METHODS_JSON);
        for (String paymentMethod : merchant.paymentMethods.keySet()) {
            PaymentMethodDescriptor paymentMethodDescriptor =
                    merchant.paymentMethods.get(paymentMethod);
            methodList.setObject()
                .setString(PAYMENT_METHOD_JSON, paymentMethod)
                .setObject(KEY_HASH_JSON, new JSONObjectWriter()
                    .setString(JSONCryptoHelper.ALGORITHM_JSON, 
                               paymentMethodDescriptor.keyHashAlgorithm.getJoseAlgorithmId())
                    .setBinary(JSONCryptoHelper.VALUE_JSON, paymentMethodDescriptor.keyHashValue));
        }
        requestObject.setObject(PAYMENT_REQUEST_JSON, paymentRequest);
        if (MerchantService.noMatchingMethodsUrl != null) {
            requestObject.setString(NO_MATCHING_METHODS_URL_JSON, 
                                    MerchantService.noMatchingMethodsUrl);
        }
        
        if (debugMode) {
            debugData.InvokeWallet = new JSONObjectReader(requestObject);
        }

        // Must keep
        session.setAttribute(WALLET_REQUEST_SESSION_ATTR, requestObject);
    }
}
