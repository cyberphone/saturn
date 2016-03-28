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

import javax.servlet.http.HttpServletResponse;

import org.webpki.util.HTMLEncoder;

import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.ErrorReturn;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;

import org.webpki.w2nbproxy.ExtensionPositioning;

public class HTML {

    static final int PAYMENT_TIMEOUT_INIT            = 5000;
    
    static final String STICK_TO_HOME_URL            =
                    "history.pushState(null, null, 'home');\n" +
                    "window.addEventListener('popstate', function(event) {\n" +
                    "    history.pushState(null, null, 'home');\n" +
                    "});";
    
    static final String FONT_VERDANA = "Verdana,'Bitstream Vera Sans','DejaVu Sans',Arial,'Liberation Sans'";
    static final String FONT_ARIAL = "Arial,'Liberation Sans',Verdana,'Bitstream Vera Sans','DejaVu Sans'";
    
    static final String HTML_INIT = 
        "<!DOCTYPE html>"+
        "<html><head><meta charset=\"UTF-8\"><link rel=\"shortcut icon\" href=\"favicon.ico\">"+
//        "<meta name=\"viewport\" content=\"initial-scale=1.0\"/>" +
        "<title>W2NB Payment Demo</title>"+
        "<style type=\"text/css\">html {overflow:auto}\n"+
        ".point {text-align:center;font-family:courier;font-weight:bold;font-size:10pt;border-radius:3pt;border-width:1px;border-style:solid;border-color:#B0B0B0;display:inline-block;padding:1.5pt 3pt 1pt 3pt}\n" +
        ".tftable {border-collapse:collapse;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
        ".tftable th {font-size:10pt;background:" +
          "linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
          "border-width:1px;padding:4pt 10pt 4pt 10pt;border-style:solid;border-color:#a9a9a9;" +
          "text-align:center;font-family:" + FONT_ARIAL + "}\n" +
        ".tftable td {background-color:#FFFFE0;font-size:10pt;border-width:1px;padding:4pt 8pt 4pt 8pt;border-style:solid;border-color:#a9a9a9;font-family:" + FONT_ARIAL + "}\n" +
        "body {font-size:10pt;color:#000000;font-family:" + FONT_VERDANA + ";background-color:white}\n" +
        "a {font-weight:bold;font-size:8pt;color:blue;font-family:" + FONT_ARIAL + ";text-decoration:none}\n" +
        "td {font-size:8pt;font-family:" + FONT_VERDANA + "}\n" +
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
            + "Web2Native Bridge<br><span style=\"font-size:8pt\">Payment Demo Home</span></div>"
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

