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

import org.webpki.saturn.common.BaseProperties;

import org.webpki.webutil.ServletUtil;

import net.glxn.qrgen.QRCode;

import net.glxn.qrgen.image.ImageType;


public class QRDisplayServlet extends HttpServlet implements BaseProperties, MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(QRDisplayServlet.class.getCanonicalName());

    static final String QR_RETURN_TO_SHOP = "r";
    static final String QR_SUCCESS        = "s";
    static final String QR_CONTINUE       = "c";

    public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String id = new String(ServletUtil.getData(request), "UTF-8");
        logger.info("QR DISP=" + id);
        Synchronizer synchronizer = QRSessions.getSynchronizer(id);
        if (synchronizer == null) {
            sendResult(response, QR_RETURN_TO_SHOP);
        } else if (synchronizer.perform(QRSessions.COMET_WAIT)) {
            QRSessions.removeSession(id);
            sendResult(response, QR_SUCCESS);
        } else {
            logger.info("QR Continue");
            sendResult(response, QR_CONTINUE);
        }
    }

    private void sendResult(HttpServletResponse response, String result) throws IOException {
        response.setContentType("text/plain");
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(result.getBytes("UTF-8"));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        String id = QRSessions.createSession();
        session.setAttribute(QR_SESSION_ID_ATTR, id);
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
        HTML.printQRCode(response,
                         QRCode.from("webpki.org="
                                + URLEncoder.encode(
                                        MerchantService.merchantBaseUrl
                                                + "/plugin?" + "=" + id,
                                        "UTF-8")).to(ImageType.PNG)
                               .withSize(200, 200).stream().toByteArray(),
                         request.getRequestURL().toString(),
                         id);
    }
}
