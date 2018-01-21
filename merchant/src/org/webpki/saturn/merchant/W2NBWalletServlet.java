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
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONOutputFormats;

public class W2NBWalletServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
   
    static Logger logger = Logger.getLogger(W2NBWalletServlet.class.getName());
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
        }
        String userAgent = request.getHeader("User-Agent");
        WalletRequest walletRequest = new WalletRequest(session, null);
        HTML.w2nbWalletPay(response,
                           userAgent.contains("Mozilla/") && userAgent.contains(" Firefox/"),
                           walletRequest.savedShoppingCart,
                           HomeServlet.getOption(session, TAP_CONNECT_MODE_SESSION_ATTR),
                           walletRequest.debugMode,
                           walletRequest.requestObject.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE));
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
