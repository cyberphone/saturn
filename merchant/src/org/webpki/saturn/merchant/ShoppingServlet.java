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

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ShoppingServlet extends HttpServlet implements MerchantSessionProperties {

    private static final long serialVersionUID = 1L;
    
    static final String COMMON_NAME = "Space Shop";
    
    static Logger logger = Logger.getLogger(ShoppingServlet.class.getName ());
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        ShoppingCart savedShoppingCart =
            (session.getAttribute(W2NBWalletServlet.SHOPPING_CART_SESSION_ATTR) != null) ?
                (ShoppingCart)session.getAttribute(W2NBWalletServlet.SHOPPING_CART_SESSION_ATTR)
                                       :
                new ShoppingCart();
        session.removeAttribute(W2NBWalletServlet.SHOPPING_CART_SESSION_ATTR);
        HTML.merchantPage(response, savedShoppingCart);
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
        session.setAttribute(MERCHANT_COMMON_NAME_ATTR, MerchantService.ID_DEMO_MERCHANT);
        HTML.merchantPage(response, new ShoppingCart());
    }
}
