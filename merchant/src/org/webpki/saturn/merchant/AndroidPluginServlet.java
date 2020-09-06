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

import java.net.URLEncoder;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.saturn.common.MobileProxyParameters;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.NonDirectPaymentEncoder;

import org.webpki.json.JSONObjectWriter;

public class AndroidPluginServlet extends HttpServlet implements MerchantSessionProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(AndroidPluginServlet.class.getCanonicalName());

    static final String ANDROID_CANCEL                = "qric";
    static final String QR_SUCCESS_URL                = "local";

    ////////////////////////////////////////////////////////////////////////////////////////////
    // The following is the actual contract between an issuing server and a Saturn client.
    // The PUP_INIT_URL argument bootstraps the protocol via an HTTP GET
    ////////////////////////////////////////////////////////////////////////////////////////////
    static String getInvocationUrl(String scheme, String httpSessionId, String qrSessionId)
    throws IOException {
        String encodedUrl = URLEncoder.encode(MerchantService.merchantBaseUrl, "utf-8");
        String cancelUrl = encodedUrl + "%2Fandroidplugin%3F" + ANDROID_CANCEL + "%3D";
        if (qrSessionId != null) {
            cancelUrl += qrSessionId;
        }
        return scheme + "://" + MobileProxyParameters.HOST_SATURN +
               "?" + MobileProxyParameters.PUP_COOKIE     + "=" + "JSESSIONID%3D" + httpSessionId +
               "&" + MobileProxyParameters.PUP_INIT_URL   + "=" + encodedUrl + "%2Fandroidplugin" +
               "&" + MobileProxyParameters.PUP_MAIN_URL   + "=" + encodedUrl + "%2Fauthorize" + 
               "&" + MobileProxyParameters.PUP_CANCEL_URL + "=" + cancelUrl +
               "&" + MobileProxyParameters.PUP_VERSIONS   + "=" + MerchantService.androidWebPkiVersions;
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // The POST method is only called by Saturn Web pay for Android using URLhandler //
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        HTML.androidPluginActivate(response,
                                   getInvocationUrl(MobileProxyParameters.SCHEME_URLHANDLER, session.getId(), null) +
                                       "#Intent;scheme=webpkiproxy;package=" +
                                       MobileProxyParameters.ANDROID_PACKAGE_NAME +
                                       ";end");
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // The GET method used for multiple purposes                                     //
    //                                                                               //
    // Note: Most of this slimy and error-prone code is redundant when Android is    //
    // using the W3C PaymentRequest API.                                             //
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (MerchantService.logging) {
            logger.info("GET from: " + request.getRemoteAddr());
        }
        String id = request.getParameter(ANDROID_CANCEL);
        if (id != null) {
            if (id.isEmpty()) {
                HttpSession session = request.getSession(false);
                if (session == null) {
                    ErrorServlet.sessionTimeout(response);
                    return;
                }
                // When user clicks "Cancel" in App mode we must return to
                // the shop using a POST operation
                HTML.autoPost(response, MerchantService.merchantBaseUrl + "/shop");
            } else {
                // When user clicks "Cancel" in QR mode we must cancel the operation
                // at the merchant side and return a suitable page to the QR client
                QRSessions.cancelSession(id);
            }
            return;
        }

        id = request.getParameter(QRSessions.QR_SESSION_ID);
        if (id == null) {
            try {
                MerchantService.slowOperationSimulator();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            // Here we assume that we are being called from the Android client trying
            // to retrieve the payment request
//            logger.info(request.getRequestURL().toString());
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.info("nosession");
                ErrorServlet.sessionTimeout(response);
                return;
            }
//            logger.info(session.getId());
            if (session.getAttribute(RESULT_DATA_SESSION_ATTR) != null) {
                ErrorServlet.systemFail(response, "Session already used");
            }

            NonDirectPaymentEncoder optionalNonDirectPayment = 
                    (NonDirectPaymentEncoder)session.getAttribute(GAS_STATION_SESSION_ATTR);

            JSONObjectWriter walletRequest = 
                    new WalletRequest(session, optionalNonDirectPayment).requestObject;
            if (MerchantService.logging) {
                logger.info("Sent to wallet:\n" + walletRequest.toString());
            }
            HttpSupport.writeJsonData(response, walletRequest);
        } else {
            String httpSessionId = QRSessions.getHttpSessionId(id);
            if (httpSessionId == null) {
                logger.severe("QR session not found");
                response.sendRedirect(MerchantService.merchantBaseUrl);
            } else {
                Synchronizer synchronizer = QRSessions.getSynchronizer(id);
                if (synchronizer != null) {
                    synchronizer.setInProgress();
                    HTML.output(response, getInvocationUrl(MobileProxyParameters.SCHEME_QRCODE, httpSessionId, id));
                }
            }
        }
    }
}
