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

import java.math.BigDecimal;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.util.HTMLEncoder;

import org.webpki.saturn.common.MobileProxyParameters;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.Messages;

import org.webpki.saturn.w2nb.support.W2NB;

import org.webpki.w2nbproxy.ExtensionPositioning;

public class HTML implements MerchantSessionProperties {

    static final String STICK_TO_HOME_URL            =
                    "history.pushState(null, null, 'home');\n" +
                    "window.addEventListener('popstate', function(event) {\n" +
                    "    history.pushState(null, null, 'home');\n" +
                    "});\n";
    
    static final String FONT_VERDANA = "Verdana,'Bitstream Vera Sans','DejaVu Sans',Arial,'Liberation Sans'";
    static final String FONT_ARIAL = "Arial,'Liberation Sans',Verdana,'Bitstream Vera Sans','DejaVu Sans'";
    
    static final String GAS_PUMP_LOGO = "<img src=\"images/gaspump.svg\" title=\"For showing context\" style=\"width:30pt;position:absolute;right:10pt;top:10pt\"";

    static final String SATURN_PAY_BUTTON = "SaturnPay";
    static final String SATURN_PAY_WAIT   = "Waiting";
    
    static final BigDecimal HUNDRED = new BigDecimal(100);

    static String getHTML(String javascript, String bodyscript, String box) {
        StringBuilder s = new StringBuilder(
            "<!DOCTYPE html>"+
            "<html><head>" +
            "<meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<link rel=\"icon\" href=\"saturn.png\" sizes=\"192x192\">"+
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">" +
            "<title>Saturn Payment Demo</title>");

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
        s.append("><div onclick=\"document.location.href='home" + 
            "'\" title=\"Home sweet home...\" style=\"cursor:pointer;position:absolute;top:15px;" + 
            "left:15px;z-index:5;visibility:visible;padding:5pt 8pt 5pt 8pt;font-size:10pt;" + 
            "text-align:center;background: radial-gradient(ellipse at center, rgba(255,255,255,1) " + 
            "0%,rgba(242,243,252,1) 38%,rgba(196,210,242,1) 100%);border-radius:8pt;border-width:1px;" + 
            "border-style:solid;border-color:#B0B0B0;box-shadow:3pt 3pt 3pt #D0D0D0;}\">" + 
            "Saturn <font color=\"red\">3</font><br><span style=\"font-size:8pt\">Payment Demo</span></div>" + 
            "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" height=\"100%\">")
         .append(box)
         .append("</table></body></html>");
        return s.toString();
    }
    
    static void output(HttpServletResponse response, String html) throws IOException, ServletException {
//        System.out.println(html);
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        byte[] data = html.getBytes("UTF-8");
        response.setContentLength(data.length);
        ServletOutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write(data);
        servletOutputStream.flush();
    }

