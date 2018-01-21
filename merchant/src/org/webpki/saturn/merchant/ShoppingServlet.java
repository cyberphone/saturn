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

import java.util.LinkedHashMap;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ShoppingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    static final String COMMON_NAME = "Demo Merchant";
    
    static Logger logger = Logger.getLogger(ShoppingServlet.class.getName ());
    
    static LinkedHashMap<String,ProductEntry> products = new LinkedHashMap<String,ProductEntry>();
    
    static {
        products.put("7d688", new ProductEntry("product-car.png", "Sports Car", 8599900)); 
        products.put("90555", new ProductEntry("product-icecream.png", "Ice Cream", 325)); 
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        SavedShoppingCart saved_shopping_cart =
            (session.getAttribute(W2NBWalletServlet.SHOPPING_CART_SESSION_ATTR) != null) ?
                (SavedShoppingCart)session.getAttribute(W2NBWalletServlet.SHOPPING_CART_SESSION_ATTR)
                                       :
                new SavedShoppingCart();
        session.removeAttribute(W2NBWalletServlet.SHOPPING_CART_SESSION_ATTR);
        HTML.merchantPage(response, saved_shopping_cart);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!HomeServlet.browserIsSupported(request, response)) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        HTML.merchantPage(response, new SavedShoppingCart());
    }
}
