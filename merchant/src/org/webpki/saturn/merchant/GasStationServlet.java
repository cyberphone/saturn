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

import org.webpki.saturn.common.NonDirectPaymentEncoder;
import org.webpki.saturn.common.ReservationSubTypes;
import org.webpki.saturn.common.TimeUtils;

public class GasStationServlet extends HttpServlet implements MerchantSessionProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(GasStationServlet.class.getName ());
    
    static final int STANDARD_RESERVATION_AMOUNT_X_100 = 20000;
    
    static final int ROUND_UP_FACTOR_X_10               = 50;  // 5 cents

    static final String FUEL_TYPE_FIELD                 = "fueltype";
    static final String FUEL_DECILITRE_FIELD            = "fueldeci";
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        if (HomeServlet.isAndroid(request)) {
            HTML.notification(response, "This application is supposed to be invoked from a <i>desktop</i> browser");
            return;
        }
        if (!HomeServlet.browserIsSupported(request, response)) {
            return;
        }
        session.setAttribute(GAS_STATION_SESSION_ATTR,
                             NonDirectPaymentEncoder.reservation(ReservationSubTypes.GAS_STATION,
                                                                 TimeUtils.inMinutes(45),
                                                                 true));
        session.setAttribute(MERCHANT_COMMON_NAME_ATTR, MerchantService.ID_PLANET_GAS);
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.products = FuelTypes.products;  // Needed for receipts
        shoppingCart.roundedPaymentAmount = STANDARD_RESERVATION_AMOUNT_X_100;
        session.setAttribute(SHOPPING_CART_SESSION_ATTR, shoppingCart);
        response.sendRedirect("qrdisplay");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        FuelTypes fuelType = (FuelTypes) FuelTypes.products
                .get(request.getParameter(FUEL_TYPE_FIELD));
        int maxVolumeInDecilitres = (STANDARD_RESERVATION_AMOUNT_X_100 * 10) / fuelType.pricePerLitreX100;
        int priceX1000 = fuelType.pricePerLitreX100 * maxVolumeInDecilitres;
        if (priceX1000 % GasStationServlet.ROUND_UP_FACTOR_X_10 != 0) {
            maxVolumeInDecilitres--;
        }
        ShoppingCart savedShoppingCart = 
                (ShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);
        savedShoppingCart.items.put(fuelType.toString(), BigDecimal.ZERO);
        HTML.gasFillingPage(response, fuelType, maxVolumeInDecilitres);
    }
}
