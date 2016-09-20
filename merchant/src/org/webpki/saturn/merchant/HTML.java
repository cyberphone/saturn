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

import java.math.BigDecimal;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.util.Base64;
import org.webpki.util.HTMLEncoder;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.Messages;

import org.webpki.w2nbproxy.ExtensionPositioning;

public class HTML implements MerchantProperties {

    static final String STICK_TO_HOME_URL            =
                    "history.pushState(null, null, 'home');\n" +
                    "window.addEventListener('popstate', function(event) {\n" +
                    "    history.pushState(null, null, 'home');\n" +
                    "});\n";
    
    static final String FONT_VERDANA = "Verdana,'Bitstream Vera Sans','DejaVu Sans',Arial,'Liberation Sans'";
    static final String FONT_ARIAL = "Arial,'Liberation Sans',Verdana,'Bitstream Vera Sans','DejaVu Sans'";
    
    static final String GAS_PUMP_LOGO = "<img src=\"images/gaspump.svg\" title=\"For showing context\" style=\"width:30pt;position:absolute;right:10pt;top:10pt\"";
    
    static final String HTML_INIT = 
        "<!DOCTYPE html>"+
        "<html><head><meta charset=\"UTF-8\"><link rel=\"icon\" href=\"saturn.png\" sizes=\"192x192\">"+
        "<meta name=\"viewport\" content=\"initial-scale=1.0\"/>" +
        "<title>Saturn Payment Demo</title>"+
        "<style type=\"text/css\">html {overflow:auto}\n"+
        ".point {text-align:center;font-family:courier;font-weight:bold;font-size:10pt;border-radius:3pt;border-width:1px;border-style:solid;border-color:#B0B0B0;display:inline-block;padding:1.5pt 3pt 1pt 3pt}\n" +
        ".tftable {border-collapse:collapse;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
        ".tftable th {font-size:10pt;background:" +
          "linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
          "border-width:1px;padding:4pt 10pt 4pt 10pt;border-style:solid;border-color:#a9a9a9;" +
          "text-align:center;font-family:" + FONT_ARIAL + "}\n" +
        ".tftable td {background-color:#FFFFE0;font-size:11pt;border-width:1px;padding:4pt 8pt 4pt 8pt;border-style:solid;border-color:#a9a9a9;font-family:" + FONT_ARIAL + ";white-space:nowrap}\n" +
        "body {font-size:10pt;color:#000000;font-family:" + FONT_VERDANA + ";background-color:white}\n" +
        "a {font-weight:bold;font-size:8pt;color:blue;font-family:" + FONT_ARIAL + ";text-decoration:none}\n" +
        "td {font-size:8pt;font-family:" + FONT_VERDANA + "}\n" +
        "li {padding-top:5pt}\n" +
        ".quantity {text-align:right;font-weight:normal;font-size:10pt;font-family:" + FONT_ARIAL + "}\n" +
        ".stdbtn {font-weight:normal;font-size:10pt;font-family:" + FONT_ARIAL + "}\n" +
        ".updnbtn {vertical-align:middle;text-align:center;font-weight:normal;font-size:8px;font-family:" + FONT_VERDANA + ";margin:0px;border-spacing:0px;padding:2px 3px 2px 3px}\n";
    
    static String getHTML(String javascript, String bodyscript, String box) {
        StringBuffer s = new StringBuffer(HTML_INIT + "html, body {margin:0px;padding:0px;height:100%}</style>");
        if (javascript != null) {
            s.append("<script type=\"text/javascript\">").append(javascript).append("</script>");
        }
        s.append("</head><body");
        if (bodyscript != null) {
            if (bodyscript.charAt(0) != '>') {
                s.append(' ');
            }
            s.append(bodyscript);
        }
        s.append("><div onclick=\"document.location.href='home"
            + "'\" title=\"Home sweet home...\" style=\"cursor:pointer;position:absolute;top:15px;"
            + "left:15px;z-index:5;visibility:visible;padding:5pt 8pt 5pt 8pt;font-size:10pt;"
            + "text-align:center;background: radial-gradient(ellipse at center, rgba(255,255,255,1) "
            + "0%,rgba(242,243,252,1) 38%,rgba(196,210,242,1) 100%);border-radius:8pt;border-width:1px;"
            + "border-style:solid;border-color:#B0B0B0;box-shadow:3pt 3pt 3pt #D0D0D0;}\">"
            + "Saturn<br><span style=\"font-size:8pt\">Payment Demo</span></div>"
            + "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" height=\"100%\">")
         .append(box)
         .append("</table></body></html>");
        return s.toString();
    }
    
