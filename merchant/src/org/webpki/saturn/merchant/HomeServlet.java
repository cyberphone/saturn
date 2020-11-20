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

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class HomeServlet extends HttpServlet implements MerchantSessionProperties {

    private static final long serialVersionUID = 1L;

    static final String GOTO_URL = "gotoUrl";

    boolean isTapConnect() {
        return false;
    }
    
    static boolean getOption(HttpSession session, String name) {
        return session.getAttribute(name) != null && (Boolean)session.getAttribute(name);
    }

    static boolean browserIsSupported(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userAgent = request.getHeader("User-Agent");
        boolean notOk = false;
        if (userAgent.contains("Android ")) {
            notOk = true;
            int i = userAgent.indexOf(" Chrome/");
            if (i > 0) {
                String chromeVersion = userAgent.substring(i + 8, userAgent.indexOf('.', i));
                if (Integer.parseInt(chromeVersion) >= MerchantService.androidChromeVersion) {
                    if (!userAgent.contains("; wv)")) {
                        notOk = false;
                    }
                }
            }
        }
        if (notOk) {
            ErrorServlet.systemFail(response, "This proof-of-concept system requires \"Chrome\" (min version: " + 
                                              MerchantService.androidChromeVersion + ") when using Android");
            return false;
        }
        if (userAgent.contains(" Chrome/") ||
            userAgent.contains(" Edge/") ||
            userAgent.contains(" Safari/") ||
            userAgent.contains(" Firefox/")) {
            return true;
        }
        ErrorServlet.systemFail(response, "This proof-of-concept application only supports Chrome/Chromium, Safari, Edge and Firefox");
        return false;
    }
    
    static boolean isAndroid(HttpServletRequest request) {
        return request.getHeader("User-Agent").contains("Android");
    }

    boolean checkBoxGet(HttpSession session, String name) {
        boolean argument = false;
        if (session.getAttribute(name) == null) {
            session.setAttribute(name, argument);
        } else {
            argument = (Boolean) session.getAttribute(name);
        }
        return argument;
    }
    
    void checkBoxSet(HttpSession session, HttpServletRequest request, String name) {
        session.setAttribute(name, request.getParameter(name) != null);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        HTML.homePage(response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        checkBoxSet(session, request, DEBUG_MODE_SESSION_ATTR);
        checkBoxSet(session, request, REFUND_MODE_SESSION_ATTR);
        session.setAttribute(TAP_CONNECT_MODE_SESSION_ATTR, isTapConnect());
        response.sendRedirect(request.getParameter(GOTO_URL));
    }
}