    static void homePage(HttpServletResponse response) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(
                "function invoke(relativeUrl) {\n" +
                "  document.forms.shoot." + HomeServlet.GOTO_URL + ".value = relativeUrl;\n" +
                "  document.forms.shoot.submit();\n" +
                "}"
                , null,
                "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                "<table class=\"tighttable\" style=\"max-width:600px;\" cellpadding=\"4\">" +
                   "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Saturn - Web Payment Demo<br>&nbsp;</td></tr>" +
                   "<tr><td style=\"text-align:left\">This application is a demo of version 3 of the Saturn <i>payment authorization</i> scheme.&nbsp; " +
                   "Saturn depends on a <i>native</i> &quot;Wallet&quot; application" +
                   ".&nbsp; Primary Saturn features include:<ul>" +
                   "<li style=\"padding-top:0pt\">Fully <i>decentralized</i> operation (no dependency on central registries like in 3D Secure)</li>" +
                   "<li><i>Digitally signed</i> messages enable a protocol-level security comparable to a payment terminal and EMV card in a physical shop</li>" +
                   "<li>Encryption hides customer data from merchants, eliminating " +
                   "the need for third-party &quot;tokenization&quot; services as well as supporting GDPR</li>" +
                   "<li>Private messaging through the payment backend makes it easy applying RBA (Risk Based Authentication) for high-value or &quot;suspicious&quot; transactions</li>" +
                   "<li>Equally applicable for legacy card payment networks as for bank-2-bank schemes including SEPA SCT</li>" +
                   "<li>In addition to supporting Web payments, Saturn is <i>also intended to be usable in traditional payment scenarios " +
                   "including with POS terminals and gas pumps using an NFC/BLE connection to the Wallet</i></li>" +
                   "<li>JSON based messaging</li>" +
                   "<li>Consumers only deal with payment instruments visualized as cards (like they did <i>before</i> the Web)</li>" +
                   "</ul>" +
                   "For testing you need " +
                   (MerchantService.desktopWallet ? "either the <a href=\"android\">Android</a> or the " +
                   "<a href=\"https://github.com/cyberphone/web2native-bridge#installation\">desktop</a>" :
                   " the <a href=\"android\">Android</a>") +
                   " version of the Wallet software.</td></tr>" +
                   "<tr><td align=\"center\"><table cellspacing=\"0\">" +
                   "<tr><td style=\"text-align:left;padding-bottom:5pt\">" +
                     "<a href=\"javascript:invoke('shop')\">Go To Merchant</a>&nbsp;&nbsp;</td>" +
                     "<td style=\"text-align:left;padding-bottom:5pt\">Shop Till You Drop!</td>" +
                   "</tr>" +
                   "<tr><td style=\"text-align:left;padding-bottom:5pt\">" +
                     "<a href=\"javascript:invoke('gasstation')\">Gas Station</a></td>" +
                     "<td style=\"text-align:left;padding-bottom:5pt\">Another Payment Scenario</td>" +
                   "</tr>" +
                   "<form name=\"shoot\" method=\"POST\">" +
                   "<input type=\"hidden\" name=\"" + HomeServlet.GOTO_URL +"\">" +
                   "<tr><td style=\"text-align:center\"><input type=\"checkbox\" name=\"" +
                   DEBUG_MODE_SESSION_ATTR + "\"></td><td>Debug (JSON Message Dump) Option</td></tr>" +
                   "<tr><td style=\"text-align:center\"><input type=\"checkbox\" name=\"" +
                   REFUND_MODE_SESSION_ATTR + "\"></td><td>Refund Option</td></tr>" +
                   "</form>" +
                   "<tr><td style=\"text-align:center;padding-top:15pt;padding-bottom:5pt\" colspan=\"2\"><b>Documentation</b></td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/saturn/\">Saturn Home</a></td><td>Presentation Etc.</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://github.com/cyberphone/saturn\">Source Code</a></td><td>For Nerds...</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/jsf.html\">JSF</a></td><td>JSON Signature Format</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/jef.html\">JEF</a></td><td>JSON Encryption Format</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/web/yasmin.html\">YASMIN</a></td><td>JSON Message Scheme</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/keygen2.html\">KeyGen2</a></td><td>Wallet Credential Enrollment Protocol</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/sks-api-arch.pdf\">SKS</a></td><td>Wallet Credential Store</td></tr>" +
                 "</table></td></tr></table></td></tr>"));
    }

    static String javaScript(String string) {
        StringBuilder s = new StringBuilder();
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

    private static StringBuilder productEntry(StringBuilder temp_string,
                                              SpaceProducts product_entry,
                                              String sku,
                                              ShoppingCart savedShoppingCart,
                                              int index) throws IOException {
        int quantity = savedShoppingCart.items.containsKey(sku) ? 
                    savedShoppingCart.items.get(sku).intValue() : 0;
        StringBuilder s = new StringBuilder(
            "<tr style=\"text-align:center\"><td><img src=\"images/")
        .append(product_entry.imageUrl)
        .append("\" class=\"product\"></td><td style='text-align:left'>")
        .append(ReceiptServlet.showLines(product_entry.description))
        .append("</td><td style=\"text-align:right\">")
        .append(price(product_entry.unitPriceX100))
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
            "<td style=\"border-width:0px;padding:0px;margin:0px\"><input max=\"6\" style='width:2em' type=\"text\" name=\"p")
        .append(index)
        .append("\" value=\"")
        .append(quantity)
        .append("\" class=\"quantity\" oninput=\"updateInput(")
        .append(index)
        .append(", this);\" autocomplete=\"off\"/></td>" +
            "</tr>" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\">" + 
            "<input type=\"button\" class=\"updnbtn\" value=\"&#x25bc;\" title=\"Less\" " + 
            "onclick=\"updateQuantity(this.form.p")
        .append(index)
        .append(", -1, ")
        .append(index)
        .append(")\"></td></tr></table></form></td></tr>");
        temp_string.insert(0, "shoppingCart[" + index + "] = new webpki.ShopEntry(" +
                       product_entry.unitPriceX100 + 
                       ",'" + product_entry.name + "','" + sku + "'," + quantity + ");\n");        
        return s;
    }

    private static String price(long priceX100) throws IOException {
        return MerchantService.currency.amountToDisplayString(
                new BigDecimal(priceX100).divide(HUNDRED), false);
    }
    
    static void merchantPage(HttpServletResponse response,
                             ShoppingCart savedShoppingCart) throws IOException, ServletException {
        StringBuilder temp_string = new StringBuilder(
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

        StringBuilder page_data = new StringBuilder(
            "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
            "<table>" +
            "<tr><td style='text-align:center;padding-bottom:2em'>" +
            "<img src='images/spaceshop-logo.svg'></td></tr>" +
            "<tr><td id=\"result\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
            "<tr><th>Image</th><th>Description</th><th>Price</th><th>Quantity</th></tr>");
        int q = 0;
        for (String sku : SpaceProducts.products.keySet()) {
            page_data.append(productEntry(temp_string,
                                          (SpaceProducts) SpaceProducts.products.get(sku),
                                          sku, 
                                          savedShoppingCart, q++));
        }
        page_data.append(
            "</table></td></tr><tr><td style=\"padding-top:10pt\">" +
            "<table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
                    "<tr><th>Subtotal</th><td style=\"text-align:right\" id=\"total\">")
        .append(price(savedShoppingCart.subtotal))
        .append("</td></tr>" +
            "</table></td></tr>" +
            "<tr><td style=\"text-align:center;padding-top:10pt\" id=\"pay\">")
        .append(fancyButton("Checkout..", "Paying time has come...", "userPay()"))
        .append("</td></tr>" +
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
            "><div id=\"emptybasket\" class=\"toasting\">Nothing ordered yet...</div",
            page_data.toString()));
    }

    static StringBuilder currentOrder(ShoppingCart savedShoppingCart) throws IOException {
        StringBuilder s = new StringBuilder(
                "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                "<table>" +
                "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:"
                + FONT_ARIAL + "\">Current Order<br>&nbsp;</td></tr>" +
                "<tr><td id=\"result\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
                "<tr><th>Description</th><th>Price</th><th>Quantity</th><th>Subtotal</th></tr>");
            for (String sku : savedShoppingCart.items.keySet()) {
                SpaceProducts product_entry = (SpaceProducts) SpaceProducts.products.get(sku);
                s.append("<tr><td>")
                 .append(ReceiptServlet.showLines(product_entry.description))
                 .append("</td><td style=\"text-align:right\">")
                 .append(price(product_entry.unitPriceX100))
                 .append("</td><td style=\"text-align:right\">")
                 .append(savedShoppingCart.items.get(sku).intValue())
                 .append("</td><td style=\"text-align:right\">")
                 .append(price(product_entry.unitPriceX100 * savedShoppingCart.items.get(sku).intValue()))
                 .append("</td></tr>");                
            }
            s.append(
                 "<tr><td colspan=\"4\" style=\"height:1px;padding:0px\"></td></tr>" +
                 "<tr><td colspan=\"3\" style=\"text-align:right\">Subtotal</td><td style=\"text-align:right\">")
             .append(price(savedShoppingCart.subtotal))
             .append("</td></tr>" +
                 "<tr><td colspan=\"3\" style=\"text-align:right\">Tax (" +
                     ShoppingCart.TAX + "%)</td><td style=\"text-align:right\">")
             .append(price(savedShoppingCart.tax))
             .append("</td></tr>" +
                "</table></td></tr><tr><td style=\"padding-top:10pt\">" +
                "<table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
                "<tr><th style=\"text-align:center\">Amount to Pay</th><td style=\"text-align:right\" id=\"total\">")
             .append(price(savedShoppingCart.roundedPaymentAmount))
             .append("</td></tr></table></td></tr>");
       return s;
    }

    static void w2nbWalletPay(HttpServletResponse response,
                              boolean firefox,
                              ShoppingCart savedShoppingCart, 
                              boolean tapConnectMode,
                              boolean debugMode,
                              String walletRequest) throws IOException, ServletException {
        String connectMethod = tapConnectMode ? "tapConnect" : "nativeConnect";
        StringBuilder s = currentOrder(savedShoppingCart);
      
        if (tapConnectMode) {
            s.append("<tr><td align=\"center\"><img id=\"state\" title=\"Please tap your mobile wallet!\" " +
                     "src=\"images/NFC-N-Mark-Logo.svg\" style=\"height:120pt;margin-top:10pt\"></td>");
        } else {
            s.append("<tr><td style=\"padding:20pt;text-align:center\" id=\"wallet\"><img src=\"images/waiting.gif\"></td>");
        }
        s.append("</tr></table>" +
                 "<form name=\"restore\" method=\"POST\" action=\"shop\">" +
                 "</form></td></tr>");
        
        StringBuilder temp_string = new StringBuilder("\n\n\"use strict\";\n\nvar invocationData = ")
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
                    "  document.location.href='home';\n" +
                    "}\n\n" +

                    "function activateWallet() {\n" +
                    "  var initMode = true;\n");
        if (firefox) {
            temp_string.append("  setTimeout(function() {\n");
        }
                    
        temp_string.append("  if (!navigator.")
             .append(connectMethod)
             .append(") {\n" +
                    "    setFail('\"navigator.")
             .append(connectMethod)
             .append("\" not found, \\ncheck browser extension install and settings');\n" +
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
                    "      if ((initMode && qualifier != '" + W2NB.PAYMENT_CLIENT_IS_READY.toString() + "')  ||\n" +
                    "          (!initMode && qualifier != '" +  Messages.PAYER_AUTHORIZATION.toString() + "')) {\n" +  
                    "        setFail('Wrong or missing \"@qualifier\"');\n" +
                    "        return;\n" +
                    "      }\n" +
                    "      if (initMode) {\n");
       if (debugMode) {
           temp_string.append(
                    "        console.info(JSON.stringify(message));\n");
       }
       if (!tapConnectMode) {
           temp_string.append(
                    "        document.getElementById('wallet').style.height = message." + 
                                W2NB.WINDOW_JSON + "." + W2NB.HEIGHT_JSON + " + 'px';\n");
       }
       temp_string.append(
                    "        initMode = false;\n" +
                    "        nativePort.postMessage(invocationData);\n" +
                    "      } else {\n" +
                    "// This is it...transfer the Wallet authorization data back to the Merchant server\n" +
                    "        fetch('authorize', {\n" +
                    "           headers: {\n" +
                    "             'Content-Type': 'application/json'\n" +
                    "           },\n" +
                    "           method: 'POST',\n" +
                    "           credentials: 'same-origin',\n" +
                    "           redirect: 'manual',\n" +
                    "           body: JSON.stringify(message)\n" +
                    "        }).then(function (response) {\n" +
                    "          if (response.type == 'opaqueredirect') {\n" +
                    "// \"Normal\" return\n" +
                    "            document.location.href='result';\n" +
                    "          }\n" +
                    "          return response.json();\n" +
                    "        }).then(function (resultData) {\n" +
                    "          if (typeof resultData == 'object' && !Array.isArray(resultData)) {\n" +
                    "// \"Exceptional\" return with error or RBA\n" +
                    "            nativePort.postMessage(resultData);\n" +
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
                   "        if (initMode) console.info('Wallet prematurely closed!');\n" +
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
                    "    console.info(err);\n" +
                    "  });\n");
       if (firefox) {
           temp_string.append("}, 10);\n");
       }
       temp_string.append("}\n\n");

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

    static StringBuilder receiptCore(ResultData resultData, boolean debugMode) throws IOException {
        StringBuilder s = new StringBuilder("<tr><td><table class=\"tftable\">"
                + "<tr><th>Our Reference</th><th>Amount</th><th>")
            .append(resultData.authorization.getPaymentMethod().isCardPayment() ? 
                                                                         "Card" : "Account")
            .append(" Type</th><th>")
            .append(resultData.authorization.getPaymentMethod().isCardPayment() ?
                                                               "Card Reference" : "Account Number")   
            .append("</th></tr><tr><td style='text-align:center;color:blue;cursor:pointer' " +
                    "onclick=\"document.location.href='")
            .append(resultData.walletRequest.receiptUrl)
            .append("'\">")  
            .append(resultData.authorization.getPayeeReferenceId())
            .append("</td><td style=\"text-align:center\">")
            .append(resultData.authorization.getCurrency().amountToDisplayString(
                    resultData.authorization.getAmount(), false))
            .append("</td><td style=\"text-align:center\">")
            .append(resultData.authorization.getPaymentMethod().getCommonName())
            .append("</td><td style=\"text-align:center\">")
            .append(resultData.authorization.getAccountReference())
            .append("</td></tr></table></td></tr>");
        if (debugMode) {
            s.append("<tr><td style=\"text-align:center;padding-top:20pt\">"
                    + "<a href=\"debug\">Show Debug Info</a></td></tr>");
        }
        return s;
    }

    static void shopResultPage(HttpServletResponse response,
                               boolean debugMode,
                               boolean pleaseRefund,
                               ResultData resultData) throws IOException, ServletException {
        StringBuilder s = new StringBuilder("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">")
            .append("<table>" +
                    "<tr><td style='text-align:center;padding-bottom:1em'><img src='images/spaceshop-logo.svg'></td></tr>" +
                    "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL)
            .append(resultData.transactionError == null ?
                      "\">Order Status" : ";color:red\">Failed = " + resultData.transactionError.toString())
            .append("<br>&nbsp;</td></tr>")
            .append(resultData.transactionError == null ?
                    "<tr><td style=\"text-align:center\">" +
                    "Dear customer, your order has been successfully processed!<br>&nbsp;</td></tr>" : "")
            .append(receiptCore(resultData, debugMode && !pleaseRefund))
            .append(optionalRefund(pleaseRefund))
            .append("</table></td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(STICK_TO_HOME_URL, null, s.toString()));
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
                                     boolean pleaseRefund,
                                     ResultData resultData) throws IOException, ServletException {
        if (resultData.transactionError == null) {
            StringBuilder s = new StringBuilder()
                .append(gasStation("Thank You - Welcome Back!", true))
                .append(selectionButtons(new FuelTypes[]{fuelType}))
                .append("<tr><td style=\"height:15pt\"></td></tr>")
                .append(receiptCore(resultData, debugMode && !pleaseRefund))
                .append(optionalRefund(pleaseRefund))
                .append("</table></td></tr>");
            HTML.output(response, 
                        HTML.getHTML(STICK_TO_HOME_URL + updatePumpDisplay(fuelType),
                                     "onload=\"updatePumpDisplay(" + decilitres + ")\">" +
                                     GAS_PUMP_LOGO,
                                     s.toString()));
        } else {
            shopResultPage(response, debugMode, pleaseRefund, resultData);
        }
    }

    static String optionalRefund(boolean pleaseRefund) {
        return !pleaseRefund ? "" :
            "<tr><td style=\"text-align:center;padding-top:20pt\">" +
            fancyButton("Please refund this transaction...", "Payback time has come...", "location.href='refund'") +
            "</td></tr>";
    }

    static void debugPage(HttpServletResponse response, String string, boolean clean) throws IOException, ServletException {
        StringBuilder s = new StringBuilder("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
                  "<table>" +
                  "<tr><td style=\"padding-top:50pt;text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                  "\">Payment Session Debug Information&nbsp;<br></td></tr><tr><td style=\"text-align:left\">")
          .append(string)
          .append("</td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(clean ? null : STICK_TO_HOME_URL, null,s.toString()));
    }

    static void errorPage(HttpServletResponse response, String error, boolean system)
                                 throws IOException, ServletException {
        StringBuilder s = new StringBuilder("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
                 "<table>" +
                 "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                 "\">")
         .append(system ? "System " : "")
         .append("Failure&nbsp;<br></td></tr><tr><td style=\"text-align:center\">")
         .append(HTMLEncoder.encodeWithLineBreaks(error.getBytes("UTF-8")))
         .append("</td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(STICK_TO_HOME_URL, null,s.toString()));
    }

    static void notification(HttpServletResponse response, String notification)
            throws IOException, ServletException {
        StringBuilder s = new StringBuilder("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" + 
            "<table>" +
            "<tr><td style=\"font-size:10pt;font-family:" + FONT_ARIAL +
            "\">")
        .append(notification)
        .append("</td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(STICK_TO_HOME_URL, null,s.toString()));
    }
    
    static StringBuilder fancyButton(String value, String title, String onclick) {
        return new StringBuilder("<div class=\"stdbtn\" id=\"cmd\" title=\"")
            .append(title)
            .append("\" onclick=\"")
            .append(onclick)
            .append("\">")
            .append(value)
            .append("</div>");
     }

    static void userChoosePage(HttpServletRequest request,
                               HttpServletResponse response,
                               ShoppingCart savedShoppingCart) throws IOException, ServletException {
        boolean android = HomeServlet.isAndroid(request);
        StringBuilder s = currentOrder(savedShoppingCart)
            .append("<tr><td style=\"padding-top:15pt\"><table style=\"margin-left:auto;margin-right:auto\">" +
                    "<tr><td style=\"padding-bottom:10pt;text-align:center;font-weight:bolder;font-size:10pt;font-family:" +
                    FONT_ARIAL + "\">Select Payment Method</td></tr>")
            .append(MerchantService.desktopWallet || android ?
                      "<tr><td id=\"" + SATURN_PAY_BUTTON + 
                      "\" style='text-align:center'><img title=\"Saturn\" style=\"cursor:pointer;height:4em\" src=\"images/saturn-pay.svg\" onclick=\"goSaturnGo()\"></td></tr>" +
                      "<tr><td id=\"" + SATURN_PAY_WAIT + 
                      "\" style=\"display:none\"><img title=\"Wait\" src=\"images/waiting.gif\"></td></tr>" 
                                                                :
                      "")
            .append(!android ?
                      "<tr><td style=\"padding-top:10pt;text-align:center\">" +
                      "<img title=\"Saturn QR\" style=\"cursor:pointer;height:4em\" " +
                      "src=\"images/saturn-pay-qr.svg\" onclick=\"document.location.href='qrdisplay'\"></td></tr>"
                                                               :
                      "")
            .append("<tr><td style='padding:10pt 0;text-align:center'><img title=\"VISA &amp; MasterCard\" style=\"cursor:pointer;height:4em\" src=\"images/legacy-visamc-pay.svg\" onclick=\"noSuchMethod(this)\"></td></tr>" +
                    "<tr><td style='text-align:center'><img title=\"PayPal\" style=\"cursor:pointer;height:4em\" src=\"images/paypal-pay.svg\" onclick=\"noSuchMethod(this)\"></td></tr>" +
                    "<tr><td style=\"text-align:center;padding:15pt\">")
            .append(fancyButton("Return to shop..", "Changed your mind?", "document.forms.restore.submit()"))
            .append("</td></tr></table></td></tr></table></td></tr>");

        HTML.output(response, HTML.getHTML(
                STICK_TO_HOME_URL +
                "\n" +
                "function noSuchMethod(element) {\n" +
                "  document.getElementById('notimplemented').style.top = (element.getBoundingClientRect().top + window.scrollY - document.getElementById('notimplemented').offsetHeight * 1.5) + 'px';\n" +
                "  document.getElementById('notimplemented').style.left = ((window.innerWidth - document.getElementById('notimplemented').offsetWidth) / 2) + 'px';\n" +
                "  document.getElementById('notimplemented').style.visibility = 'visible';\n" +
                "  setTimeout(function() {\n" +
                "    document.getElementById('notimplemented').style.visibility = 'hidden';\n" +
                "  }, 1000);\n" +
                "}\n\n" +
                (MerchantService.useW3cPaymentRequest && android?
                "function buildW3cPaymentRequest() {\n" +
                "  if (!window.PaymentRequest) {\n" +
                "    return null;\n" +
                "  }\n\n" +

                // The following may look strange but Saturn builds on an "omnichannel" concept
                // using a JSON based API rather than JavaScript.  That is, Saturn only uses the
                // PaymentRequest API as a "bridge" between the Web and the native world.  When
                // Saturn is invoked from another channel like QR, the very same JSON API is used.
                
                // On the Web the "url" parameter in "supportedMethods" holds a "bootstrap" URL
                // for OOB communication with the Merchant application. Using QR you get the URL
                // from the QR code (including data about which native application to start).

                // After collecting the bootstrap URL the rest is almost independent of how the
                // payment authorization was initiated, making the wallet code more manageable.

                // In retrospect this appears to be a pretty good idea since it has proved to be
                // unrealistic standardizing the input or output from payment applications.  If
                // you take a peek into Google Pay for Android you will find that "amount" and
                // "currency" are insufficient. For more advanced designs like Saturn which also
                // tags request type (like Gas Station which changes the UI), they may therefore
                // equally well be ignored.
                
                // The "methodData" is also a bit of a misnomer; in reality it is more
                // likely to represent specific "wallet" or payment authorization schemes.
                // Recent (2019) W3C activities mentions "skipping the list" (in the browser),
                // which also supports his notion.  Dealing with lots of quite different payment
                // authorization schemes in a single page is also bound to create unnecessary
                // difficult code.
                
                // As a consequence we give PaymentRequest some fictitious data to keep it happy,
                // while doing the interesting stuff "behind the curtain" :-)
                "  const dummyDetails = {total:{label:'total',amount:{currency:'EUR',value:'1.00'}}};\n\n" +
                "  const methodData = [{\n" +
                "    supportedMethods: '" + MerchantService.w3cPaymentRequestUrl + "',\n" +
                "    data: ['" + 
                        AndroidPluginServlet.getInvocationUrl(MobileProxyParameters.SCHEME_W3CPAY, 
                                                              request.getSession(false).getId(),
                                                              null) + 
                           "']\n" +
//                "    supportedMethods: 'weird-pay',\n" +
//                "    supportedMethods: 'basic-card',\n" +
//                "    data: {supportedNetworks: ['visa', 'mastercard']}\n" +
                "  }];\n\n" +

                "  let request = null;\n\n" +
                "  try {\n" +
                "    request = new PaymentRequest(methodData, dummyDetails);\n" +
                "  } catch (e) {\n" +
                "    console.info('Developer mistake:' + e.message);\n" +
                "  }\n" +
                "  return request;\n" +
                "}\n\n" +
                "let w3cPayReq = buildW3cPaymentRequest();\n\n" +
                "async function goSaturnGo() {\n" +
                "  document.getElementById('" + SATURN_PAY_BUTTON + "').style.display = 'none';\n" +
                "  document.getElementById('" + SATURN_PAY_WAIT + "').style.display = 'block';\n" +
                "  if (w3cPayReq) {\n" +
                "    try {\n" +
                "      if (await w3cPayReq.canMakePayment()) {\n" +
                "        const w3cPayRes = await w3cPayReq.show();\n" +
                "        w3cPayRes.complete('success');\n" +
                // Note that success does not necessarily mean that the payment succeeded,
                // it just means that the result is a URL to be redirected to.
                "        document.location.href = w3cPayRes.details." + 
                  MobileProxyParameters.W3CPAY_GOTO_URL + ";\n" +
                "      } else {\n" +
                "        console.info('Cannot make payment');\n" +
                "        document.getElementById('" + SATURN_PAY_WAIT + "').style.display = 'none';\n" +
                "        document.getElementById('" + SATURN_PAY_BUTTON + "').style.display = 'block';\n" +
                "        document.getElementById('" + SATURN_PAY_BUTTON + "').innerHTML = " +
                  "'<div>In order to use this application you need to install<br>" +
                  "the Android app and enroll payment credentials</div><div style=\"text-align:center;padding:10pt\">" +
                  fancyButton("OK - Let\\'s do that!", "Enroll etc", "document.location.href = \\'" + 
                  MerchantService.noMatchingMethodsUrl + "\\'") + 
                  "</div>';\n" +
                "      }\n" +
                "    } catch (e) {\n" +
                "      console.info('Cancel or error:' + e.message);\n" +
                "      document.forms.restore.submit();\n" +
                "    }\n" +
                "  } else {\n" +
                "    document.forms.shoot.submit();\n" +
                "  }\n" +
                "}\n"
                            :
                "function goSaturnGo() {\n" +
                "  document.forms.shoot.submit();\n" +
                "}\n"),
                "><form name=\"shoot\" method=\"POST\" action=\"" + 
                (android ? "androidplugin" : "w2nbwallet") +
                "\"></form><form name=\"restore\" method=\"POST\" action=\"shop\">" +
                "</form><div id=\"notimplemented\" class=\"toasting\">This demo only supports Saturn!</div",
                s.toString()));
    }
    
    static StringBuilder pumpDigit(int digit, String prefix) {
        return new StringBuilder("<td><div style=\"background-color:white;padding:0pt 3pt 1pt 3pt;font-size:14pt;border-radius:2pt\" id=\"")
            .append(prefix)
            .append(digit)
            .append("\">0</div></td>");
    }
    
    static StringBuilder pumpDisplay(int digits, int decimals, String leader, String trailer, String prefix) {
        StringBuilder s = new StringBuilder("<tr><td style=\"color:white;text-align:right\">")
            .append(leader)
            .append("</td>");
        while (digits-- > 0) {
            s.append(pumpDigit(digits + decimals, prefix));
        }
        s.append("<td style=\"color:white\">&#x2022;</td>");
        String tableFix = decimals == 1 ? " colspan=\"2\"":"";
        while (decimals-- > 0) {
            s.append(pumpDigit(decimals, prefix));
        }
        return s.append("<td style=\"color:white\"")
                .append(tableFix)
                .append('>')
                .append(trailer)
                .append("</td></tr>");
    }
    
    static String gasStation(String header, boolean visiblePumpDisplay) {
        StringBuilder s = new StringBuilder("<tr><td width=\"100%\" align=\"center\" valign=\"middle\"><table>" +
          "<tr><td id=\"phase\" style=\"padding-bottom:10pt;text-align:center;" +
                "font-weight:bolder;font-size:11pt;font-family:" +
                FONT_ARIAL + "\">")
            .append(header)
            .append("</td></tr>");
        if (visiblePumpDisplay) {
            s.append("<tr><td style=\"padding-bottom:15pt\" align=\"center\"><table title=\"This is [sort of] a pump display\"><tr>"+
                     "<td align=\"center\" style=\"box-shadow:5pt 5pt 5pt #c0c0c0;background:linear-gradient(135deg, #516287 0%,#5697e2 71%,#5697e2 71%,#516287 100%);border-radius:4pt\">" +
                     "<div style=\"padding:3pt;font-size:12pt;color:white\">Pump O'Matic</div>" +
                     "<table cellspacing=\"5\">")
             .append(pumpDisplay(3, 1, "Volume", "Litres", "pvol"))
             .append("<tr><td colspan=\"8\" style=\"max-height:3px\"></td></tr>")
             .append(pumpDisplay(3, 2, "Total", "<span style=\"font-size:14pt\">&#x20ac;</span>", "ppri"))
             .append("</table></td></tr></table></td></tr>");
        }
        return s.toString();

    }

    static String cometJavaScriptSupport(String id, 
                                         HttpServletRequest request,
                                         String returnAction,
                                         String progressAction,
                                         String successAction) {
        return new StringBuilder(
            "\"use strict\";\n" +
            STICK_TO_HOME_URL +
            "function setQRDisplay(turnOn) {" +
            "  var displayArg = turnOn ? 'table-row' : 'none';\n" +
            "  document.getElementById('qr1').style.display = displayArg;\n" +
            "  document.getElementById('qr2').style.display = displayArg;\n" +
            "  document.getElementById('waiting').style.display = turnOn ? 'none' : 'table-row';\n" +
            "}\n" +
            "function flashQRInfo() {\n" +
            "  document.getElementById('qridflasher').style.top = ((window.innerHeight - document.getElementById('qridflasher').offsetHeight) / 2) + 'px';\n" +
            "  document.getElementById('qridflasher').style.left = ((window.innerWidth - document.getElementById('qridflasher').offsetWidth) / 2) + 'px';\n" +
            "  document.getElementById('qridflasher').style.visibility = 'visible';\n" +
            "  setTimeout(function() {\n" +
            "    document.getElementById('qridflasher').style.visibility = 'hidden';\n" +
            "  }, 2000);\n" +
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
        .append("      document.getElementById('authhelp').style.display = 'table-row';\n")
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
               "<div id=\"qridflasher\" class=\"toasting\">" +
               "You get it automatically when you install the<br>&quot;WebPKI&nbsp;Suite&quot;, just look for the icon!</div";       
    }
    
    static StringBuilder bodyEndQR(byte[] qrImage, boolean qrInitOn) {
        String display = qrInitOn ? "" : " style=\"display:none\"";
        return new StringBuilder()
            .append(
                "<tr")
            .append(display)
            .append(
                " id=\"qr1\"><td style=\"padding-top:10pt\" align=\"left\">Now use the QR ID&trade; <a href=\"javascript:flashQRInfo()\">" +
                "<img border=\"1\" src=\"images/qr_launcher.png\"></a> application to start the Wallet</td></tr>" +
                "<tr")
             .append(display)
             .append(" id=\"qr2\"><td style=\"cursor:none\" align=\"center\"><img src=\"data:image/png;base64,")
             .append(Base64.getEncoder().encodeToString(qrImage))
             .append("\"></td></tr>"+
                     "<tr id=\"authhelp\" style=\"display:none\"><td style=\"padding-top:15pt;text-align:center\">"+
                     "Waiting for you to <i>authorize</i> the transaction in the mobile device...</td></tr>" +    
                     "<tr id=\"waiting\" style=\"display:none\"><td style=\"padding-top:10pt;text-align:center\">"+
                     "<img title=\"Something is running...\" src=\"images/waiting.gif\"></td></tr></table></td></tr>");     
    }

    static void printQRCode4Shop(HttpServletResponse response,
                                 ShoppingCart savedShoppingCart,
                                 byte[] qrImage,
                                 HttpServletRequest request,
                                 String id) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(
            cometJavaScriptSupport(id, request, 
                                   "document.forms.restore.submit()",
                                   "",
                                   "document.location.href = 'result'"),
            bodyStartQR("<form name=\"restore\" method=\"POST\" action=\"shop\"></form>"),
            currentOrder(savedShoppingCart).toString() +
            bodyEndQR(qrImage, true)));
    }
    
    static String selectionButtons(FuelTypes[] fuelTypes) throws IOException {
        StringBuilder s = new StringBuilder("<tr><td><table cellpadding=\"0\" cellspacing=\"0\" align=\"center\">" +
                                          "<tr style=\"text-align:center\"><td>Fuel Type</td><td>Price/Litre</td></tr>");
        for (FuelTypes fuelType : fuelTypes) {
            s.append("<tr id=\"")
             .append(fuelType.toString())
             .append(".\"><td colspan=\"2\" style=\"height:6pt\"></td></tr><tr title=\"Selected fuel type\" id=\"")
             .append(fuelType.toString())
             .append("\" style=\"box-shadow:3pt 3pt 3pt #D0D0D0;text-align:center;background:")
             .append(fuelType.background);
            if (fuelTypes.length > 1) {
                s.append(";cursor:pointer\" onclick=\"selectFuel('")
                 .append(fuelType.toString())
                 .append("', this)");
            }
            s.append("\"><td style=\"font-size:11pt;font-family:" + FONT_ARIAL + ";padding:6pt;min-width:10em\">")
             .append(ReceiptServlet.showLines(fuelType.description))
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
        StringBuilder s = new StringBuilder("function selectFuel(fuelType, element) {\n");
        for (String fuelType : FuelTypes.products.keySet()) {
            s.append("  if (fuelType == '")
             .append(fuelType)
             .append("') {\n" +
             "    document.getElementById('" + GasStationServlet.FUEL_TYPE_FIELD + "').value = fuelType;\n" +
             "    element.style.cursor = 'default';\n" +
             "   } else {\n" +
             "    document.getElementById('")
             .append(fuelType)
             .append("').style.display = 'none';\n" +
             "    document.getElementById('")
            .append(fuelType)
            .append(".').style.display = 'none';\n" +
            "}\n");
        }
        HTML.output(response, HTML.getHTML(
            cometJavaScriptSupport(id, request, 
                                  "document.location.href='home'",
                                  "      document.getElementById('phase').innerHTML = '3. User Authorization';\n" +
                                  "      document.getElementById('authtext').style.display = 'table-row';\n",
                                  "document.forms.fillgas.submit()") +
            s.append(
            "  setQRDisplay(true);\n" +
            "  document.getElementById('phase').innerHTML = '2. Initiate Payment';\n" +
            "}\n").toString(),
            bodyStartQR("<form name=\"fillgas\" method=\"POST\" action=\"gasstation\">" +
                        "<input name=\"" + GasStationServlet.FUEL_TYPE_FIELD + "\" " +
                        "id=\"" + GasStationServlet.FUEL_TYPE_FIELD + "\" type=\"hidden\"></form>" +
                        GAS_PUMP_LOGO + ">"),
            gasStation("1. Select Fuel Type", false) +
            "<tr id=\"authtext\" style=\"display:none\">" +
            "<td style=\"width:40em;padding-bottom:15pt\">Since the quantity of fuel is usually not known in an advance, " +
            "automated fueling stations require <i>pre-authorization</i> of a fixed maximum amount of money (" +
            Currencies.EUR.amountToDisplayString(new BigDecimal(GasStationServlet.STANDARD_RESERVATION_AMOUNT_X_100 / 100), true) +
            " in the demo), while only the actual amount " +
            "needed is eventually withdrawn from the client's account. This should be reflected in the Wallet's " +
            "authorization display as well.</td></tr>" +
            selectionButtons(FuelTypes.products.values().toArray(new FuelTypes[0])) +
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
                         "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                         "<b>Please wait while the Wallet plugin starts...</b></td></tr>").getBytes("UTF-8"));
    }

    static void autoPost(HttpServletResponse response, String url) throws IOException, ServletException {
        HTML.output(response, "<html><body onload=\"document.forms.posting.submit()\">Redirecting..." +
                              "<form name=\"posting\" action=\"" + url + "\" method=\"POST\"></form></body></html>");
    }

    static void androidPage(HttpServletResponse response) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(null, null,
            "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
            "<table class=\"tighttable\" style=\"max-width:600px;\" cellpadding=\"4\">" +
            "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + 
            FONT_ARIAL + "\">Android Wallet<br>&nbsp;</td></tr>" +
            "<tr><td style=\"text-align:left\">" +
              "Note: The Android Wallet is a <i>proof-of-concept implementation</i> rather than a product.</td></tr>" +
            "<tr><td>Installation: <a href=\"https://play.google.com/store/apps/details?id=org.webpki.mobile.android\">" +
              "https://play.google.com/store/apps/details?id=org.webpki.mobile.android</a></td></tr>" +
            (MerchantService.noMatchingMethodsUrl == null ? "" :  
            "<tr><td>Enroll payment <i>test credentials</i> " +
              "by surfing (with the Android device...) to: <a href=\"" +
              MerchantService.noMatchingMethodsUrl + "\">" +
              MerchantService.noMatchingMethodsUrl + "</td></tr>") +
            (MerchantService.desktopWallet ?
              "<tr><td>Unlike the desktop (Windows, Linux, and OS/X-based) Wallet, " +
              "the Android version also supports remote operation using QR codes.  This mode is " +
              "indicated by the following image in the Merchant Web application:</td></tr>" +
              "<tr><td align=\"center\"><img src=\"images/qr_launcher.png\"></td></tr>" : "") +
            "</table></td></tr>"));       
    }

    static void gasFillingPage(HttpServletResponse response, FuelTypes fuelType, int maxVolume) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(
            STICK_TO_HOME_URL +
            updatePumpDisplay(fuelType) +
            "var decilitres = 0;\n" +
            "var timer = null;\n" +
            "function finishPumping() {\n" +
            "  document.getElementById('waiting').style.display = 'table-row';\n" +
            "  clearInterval(timer);\n" +
            "  document.getElementById('pumpbtn').innerHTML = 'Please wait while we are finalizing the payment...';\n" +
            "  setTimeout(function() {\n" +
            "    doneFilling();\n" +
            "  }, 1000);\n" +
            "}\n" +
            "function execute() {\n" +
            "  if (timer) {\n"+
            "    finishPumping();\n" +
            "  } else {\n" +
            "    timer = setInterval(function () {\n" +
            "      updatePumpDisplay(++decilitres);\n" +
            "      if (decilitres == " + maxVolume + ") {\n" +
            "        finishPumping();\n" +
            "      }\n" +
            "    }, 100);\n" +
            "    document.getElementById('waiting').style.display = 'table-row';\n" +
            "    setTimeout(function() {\n" +
            "      document.getElementById('cmd').innerHTML = 'Click here to <i style=\"color:red;font-weight:bolder\">stop</i> pumping...';\n" +
            "      document.getElementById('waiting').style.display = 'none';\n" +
            "    }, 1000);\n" +
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
            "<tr><td style=\"padding-top:20pt;text-align:center\" id=\"pumpbtn\">" +
            fancyButton("Click here to <i style=\"color:green;font-weight:bolder\">start</i> pumping!",
                        "Since we don't have a real pump we &quot;simulate&quot; one",
                        "execute()") +
            "</td></tr>" +
            "<tr id=\"waiting\" style=\"display:none\"><td style=\"padding-top:10pt\" align=\"center\">" +
            "<img title=\"Something is running...\" src=\"images/waiting.gif\"></td></tr>" +
            "</table></td></tr>"));
    }

    static void refundResultPage(HttpServletResponse response, boolean debugMode, ResultData resultData) throws IOException, ServletException {
        StringBuilder s = new StringBuilder("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">")
            .append("<table>" +
                "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL +
                "\">Refund Result<br>&nbsp;</td></tr>" +
                 "<tr><td style=\"text-align:center;padding-bottom:15pt;font-size:10pt\">" +
                "Dear customer, your payment has been refunded!</td></tr>")
            .append(receiptCore(resultData, debugMode))
            .append("</table></td></tr></table></td></tr>");
        HTML.output(response, HTML.getHTML(STICK_TO_HOME_URL, null, s.toString()));
    }
}
