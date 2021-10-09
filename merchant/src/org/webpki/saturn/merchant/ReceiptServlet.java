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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

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
import org.webpki.saturn.common.ReceiptEncoder;
import org.webpki.saturn.common.ReceiptDecoder;
import org.webpki.saturn.common.ReceiptLineItem;
import org.webpki.saturn.common.ReceiptBarcode;
import org.webpki.saturn.common.ReceiptShippingRecord;
import org.webpki.saturn.common.ReceiptTaxRecord;
import org.webpki.saturn.common.TimeUtils;
import org.webpki.saturn.common.UrlHolder;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;

import com.google.zxing.client.j2se.MatrixToImageWriter;

import com.google.zxing.common.BitMatrix;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ReceiptServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW_AS_JSON = "json";
    
    static Logger logger = Logger.getLogger(ReceiptServlet.class.getName());
    
    String optional(Object o) {
        return o == null ? "" : o.toString();
    }

    static final HashMap<ReceiptBarcode.BarcodeTypes, BarcodeFormat> saturn2Xzing = new HashMap<>();
    
    static {
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.UPC_A, BarcodeFormat.UPC_A);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.UPC_E, BarcodeFormat.UPC_E);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.EAN_8, BarcodeFormat.EAN_8);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.EAN_13, BarcodeFormat.EAN_13);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.UPC_EAN_EXTENSION, BarcodeFormat.UPC_EAN_EXTENSION);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.CODE_39, BarcodeFormat.CODE_39);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.CODE_93, BarcodeFormat.CODE_93);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.CODE_128, BarcodeFormat.CODE_128);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.CODABAR, BarcodeFormat.CODABAR);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.ITF, BarcodeFormat.ITF);
        saturn2Xzing.put(ReceiptBarcode.BarcodeTypes.QR_CODE, BarcodeFormat.QR_CODE);
    }
    
    static final Map<EncodeHintType, Object> xzingHints = new EnumMap<>(EncodeHintType.class);
    
    static {
        xzingHints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        xzingHints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
     }

    static String showMoney(ReceiptDecoder receiptDecoder, BigDecimal amount) throws IOException {
        return amount == null ? "N/A" : receiptDecoder.getCurrency().plainAmountString(amount) +
                                            " " +
                                            receiptDecoder.getCurrency().toString();
    }
    
    static String showLines(String[] description) {
        StringBuilder lines = new StringBuilder();
        boolean next = false;
        for (String line : description) {
            if (next) {
                lines.append("<br>");
            } else {
                next = true;
            }
            lines.append(line);
        }
        return lines.toString();
    }

    static class HtmlTable {
        
        static final String RIGHT_ALIGN = "text-align:right";

        StringBuilder html = new StringBuilder();
        
        
        HtmlTable(String headerText) {
            html.append("<div style='text-align:center' class='spacepara'>")
                .append(headerText)
                .append("</div>" +
            "<div style='overflow-x:auto'>" + 
            "<table style='margin-left:auto;margin-right:auto' class='tftable'><tr>");
        }
        
        boolean headerMode = true;
        
        int headerCount;
        
        int cellCount;
        
        StringBuilder render() {
            return html.append("</table></div>");
        }
        
        HtmlTable addHeader(String name) {
            html.append("<th>")
                .append(name)
                .append("</th>");
            headerCount++;
            return this;
        }
        
        HtmlTable addCell(String data, String style) {
            if (headerMode) {
                headerMode = false;
                html.append("</tr>");
            }
            if (cellCount++ % headerCount == 0) {
                html.append("<tr>");
            }
            html.append(style == null  ? "<td>" : "<td style='" + style + "'>")
                .append(data == null ? "N/A" : data)
                .append("</td>");
            if (cellCount % headerCount == 0) {
                html.append("</tr>");
            }
            return this;
        }
        
        HtmlTable addCell(String data) {
            return addCell(data, null);
        }
    }
    
    StringBuilder printBarcode(ReceiptBarcode barcode) throws WriterException, IOException {
        StringBuilder html = new StringBuilder("<div style='margin:2em auto 0 auto;width:20em'>");
        BarcodeFormat xzingFormat = saturn2Xzing.get(barcode.getBarcodeType());
        if (xzingFormat == null) {
            html.append("Barcode type '")
                .append(barcode.getBarcodeType().toString())
                .append("' not implemented");
        } else {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    barcode.getBarcodeValue(), 
                    xzingFormat,
                    400,
                    xzingFormat == BarcodeFormat.QR_CODE ? 400 : 100,
                    xzingHints);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            html.append("<img src='data:image/png;base64,")
                .append(Base64.getEncoder().encodeToString(pngOutputStream.toByteArray()))
                .append("' alt='barcode' style='width:100%'>");
            if (xzingFormat != BarcodeFormat.QR_CODE) {
                html.append("<div style='text-align:center'><code>")
                    .append(barcode.getBarcodeValue())
                    .append("</code></div>");
            }
        }
        return html.append("</div>");
    }
    
    void buildHtmlReceipt(StringBuilder html, 
                          ReceiptDecoder receiptDecoder,
                          HttpServletRequest request) 
            throws IOException, WriterException, GeneralSecurityException {
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
                payeeAuthority.getPayeeCoreProperties().getCommonName(),
                true));
        if (receiptDecoder.getOptionalPhysicalAddress() != null ||
            receiptDecoder.getOptionalPhoneNumber() != null ||
            receiptDecoder.getOptionalEmailAddress() != null) {
            html.append("<div style='overflow-x:auto'><table style='margin:0.5em auto 0 auto'>");
            if (receiptDecoder.getOptionalPhysicalAddress() != null) {
                html.append("<tr><td>")
                    .append(showLines(receiptDecoder.getOptionalPhysicalAddress()))
                    .append("</td></tr>");
            }
            if (receiptDecoder.getOptionalPhoneNumber() != null ||
                receiptDecoder.getOptionalEmailAddress() != null) {
                html.append("<tr><td>");
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
                html.append("</td></tr>");
            }
            html.append("</table></div>");
        }
        BigDecimal optionalSubtotal = receiptDecoder.getOptionalSubtotal();
        BigDecimal optionalDiscount = receiptDecoder.getOptionalDiscount();
        ReceiptTaxRecord optionalTaxRecord = receiptDecoder.getOptionalTaxRecord();
 
        HtmlTable coreData = 
                new HtmlTable("Core Receipt Data")
                    .addHeader("Payee Name")
                    .addHeader("Total");
        if (optionalSubtotal != null) {
            coreData.addHeader("Subtotal");
        }
        if (optionalDiscount != null) {
            coreData.addHeader("Discount");
        }
        if (optionalTaxRecord != null) {
            coreData.addHeader("Tax");
        }
        coreData.addHeader("Time Stamp")
                .addHeader("Reference Id")
                .addCell("<a href='" + 
                         payeeAuthority.getPayeeCoreProperties().getHomePage() + 
                        "'>" + receiptDecoder.getPayeeCommonName() + "</a>")
                .addCell(showMoney(receiptDecoder, receiptDecoder.getAmount()),
                         HtmlTable.RIGHT_ALIGN);
        if (optionalSubtotal != null) {
            coreData.addCell(showMoney(receiptDecoder, optionalSubtotal),
                             HtmlTable.RIGHT_ALIGN);
        }
        if (optionalDiscount != null) {
            coreData.addCell(showMoney(receiptDecoder, optionalDiscount),
                             HtmlTable.RIGHT_ALIGN);
        }
        if (optionalTaxRecord != null) {
            coreData.addCell(showMoney(receiptDecoder,optionalTaxRecord.getAmount()) +
                    " (" +
                    optionalTaxRecord.getPercentage().toPlainString() +
                    "%)");
        }
        html.append(coreData.addCell(TimeUtils.displayUtcTime(receiptDecoder.getPayeeTimeStamp()))
                            .addCell(receiptDecoder.getPayeeReferenceId(), HtmlTable.RIGHT_ALIGN)
                            .render());

        HtmlTable orderData = new HtmlTable("Order Data");
        orderData.addHeader("Description");
        if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.SKU)) {
            orderData.addHeader("SKU");
        }
        orderData.addHeader("Quantity");
        if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.PRICE)) {
            orderData.addHeader("Price/Unit");
        }
        if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.SUBTOTAL)) {
            orderData.addHeader("Subtotal");
        }
        if (receiptDecoder.getOptionalLineItemElements()
                .contains(ReceiptLineItem.OptionalElements.DISCOUNT)) {
            orderData.addHeader("Discount");
        }
        for (ReceiptLineItem lineItem : receiptDecoder.getLineItems()) {
            String quantity = lineItem.getQuantity().toPlainString();
            if (lineItem.getOptionalUnit() != null) {
                quantity += " " + lineItem.getOptionalUnit();
            }
            orderData.addCell(showLines(lineItem.getDescription()));
            if (receiptDecoder.getOptionalLineItemElements()
                    .contains(ReceiptLineItem.OptionalElements.SKU)) {
                orderData.addCell(optional(lineItem.getOptionalSku()));
            }
            orderData.addCell(quantity, HtmlTable.RIGHT_ALIGN);
            if (receiptDecoder.getOptionalLineItemElements()
                    .contains(ReceiptLineItem.OptionalElements.PRICE)) {
                BigDecimal price = lineItem.getOptionalPrice();
                orderData.addCell(showMoney(receiptDecoder, price), HtmlTable.RIGHT_ALIGN);
            }
            if (receiptDecoder.getOptionalLineItemElements()
                    .contains(ReceiptLineItem.OptionalElements.SUBTOTAL)) {
                BigDecimal subtotal = lineItem.getOptionalSubtotal();
                orderData.addCell(showMoney(receiptDecoder, subtotal), HtmlTable.RIGHT_ALIGN);
            }
            if (receiptDecoder.getOptionalLineItemElements()
                    .contains(ReceiptLineItem.OptionalElements.DISCOUNT)) {
                BigDecimal discount = lineItem.getOptionalDiscount();
                orderData.addCell(showMoney(receiptDecoder, discount), 
                                  HtmlTable.RIGHT_ALIGN + ";color:red");
            }
        }
        html.append(orderData.render());

        ReceiptShippingRecord optionalShippingRecord = receiptDecoder.getOptionalShippingRecord();
        if (optionalShippingRecord != null) {
            html.append(new HtmlTable("Shipping")
                    .addHeader("Description")
                    .addHeader("Cost")
                    .addCell(showLines(optionalShippingRecord.getDescription()))
                    .addCell(showMoney(receiptDecoder, optionalShippingRecord.getAmount()),
                             HtmlTable.RIGHT_ALIGN)
                    .render());
        }

        if (receiptDecoder.getOptionalBarcode() != null) {
            html.append(printBarcode(receiptDecoder.getOptionalBarcode()));
        }
        
        if (receiptDecoder.getOptionalFreeText() != null) {
            html.append("<table style='margin:1.5em auto 0 auto'><tr><td>")
                .append(showLines(receiptDecoder.getOptionalFreeText()))
                .append("</td></tr></table>");
        }
        
        html.append(new HtmlTable("Payment Details")
                .addHeader("Provider Name")
                .addHeader("Account Type")
                .addHeader("Account Id")
                .addHeader("Transaction Id")
                .addHeader("Time Stamp")
                .addHeader("Request Id")
                .addCell("<a href='" + 
                         providerAuthority.getHomePage() + 
                        "'>" + receiptDecoder.getProviderCommonName() + "</a>")
                .addCell(receiptDecoder.getPaymentMethodName())
                .addCell(receiptDecoder.getOptionalAccountReference())
                .addCell(receiptDecoder.getProviderReferenceId())
                .addCell(TimeUtils.displayUtcTime(receiptDecoder.getProviderTimeStamp()))
                .addCell(receiptDecoder.getPayeeRequestId())
                .render());
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException {
        try {
            String pathInfo = request.getPathInfo();
//            System.out.println(pathInfo);
            int endOrderId = pathInfo.lastIndexOf('/');
            if (endOrderId < 5) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.flushBuffer();
                return;
            }
            String orderId = pathInfo.substring(1, endOrderId);
            DataBaseOperations.OrderInfo orderInfo = DataBaseOperations.getOrderStatus(orderId);
            if (orderInfo == null ||
                !orderInfo.pathData.equals(pathInfo.substring(endOrderId + 1))) {
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
                        "<link rel='icon' href='../../saturn.png' sizes='192x192'>"+
                        "<title>Receipt</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body>");
                ReceiptDecoder receiptDecoder = new ReceiptDecoder(JSONParser.parse(receipt));
                if (request.getParameter(VIEW_AS_JSON) == null) {
                    if (receiptDecoder.getStatus() == ReceiptDecoder.Status.AVAILABLE) {
                        buildHtmlReceipt(html, receiptDecoder, request);
                    } else {
                        html.append("<i>Status</i>: ")
                            .append(receiptDecoder.getStatus().toString());
                    }
                    html.append("<div style='margin-top:2em'><a href='../")
                        .append(pathInfo.substring(1))
                        .append("?" + VIEW_AS_JSON)
                        .append("'>Switch to JSON view</a></div>");
                } else {
                    html.append("<div class='header'>Receipt in JSON format</div><div class='json'>")
                        .append(JSONParser.parse(receipt)
                                .serializeToString(JSONOutputFormats.PRETTY_HTML))
                        .append("</div><div>&nbsp;<br><a href='../")
                        .append(pathInfo.substring(1))
                        .append("'>Switch to normal view</a></div>");
                }
                HttpSupport.writeHtml(response, html.append("</body></html>"));
            } else {
                logger.info("Receipt request from: " + request.getRemoteAddr());
                HttpSupport.writeData(response, receipt, BaseProperties.JSON_CONTENT_TYPE);
            }
        } catch (Exception e) {
            response.getWriter().append(e.getMessage()).flush();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.flushBuffer();
        }
    }
}
