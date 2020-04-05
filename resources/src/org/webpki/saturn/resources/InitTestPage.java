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

// Web2Native Bridge emulator Payment Agent (a.k.a. Wallet) application

package org.webpki.saturn.resources;

import java.io.FileOutputStream;
import java.io.IOException;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.webpki.crypto.AsymKeySignerInterface;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.DeterministicSignatureWrapper;

import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.PaymentRequestEncoder;

import org.webpki.saturn.w2nb.support.W2NB;

import org.webpki.util.ISODateTime;

import org.webpki.w2nbproxy.ExtensionPositioning;

public class InitTestPage implements BaseProperties {
    
    public static class BotchedAsymKeySigner extends JSONAsymKeySigner {
        
        private static final long serialVersionUID = 1L;

        public BotchedAsymKeySigner(final KeyStoreEnumerator key) throws IOException {
            super(new AsymKeySignerInterface() {
                @Override
                public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm) throws IOException {
                    try {
                        return new DeterministicSignatureWrapper(algorithm, key.getPrivateKey()).update(data).sign();
                    } catch (GeneralSecurityException e) {
                        throw new IOException (e);
                    }
                }
                @Override
                public PublicKey getPublicKey() throws IOException {
                    return key.getPublicKey();
                }
            });
            setSignatureAlgorithm(key.getPublicKey() instanceof RSAPublicKey ?
                      AsymSignatureAlgorithms.RSA_SHA256 : AsymSignatureAlgorithms.ECDSA_SHA256);
        }
    }

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
        write(json.serializeToBytes(JSONOutputFormats.PRETTY_JS_NATIVE));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("\nUsage: " +
                               InitTestPage.class.getCanonicalName() +
                               "testpage w2nbName");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        
        fos = new FileOutputStream(args[0]);

        // Create payment request
        JSONObjectWriter paymentRequest = 
            PaymentRequestEncoder.encode("Demo Merchant",
                                         "https://demomerchant.com",
                                         new BigDecimal("306.25"),
                                         Currencies.USD,
                                         null,
                                         "#6100004",
                                         ISODateTime.parseDateTime("2016-12-27T09:45:23Z",
                                                                   ISODateTime.UTC_NO_SUBSECONDS),
                                         ISODateTime.parseDateTime("2030-09-14T00:00:00Z", 
                                                                   ISODateTime.UTC_NO_SUBSECONDS));
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
        // Create a payment request
        
        write(Messages.PAYMENT_CLIENT_REQUEST.createBaseMessage()
                .setStringArray(SUPPORTED_PAYMENT_METHODS_JSON,
                                new String[]{"https://nosuchcard.com",
                                    PaymentMethods.SUPER_CARD.getPaymentMethodUrl(),
                                    PaymentMethods.BANK_DIRECT.getPaymentMethodUrl()})
                .setObject(PAYMENT_REQUEST_JSON, paymentRequest));

        // The normal request is cloned and modified for testing error handling
        write(";\n\n" +
              "// All our cards/accounts should match during the discovery phase...\n" +
              "var scrollMatchingRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
              "scrollMatchingRequest."  + SUPPORTED_PAYMENT_METHODS_JSON + " = [\"https://nosuchcard.com\"");
        for (PaymentMethods paymentMethod : PaymentMethods.values()) {
            write(", \"");
            write(paymentMethod.getPaymentMethodUrl());
            write("\"");
        }

        write("];\n\n" +
                "// No card/account should match during the discovery phase...\n" +
                "var nonMatchingRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
                "nonMatchingRequest." + SUPPORTED_PAYMENT_METHODS_JSON + " = [\"https://nosuchcard.com\"];\n\n");

        write("// Note the modified \"" + PAYEE_JSON + "\" property...\n" +
              "var badSignatureRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
              "badSignatureRequest." + PAYMENT_REQUEST_JSON + "." +  PAYEE_JSON + "." + COMMON_NAME_JSON + "= \"DEmo Merchant\";\n\n");

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
        write(args[1]);
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
              "      if ((initMode && qualifier != \"" + W2NB.PAYMENT_CLIENT_IS_READY.toString() + "\" ) ||\n" +
              "          (!initMode && qualifier != \"" + Messages.PAYER_AUTHORIZATION.toString() + "\")) {\n" +  
              "        setString(\"Wrong or missing \\\"@qualifier\\\"\");\n" +
              "        closeExtension();\n" +
              "        return;\n" +
              "      }\n" +
              "      if (initMode) {\n" +
              "        initMode = false;\n" +
              "        if (document.getElementById(\"positionWallet\").checked) {\n" +
              "          document.getElementById(\"wallet\").style.width = message." + W2NB.WINDOW_JSON + "." + W2NB.WIDTH_JSON + " + 'px';\n" +
              "          document.getElementById(\"wallet\").style.height = message." + W2NB.WINDOW_JSON + "." + W2NB.HEIGHT_JSON + " + 'px';\n" +
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
              "<div id=\"response\" style=\"font-family:courier;font-size:10pt;word-break:break-all;width:800pt\"></div>\n" +
              "<div id=\"wallet\" style=\"position:absolute;top:50px;background:yellow;" +
              "right:50px;z-index:5;visibility:hidden\">The wallet should launch in this<br>corner and update width+height</div>" +
              "\n</body></html>");
        fos.close();
    }
}