    public static void homePage(HttpServletResponse response,
                                boolean debugMode,
                                boolean reserveMode) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(null, null,
                "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                "<table style=\"max-width:600px;\" cellpadding=\"4\">" +
                   "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Web2Native Bridge - Web Payment Demo<br>&nbsp;</td></tr>" +
                   "<tr><td style=\"text-align:left\">This application is a demo of what a &quot;Wallet&quot; based on the Web2Native Bridge " +
                   "could offer for <span style=\"color:red\">Decentralized Web Payments</span>.&nbsp; Primary features:<ul>" +
                   "<li>All messages are <i>digitally signed</i> enabling a security level comparable to a PIN-code terminal and chip-card in a brick-and-mortar shop</li>" +
                   "<li>Tunneling encrypted data (like in SET) hides senstive customer data from merchants without using &quot;tokenization&quot;</li>" +
                   "<li>Needs an &quot;extra pipe to the bank&quot; like 3D Secure but without a central registry</li>" +
                   "<li>Equally applicable for traditional card payment (&quot;pull&quot;) networks  as for bank-2-bank (&quot;push&quot;) schemes</li>" +
                   "<li>All messages are coded in JSON</li>" +
                   "<li>Consumers only deal with payment instruments visualized as cards (like they did <i>before</i> the Web)</li>" +
                   "<li>Designed to also work in an NFC/BLE setup where the wallet resides in a mobile device and payments are " +
                   "performed locally as well as on the Web (through Web-NFC)</li>" +
                   "</ul>" +
                   "Note that the Wallet is <i>pre-configured</i> with payment credentials requiring no signup etc.&nbsp;&nbsp;" +
                   "<a href=\"https://github.com/cyberphone/web2native-bridge#installation\">Install Wallet</a>." +
                   "</td></tr>" +
                   "<tr><td align=\"center\"><table cellspacing=\"0\">" +
//TODO
//                   "<tr style=\"text-align:left\"><td><a href=\"" + "hh" + "/cards\">Initialize Payment Cards&nbsp;&nbsp;</a></td><td><i>Mandatory</i> First Step</td></tr>" +
                   "<tr><td style=\"text-align:left;padding-bottom:5pt\"><a href=\"" + "shop" + 
                     "\">Go To Merchant</a></td><td style=\"text-align:left;padding-bottom:5pt\">Shop Till You Drop!</td></tr>" +
                   "<form name=\"options\" method=\"POST\"><tr>" +
                   "<td style=\"text-align:center\"><input type=\"checkbox\" name=\"" + 
                   HomeServlet.RESERVE_MODE_SESSION_ATTR + "\" onchange=\"document.forms.options.submit()\"" +
                   (reserveMode ? " checked" : "") +
                   "></td><td>Reserve+Finalize Payment Mode</td></tr>" +
                   "<tr><td style=\"text-align:center\"><input type=\"checkbox\" name=\"" +
                   HomeServlet.DEBUG_MODE_SESSION_ATTR + "\" onchange=\"document.forms.options.submit()\"" +
                   (debugMode ? " checked" : "") +
                   "></td><td>Debug (JSON Message Dump) Option</td></form></tr>" +
                    "<tr><td style=\"text-align:center;padding-top:15pt;padding-bottom:5pt\" colspan=\"2\"><b>Documentation</b></td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/openkeystore/resources/docs/web2native-bridge.pdf\">Web2Native Bridge</a></td><td>&quot;Executive Level&quot; Description</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"http://xmlns.webpki.org/webpay/v1\">Payment System</a>&nbsp;&nbsp;</td><td>State Diagram Etc.</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://github.com/cyberphone/web2native-bridge\">Demo Source Code</a></td><td>For Nerds...</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://mobilepki.org/jcs\">JCS</a></td><td>JSON Cleartext Signature</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/openkeystore/resources/docs/keygen2.html\">KeyGen2</a></td><td>Wallet Enrollment Protocol</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/openkeystore/resources/docs/sks-api-arch.pdf\">SKS</a></td><td>Wallet Credential Store</td></tr>" +
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
                                             int index) {
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

    private static String price(long priceX100) {
        return (MerchantService.currency.symbolFirst ? MerchantService.currency.symbol : "")
               + String.valueOf(priceX100 / 100) + "."
               + String.valueOf((priceX100 % 100) / 10)
               + String.valueOf(priceX100 % 10)
               + (MerchantService.currency.symbolFirst ? "" : MerchantService.currency.symbol);
    }
    
    public static void merchantPage(HttpServletResponse response,
                                    SavedShoppingCart savedShoppingCart) throws IOException, ServletException {
        StringBuffer temp_string = new StringBuffer(
            "\nfunction closeFundFlash() {\n" +
            "    setTimeout(function() {\n" +
            "        document.getElementById('fundlimit').style.visibility = 'hidden';\n" +
            "    }, 5000);\n" +
            "}\n\n" +
            "function userPay() {\n" +
            "    if (getTotal()) {\n" +
            "        document.getElementById('" + UserPaymentServlet.SHOPPING_CART_FORM_ATTR + "').value = JSON.stringify(shoppingCart);\n" +
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
            "    return ");
        if (MerchantService.currency.symbolFirst) {
            temp_string.append('\'')
                       .append(MerchantService.currency.symbol)
                       .append("' + ");
        }
        temp_string.append("Math.floor(priceX100 / 100) + '.' +  Math.floor((priceX100 % 100) / 10) +  Math.floor(priceX100 % 10)");
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
            "<form name=\"shoot\" method=\"POST\" action=\"userpay\">" +
            "<input type=\"hidden\" name=\"" + UserPaymentServlet.SHOPPING_CART_FORM_ATTR + "\" id=\"" + UserPaymentServlet.SHOPPING_CART_FORM_ATTR + "\">" +
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

    public static void userPayPage(HttpServletResponse response,
                                   SavedShoppingCart savedShoppingCart, 
                                   boolean tapConnectMode,
                                   boolean debugMode,
                                   String walletRequest) throws IOException, ServletException {
        String connectMethod = tapConnectMode ? "tapConnect" : "nativeConnect";
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
         .append("</td></tr></table></td></tr><tr>");
        if (tapConnectMode) {
            s.append("<td align=\"center\"><img id=\"state\" title=\"Please tap your mobile wallet!\" " +
                     "src=\"images/NFC-N-Mark-Logo.svg\" style=\"height:120pt;margin-top:10pt\"></td>");
        } else {
            s.append("<td style=\"padding:20pt\" id=\"wallet\">&nbsp;</td>");
        }
        s.append("</tr></table>" +
                 "<form name=\"shoot\" method=\"POST\" action=\"transact\">" +
                 "<input type=\"hidden\" name=\"" + UserPaymentServlet.AUTHDATA_FORM_ATTR + "\" id=\"" + UserPaymentServlet.AUTHDATA_FORM_ATTR + "\">");
        if (debugMode) {
            s.append("<input type=\"hidden\" name=\"" + UserPaymentServlet.INITMSG_FORM_ATTR + "\" id=\"" + UserPaymentServlet.INITMSG_FORM_ATTR + "\">");
        }
        s.append("</form>" +
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
                    "      if (message[\"@context\"] != \"" + BaseProperties.W2NB_WEB_PAY_CONTEXT_URI + "\") {\n" +
                    "        setFail(\"Missing or wrong \\\"@context\\\"\");\n" +
                    "        return;\n" +
                    "      }\n" +
                    "      var qualifier = message[\"@qualifier\"];\n" +
                    "      if ((initMode && qualifier != \"" + Messages.WALLET_INITIALIZED.toString() + "\")  ||\n" +
                    "          (!initMode && qualifier != \"" +  Messages.PAYER_AUTHORIZATION.toString() + "\")) {\n" +  
                    "        setFail(\"Wrong or missing \\\"@qualifier\\\"\");\n" +
                    "        return;\n" +
                    "      }\n" +
                    "      if (initMode) {\n");
       if (debugMode) {
           temp_string.append(
                    "        document.getElementById(\"" + UserPaymentServlet.INITMSG_FORM_ATTR + "\").value = JSON.stringify(message);\n");
       }
       if (!tapConnectMode) {
           temp_string.append(
                    "        document.getElementById(\"wallet\").style.height = message." + 
                                         BaseProperties.WINDOW_JSON + "." + BaseProperties.HEIGHT_JSON + " + 'px';\n");
       }
       temp_string.append(
                    "        initMode = false;\n" +
                    "        nativePort.postMessage(invocationData);\n" +
                    "      } else {\n" +
                    "        document.getElementById(\"" + UserPaymentServlet.AUTHDATA_FORM_ATTR + "\").value = JSON.stringify(message);\n" +
                    "        document.forms.shoot.submit();\n" +
                    "      }\n"+
                    "    });\n");
       if (tapConnectMode) {
           temp_string.append(
                   "    port.addConnectionListener(function(initialize) {\n" +
                   "      if (initialize) {\n" +
                   "        document.getElementById(\"state\").src = \"images/loading-gears-animation-3.gif\";\n" +
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
                    "window.addEventListener(\"beforeunload\", function(event) {\n" +
                    "  closeWallet();\n" +
                    "});\n\n");
        HTML.output(response, HTML.getHTML(temp_string.toString(),
                              "onload=\"activateWallet()\"",
                              s.toString()));
    }

    public static void resultPage(HttpServletResponse response,
                                  boolean debugMode,
                                  PaymentRequest paymentRequest, 
                                  PayerAccountTypes accountType,
                                  String accountReference) throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">");
        s.append("<table>" +
                 "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Order Status<br>&nbsp;</td></tr>" +
                 "<tr><td style=\"text-align:center;padding-bottom:15pt;font-size:10pt\">Dear customer, your order has been successfully processed!</td></tr>" +
                 "<tr><td><table class=\"tftable\"><tr><th>Our Reference</th><th>Amount</th><th>")
         .append(accountType.isAcquirerBased() ? "Card" : "Account")
         .append(" Type</th><th>")
         .append(accountType.isAcquirerBased() ? "Card Reference" : "Account Number")
         .append("</th></tr>" +
                 "<tr><td style=\"text-align:center\">")
         .append(paymentRequest.getReferenceId())
         .append("</td><td style=\"text-align:center\">")
         .append(paymentRequest.getCurrency().amountToDisplayString(paymentRequest.getAmount()))
         .append("</td><td style=\"text-align:center\">")
         .append(accountType.getCommonName())
         .append("</td><td style=\"text-align:center\">")
         .append(accountReference)
         .append("</td></tr></table></td></tr>");
        if (debugMode) {
            s.append("<tr><td style=\"text-align:center;padding-top:20pt\"><a href=\"debug\">Show Debug Info</a></td></tr>");
        }
        s.append("</table></td></tr></table></td></tr>");
        HTML.output(response, 
                    HTML.getHTML(STICK_TO_HOME_URL, null, s.toString()));
    }

    public static void debugPage(HttpServletResponse response, String string, boolean clean) throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
                  "<table>" +
                  "<tr><td style=\"padding-top:50pt;text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                  "\">Payment Session Debug Information&nbsp;<br></td></tr><tr><td style=\"text-align:left\">")
          .append(string)
          .append("</td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(clean ? null : STICK_TO_HOME_URL, null,s.toString()));
    }

    public static void paymentError(HttpServletResponse response, boolean debugMode, ErrorReturn errorReturn)
                                    throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
                 "<table>" +
                 "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                 "\">Payment Failure&nbsp;<br></td></tr><tr><td style=\"text-align:center\">")
         .append(errorReturn.getClearText())
         .append("</td></tr>");
        if (debugMode) {
            s.append("<tr><td style=\"text-align:center;padding-top:20pt\"><a href=\"debug\">Show Debug Info</a></td></tr>");
        }
        s.append("</table></td></tr>");
        HTML.output(response, HTML.getHTML(STICK_TO_HOME_URL, null,s.toString()));
     }

    public static void errorPage(HttpServletResponse response, String error, boolean system)
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
}
