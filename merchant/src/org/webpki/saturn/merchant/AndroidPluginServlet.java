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

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

public class AndroidPluginServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(AndroidPluginServlet.class.getCanonicalName());

    static final String ANDROID_WEBPKI_VERSION_TAG    = "VER";
    static final String ANDROID_WEBPKI_VERSION_MACRO  = "$VER$";

    void doPlugin (String httpSessionId, HttpServletResponse response) throws IOException, ServletException {
        String url = "webpkiproxy://keygen2?cookie=JSESSIONID%3D" + httpSessionId +
               "&url=" + URLEncoder.encode(MerchantService.merchantBaseUrl + "/androidplugin" +
               (MerchantService.grantedVersions == null ? "" : "?" + ANDROID_WEBPKI_VERSION_TAG + "=" + ANDROID_WEBPKI_VERSION_MACRO), "UTF-8");
        HTML.androidPluginActivate(response, url);
    }

    public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        doPlugin(session.getId(), response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String id = request.getParameter(QRSessions.QR_SESSION_ID);
        if (id == null) {
            logger.info(request.getRequestURL().toString());
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.info("nosession");
                ErrorServlet.sessionTimeout(response);
                return;
            }
            logger.info(session.getId());
            JSONObjectWriter ow = new JSONObjectWriter().setString("KI", "BI");
            TransactionServlet.returnJsonData(response,
                    ow.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        } else {
            String httpSessionId = QRSessions.getHttpSessionId(id);
            if (httpSessionId == null) {
                logger.severe("QR session not found");
            } else {
                doPlugin(httpSessionId, response);
            }
        }
    }
}
