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

import java.net.URLEncoder;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONOutputFormats;

public class AndroidPluginServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(AndroidPluginServlet.class.getCanonicalName());

    static final String ANDROID_WEBPKI_VERSION_TAG    = "VER";
    static final String ANDROID_WEBPKI_VERSION_MACRO  = "$VER$";

    static final String ANDROID_CANCEL                = "qric";
    static final String QR_ANDROID_SUCCESS            = "qris";
    static final String QR_RETRIEVE                   = "qrir";
    
    String getPluginUrl() {
        return MerchantService.merchantBaseUrl + "/androidplugin";
    }

    void doPlugin (String httpSessionId, boolean qrMode, HttpServletResponse response) throws IOException, ServletException {
        String url = "webpkiproxy://saturn?cookie=JSESSIONID%3D" + httpSessionId +
               "&url=" + URLEncoder.encode(getPluginUrl() + (qrMode ? "?" + QR_RETRIEVE + "=true" : "") +
                     (MerchantService.grantedVersions == null ?
                         ""
                         :
                     (qrMode ? "&" : "?") + ANDROID_WEBPKI_VERSION_TAG + "=" + ANDROID_WEBPKI_VERSION_MACRO), "UTF-8");
        HTML.androidPluginActivate(response, url);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // The POST method is only called by Saturn Web pay for Android                  //
    ///////////////////////////////////////////////////////////////////////////////////
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
    ///////////////////////////////////////////////////////////////////////////////////
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (request.getParameter(QR_ANDROID_SUCCESS) != null) {
            // If the Wallet in QR mode receives success it should call this URL
            HTML.qrClientResult(response, true);
            return;
        }
        
        String id = request.getParameter(ANDROID_CANCEL);
        if (id != null) {
            if (id.isEmpty()) {
                // When user clicks "Cancel" in App mode we must return to
                // the shop using a POST operation
                HTML.autoPost(response, MerchantService.merchantBaseUrl + "/shop");
            } else {
                // When user clicks "Cancel" in QR mode we must cancel the operation
                // at the merchant side and return a suitable page to the QR client
                QRSessions.cancelSession(id);
                HTML.qrClientResult(response, false);
            }
            return;
        }

        id = request.getParameter(QRSessions.QR_SESSION_ID);
        if (id == null) {
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
            String cancelUrl = getPluginUrl() + "?" + ANDROID_CANCEL + "=" + 
                    (qrMode ? (String) session.getAttribute(QR_SESSION_ID_ATTR) : "");
            String successUrl = qrMode ?
                    getPluginUrl() + "?" + QR_ANDROID_SUCCESS + "=true"
                                       :
                    MerchantService.merchantBaseUrl + "/result";
            TransactionServlet.returnJsonData(response, new WalletRequest(session, 
                                                                          cancelUrl,
                                                                          successUrl).requestObject);
        } else {
            String httpSessionId = QRSessions.getHttpSessionId(id);
            if (httpSessionId == null) {
                logger.severe("QR session not found");
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