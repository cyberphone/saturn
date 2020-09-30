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
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.PayeeAuthorityDecoder;
import org.webpki.saturn.common.ProviderAuthorityDecoder;
import org.webpki.saturn.common.ReceiptDecoder;
import org.webpki.saturn.common.ReceiptEncoder;
import org.webpki.saturn.common.TimeUtils;
import org.webpki.saturn.common.UrlHolder;

public class ReceiptServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW_AS_JSON = "json";
    
    static Logger logger = Logger.getLogger(ReceiptServlet.class.getName());
    
    static class HtmlTable {

        StringBuilder html = new StringBuilder();
        
        
        HtmlTable(String headerText) {
            html.append("<div class='tableheader'>")
                .append(headerText)
                .append("</div>" +
            "<table class='tftable'>");
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
    
    void buildHtmlReceipt(StringBuilder html, 
                          ReceiptDecoder receiptDecoder,
                          HttpServletRequest request) throws IOException {
        PayeeAuthorityDecoder payeeAuthority = 
                MerchantService.externalCalls.getPayeeAuthority(
                        new UrlHolder(request).setUrl(receiptDecoder.getPayeeAuthorityUrl()),
                        receiptDecoder.getPayeeAuthorityUrl());
            ProviderAuthorityDecoder providerAuthority = 
                MerchantService.externalCalls.getProviderAuthority(
                        new UrlHolder(request).setUrl(receiptDecoder.getProviderAuthorityUrl()),
                        receiptDecoder.getProviderAuthorityUrl());
            html.append(AuthorityBaseServlet.addLogotype(
                    payeeAuthority.getPayeeCoreProperties().getLogotypeUrl(),
                    payeeAuthority.getPayeeCoreProperties().getCommonName()));
            if (receiptDecoder.getOptionalPhysicalAddress() != null) {
                html.append("<div class='para'>");
                boolean next = false;
                for (String addressLine : receiptDecoder.getOptionalPhysicalAddress()) {
                    if (next) {
                        html.append("<br>");
                    }
                    next = true;
                    html.append(addressLine);
                }
                html.append("</div>");
            }
            if (receiptDecoder.getOptionalPhoneNumber() != null ||
                receiptDecoder.getOptionalEmailAddress() != null) {
                html.append("<div class='para'>");
                if (receiptDecoder.getOptionalPhoneNumber() != null) {
                    html.append("<i>Phone</i>: ")
                        .append(receiptDecoder.getOptionalPhoneNumber());
                }
                if (receiptDecoder.getOptionalEmailAddress() != null) {
                    if (receiptDecoder.getOptionalPhoneNumber() != null) {
                        html.append("<br>");
                    }
                    html.append("<i>e-mail</i>: ")
                        .append(receiptDecoder.getOptionalEmailAddress());
                }
                html.append("</div>");
            }
            html.append(new HtmlTable("Core Receipt Data")
                        .addHeader("Payee Name")
                        .addHeader("Reference Id")
                        .addHeader("Total")
                        .addHeader("Time Stamp")
                        .addCell("<a href='" + 
                                 payeeAuthority.getPayeeCoreProperties().getHomePage() + 
                                "'>" + receiptDecoder.getPayeeCommonName() + "</a>")
                        .addCell(receiptDecoder.getPayeeReferenceId(), "text-align:right")
                        .addCell(receiptDecoder.getCurrency()
                                .amountToDisplayString(receiptDecoder.getAmount(), false))
                        .addCell(TimeUtils.displayUtcTime(receiptDecoder.getPayeeTimeStamp()))
                        .render());
            html.append(new HtmlTable("Payment Details")
                        .addHeader("Provider Name")
                        .addHeader("Account Type")
                        .addHeader("Account Id")
                        .addHeader("Transaction Id")
                        .addHeader("Request Id")
                        .addHeader("Time Stamp")
                        .addCell("<a href='" + 
                                 providerAuthority.getHomePage() + 
                                "'>" + receiptDecoder.getProviderCommonName() + "</a>")
                        .addCell(receiptDecoder.getPaymentMethodName())
                        .addCell(receiptDecoder.getOptionalAccountReference())
                        .addCell(receiptDecoder.getProviderReferenceId())
                        .addCell(receiptDecoder.getPayeeRequestId())
                        .addCell(TimeUtils.displayUtcTime(receiptDecoder.getProviderTimeStamp()))
                        .render());
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

            byte[] receipt;
            // Receipt URL is valid but that doesn't mean that there is any receipt data...
            if (orderInfo.status == ReceiptDecoder.Status.AVAILABLE) {
                receipt = DataBaseOperations.getReceiptData(orderId);
            } else {
                receipt = new ReceiptEncoder(orderInfo.status).getReceiptDocument()
                                .serializeToBytes(JSONOutputFormats.NORMALIZED);
            }

            // Are we rather called by a browser?
            String accept = request.getHeader(HttpSupport.HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(HttpSupport.HTML_CONTENT_TYPE)) {
//TODO HTML "cleaning"
                StringBuilder html = new StringBuilder(AuthorityBaseServlet.TOP_ELEMENT +
                        "<link rel='icon' href='../saturn.png' sizes='192x192'>"+
                        "<title>Receipt</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body>");
                ReceiptDecoder receiptDecoder = new ReceiptDecoder(JSONParser.parse(receipt));
                if (request.getParameter(VIEW_AS_JSON) == null) {
                    html.append("<div class='header'>Customer Receipt</div>");
                    if (receiptDecoder.getStatus() == ReceiptDecoder.Status.AVAILABLE) {
                        buildHtmlReceipt(html, receiptDecoder, request);
                    } else {
                        html.append("<i>Status</i>: ")
                            .append(receiptDecoder.getStatus().toString());
                    }
                    html.append("<div>&nbsp;<br><a href='")
                        .append(pathInfo.substring(1))
                        .append("?" + VIEW_AS_JSON)
                        .append("'>Switch to JSON view</a></div>");
                } else {
                    html.append("<div class='header'>Receipt in JSON format</div><div class='json'>")
                        .append(JSONParser.parse(receipt)
                                .serializeToString(JSONOutputFormats.PRETTY_HTML))
                        .append("</div><div>&nbsp;<br><a href='")
                        .append(pathInfo.substring(1))
                        .append("'>Switch to normal view</a></div>");
                }
                HttpSupport.writeHtml(response, html.append("</body></html>"));
            } else {
                HttpSupport.writeData(response, receipt, BaseProperties.JSON_CONTENT_TYPE);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.flushBuffer();
        }
    }
}
