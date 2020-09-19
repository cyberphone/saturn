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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.ReceiptDecoder;
import org.webpki.saturn.common.ReceiptEncoder;
import org.webpki.util.ISODateTime;

public class ReceiptServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ReceiptServlet.class.getName());
    
    static class HtmlTable {

        StringBuilder html = new StringBuilder();
        
        
        HtmlTable(String headerText) {
            html.append("<div style='text-align:center;font-weight:bold;margin:20pt auto 10pt auto'>")
                .append(headerText)
                .append("</div>" +
            "<table style='margin-left:auto;margin-right:auto' class='tftable'>");
        }
        
        boolean header = true;
        
        boolean initial = true;
        
        StringBuilder render() {
            return html.append("</table>");
        }
        
        HtmlTable addHeader(String name) {
            if (initial) {
                initial = false;
                html.append("<tr>");
            }
            html.append("<th>")
                .append(name)
                .append("</th>");
            return this;
        }
        
        HtmlTable addCell(String data, String style) {
            if (header) {
                header = false;
                initial = true;
                html.append("</tr>");
            }
            if (initial) {
                initial = false;
                html.append("<tr>");
            }
            html.append(style == null  ? "<td>" : "<td style='" + style + "'>")
                .append(data == null ? "N/A" : data)
                .append("</td>");
            return this;
        }
        
        HtmlTable addCell(String data) {
            return addCell(data, null);
        }
    }

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
            DataBaseOperations.OrderInfo orderInfo = DataBaseOperations.getOrderStatus(orderId);
            if (orderInfo == null || !orderInfo.pathData.equals(pathInfo.substring(17))) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.flushBuffer();
                return;
            }

            ReceiptEncoder receiptEncoder;
            // Receipt URL is valid but that doesn't mean that there is any receipt data...
            if (orderInfo.status == ReceiptDecoder.Status.AVAILABLE) {
                receiptEncoder = DataBaseOperations.getReceiptData(orderId, orderInfo.timeStamp);
            } else {
                receiptEncoder = new ReceiptEncoder(orderInfo.status);
            }

            // Are we rather called by a browser?
            String accept = request.getHeader(HttpSupport.HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(HttpSupport.HTML_CONTENT_TYPE)) {
                ReceiptDecoder receiptDecoder = 
                        new ReceiptDecoder(new JSONObjectReader(receiptEncoder.getReceiptDocument()));
                StringBuilder html = new StringBuilder(AuthorityBaseServlet.TOP_ELEMENT +
                        "<link rel='icon' href='../saturn.png' sizes='192x192'>"+
                        "<title>Receipt</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body>")
                    .append(new HtmlTable("Core Receipt Data")
                            .addHeader("Payee Name")
                            .addHeader("Order Id")
                            .addHeader("Total")
                            .addHeader("Time Stamp")
                            .addCell(receiptDecoder.getPayeeCommonName())
                            .addCell(receiptDecoder.getPayeeReferenceId(), "text-align:right")
                            .addCell(receiptDecoder.getCurrency()
                                    .amountToDisplayString(receiptDecoder.getAmount(), false))
                            .addCell(ISODateTime.formatDateTime(receiptDecoder.getPayeeTimeStamp(),
                                    ISODateTime.UTC_NO_SUBSECONDS))
                            .render());
                html.append(new HtmlTable("Payment Provider Details")
                            .addHeader("Provider Name")
                            .addHeader("Account Type")
                            .addHeader("Account Id")
                            .addHeader("Transaction Id")
                            .addHeader("Time Stamp")
                            .addCell(receiptDecoder.getProviderCommonName())
                            .addCell(receiptDecoder.getPaymentMethodName())
                            .addCell(receiptDecoder.getOptionalAccountReference())
                           .addCell(receiptDecoder.getProviderReferenceId(), "text-align:right")
                            .addCell(ISODateTime.formatDateTime(receiptDecoder.getProviderTimeStamp(),
                                                                ISODateTime.UTC_NO_SUBSECONDS))
                            .render());
                HttpSupport.writeHtml(response, html.append("</body></html>"));
            } else {
                HttpSupport.writeJsonData(response, receiptEncoder.getReceiptDocument());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.flushBuffer();
        }
    }
}
