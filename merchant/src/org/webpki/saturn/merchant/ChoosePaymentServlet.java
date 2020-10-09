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

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.BaseProperties;

public class ChoosePaymentServlet extends HttpServlet implements BaseProperties, MerchantSessionProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ChoosePaymentServlet.class.getName());
    
    static boolean getOption(HttpSession session, String name) {
        return session.getAttribute(name) != null && (Boolean)session.getAttribute(name);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        JSONArrayReader ar = JSONParser.parse(request.getParameter(SHOPPING_CART_FORM_ATTR)).getJSONArrayReader();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.products = SpaceProducts.products;  // Needed for receipts
        long total = 0;
        while (ar.hasMore()) {
            JSONObjectReader or = ar.getObject();
            int quantity = or.getInt("quantity");
            if (quantity != 0) {
                String sku = or.getString("sku");
                shoppingCart.items.put(sku, BigDecimal.valueOf(quantity));
                total += quantity * or.getInt53("priceX100");
            }
        }
        shoppingCart.subtotal = total;

        // We add sales tax as well
        shoppingCart.tax = (total * ShoppingCart.TAX) / 100;

        // Then we round up to the nearest 25 centimes, cents, or pennies
        shoppingCart.roundedPaymentAmount = ((shoppingCart.tax + total + 24) / 25) * 25;
        session.setAttribute(SHOPPING_CART_SESSION_ATTR, shoppingCart);

        HTML.userChoosePage(request, response, shoppingCart);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
