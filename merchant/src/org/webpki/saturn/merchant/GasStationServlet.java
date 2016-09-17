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

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.saturn.common.NonDirectPayments;

public class GasStationServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(GasStationServlet.class.getName ());
    
    static final long STANDARD_RESERVATION_AMOUNT_X_100 = 20000;
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        session.setAttribute(GAS_STATION_SESSION_ATTR, NonDirectPayments.GAS_STATION.toString());
        session.setAttribute(RESERVE_MODE_SESSION_ATTR, true);
        SavedShoppingCart savedShoppingCart = new SavedShoppingCart();
        savedShoppingCart.roundedPaymentAmount = STANDARD_RESERVATION_AMOUNT_X_100;
        session.setAttribute(SHOPPING_CART_SESSION_ATTR, savedShoppingCart);
        response.sendRedirect("qrdisplay");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        HTML.gasFillingPage(response);
    }
}
