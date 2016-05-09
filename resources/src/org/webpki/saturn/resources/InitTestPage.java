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

// Web2Native Bridge emulator Payment Agent (a.k.a. Wallet) application

package org.webpki.saturn.resources;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.math.BigDecimal;

import org.webpki.crypto.CustomCryptoProvider;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ServerAsymKeySigner;

import org.webpki.util.ISODateTime;

import org.webpki.w2nbproxy.ExtensionPositioning;

public class InitTestPage implements BaseProperties {
    
    enum TESTS {
        Normal      ("Normal"), 
        Slow        ("Slow (but legal) response"),
        Scroll      ("Many matching cards (=scroll view)"),
        NonMatching ("No card should match"),
        Timeout     ("Timeouted response"),
        Syntax      ("Bad message syntax"),
        Signature   ("Bad signature");
        
        String descrition;
        
        TESTS(String description) {
            this.descrition = description;
        }
    };
   
    static FileOutputStream fos;
    
    static void write(byte[] utf8) throws Exception {
        fos.write(utf8);
    }
    
    static void write(String utf8) throws Exception {
        write(utf8.getBytes("UTF-8"));
    }
    static void write(JSONObjectWriter json) throws Exception {
        write(json.serializeJSONObject(JSONOutputFormats.PRETTY_JS_NATIVE));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.out.println("\nUsage: " +
                               InitTestPage.class.getCanonicalName() +
                               "testpage merchantCertFile certFilePassword merchantCn merchantId w2nbName");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        
        fos = new FileOutputStream(args[0]);
        
        // Read key/certificate to be imported and create signer
        ServerAsymKeySigner signer = new ServerAsymKeySigner(new KeyStoreEnumerator (new FileInputStream(args[1]), args[2]));

        // Create signed payment request
        JSONObjectWriter standardRequest = 
            PaymentRequest.encode(Payee.init(args[3], args[4]),
                                  new BigDecimal("306.25"),
                                  Currencies.USD,
                                  "#6100004",
                                  ISODateTime.parseDateTime("2030-09-14T00:00:00Z").getTime(),
                                  signer);
        // Header
        write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Payment Agent (Wallet) Tester</title>"
              + "</head>\n<body onload=\"getTargetDimensions()\"><script>\n\n" +

              "\"use strict\";\n\n" +

              "function setString(rawString) {\n" +
              "  var text = \"\";\n" +
              "  for (var n = 0; n < rawString.length; n++) {\n" +
              "    var c = rawString.charAt(n);\n" +
              "    if (c == \"<\") {\n" +
              "      c = \"&lt;\";\n" +
              "    } else if (c == \">\") {\n" +
              "      c = \"&gt;\";\n" + 
              "    } else if  (c == \"&\") {\n" +
              "      c = \"&amp;\";\n" +
              "    }\n" +
              "    text += c;\n" +
              "  }\n" +
              "  document.getElementById(\"response\").innerHTML = text;\n" +
              "}\n\n" +
    
              "var nativePort = null;\n\n" +
              "var normalRequest = ");

        // The payment request is wrapped in an unsigned wallet invocation message
        write(Messages.createBaseMessage(Messages.WALLET_REQUEST)
            .setStringArray(ACCEPTED_ACCOUNT_TYPES_JSON,
                            new String[]{"https://nosuchcard.com",
                                          PayerAccountTypes.SUPER_CARD.getTypeUri(),
                                          PayerAccountTypes.BANK_DIRECT.getTypeUri()})
            .setObject(PAYMENT_REQUEST_JSON, standardRequest));

        // The normal request is cloned and modified for testing error handling
        write(";\n\n" +
              "// All our cards/accounts should match during the discovery phase...\n" +
              "var scrollMatchingRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
              "scrollMatchingRequest." + ACCEPTED_ACCOUNT_TYPES_JSON + " = [\"https://nosuchcard.com\"");
        for (PayerAccountTypes accountType : PayerAccountTypes.values()) {
            write(", \"");
            write(accountType.getTypeUri());
            write("\"");
        }

