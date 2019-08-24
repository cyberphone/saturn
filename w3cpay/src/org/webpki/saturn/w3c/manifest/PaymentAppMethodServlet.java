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
package org.webpki.saturn.w3c.manifest;

import java.io.IOException;

import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PaymentAppMethodServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger(PaymentAppMethodServlet.class.getCanonicalName());

    static void outputManifestData(HttpServletResponse response, byte[] manifestData) 
            throws IOException {
        response.setContentType("application/manifest+json");
        response.setContentLength(manifestData.length);
        response.setHeader("Connection", "Close");
        ServletOutputStream os = response.getOutputStream();
        os.write(manifestData);
        os.close();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        logger.info("GET");
        outputManifestData(response, PaymentAppMethodService.appManifest);
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        logger.info("HEAD");
        response.setHeader("Connection", "Close");
        response.setHeader("Link", "<payment-manifest.json>; rel=\"payment-method-manifest\"");
    }
}
