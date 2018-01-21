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

import org.webpki.webutil.ServletUtil;

import net.glxn.qrgen.QRCode;

import net.glxn.qrgen.image.ImageType;


public class QRDisplayServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(QRDisplayServlet.class.getCanonicalName());

    public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String id = new String(ServletUtil.getData(request), "UTF-8");
        if (MerchantService.logging) {
            logger.info("QR DISP=" + id);
        }
        Synchronizer synchronizer = QRSessions.getSynchronizer(id);
        if (synchronizer == null) {
            sendResult(response, QRSessions.QR_RETURN);
        } else if (synchronizer.perform(QRSessions.COMET_WAIT)) {
            QRSessions.removeSession(id);
            sendResult(response, QRSessions.QR_SUCCESS);
        } else {
            if (MerchantService.logging) {
                logger.info("QR Continue");
            }
            sendResult(response, synchronizer.isInProgress() ? QRSessions.QR_PROGRESS : QRSessions.QR_CONTINUE);
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
        String id = QRSessions.createSession(session);
        session.setAttribute(QR_SESSION_ID_ATTR, id);
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
        String url = "webpki.org=" + URLEncoder.encode(MerchantService.merchantBaseUrl +
                                                         "/androidplugin?" + QRSessions.QR_SESSION_ID  + "=" + id,
                                                       "UTF-8");
        logger.info("URL=" + url + " SID=" + session.getId());
        byte[] qrImage = QRCode.from(url).to(ImageType.PNG).withSize(200, 200).stream().toByteArray();

        if (session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {
            SavedShoppingCart savedShoppingCart = (SavedShoppingCart) session.getAttribute(SHOPPING_CART_SESSION_ATTR);
            if (savedShoppingCart == null) {
                ErrorServlet.systemFail(response, "Missing shopping cart");
                return;
            }
            HTML.printQRCode4Shop(response, savedShoppingCart, qrImage, request, id);
        } else {
            HTML.printQRCode4GasStation(response, qrImage, request, id);
        }
    }
}
