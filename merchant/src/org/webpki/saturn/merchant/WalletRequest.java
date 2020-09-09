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

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpSession;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.TimeUtils;
import org.webpki.saturn.common.NonDirectPaymentEncoder;
import org.webpki.saturn.common.PaymentRequestEncoder;
import org.webpki.saturn.common.PaymentClientRequestEncoder;

import org.webpki.saturn.common.PaymentClientRequestEncoder.SupportedPaymentMethod;

public class WalletRequest implements BaseProperties, MerchantSessionProperties {

    boolean debugMode;
    SavedShoppingCart savedShoppingCart;
    JSONObjectWriter requestObject;
    JSONObjectWriter paymentRequest;
    String receiptUrl;
    
    private static int receiptNumber;
    private static String currentDate = ""; 

    WalletRequest(HttpSession session,
                  NonDirectPaymentEncoder optionalNonDirectPayment) throws IOException {
        debugMode = HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        savedShoppingCart = (SavedShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);
        MerchantDescriptor merchant = MerchantService.getMerchant(session);

        // Create a payment request
        paymentRequest =
            PaymentRequestEncoder.encode(merchant.commonName, 
                                         merchant.homePage,
                                         new BigDecimal(
                                             BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount),
                                             MerchantService.currency.getDecimals()),
                                         MerchantService.currency,
                                         optionalNonDirectPayment,
                                         merchant.getReferenceId(),
                                         new GregorianCalendar(),
                                         TimeUtils.inMinutes(30));
        
        List<SupportedPaymentMethod> supportedPaymentMethods = new ArrayList<>();
        for (String paymentMethod : merchant.paymentMethods.keySet()) {
            PaymentMethodDescriptor paymentMethodDescriptor =
                    merchant.paymentMethods.get(paymentMethod);
            supportedPaymentMethods.add(
                    new SupportedPaymentMethod(paymentMethod,
                                               paymentMethodDescriptor.authorityUrl));
        }
        if (true) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String yyyyMmDd = sdf.format(System.currentTimeMillis());
            if (!yyyyMmDd.equals(currentDate)) {
                currentDate = yyyyMmDd;
                receiptNumber = 0;
            }
            receiptUrl = MerchantService.merchantBaseUrl + 
                             "/receipts/" +
                             yyyyMmDd +
                             "." + (++receiptNumber);
        }

        requestObject = PaymentClientRequestEncoder.encode(supportedPaymentMethods,
                                                           receiptUrl,
                                                           paymentRequest,
                                                           MerchantService.noMatchingMethodsUrl); 
        
        if (debugMode) {
            debugData.InvokeWallet = new JSONObjectReader(requestObject);
        }

        // Must keep
        session.setAttribute(WALLET_REQUEST_SESSION_ATTR, this);
    }
}