    static void output(HttpServletResponse response, String html) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(html.getBytes("UTF-8"));
    }

    static void homePage(HttpServletResponse response,
                         boolean debugMode,
                         boolean reserveMode) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(null, null,
                "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                "<table style=\"max-width:600px;\" cellpadding=\"4\">" +
                   "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Saturn - Web Payment Demo<br>&nbsp;</td></tr>" +
                   "<tr><td style=\"text-align:left\">This application is a demo of the Saturn payment scheme.&nbsp; " +
                   "Using Saturn the client is supposed to have a native &quot;Wallet&quot; based on the Web2Native Bridge" +
                   ".&nbsp; Primary Saturn features include:<ul>" +
                   "<li style=\"padding-top:0pt\">Fully <i>decentralized</i> operation (no dependency on central registries like in 3D Secure)</li>" +
                   "<li><i>Digitally signed</i> messages enable a protocol-level security comparable to a PIN-code terminal and chip-card in a physical shop</li>" +
                   "<li>Encryption (like in SET) hides sensitive customer data from merchants, eliminating the need for third-party &quot;tokenization&quot; services</li>" +
                   "<li>Private messaging through the payment backend makes it easy applying RBA (Risk Based Authentication) for high-value or &quot;suspicious&quot; transactions</li>" +
                   "<li>Equally applicable for legacy card payment networks as for bank-2-bank schemes</li>" +
                   "<li>In addition to supporting Web payments, Saturn is <i>also intended to be usable in traditional payment scenarios " +
                   "including with POS terminals and gas pumps using an NFC/BLE connection to the Wallet</i></li>" +
                   "<li>All messages are coded in JSON</li>" +
                   "<li>Consumers only deal with payment instruments visualized as cards (like they did <i>before</i> the Web)</li>" +
                   "</ul>" +
                   "Note that the Wallet is <i>pre-configured</i> with payment credentials requiring no signup etc.&nbsp;&nbsp;" +
                   "<a href=\"https://github.com/cyberphone/web2native-bridge#installation\">Install Wallet</a>.&nbsp; " +
                   "There is also an <a href=\"android\">Android version</a> of the Wallet.</td></tr>" +
                   "<tr><td align=\"center\"><table cellspacing=\"0\">" +
//TODO
//                   "<tr style=\"text-align:left\"><td><a href=\"" + "hh" + "/cards\">Initialize Payment Cards&nbsp;&nbsp;</a></td><td><i>Mandatory</i> First Step</td></tr>" +
                   "<tr><td style=\"text-align:left;padding-bottom:5pt\"><a href=\"" + "shop" + 
                     "\">Go To Merchant</a></td><td style=\"text-align:left;padding-bottom:5pt\">Shop Till You Drop!</td></tr>" +
                   "<form name=\"options\" method=\"POST\"><tr>" +
                   "<td style=\"text-align:center\"><input type=\"checkbox\" name=\"" + 
                   RESERVE_MODE_SESSION_ATTR + "\" onchange=\"document.forms.options.submit()\"" +
                   (reserveMode ? " checked" : "") +
                   "></td><td>Reserve+Finalize Payment Mode</td></tr>" +
                   "<tr><td style=\"text-align:center\"><input type=\"checkbox\" name=\"" +
                   DEBUG_MODE_SESSION_ATTR + "\" onchange=\"document.forms.options.submit()\"" +
                   (debugMode ? " checked" : "") +
                   "></td><td>Debug (JSON Message Dump) Option</td></form></tr>" +
                   "<tr><td style=\"text-align:left;padding-top:10pt\"><a href=\"" + "gasstation" + 
                   "\">Gas Station</a></td><td style=\"text-align:left;padding-top:10pt\">A <i>Different</i> Payment Scenario</td></tr>" +
                   "<tr><td style=\"text-align:center;padding-top:15pt;padding-bottom:5pt\" colspan=\"2\"><b>Documentation</b></td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"http://xmlns.webpki.org/webpay/v2\">Payment System</a>&nbsp;&nbsp;</td><td>State Diagram Etc.</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://github.com/cyberphone/saturn\">Demo Source Code</a></td><td>For Nerds...</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://github.com/cyberphone/web2native-bridge\">Web2Native Bridge</a>&nbsp;&nbsp;&nbsp;</td><td>API and &quot;Executive Level&quot; Description</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://mobilepki.org/jcs\">JCS</a></td><td>JSON Cleartext Signature</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/jef.html\">JEF</a></td><td>JSON Encryption Format</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/keygen2.html\">KeyGen2</a></td><td>Wallet Credential Enrollment Protocol</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/sks-api-arch.pdf\">SKS</a></td><td>Wallet Credential Store</td></tr>" +
                   "<tr><td style=\"text-align:center;padding-top:15pt;padding-bottom:5pt\" colspan=\"2\"><b>Related Application</b></td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://play.google.com/store/apps/details?id=org.webpki.mobile.android\">SKS/KeyGen2</a></td><td>Android PoC</td></tr>" +
                 "</table></td></tr></table></td></tr>"));
    }

    static String javaScript(String string) {
        StringBuffer s = new StringBuffer();
        for (char c : string.toCharArray()) {
            if (c == '\n') {
                s.append("\\n");
            } else if (c == '\'') {
                s.append("\\'");
            } else if (c == '\\') {
                s.append("\\\\");
            } else {
                s.append(c);
            }
        }
        return s.toString();
    }

    private static StringBuffer productEntry(StringBuffer temp_string,
                                             ProductEntry product_entry,
                                             String sku,
                                             SavedShoppingCart savedShoppingCart,
                                             int index) throws IOException {
        int quantity = savedShoppingCart.items.containsKey(sku) ? savedShoppingCart.items.get(sku): 0;
        StringBuffer s = new StringBuffer(
            "<tr style=\"text-align:center\"><td><img src=\"images/")
        .append(product_entry.imageUrl)
        .append("\"></td><td>")
        .append(product_entry.name)
        .append("</td><td style=\"text-align:right\">")
        .append(price(product_entry.priceX100))
        .append(
            "</td><td><form>" +
            "<table style=\"border-width:0px;padding:0px;margin:0px;border-spacing:2px;border-collapse:separate\">" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\"><input type=\"button\" class=\"updnbtn\" value=\"&#x25b2;\" title=\"More\" onclick=\"updateQuantity(this.form.p")
        .append(index)
        .append(", 1, ")
        .append(index)
        .append(")\"></td>" +
            "</tr>" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\"><input size=\"6\" type=\"text\" name=\"p")
        .append(index)
        .append("\" value=\"")
        .append(quantity)
        .append("\" class=\"quantity\" oninput=\"updateInput(")
        .append(index)
        .append(", this);\" autocomplete=\"off\"/></td>" +
            "</tr>" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\">"
            + "<input type=\"button\" class=\"updnbtn\" value=\"&#x25bc;\" title=\"Less\" "
            + "onclick=\"updateQuantity(this.form.p")
        .append(index)
        .append(", -1, ")
        .append(index)
        .append(")\"></td></tr></table></form></td></tr>");
        temp_string.insert(0, "shoppingCart[" + index + "] = new webpki.ShopEntry(" 
                       + product_entry.priceX100 + ",'" + product_entry.name + "','" + sku + "'," + quantity + ");\n");        
        return s;
    }

    private static String price(long priceX100) throws IOException {
        BigDecimal amount = new BigDecimal(priceX100);
        amount.setScale(MerchantService.currency.getDecimals());
        amount = amount.divide(new BigDecimal(100));
        return MerchantService.currency.amountToDisplayString(amount, false);
    }
    
    static void merchantPage(HttpServletResponse response,
                             SavedShoppingCart savedShoppingCart) throws IOException, ServletException {
        StringBuffer temp_string = new StringBuffer(
            "\nfunction closeFundFlash() {\n" +
            "    setTimeout(function() {\n" +
            "        document.getElementById('fundlimit').style.visibility = 'hidden';\n" +
            "    }, 5000);\n" +
            "}\n\n" +
            "function userPay() {\n" +
            "    if (getTotal()) {\n" +
            "        document.getElementById('" + W2NBWalletServlet.SHOPPING_CART_FORM_ATTR + "').value = JSON.stringify(shoppingCart);\n" +
            "        document.forms.shoot.submit();\n" +           
            "    } else {\n" +
            "        document.getElementById('emptybasket').style.top = ((window.innerHeight - document.getElementById('emptybasket').offsetHeight) / 2) + 'px';\n" +
            "        document.getElementById('emptybasket').style.left = ((window.innerWidth - document.getElementById('emptybasket').offsetWidth) / 2) + 'px';\n" +
            "        document.getElementById('emptybasket').style.visibility = 'visible';\n" +
            "        setTimeout(function() {\n" +
            "            document.getElementById('emptybasket').style.visibility = 'hidden';\n" +
            "        }, 1000);\n" +
            "    }\n" +
            "}\n\n" +
            "function getTotal() {\n" +
            "    var total = 0;\n" +
            "    for (var i = 0; i < shoppingCart.length; i++) {\n" +
            "        total += shoppingCart[i].priceX100 * shoppingCart[i].quantity;\n" +
            "    }\n" +
            "    return total;\n"+
            "}\n\n" +
            "function getPriceString() {\n" +
            "    var priceX100 = getTotal();\n" +
            "    var intPart = Math.floor(priceX100 / 100).toString();\n" +
            "    var adjusted='';\n" +
            "    for (var i = 0; i < intPart.length; i++) {\n" +
            "      adjusted += intPart.charAt(i);\n" +
            "      if (i < intPart.length - 1 && (intPart.length - i - 1) % 3 == 0) {\n" +
            "        adjusted += ',';\n" +
            "      }\n" +
            "    }\n" +
            "    return ");
        if (MerchantService.currency.symbolFirst) {
            temp_string.append('\'')
                       .append(MerchantService.currency.symbol)
                       .append("' + ");
        }
        temp_string.append("adjusted + '.' +  Math.floor((priceX100 % 100) / 10) +  Math.floor(priceX100 % 10)");
        if (!MerchantService.currency.symbolFirst) {
            temp_string.append(" + '")
                       .append(MerchantService.currency.symbol)
                       .append('\'');
        }
        temp_string.append(";\n" +
            "}\n\n" +
            "function updateTotal() {\n" +
            "    document.getElementById('total').innerHTML = getPriceString();\n" +
            "}\n\n" +
            "function updateInput(index, control) {\n" +
            "    if (!numeric_only.test(control.value)) control.value = '0';\n" +
            "    while (control.value.length > 1 && control.value.charAt(0) == '0') control.value = control.value.substring(1);\n" +
            "    shoppingCart[index].quantity = parseInt(control.value);\n" +
            "    updateTotal();\n" +
            "}\n\n" +
            "function updateQuantity(control, value, index) {\n" +
            "    control.value = parseInt(control.value) + value;\n" +
            "    updateInput(index, control);\n" +
            "}\n");

        StringBuffer page_data = new StringBuffer(
            "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
            "<table>" +
            "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">" +
            ShoppingServlet.COMMON_NAME +
            "<br>&nbsp;</td></tr>" +
            "<tr><td id=\"result\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
            "<tr><th>Image</th><th>Description</th><th>Price</th><th>Quantity</th></tr>");
        int q = 0;
        for (String sku : ShoppingServlet.products.keySet()) {
            page_data.append(productEntry(temp_string, ShoppingServlet.products.get(sku), sku, savedShoppingCart, q++));
        }
        page_data.append(
            "</table></tr></td><tr><td style=\"padding-top:10pt\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\"><tr><th>Total</th><td style=\"text-align:right\" id=\"total\">")
            .append(price(savedShoppingCart.total))
            .append("</td></tr>" +
            "</table></td></tr>" +
            "<tr><td style=\"text-align:center;padding-top:10pt\" id=\"pay\"><input class=\"stdbtn\" type=\"button\" value=\"Checkout..\" title=\"Paying time has come...\" onclick=\"userPay()\"></td></tr>" +
            "</table>" +
            "<form name=\"shoot\" method=\"POST\" action=\"choose\">" +
            "<input type=\"hidden\" name=\"" + W2NBWalletServlet.SHOPPING_CART_FORM_ATTR + "\" id=\"" + W2NBWalletServlet.SHOPPING_CART_FORM_ATTR + "\">" +
            "</form></td></tr>");
         temp_string.insert(0,
            "\n\n\"use strict\";\n\n" +
            "var numeric_only = new RegExp('^[0-9]{1,6}$');\n\n" +
            "var webpki = {};\n\n" +
            "webpki.ShopEntry = function(priceX100, name,sku, quantity) {\n" +
            "    this.priceX100 = priceX100;\n" +
            "    this.name = name;\n" +
            "    this.sku = sku;\n" +
            "    this.quantity = quantity;\n" +
            "};\n\n" +
            "var shoppingCart = [];\n");

        HTML.output(response, HTML.getHTML(temp_string.toString(),
            "onload=\"closeFundFlash()\"><div id=\"fundlimit\" style=\"position:absolute;left:15pt;bottom:15pt;z-index:3;font-size:8pt\">Your funds at the bank are limited to $1M...</div>" +
            "<div id=\"emptybasket\" style=\"border-color:grey;border-style:solid;border-width:3px;text-align:center;font-family:"
            + FONT_ARIAL+ ";z-index:3;background:#f0f0f0;position:absolute;visibility:hidden;padding:5pt 10pt 5pt 10pt\">Nothing ordered yet...</div",
            page_data.toString()));
    }

    static StringBuffer currentOrder(SavedShoppingCart savedShoppingCart) throws IOException {
        StringBuffer s = new StringBuffer(
                "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                "<table>" +
                "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:"
                + FONT_ARIAL + "\">Current Order<br>&nbsp;</td></tr>" +
                "<tr><td id=\"result\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
                "<tr><th>Description</th><th>Price</th><th>Quantity</th><th>Sum</th></tr>");
            for (String sku : savedShoppingCart.items.keySet()) {
                ProductEntry product_entry = ShoppingServlet.products.get(sku);
                s.append("<tr style=\"text-align:center\"><td>")
                 .append(product_entry.name)
                 .append("</td><td style=\"text-align:right\">")
                 .append(price(product_entry.priceX100))
                 .append("</td><td>")
                 .append(savedShoppingCart.items.get(sku).intValue())
                 .append("</td><td style=\"text-align:right\">")
                 .append(price(product_entry.priceX100 * savedShoppingCart.items.get(sku).intValue()))
                 .append("</td></tr>");                
            }
            s.append(
                 "<tr><td colspan=\"4\" style=\"height:1px;padding:0px\"></td></tr>" +
                 "<tr><td colspan=\"3\" style=\"text-align:right\">Total</td><td style=\"text-align:right\">")
             .append(price(savedShoppingCart.total))
             .append("</td></tr>" +
                 "<tr><td colspan=\"3\" style=\"text-align:right\">Tax (10%)</td><td style=\"text-align:right\">")
             .append(price(savedShoppingCart.tax))
             .append("</td></tr>" +
                "</table></td></tr><tr><td style=\"padding-top:10pt\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\"><tr><th style=\"text-align:center\">Amount to Pay</th><td style=\"text-align:right\" id=\"total\">")
             .append(price(savedShoppingCart.roundedPaymentAmount))
             .append("</td></tr></table></td></tr>");
       return s;
    }

    static void w2nbWalletPay(HttpServletResponse response,
                              SavedShoppingCart savedShoppingCart, 
                              boolean tapConnectMode,
                              boolean debugMode,
                              String walletRequest) throws IOException, ServletException {
        String connectMethod = tapConnectMode ? "tapConnect" : "nativeConnect";
        StringBuffer s = currentOrder(savedShoppingCart);
      
        if (tapConnectMode) {
            s.append("<tr><td align=\"center\"><img id=\"state\" title=\"Please tap your mobile wallet!\" " +
                     "src=\"images/NFC-N-Mark-Logo.svg\" style=\"height:120pt;margin-top:10pt\"></td>");
        } else {
            s.append("<tr><td style=\"padding:20pt;text-align:center\" id=\"wallet\"><img src=\"images/waiting.gif\"></td>");
        }
        s.append("</tr></table>" +
                 "<form name=\"restore\" method=\"POST\" action=\"shop\">" +
                 "</form></td></tr>");
        
        StringBuffer temp_string = new StringBuffer("\n\n\"use strict\";\n\nvar invocationData = ")
            .append(walletRequest)
            .append(";\n\n" +

                    "var nativePort = null;\n\n" +

                    "function closeWallet() {\n" +
                    "  if (nativePort) {\n" +
                    "    nativePort.disconnect();\n" +
                    "    nativePort = null;\n" +
                    "  }\n" +
                    "}\n\n" +

                    "function setFail(message) {\n" +
                    "  closeWallet();\n" +
                    "  alert(message);\n" +
                    "}\n\n" +

                    "function activateWallet() {\n" +
                    "  var initMode = true;\n" +
                    "  if (!navigator.")
             .append(connectMethod)
             .append(") {\n" +
                    "    setFail('\"navigator.")
             .append(connectMethod)
             .append("\" not found, \\ncheck Chrome extension settings');\n" +
                    "    return;\n" +
                    "  }\n" +
                    "  navigator.")
             .append(connectMethod)
             .append("('")
             .append(MerchantService.w2nbWalletName)
             .append("'");
        if (!tapConnectMode) {
            temp_string.append(",\n                          ")
             .append(ExtensionPositioning.encode(ExtensionPositioning.HORIZONTAL_ALIGNMENT.Center,
                                                 ExtensionPositioning.VERTICAL_ALIGNMENT.Center,
                                                 "wallet"));
        }
        temp_string.append(").then(function(port) {\n" +
                    "    nativePort = port;\n" +
                    "    port.addMessageListener(function(message) {\n" +
                    "      if (message['@context'] != '" + BaseProperties.SATURN_WEB_PAY_CONTEXT_URI + "') {\n" +
                    "        setFail('Wrong or missing \"@context\"');\n" +
                    "        return;\n" +
                    "      }\n" +
                    "      var qualifier = message['@qualifier'];\n" +
                    "      if ((initMode && qualifier != '" + Messages.PAYMENT_CLIENT_IS_READY.toString() + "')  ||\n" +
                    "          (!initMode && qualifier != '" +  Messages.PAYER_AUTHORIZATION.toString() + "')) {\n" +  
                    "        setFail('Wrong or missing \"@qualifier\"');\n" +
                    "        return;\n" +
                    "      }\n" +
                    "      if (initMode) {\n");
       if (debugMode) {
           temp_string.append(
                    "        console.debug(JSON.stringify(message));\n");
       }
       if (!tapConnectMode) {
           temp_string.append(
                    "        document.getElementById('wallet').style.height = message." + 
                                         BaseProperties.WINDOW_JSON + "." + BaseProperties.HEIGHT_JSON + " + 'px';\n");
       }
       temp_string.append(
                    "        initMode = false;\n" +
                    "        nativePort.postMessage(invocationData);\n" +
                    "      } else {\n" +
                    "// This is it...transfer the Wallet authorization data back to the Merchant server\n" +
                    "        fetch('transact', {\n" +
                    "           headers: {\n" +
                    "             'Content-Type': 'application/json'\n" +
                    "           },\n" +
                    "           method: 'POST',\n" +
                    "           credentials: 'same-origin',\n" +
                    "           body: JSON.stringify(message)\n" +
                    "        }).then(function (response) {\n" +
                    "          return response.json();\n" +
                    "        }).then(function (resultData) {\n" +
                    "          if (typeof resultData == 'object' && !Array.isArray(resultData)) {\n" +
                    "            if (resultData['@qualifier'] == '" + Messages.PAYMENT_CLIENT_SUCCESS.toString() + "') {\n" +
                    "// \"Normal\" return\n" +
                    "              document.location.href='result';\n" +
                    "            } else {\n" +
                    "// \"Exceptional\" return with error or RBA\n" +
                    "              nativePort.postMessage(resultData);\n" +
                    "            }\n" +
                    "          } else {\n" +
                    "            setFail('Unexpected wallet return data');\n" +
                    "          }\n" +
                    "        }).catch (function (error) {\n" +
                    "          console.log('Request failed', error);\n" +
                    "        });\n" +                           
                    "      }\n"+
                    "    });\n");
       if (tapConnectMode) {
           temp_string.append(
                   "    port.addConnectionListener(function(initialize) {\n" +
                   "      if (initialize) {\n" +
                   "        document.getElementById('state').src = 'images/loading-gears-animation-3.gif';\n" +
                   "      } else {\n" +
                   "        if (initMode) console.debug('Wallet prematurely closed!');\n" +
                   "        nativePort = null;\n" +
                   "        document.forms.restore.submit();\n" +
                   "      }\n" +
                   "    });\n");
       } else {
           temp_string.append(
                    "    port.addDisconnectListener(function() {\n" +
                    "      if (initMode) alert('Wallet application \"" + 
                                     MerchantService.w2nbWalletName + ".jar\" appears to be missing!');\n" +
                    "      nativePort = null;\n" +
                    "// User cancel\n" +
                    "      document.forms.restore.submit();\n" +
                    "    });\n");
       }
       temp_string.append(
                    "  }, function(err) {\n" +
                    "    console.debug(err);\n" +
                    "  });\n" +
                    "}\n\n");

       if (!tapConnectMode) {
           temp_string.append(ExtensionPositioning.SET_EXTENSION_POSITION_FUNCTION_TEXT + "\n");
       }

       temp_string.append(
                    "window.addEventListener('beforeunload', function(event) {\n" +
                    "  closeWallet();\n" +
                    "});\n\n");
        HTML.output(response, HTML.getHTML(temp_string.toString(),
                              "onload=\"activateWallet()\"",
                              s.toString()));
    }

    static StringBuffer receiptCore(ResultData resultData, boolean debugMode) throws IOException {
        StringBuffer s = new StringBuffer("<tr><td><table class=\"tftable\"><tr><th>Our Reference</th><th>Amount</th><th>")
            .append(resultData.accountType.isCardPayment() ? "Card" : "Account")
            .append(" Type</th><th>")
            .append(resultData.accountType.isCardPayment() ? "Card Reference" : "Account Number")   
            .append("</th></tr><tr><td style=\"text-align:center\">")  
            .append(resultData.referenceId)
            .append("</td><td style=\"text-align:center\">")
            .append(resultData.currency.amountToDisplayString(resultData.amount, false))
            .append("</td><td style=\"text-align:center\">")
            .append(resultData.accountType.getCommonName())
            .append("</td><td style=\"text-align:center\">")
            .append(resultData.accountReference)
            .append("</td></tr></table></td></tr>");
        if (debugMode) {
            s.append("<tr><td style=\"text-align:center;padding-top:20pt\"><a href=\"debug\">Show Debug Info</a></td></tr>");
        }
        return s;
    }

    static void shopResultPage(HttpServletResponse response,
                               boolean debugMode,
                               ResultData resultData) throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">")
            .append("<table>" +
                    "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Order Status<br>&nbsp;</td></tr>" +
                    "<tr><td style=\"text-align:center;padding-bottom:15pt;font-size:10pt\">Dear customer, your order has been successfully processed!</td></tr>")
            .append(receiptCore(resultData, debugMode))
            .append("</table></td></tr></table></td></tr>");
        HTML.output(response, 
                    HTML.getHTML(STICK_TO_HOME_URL, null, s.toString()));
    }

    static String updatePumpDisplay(FuelTypes fuelType) {
        return "function setDigits(prefix, value) {\n" +
               "  var q = 0;\n" +
               "  while(value) {\n" +
               "    var digit = value % 10;\n" +
               "    document.getElementById(prefix + (q++)).innerHTML = digit;\n" +
               "    value = (value - digit) / 10;\n" +
               "  }\n" +
               "}\n" +
               "function updatePumpDisplay(decilitres) {\n" +
               "  var priceX1000 = " + fuelType.pricePerLitreX100 + " * decilitres;\n" +
               "  var roundup = priceX1000 % " + GasStationServlet.ROUND_UP_FACTOR_X_10 + ";\n" +
               "  if (roundup) priceX1000 += " + GasStationServlet.ROUND_UP_FACTOR_X_10 + " - roundup;\n" +
               "  setDigits('pvol', decilitres);\n" +
               "  setDigits('ppri', priceX1000 / 10);\n" +
               "}\n";
    }
    static void gasStationResultPage(HttpServletResponse response,
                                     FuelTypes fuelType,
                                     int decilitres,
                                     boolean debugMode,
                                     ResultData resultData) throws IOException, ServletException {
        StringBuffer s = new StringBuffer()
            .append(gasStation("Thank You - Welcome Back!", true))
            .append(selectionButtons(new FuelTypes[]{fuelType}))
            .append("<tr><td style=\"height:15pt\"></td></tr>")
            .append(receiptCore(resultData, debugMode))
            .append("</table></td></tr></table></td></tr>");
        HTML.output(response, 
                    HTML.getHTML(STICK_TO_HOME_URL + updatePumpDisplay(fuelType),
                                 "onload=\"updatePumpDisplay(" + decilitres + ")\">" +
                                 GAS_PUMP_LOGO,
                                 s.toString()));
    }

    static void debugPage(HttpServletResponse response, String string, boolean clean) throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
                  "<table>" +
                  "<tr><td style=\"padding-top:50pt;text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                  "\">Payment Session Debug Information&nbsp;<br></td></tr><tr><td style=\"text-align:left\">")
          .append(string)
          .append("</td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(clean ? null : STICK_TO_HOME_URL, null,s.toString()));
    }

    static void errorPage(HttpServletResponse response, String error, boolean system)
                                 throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
                 "<table>" +
                 "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                 "\">")
         .append(system ? "System " : "")
         .append("Failure&nbsp;<br></td></tr><tr><td style=\"text-align:center\">")
         .append(HTMLEncoder.encodeWithLineBreaks(error.getBytes("UTF-8")))
         .append("</td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(STICK_TO_HOME_URL, null,s.toString()));
    }

    static void userChoosePage(HttpServletResponse response,
                               SavedShoppingCart savedShoppingCart,
                               boolean android) throws IOException, ServletException {
        StringBuffer s = currentOrder(savedShoppingCart)
            .append("<tr><td style=\"padding-top:15pt\"><table style=\"margin-left:auto;margin-right:auto\">" +
                    "<tr><td style=\"padding-bottom:10pt;text-align:center;font-weight:bolder;font-size:10pt;font-family:"
                + FONT_ARIAL + "\">Select Payment Method</td></tr>" +
                    "<tr><td><img title=\"Saturn\" style=\"cursor:pointer\" src=\"images/paywith-saturn.png\" onclick=\"document.forms.shoot.submit()\"></td></tr>" +
                    "<tr><td style=\"padding-top:10pt\"><img title=\"Saturn QR\" style=\"cursor:pointer\" src=\"images/paywith-saturnqr.png\" onclick=\"document.location.href='qrdisplay'\"></td></tr>" +
                    "<tr><td style=\"padding: 10pt 0 10pt 0\"><img title=\"VISA &amp; MasterCard\" style=\"cursor:pointer\" src=\"images/paywith-visa-mc.png\" onclick=\"noSuchMethod()\"></td></tr>" +
                    "<tr><td><img title=\"PayPal\" style=\"cursor:pointer\" src=\"images/paywith-paypal.png\" onclick=\"noSuchMethod()\"></td></tr>" +
                    "<tr><td style=\"text-align:center;padding:15pt\"><input class=\"stdbtn\" type=\"button\" value=\"Return to shop..\" title=\"Changed your mind?\" onclick=\"document.forms.restore.submit()\"></td></tr>" +
                    "</table></td></tr></table></td></tr>");

        HTML.output(response, HTML.getHTML(
                STICK_TO_HOME_URL +
                "\nfunction noSuchMethod() {\n" +
                        "    document.getElementById('notimplemented').style.top = ((window.innerHeight - document.getElementById('notimplemented').offsetHeight) / 2) + 'px';\n" +
                        "    document.getElementById('notimplemented').style.left = ((window.innerWidth - document.getElementById('notimplemented').offsetWidth) / 2) + 'px';\n" +
                        "    document.getElementById('notimplemented').style.visibility = 'visible';\n" +
                        "    setTimeout(function() {\n" +
                        "        document.getElementById('notimplemented').style.visibility = 'hidden';\n" +
                        "    }, 1000);\n" +
                        "}\n\n",
                "><form name=\"shoot\" method=\"POST\" action=\"" + 
                (android ? "androidplugin" : "w2nbwallet") +
                "\"></form><form name=\"restore\" method=\"POST\" action=\"shop\">" +
                "</form><div id=\"notimplemented\" style=\"border-color:grey;border-style:solid;border-width:3px;text-align:center;font-family:" +
                FONT_ARIAL+ ";z-index:3;background:#f0f0f0;position:absolute;visibility:hidden;padding:5pt 10pt 5pt 10pt\">This demo only supports Saturn!</div",
                s.toString()));
    }
    
    static StringBuffer pumpDigit(int digit, String prefix) {
        return new StringBuffer("<td><div style=\"background-color:white;padding:0pt 3pt 1pt 3pt;font-size:14pt;border-radius:2pt\" id=\"")
            .append(prefix)
            .append(digit)
            .append("\">0</div></td>");
    }
    
    static StringBuffer pumpDisplay(int digits, int decimals, String leader, String trailer, String prefix) {
        StringBuffer s = new StringBuffer("<table cellspacing=\"5\"><tr><td style=\"color:white\">")
            .append(leader)
            .append("</td>");
        while (digits-- > 0) {
            s.append(pumpDigit(digits + decimals, prefix));
        }
        s.append("<td style=\"color:white\">&#x2022;</td>");
        while (decimals-- > 0) {
            s.append(pumpDigit(decimals, prefix));
        }
        return s.append("<td style=\"color:white\">")
                .append(trailer)
                .append("</td></tr></table>");
    }
    
    static String gasStation(String header, boolean visiblePumpDisplay) {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\"><table>");
        if (visiblePumpDisplay) {
            s.append("<tr><td style=\"padding-bottom:15pt\" align=\"center\"><table title=\"This is [sort of] a pump display\"><tr>"+
                     "<td align=\"center\" style=\"box-shadow:5pt 5pt 5pt #c0c0c0;background:linear-gradient(135deg, #516287 0%,#5697e2 71%,#5697e2 71%,#516287 100%);border-radius:4pt\">" +
                     "<div style=\"padding:3pt;font-size:12pt;color:white\">Pump O'Matic</div>")
             .append(pumpDisplay(3, 1, "Volume", "Litres", "pvol"))
             .append(pumpDisplay(3, 2, "To Pay", "$", "ppri"))
             .append("</td></tr></table></td></tr>");
        }
        return s.append("<tr><td id=\"phase\" style=\"padding-bottom:10pt;text-align:center;" +
                        "font-weight:bolder;font-size:11pt;font-family:" +
                        FONT_ARIAL + "\">")
                .append(header)
                .append("</td></tr>").toString();

    }

    static String cometJavaScriptSupport(String id, 
                                         HttpServletRequest request,
                                         String returnAction,
                                         String progressAction,
                                         String successAction) {
        return new StringBuffer(
            "\"use strict\";\n" +
            STICK_TO_HOME_URL +
            "function setQRDisplay(turnOn) {" +
            "  var displayArg = turnOn ? 'table-row' : 'none';\n" +
            "  document.getElementById('qr1').style.display = displayArg;\n" +
            "  document.getElementById('qr2').style.display = displayArg;\n" +
            "  document.getElementById('waiting').style.paddingTop = turnOn ? '0pt' : '15pt';\n" +
            "}\n" +
            "function flashQRInfo() {\n" +
            "  document.getElementById('qridflasher').style.top = ((window.innerHeight - document.getElementById('qridflasher').offsetHeight) / 2) + 'px';\n" +
            "  document.getElementById('qridflasher').style.left = ((window.innerWidth - document.getElementById('qridflasher').offsetWidth) / 2) + 'px';\n" +
            "  document.getElementById('qridflasher').style.visibility = 'visible';\n" +
            "  setTimeout(function() {\n" +
            "    document.getElementById('qridflasher').style.visibility = 'hidden';\n" +
            "  }, 5000);\n" +
            "}\n\n" +
            "function startComet() {\n" +
            "  fetch('")
        .append(request.getRequestURL().toString())
        .append(
            "', {\n" +
            "     headers: {\n" +
            "       'Content-Type': 'text/plain'\n" +
            "     },\n" +
            "     method: 'POST',\n" +
            "     body: '" + id + "'\n" +
            "  }).then(function (response) {\n" +
            "    return response.text();\n" +
            "  }).then(function (resultData) {\n" +
            "    console.log('Response', resultData);\n" +
            "    switch (resultData) {\n" +
            "      case '" + QRSessions.QR_PROGRESS + "':\n")
        .append(progressAction)
        .append(
            "        setQRDisplay(false);\n" +
            "      case '" + QRSessions.QR_CONTINUE + "':\n" +
            "        startComet();\n" +
            "        break;\n" +
            "      case '" + QRSessions.QR_RETURN + "':\n" +
            "        ")
        .append(returnAction)
        .append(";\n" +
            "        break;\n" +
            "      default:\n" +
            "        ")
        .append(successAction)
        .append(";\n" +
            "    }\n" +
            "  }).catch (function(error) {\n" +
            "    console.log('Request failed', error);\n" +
            "  });\n" +                           
            "}\n").toString();
    }
    
    static String bodyStartQR(String optional) {
        return "onload=\"startComet()\">" + optional + 
               GAS_PUMP_LOGO +
               "><div id=\"qridflasher\" style=\"border-color:grey;border-style:solid;border-width:3px;text-align:center;font-family:" +
               FONT_ARIAL+ ";z-index:3;background:#f0f0f0;position:absolute;visibility:hidden;padding:5pt 10pt 5pt 10pt\">" +
               "You get it automatically when you install the<br>&quot;WebPKI&nbsp;Suite&quot;, just look for the icon!</div";       
    }
    
    static StringBuffer bodyEndQR(byte[] qrImage, boolean qrInitOn) {
        String display = qrInitOn ? "" : " style=\"display:none\"";
        return new StringBuffer()
            .append(
                "<tr")
            .append(display)
            .append(
                " id=\"qr1\"><td style=\"padding-top:10pt\" align=\"left\">Now use the QR ID&trade; <a href=\"javascript:flashQRInfo()\">" +
                "<img border=\"1\" src=\"images/qr_launcher.png\"></a> application to start the Wallet</td></tr>" +
                "<tr")
             .append(display)
             .append(" id=\"qr2\"><td align=\"center\"><img title=\"Do NOT put the cursor here because then the QR reader won't work\" src=\"data:image/png;base64,")
             .append(new Base64(false).getBase64StringFromBinary(qrImage))
             .append("\"></td></tr><tr><td id=\"waiting\" style=\"padding-top:")
             .append(qrInitOn ? 0 : 15)
             .append("pt\" align=\"center\"><img title=\"Something is running...\" src=\"images/waiting.gif\"></td></tr></table></td></tr>");     
    }

    static void printQRCode4Shop(HttpServletResponse response,
                                 SavedShoppingCart savedShoppingCart,
                                 byte[] qrImage,
                                 HttpServletRequest request,
                                 String id) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(
            cometJavaScriptSupport(id, request, "document.forms.restore.submit()", "", "document.location.href = 'result'"),
            bodyStartQR("<form name=\"restore\" method=\"POST\" action=\"shop\"></form>"),
            currentOrder(savedShoppingCart).toString() +
            bodyEndQR(qrImage, true)));
    }
    
    static String selectionButtons(FuelTypes[] fuelTypes) throws IOException {
        StringBuffer s = new StringBuffer("<tr><td><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\">" +
                                          "<tr style=\"text-align:center\"><td>Fuel Type</td><td>Price/Litre</td></tr>");
        for (FuelTypes fuelType : fuelTypes) {
            s.append("<tr id=\"")
             .append(fuelType.toString())
             .append(".\"><td style=\"height:6pt\"></td></tr><tr title=\"Selected fuel type\" id=\"")
             .append(fuelType.toString())
             .append("\" style=\"box-shadow:3pt 3pt 3pt #D0D0D0;text-align:center;background:")
             .append(fuelType.background);
            if (fuelTypes.length > 1) {
                s.append(";cursor:pointer\" onclick=\"selectFuel('")
                 .append(fuelType.toString())
                 .append("', this)");
            }
            s.append("\"><td style=\"font-size:11pt;font-family:" + FONT_ARIAL + ";padding:6pt;min-width:10em\">")
             .append(fuelType.commonName)
             .append("</td><td style=\"font-size:11pt;font-family:" + FONT_ARIAL + ";padding:6pt 12pt 6pt 12pt\">")
             .append(fuelType.displayPrice())
             .append("</td></tr>");
        }
        return s.append("</table></td></tr>").toString();
    }

    static void printQRCode4GasStation(HttpServletResponse response,
                                       byte[] qrImage,
                                       HttpServletRequest request,
                                       String id) throws IOException, ServletException {
        StringBuffer s = new StringBuffer("function selectFuel(fuelType, element) {\n");
        for (FuelTypes fuelType : FuelTypes.values()) {
            s.append("  if (fuelType == '")
             .append(fuelType.toString())
             .append("') {\n" +
             "    document.getElementById('" + GasStationServlet.FUEL_TYPE_FIELD + "').value = fuelType;\n" +
             "    element.style.cursor = 'default';\n" +
             "   } else {\n" +
             "    document.getElementById('")
             .append(fuelType.toString())
             .append("').style.display = 'none';\n" +
             "    document.getElementById('")
            .append(fuelType.toString())
            .append(".').style.display = 'none';\n" +
            "}\n");
        }
        HTML.output(response, HTML.getHTML(
            cometJavaScriptSupport(id, request, 
                                  "document.location.href='home'",
                                  "      document.getElementById('phase').innerHTML = '3. Waiting For User Authorization...';\n" +
                                  "      document.getElementById('authtext').style.display = 'table-row';\n",
                                  "document.forms.fillgas.submit()") +
            s.append(
            "  setQRDisplay(true);\n" +
            "  document.getElementById('phase').innerHTML = '2. Initiate Payment';\n" +
            "}\n").toString(),
            bodyStartQR("<form name=\"fillgas\" method=\"POST\" action=\"gasstation\">" +
                        "<input name=\"" + GasStationServlet.FUEL_TYPE_FIELD + "\" " +
                        "id=\"" + GasStationServlet.FUEL_TYPE_FIELD + "\" type=\"hidden\"></form>"),
            gasStation("1. Select Fuel Type", false) +
            "<tr id=\"authtext\" style=\"display:none\"><td style=\"width:40em;padding-bottom:15pt\">Since the quantity of fuel is usually not known in an advance, " +
            "automated fueling stations require <i>pre-authorization</i> of a fixed maximum amount of money (" +
            Currencies.USD.amountToDisplayString(new BigDecimal(GasStationServlet.STANDARD_RESERVATION_AMOUNT_X_100 / 100), true) +
            " in the demo), while only the actual amount " +
            "needed is eventually withdrawn from the client's account. This should be reflected in the Wallet's " +
            "authorization display as well.</td></tr>" +
            selectionButtons(FuelTypes.values()) +
            bodyEndQR(qrImage, false)));
    }

    static void androidPluginActivate(HttpServletResponse response, String url) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(
            HTML.getHTML(null,
                         "onload=\"document.location.href='" + url + "'\"" ,
                         "<tr><td width=\"100%\" align=\"center\" valign=\"middle\"><b>Please wait while the Wallet plugin starts...</b></td></tr>").getBytes("UTF-8"));
    }

    static void autoPost(HttpServletResponse response, String url) throws IOException, ServletException {
        HTML.output(response, "<html><body onload=\"document.forms.posting.submit()\">Redirecting..." +
                              "<form name=\"posting\" action=\"" + url + "\" method=\"POST\"></form></body></html>");
    }

    static void androidPage(HttpServletResponse response) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(null, null,
            "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
            "<table style=\"max-width:600px;\" cellpadding=\"4\">" +
            "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Android Wallet<br>&nbsp;</td></tr>" +
             "<tr><td style=\"text-align:left\">Note: The Android Wallet is currently a <i>proof-of-concept implementation</i> rather than a product.</td></tr>" +
            "<tr><td>Installation: <a href=\"https://play.google.com/store/apps/details?id=org.webpki.mobile.android\">" +
              "https://play.google.com/store/apps/details?id=org.webpki.mobile.android</a></td></tr>" +
            "<tr><td>Enroll payment <i>test credentials</i> by surfing (with the Android device...) to: <a href=\"https://mobilepki.org/webpay-keyprovider\">" +
              "https://mobilepki.org/webpay-keyprovider</td></tr>" +
            "<tr><td>Unlike the Windows, Linux, and OS/X-based Wallet, the Android version also supports remote operation using QR codes.  This mode is " +
              "indicated by the following image in the Merchant Web application:</td></tr>" +
            "<tr><td align=\"center\"><img src=\"images/paywith-saturnqr.png\"></td></tr>" +
            "</table></td></tr>"));       
    }

    static void gasFillingPage(HttpServletResponse response, FuelTypes fuelType, int maxVolume) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(
            STICK_TO_HOME_URL +
            updatePumpDisplay(fuelType) +
            "var decilitres = 0;\n" +
            "var timer = null;\n" +
            "function execute() {\n" +
            "  if (timer) {\n"+
            "    clearInterval(timer);\n" +
            "    doneFilling();\n" +
            "  } else {\n" +
            "    document.getElementById('cmd').value = '\u00a0\u00a0\u00a0Click here to \"simulate\" that you have finished pumping...\u00a0\u00a0\u00a0';\n" +
            "    timer = setInterval(function () {\n" +
            "      updatePumpDisplay(++decilitres);\n" +
            "      if (decilitres == " + maxVolume + ") {\n" +
            "        clearInterval(timer);\n" +
            "        doneFilling();\n" +
            "      }\n" +
            "    }, 100);\n" +
            "  }\n" +
            "}\n" +
            "function doneFilling() {\n" +
            "  document.getElementById('" + GasStationServlet.FUEL_TYPE_FIELD + "').value = '" + fuelType.toString() + "';\n" +
            "  document.getElementById('" + GasStationServlet.FUEL_DECILITRE_FIELD + "').value = decilitres;\n" +
            "  document.forms.finish.submit();\n" +
            "}\n", 
            ">" + GAS_PUMP_LOGO + "><form name=\"finish\" action=\"result\" method=\"POST\">" +
            "<input name=\"" + GasStationServlet.FUEL_TYPE_FIELD + "\" " +
            "id=\"" + GasStationServlet.FUEL_TYPE_FIELD + "\" type=\"hidden\">" +
            "<input name=\"" + GasStationServlet.FUEL_DECILITRE_FIELD + "\" " +
            "id=\"" + GasStationServlet.FUEL_DECILITRE_FIELD + "\" type=\"hidden\">" +
            "</form",
            gasStation("4. Fill Tank", true) +
            selectionButtons(new FuelTypes[]{fuelType}) +
            "<tr><td style=\"padding-top:20pt\" align=\"center\"><input id=\"cmd\" type=\"button\" style=\"min-width:12em;font-size:11pt;padding:4pt\" value=\"Start pumping!\" onclick=\"execute()\"></td></tr>" +
            "</table></td></tr>"));
    }

}
