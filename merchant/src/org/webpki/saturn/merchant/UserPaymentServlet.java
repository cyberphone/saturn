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

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.RequestHash;

public class UserPaymentServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static final String REQUEST_HASH_SESSION_ATTR  = "REQHASH";
    static final String REQUEST_REFID_SESSION_ATTR = "REQREFID";
    static final String DEBUG_DATA_SESSION_ATTR    = "DBGDATA";
    static final String SHOPPING_CART_SESSION_ATTR = "SHOPCART";

    
    static final String AUTHDATA_FORM_ATTR         = "authdata";
    static final String INITMSG_FORM_ATTR          = "initmsg";
    static final String SHOPPING_CART_FORM_ATTR    = "shopcart";
    
    static Logger logger = Logger.getLogger(UserPaymentServlet.class.getName());
    
    static boolean getOption(HttpSession session, String name) {
        return session.getAttribute(name) != null && (Boolean)session.getAttribute(name);
    }

    static int referenceId = 1000000;
    
    static String getReferenceId() {
        return "#" + (referenceId++);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        JSONArrayReader ar = JSONParser.parse(request.getParameter(SHOPPING_CART_FORM_ATTR)).getJSONArrayReader();
        SavedShoppingCart savedShoppingCart = new SavedShoppingCart();
        long total = 0;
        while (ar.hasMore()) {
            JSONObjectReader or = ar.getObject();
            int quantity = or.getInt("quantity");
            if (quantity != 0) {
                String sku = or.getString("sku");
                savedShoppingCart.items.put(sku, quantity);
                logger.info("SKU=" + sku + " Quantity=" + quantity);
                total += quantity * or.getLong("priceX100");
            }
        }
        savedShoppingCart.total = total;

        // We add a fictitious 10% sales tax as well
        savedShoppingCart.tax = total / 10;

        // Then we round up to the nearest 25 centimes, cents, or pennies
        savedShoppingCart.roundedPaymentAmount = ((savedShoppingCart.tax + total + 24) / 25) * 25;
        boolean debugMode = getOption(session, HomeServlet.DEBUG_MODE_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        session.setAttribute(SHOPPING_CART_SESSION_ATTR, savedShoppingCart);

        String currReferenceId = getReferenceId();
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
        JSONObjectWriter invokeRequest = Messages.createBaseMessage(Messages.WALLET_REQUEST)
            .setStringArray(ACCEPTED_ACCOUNT_TYPES_JSON, acceptedAccountTypes.toArray(new String[0]))
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
   
        if (debugMode) {
            debugData.InvokeWallet = invokeRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        }
        
        HTML.userPayPage(response,
                         savedShoppingCart,
                         getOption(session, HomeServlet.TAP_CONNECT_MODE_SESSION_ATTR),
                         debugMode,
                         new String(invokeRequest.serializeJSONObject(JSONOutputFormats.PRETTY_JS_NATIVE), "UTF-8"));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
