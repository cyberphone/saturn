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

public class ErrorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ErrorServlet.class.getName());

    private static final String ERROR="Error";
    private static final String SYSTEM="System";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HTML.errorPage(response, request.getParameter(ERROR), request.getParameter(SYSTEM) != null);
    }

    private static void fail(HttpServletResponse response, String message, boolean system) throws IOException {
        logger.info(message);
        response.sendRedirect(MerchantService.merchantBaseUrl + "/error?" + ERROR + "=" + URLEncoder.encode(message, "UTF-8") +
                              (system ? "&" + SYSTEM + "=true" : ""));
    }

    public static void sessionTimeout(HttpServletResponse response) throws IOException {
        fail(response, "Session timed out", false);
    }

    static void systemFail(HttpServletResponse response, String message) throws IOException {
        fail(response, message, true);
    }
}
