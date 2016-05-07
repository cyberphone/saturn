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

import java.util.Vector;

import javax.servlet.http.HttpSession;

import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.RequestHash;

public class WalletRequest implements BaseProperties, MerchantProperties {

    boolean debugMode;
    SavedShoppingCart savedShoppingCart;
    JSONObjectWriter requestObject;
    
    WalletRequest(HttpSession session) throws IOException {
        debugMode = UserPaymentServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        savedShoppingCart = (SavedShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);

        String currReferenceId = MerchantService.getReferenceId();
        JSONObjectWriter paymentRequest =
            PaymentRequest.encode(Payee.init("Demo Merchant","86344"),
                                  new BigDecimal(BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount), 2),
                                  MerchantService.currency,
                                  currReferenceId,
                                  Expires.inMinutes(30),
                                  MerchantService.merchantKey);

        session.setAttribute(REQUEST_HASH_SESSION_ATTR, RequestHash.getRequestHash(paymentRequest));

        // For keeping track of Wallet request
        session.setAttribute(REQUEST_REFID_SESSION_ATTR, currReferenceId);

        Vector<String> acceptedAccountTypes = new Vector<String>();
        for (PayerAccountTypes account : MerchantService.acceptedAccountTypes) {
            acceptedAccountTypes.add(account.getTypeUri());
        }
        acceptedAccountTypes.add("https://nosuchcard.com");
        requestObject = Messages.createBaseMessage(Messages.WALLET_REQUEST)
            .setStringArray(ACCEPTED_ACCOUNT_TYPES_JSON, acceptedAccountTypes.toArray(new String[0]))
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
   
        if (debugMode) {
            debugData.InvokeWallet = requestObject;
        }
    }
}
