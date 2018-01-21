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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.saturn.common.UrlHolder;

//////////////////////////////////////////////////////////////////////////
// This servlet shows the result of a transaction to the user           //
//////////////////////////////////////////////////////////////////////////

public class ResultServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ResultServlet.class.getCanonicalName());
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        ResultData resultData = (ResultData) session.getAttribute(RESULT_DATA_SESSION_ATTR);
        if (resultData == null) {
            ErrorServlet.systemFail(response, "Missing result data");
            return;
        }
        HTML.shopResultPage(response,
                            HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR),
                            resultData);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = new UrlHolder(request);
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        ResultData resultData = (ResultData) session.getAttribute(RESULT_DATA_SESSION_ATTR);
        if (resultData == null) {
            ErrorServlet.systemFail(response, "Missing result data");
            return;
        }
        TransactionOperation reservation = (TransactionOperation)session.getAttribute(GAS_STATION_RES_SESSION_ATTR);
        if (reservation == null) {
            ErrorServlet.systemFail(response, "Missing reservation object");
            return;
        }
        try {
            int decilitres = Integer.parseInt(request.getParameter(GasStationServlet.FUEL_DECILITRE_FIELD));
            FuelTypes fuelType = FuelTypes.valueOf(FuelTypes.class, request.getParameter(GasStationServlet.FUEL_TYPE_FIELD));
            int priceX1000 = fuelType.pricePerLitreX100 * decilitres;
            int upround = priceX1000 % GasStationServlet.ROUND_UP_FACTOR_X_10;
            if (upround != 0) {
                priceX1000 += GasStationServlet.ROUND_UP_FACTOR_X_10 - upround;
            }
            BigDecimal actualAmount = new BigDecimal(priceX1000).divide(new BigDecimal(1000));
            resultData.amount = actualAmount;
            DebugData debugData = (DebugData) session.getAttribute(DEBUG_DATA_SESSION_ATTR);
            resultData.transactionError = AuthorizationServlet.processTransaction(reservation,
                                                                                  actualAmount,
                                                                                  urlHolder,
                                                                                  debugData);
            HTML.gasStationResultPage(response,
                                      fuelType,
                                      decilitres,
                                      debugData != null,
                                      resultData);
        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            ErrorServlet.systemFail(response, "An unexpected error occurred.<br>Please try again or contact support.");
        }
    }
}