        write("];\n\n" +
                "// No card/account should match during the discovery phase...\n" +
                "var nonMatchingRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
                "nonMatchingRequest." + ACCEPTED_ACCOUNT_TYPES_JSON + " = [\"https://nosuchcard.com\"];\n\n");

        write("// Note the modified \"" + PAYEE_JSON + "\" property...\n" +
              "var badSignatureRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
              "badSignatureRequest." + PAYMENT_REQUEST_JSON + "." +  PAYEE_JSON + "= \"DEmo Merchant\";\n\n");

        write("var badMessageRequest = {\"hi\":\"there!\"};\n\n" +

              "function closeExtension() {\n" +
              "  if (nativePort) {\n" +
              "    nativePort.disconnect();\n" +
              "    nativePort = null;\n" +
              "  }\n" +
              "}\n\n" +

              "function sendMessageConditional(message) {\n" +
              "  if (nativePort) {\n" +
              "    nativePort.postMessage(message);\n" +
              "  }\n" +
              "}\n\n" +

              "function activateExtension() {\n" +
              "  if (nativePort) {\n" +
              "    closeExtension();\n" +
              "  }\n" +
              "  setString(\"\");\n" +
              "  var initMode = true;\n" +
              "  var test = document.forms.shoot.test.value;\n" +
              "  if (!navigator.nativeConnect) {\n" +
              "    alert('\"navigator.nativeConnect\" not found, \\ncheck Chrome Web2Native Bridge extension settings');\n" +
              "    return;\n" +
              "  }\n" +
              "  navigator.nativeConnect(\"");
        write(args[5]);
        write("\",\n" +
              "                          document.getElementById(\"positionWallet\").checked ?\n" +
              "                            ");
        write(ExtensionPositioning.encode(ExtensionPositioning.HORIZONTAL_ALIGNMENT.Right,
                                          ExtensionPositioning.VERTICAL_ALIGNMENT.Top, "wallet"));
        write(" :\n" +
              "                            ");
        write(ExtensionPositioning.encode(ExtensionPositioning.HORIZONTAL_ALIGNMENT.Center,
                                          ExtensionPositioning.VERTICAL_ALIGNMENT.Center, null));
        write(").then(function(port) {\n" +
              "    nativePort = port;\n" +
              "    port.addMessageListener(function(message) {\n" +
              "      if (message[\"@context\"] != \"" + BaseProperties.SATURN_WEB_PAY_CONTEXT_URI + "\") {\n" +
              "        setString(\"Missing or wrong \\\"@context\\\"\");\n" +
              "        return;\n" +
              "      }\n" +
              "      var qualifier = message[\"@qualifier\"];\n" +
              "      if ((initMode && qualifier != \"" + Messages.WALLET_IS_READY.toString() + "\" ) ||\n" +
              "          (!initMode && qualifier != \"" + Messages.PAYER_AUTHORIZATION.toString() + "\")) {\n" +  
              "        setString(\"Wrong or missing \\\"@qualifier\\\"\");\n" +
              "        closeExtension();\n" +
              "        return;\n" +
              "      }\n" +
              "      if (initMode) {\n" +
              "        initMode = false;\n" +
              "        if (document.getElementById(\"positionWallet\").checked) {\n" +
              "          document.getElementById(\"wallet\").style.width = message." + WINDOW_JSON + "." + WIDTH_JSON + " + 'px';\n" +
              "          document.getElementById(\"wallet\").style.height = message." + WINDOW_JSON + "." + HEIGHT_JSON + " + 'px';\n" +
              "        }\n" +
              "        if (test == \"" + TESTS.Normal + "\") {\n" +
              "          sendMessageConditional(normalRequest);\n" +
              "        } else if (test == \"" + TESTS.Slow + "\") {\n" +
              "          setTimeout(function() {\n" +
              "            sendMessageConditional(normalRequest);\n" +
              "          }, 2000);\n" +
              "        } else if (test == \"" + TESTS.Scroll + "\") {\n" +
              "          sendMessageConditional(scrollMatchingRequest);\n" +
              "        } else if (test == \"" + TESTS.NonMatching + "\") {\n" +
              "          sendMessageConditional(nonMatchingRequest);\n" +
              "        } else if (test == \"" + TESTS.Timeout + "\") {\n" +
              "          setTimeout(function() {\n" +
              "            sendMessageConditional(normalRequest);\n" +
              "          }, 20000);\n" +
              "        } else if (test == \"" + TESTS.Syntax + "\") {\n" +
              "          sendMessageConditional(badMessageRequest);\n" +
              "        } else if (test == \"" + TESTS.Signature + "\") {\n" +
              "          sendMessageConditional(badSignatureRequest);\n" +
              "        } else {\n" +
              "          alert(\"Not implemented: \" + test);\n" +
              "        }\n" +
              "      } else {\n" +
              "        setTimeout(function() {\n" +
              "          setString(JSON.stringify(message));\n" +
              "          closeExtension();\n" +
              "        }, 1000);\n" +
              "      }\n"+
              "    });\n" +
              "    port.addDisconnectListener(function() {\n" +
              "      if (nativePort) {\n" +
              "        setString(\"Application Unexpectedly disconnected\");\n" +
              "      }\n" +
              "      nativePort = null;\n" +
              "    });\n" +
              "  }, function(err) {\n" +
              "    console.debug(err);\n" +
              "  });\n" +
              "}\n\n" +

              "window.addEventListener(\"beforeunload\", function(event) {\n" +
              "  closeExtension();\n" +
              "});\n\n" +
              
              "var targetWidth;\n" +
              "var targetHeight;\n" +
              "function getTargetDimensions() {\n" +
              "  targetWidth = document.getElementById(\"wallet\").style.width;\n" +
              "  targetHeight = document.getElementById(\"wallet\").style.height;\n" +
              "}\n\n" +
              
              "function setTargetState() {\n" +
              "  document.getElementById(\"wallet\").style.visibility = document.getElementById(\"positionWallet\").checked ? 'visible' : 'hidden';\n" +
              "  document.getElementById(\"wallet\").style.width = targetWidth;\n" +
              "  document.getElementById(\"wallet\").style.height = targetHeight;\n" +
              "}\n\n" +
              
              ExtensionPositioning.SET_EXTENSION_POSITION_FUNCTION_TEXT + "\n" +

              "</script>\n" +
              "<h2>Web2Native Bridge &quot;Emulator&quot; - Payment Agent (Wallet) Tester</h2>\n" +
              "<input type=\"button\" style=\"margin-bottom:10pt;width:50pt\" value=\"Run!\" onclick=\"activateExtension()\">\n" +
              "<form name=\"shoot\">\n");
        for (TESTS test : TESTS.values()) {
            write("<input type=\"radio\" name=\"test\" value=\"");
            write(test.toString());
            write("\"");
            if (test == TESTS.Normal) {
                write(" checked");
            }
            write(">");
            write(test.descrition);
            write("<br>\n");
        }
        write("</form>\n" +
              "<input type=\"checkbox\" id=\"positionWallet\" style=\"margin-top:10pt\" onchange=\"setTargetState()\">Position/update target element (default is centered)\n" +
              "<div style=\"margin-top:10pt;margin-bottom:10pt\">Result:</div>\n" +
              "<div id=\"response\" style=\"font-family:courier;font-size:10pt;word-wrap:break-word;width:800pt\"></div>\n" +
              "<div id=\"wallet\" style=\"position:absolute;top:50px;background:yellow;" +
              "right:50px;z-index:5;visibility:hidden\">The wallet should launch in this<br>corner and update width+height</div>" +
              "\n</body></html>");
        fos.close();
    }
}