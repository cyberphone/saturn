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

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class HomeServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    boolean isTapConnect() {
        return false;
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
        if (session != null && session.getAttribute(REQUEST_HASH_SESSION_ATTR) != null) {
            session.invalidate();
        }
        session = request.getSession(true);
        session.setAttribute(TAP_CONNECT_MODE_SESSION_ATTR, isTapConnect());
        HTML.homePage(response,
                      checkBoxGet(session, DEBUG_MODE_SESSION_ATTR),
                      checkBoxGet(session, RESERVE_MODE_SESSION_ATTR));
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        checkBoxSet(session, request, DEBUG_MODE_SESSION_ATTR);
        checkBoxSet(session, request, RESERVE_MODE_SESSION_ATTR);
        response.sendRedirect("home");
    }
}
