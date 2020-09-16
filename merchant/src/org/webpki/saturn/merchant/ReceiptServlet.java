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

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.ReceiptDecoder;
import org.webpki.saturn.common.ReceiptEncoder;

public class ReceiptServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ReceiptServlet.class.getName());

    public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException {
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo.length() != 39) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.flushBuffer();
                return;
            }
            String orderId = pathInfo.substring(1, 17);
            DataBaseOperations.ReceiptInfo receiptStatus = DataBaseOperations.getReceiptStatus(orderId);
            if (receiptStatus == null || !receiptStatus.pathData.equals(pathInfo.substring(17))) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.flushBuffer();
                return;
            }

            ReceiptEncoder receipt;
            // Receipt URL is valid but that doesn't mean that there is any receipt data...
            if (receiptStatus.status == ReceiptDecoder.AVAILABLE) {
                receipt = DataBaseOperations.getReceiptData(orderId);
            } else {
                receipt = new ReceiptEncoder(receiptStatus.status);
            }

            // Are we rather called by a browser?
            String accept = request.getHeader(HttpSupport.HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(HttpSupport.HTML_CONTENT_TYPE)) {

                HTML.debugPage(response, receipt.getReceiptDocument()
                                         .serializeToString(JSONOutputFormats.PRETTY_HTML), false);
            } else {
                HttpSupport.writeJsonData(response, receipt.getReceiptDocument());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.flushBuffer();
        }
    }
}
