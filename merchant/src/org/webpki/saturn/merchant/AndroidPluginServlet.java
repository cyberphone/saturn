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

import java.net.URLEncoder;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.NonDirectPayments;

public class AndroidPluginServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(AndroidPluginServlet.class.getCanonicalName());

    static final String ANDROID_WEBPKI_VERSION_TAG    = "VER";
    static final String ANDROID_WEBPKI_VERSION_MACRO  = "$VER$";

    static final String ANDROID_CANCEL                = "qric";
    static final String QR_SUCCESS_URL                = "local";
    static final String QR_RETRIEVE                   = "qrir";
    
    String getPluginUrl() {
        return MerchantService.merchantBaseUrl + "/androidplugin";
    }

    void doPlugin (String httpSessionId, boolean qrMode, HttpServletResponse response) throws IOException, ServletException {
        String url = "intent://saturn?cookie=JSESSIONID%3D" + httpSessionId +
                       ("&url=" + URLEncoder.encode(getPluginUrl() + (qrMode ? "?" + QR_RETRIEVE + "=true" : "") +
                             (MerchantService.grantedVersions == null ?
                                 ""
                                 :
                             (qrMode ? "&" : "?") + ANDROID_WEBPKI_VERSION_TAG + "=" + ANDROID_WEBPKI_VERSION_MACRO), "UTF-8")) +
                     "#Intent;scheme=webpkiproxy;package=org.webpki.mobile.android;end";
        HTML.androidPluginActivate(response, url);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // The POST method is only called by Saturn Web pay for Android                  //
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        doPlugin(session.getId(), false, response);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // The GET method used for multiple purposes                                     //
    //                                                                               //
    // Note: Most of this slimy and error-prone code would be redundant if Android   //
    // had a useful Web2App concept.                                                 //
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
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
            boolean qrMode = request.getParameter(QR_RETRIEVE) != null;
            logger.info(request.getRequestURL().toString());
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.info("nosession");
                ErrorServlet.sessionTimeout(response);
                return;
            }
            logger.info(session.getId());
            if (session.getAttribute(RESULT_DATA_SESSION_ATTR) != null) {
                ErrorServlet.systemFail(response, "Session already used");
            }
            String versionMacro = request.getParameter(ANDROID_WEBPKI_VERSION_TAG);
            boolean found = false;;
            for (String version : MerchantService.grantedVersions) {
                if (version.equals(versionMacro)) {
                    found = true;
                    break;
                  }
            }
            if (!found) {
                ErrorServlet.systemFail(response, "Wrong version of the WebPKI app, you need to update");
                return;
            }
            String cancelUrl = getPluginUrl() + "?" + ANDROID_CANCEL + "=";
            if (qrMode) {
                cancelUrl = "get:" + cancelUrl + (String) session.getAttribute(QR_SESSION_ID_ATTR);
            }
            String successUrl = qrMode ? QR_SUCCESS_URL
                                       :
                    MerchantService.merchantBaseUrl + "/result";
            String nonDirectPayment = (String)session.getAttribute(GAS_STATION_SESSION_ATTR);
            HttpSupport.writeJsonData(response, 
                                      new WalletRequest(session,
                                                        nonDirectPayment == null ? null : NonDirectPayments.fromType(nonDirectPayment),
                                                        MerchantService.merchantBaseUrl,
                                                        cancelUrl,
                                                        successUrl).requestObject);
        } else {
            String httpSessionId = QRSessions.getHttpSessionId(id);
            if (httpSessionId == null) {
                logger.severe("QR session not found");
                response.sendRedirect(MerchantService.merchantBaseUrl);
            } else {
                Synchronizer synchronizer = QRSessions.getSynchronizer(id);
                if (synchronizer != null) {
                    synchronizer.setInProgress();
                    doPlugin(httpSessionId, true, response);
                }
            }
        }
    }
}
