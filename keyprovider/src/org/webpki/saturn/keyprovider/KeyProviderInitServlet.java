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

package org.webpki.saturn.keyprovider;

import java.io.IOException;

import java.util.logging.Logger;

import java.net.URLEncoder;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.webpki.keygen2.ServerState;

import org.webpki.saturn.common.MobileProxyParameters;

// Initiation code for KeyGen2

public class KeyProviderInitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger(KeyProviderInitServlet.class.getCanonicalName());

    static final String KEYGEN2_SESSION_ATTR           = "keygen2";
    static final String USERNAME_SESSION_ATTR          = "userName";
    static final String TESTMODE_SESSION_ATTR          = "test";
    static final String W3C_PAYMENT_REQUEST_MODE_PARM  = "w3cmode";  // POST is used in two ways...
    
    static final int NAME_MAX_LENGTH                   = 50;  // Reflected in the DB

    static final String INIT_TAG  = "init";     // Note: This is currently also a part of the KeyGen2 client!
    static final String ABORT_TAG = "abort";
    static final String PARAM_TAG = "msg";
    static final String ERROR_TAG = "err";
    
    private static final String BUTTON_ID  = "gokg2";
    private static final String WAITING_ID = "wait";
    private static final String ERROR_ID   = "error";
    
    private static final String THIS_SERVLET   = "init";
    
    static final String DEFAULT_USER_NAME_HTML = "Luke Skywalker &#x1f984;";    // Unicorn emoji
    
    static final String DEFAULT_USER_NAME_JAVA = "Luke Skywalker " +
            new String(Character.toChars(Integer.parseInt("1f984", 16)));       // Unicorn emoji

    static final String BUTTON_TEXT_HTML       = "Start Enrollment &#x1f680;";  // Rocket emoji
    
    static final String AFTER_INSTALL_JS       =
            new String(Character.toChars(Integer.parseInt("1f449", 16))) + " Click here AFTER install";
    
    static final String ANONYMOUS_JAVA         = "Anonymous " + 
                 new String(Character.toChars(Integer.parseInt("1f47d", 16)));  // E.T. emoji
    
    static final String GO_HOME =              
            "history.pushState(null, null, '" + THIS_SERVLET + "');\n" +
            "window.addEventListener('popstate', function(event) {\n" +
            "  history.pushState(null, null, '" + THIS_SERVLET + "');\n" +
            "});\n";

    static final String HTML_INIT = 
            "<!DOCTYPE html><html>" + 
            "<head>" + 
            "<link rel='icon' href='saturn.png' sizes='192x192'>" + 
            "<title>Payment Credential Enrollment</title>" + 
            "<meta charset='utf-8'>" + 
            "<meta name='viewport' content='width=device-width, initial-scale=1'>" + 
            "<style type='text/css'>" + 
            ".displayContainer {" + 
            "  display:block;" + 
            "  height:100%;" + 
            "  width:100%;" + 
            "  align-items:center;" + 
            "  display:flex;" + 
            "  flex-direction:column;" + 
            "  justify-content:center;" +
//            "  background-color:red;" +
            "}" +
            
            ".link {" +
            "  font-weight:bold;" +
            "  font-size:8pt;" +
            "  color:blue;" +
            "  font-family:arial,verdana;text-decoration:none;" +
            "}" +

            ".stdbtn {" + 
            "  cursor:pointer;" + 
            "  background:linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" + 
            "  border-width:1px;" + 
            "  border-style:solid;" + 
            "  border-color:#a9a9a9;" + 
            "  border-radius:5pt;" + 
            "  padding:3pt 10pt;" + 
            "  box-shadow:3pt 3pt 3pt #d0d0d0;" + 
            "}" +
 
            ".label, .stdbtn {" +
            "  font-family:Arial,'Liberation Sans',Verdana,'Bitstream Vera Sans','DejaVu Sans';" + 
            "  font-size:11pt;" + 
            "}" +
            
            "body {" + 
            "  font-size:10pt;" + 
            "  color:#000000;" + 
            "  font-family:verdana,arial;" + 
            "  background-color:white;" + 
            "  height:100%;" + 
            "  margin:0;" + 
            "  width:100%;" + 
            "}" + 

            "html {" + 
            "  height:100%;" + 
            "  width:100%;" + 
            "}" +

            ".sitefooter {" + 
            "  display:flex;" + 
            "  align-items:center;" + 
            "  border-width:1px 0 0 0;" + 
            "  border-style:solid;" + 
            "  border-color:#a9a9a9;" + 
            "  position:absolute;" + 
            "  z-index:-5;" + 
            "  left:0px;" + 
            "  bottom:0px;" + 
            "  right:0px;" + 
            "  background-color:#ffffe0;" + 
            "  padding:0.3em 0.7em;" + 
            "}" +

            "@media (max-width:768px) {" +
 
            "  .stdbtn {" + 
            "    box-shadow:2pt 2pt 2pt #d0d0d0;" + 
            "  }" +

            "  body {" + 
            "    font-size:8pt;" + 
            "  }" +

            "}" +

            "</style>";

    static String getHTML(String javascript, String bodyscript, String box) {
        StringBuilder s = new StringBuilder(HTML_INIT);
        if (javascript != null) {
            s.append("<script>\n'use strict';\n").append(javascript).append("</script>");
        }
        s.append("</head><body");
        if (bodyscript != null) {
            s.append(' ').append(bodyscript);
        }
        s.append(
                "><div style='cursor:pointer;position:absolute;top:15pt;left:15pt;z-index:5;width:100pt'" +
                " onclick=\"document.location.href='http://cyberphone.github.io/doc/saturn'\" title='Home of Saturn'>")
         .append (KeyProviderService.saturnLogotype)
         .append ("</div><div class='displayContainer'>")
                .append(box).append("</div></body></html>");
        return s.toString();
    }
  
    static void output(HttpServletResponse response, String html) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("EXPIRES", 0);
        byte[] data = html.getBytes("utf-8");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
//        System.out.println(html);
    }
    
    static String getInvocationUrl(String scheme, HttpSession session) throws IOException {
        ////////////////////////////////////////////////////////////////////////////////////////////
        // The following is the actual contract between an issuing server and a KeyGen2 client.
        // The PUP_INIT_URL argument bootstraps the protocol via an HTTP GET
        ////////////////////////////////////////////////////////////////////////////////////////////
        String urlEncoded = URLEncoder.encode(KeyProviderService.keygen2RunUrl, "utf-8");
        return scheme + "://" + MobileProxyParameters.HOST_KEYGEN2 + 
               "?" + MobileProxyParameters.PUP_COOKIE     + "=" + "JSESSIONID%3D" + session.getId() +
               "&" + MobileProxyParameters.PUP_INIT_URL   + "=" + urlEncoded + "%3F" + INIT_TAG + "%3Dtrue" +
               "&" + MobileProxyParameters.PUP_MAIN_URL   + "=" + urlEncoded +
               "&" + MobileProxyParameters.PUP_CANCEL_URL + "=" + urlEncoded + "%3F" + ABORT_TAG + "%3Dtrue" +
               "&" + MobileProxyParameters.PUP_VERSIONS   + "=" + KeyProviderService.androidWebPkiVersions;
   }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
        String userAgent = request.getHeader("User-Agent");
        int i;
            // Android YES
        if (!userAgent.contains("Android ") ||
            // WebView NO
            userAgent.contains("; wv)") ||
            // Chrome YES
            (i = userAgent.indexOf(" Chrome/")) < 0 ||
            // However, dismiss (for the purpose) outdated versions
            Integer.parseInt(userAgent.substring(i + 8, userAgent.indexOf('.', i))) <
               KeyProviderService.androidChromeVersion) {
            output(response, 
                    getHTML(null,
                            null,
                "<div class='label'>This proof-of-concept system only supports " +
                  "Android and using the \"Chrome\" browser (min version: " + 
                  KeyProviderService.androidChromeVersion + ")" +
                "</div>"));
            return;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(USERNAME_SESSION_ATTR, DEFAULT_USER_NAME_JAVA);
        session.setAttribute(KEYGEN2_SESSION_ATTR,
                new ServerState(new KeyGen2SoftHSM(KeyProviderService.keyManagementKey), 
                                KeyProviderService.keygen2RunUrl,
                                KeyProviderService.getServerCertificate(),
                                null));
        if (request.getParameter(TESTMODE_SESSION_ATTR) == null) {
            session.removeAttribute(TESTMODE_SESSION_ATTR);
        } else {
            session.setAttribute(TESTMODE_SESSION_ATTR, "true");
        }
        output(response, 
               getHTML(GO_HOME +
            "function paymentRequestError(msg) {\n" +
            "  console.info('Payment request error:' + msg);\n" +
            "  document.getElementById('" + WAITING_ID + "').style.display = 'none';\n" +
            "  document.getElementById('" + ERROR_ID + "').innerHTML = msg;\n" +
            "  document.getElementById('" + ERROR_ID + "').style.display = 'block';\n" +
            "  document.getElementById('" + BUTTON_ID + "').style.display = 'block';\n" +
            "}\n" +
            "async function setUserName() {\n" +
            "  let formData = new URLSearchParams();\n" +
            "  formData.append('" + USERNAME_SESSION_ATTR +
              "', document.forms.shoot.elements." + USERNAME_SESSION_ATTR + ".value);\n" +
            "  formData.append('" + W3C_PAYMENT_REQUEST_MODE_PARM + "', 1);\n" +
            "  try {\n" +
            "    const httpResponse = await fetch('" + THIS_SERVLET + "', {\n" +
            "      method: 'POST',\n" +
            "       body: formData\n" +
            "    });\n" +
            "    if (httpResponse.status == " + HttpServletResponse.SC_OK + ") {\n" +
            "      await httpResponse.text();\n" +
            "    } else {\n" +
            "      paymentRequestError('Server problems, try again!');\n" +
            "    }\n" +
            "  } catch(err) {\n" +
            "    paymentRequestError(err.message);\n" +
            "  }\n" +
            "}\n" +
            "function waitForBrowserDisplay(result) {\n" +
            "  if (document.querySelector('#" + WAITING_ID + "')) {\n" +
            "    if (result) {\n" +
            "      document.getElementById('" + WAITING_ID + "').style.display = 'none';\n" +
            "      document.getElementById('" + BUTTON_ID + "').style.display = 'block';\n" +
            "    } else {\n" +
            "      document.getElementById('" + BUTTON_ID + "').innerHTML = '" +
                     AFTER_INSTALL_JS + "';\n" +
            "      document.getElementById('" + BUTTON_ID + "').onclick = function() {\n" +
            "        document.location.href = '" + THIS_SERVLET + "';\n" +
            "      }\n" +
            "      paymentRequestError('App does not seem to be installed');\n" +
            "      w3cPaymentRequest = null;\n" +
            "    }\n" +
            "  } else {\n" +
            "    setTimeout(function() {\n" +
            "      waitForBrowserDisplay(result);\n" +
            "    }, 100);\n" +
            "  }\n" +
            "}\n" +
            "let w3cPaymentRequest = null;\n" +
            "if (" +
               (KeyProviderService.useW3cPaymentRequest ? "window.PaymentRequest" : "false") + 
                 ") {\n" +
            //==================================================================//
            // W3C PaymentRequest using dummy data.                             //
            //==================================================================//
            "  const dummyDetails = {total:{label:'total',amount:{currency:'USD',value:'1.00'}}};\n" +
            "  const methodData = [{\n" +
            "    supportedMethods: '" + KeyProviderService.w3cPaymentRequestUrl + "',\n" +
// Test data
//            "        supportedMethods: 'weird-pay',\n" +
            "    data: ['" + getInvocationUrl(MobileProxyParameters.SCHEME_W3CPAY, session) + "']\n" +
// Test data
//            "        supportedMethods: 'basic-card',\n" +
//            "        data: {supportedNetworks: ['visa', 'mastercard']}\n" +
            "  }];\n" +
            "  w3cPaymentRequest = new PaymentRequest(methodData, dummyDetails);\n" +
            // Addresses https://bugs.chromium.org/p/chromium/issues/detail?id=999920#c8
            "  w3cPaymentRequest.canMakePayment().then(function(result) {\n" +
            "    waitForBrowserDisplay(result);\n" +
            "  }).catch(function(err) {\n" +
            "    paymentRequestError(err.message);\n" +
            "  });\n" +
            "} else {\n" +
            "  window.addEventListener('load', (event) => {\n" +
            "    setUserName();\n" +
            "    document.getElementById('" + BUTTON_ID + "').style.display = 'block';\n" +
            "    document.getElementById('" + WAITING_ID + "').style.display = 'none';\n" +
            "  });\n" +
            "}\n" +
            "async function enroll() {\n" +
            //////////////////////////////////////////////////////////////////////
            // PaymentRequest for key enrollment?  Right, there is currently no //
            // better way combining the Web and Android applications. You get:  //
            //  - Return value to the invoking Web page                         //
            //  - Invoking Web page security context to the App                 //
            //  - UI wise almost perfect Web2App integration                    //
            //  - Away from having to select browser for App invoked pages      //
            //  - Security beating URL handlers without adding vulnerabilities  //
            //////////////////////////////////////////////////////////////////////
            "  if (w3cPaymentRequest) {\n" +
            //==================================================================//
            // It may take a second or two to get PaymentRequest up and         //
            // running.  Indicate that to the user.                             //
            //==================================================================//
            "    document.getElementById('" + BUTTON_ID + "').style.display = 'none';\n" +
            "    document.getElementById('" + WAITING_ID + "').style.display = 'block';\n" +
            "    try {\n" +
            "      const payResponse = await w3cPaymentRequest.show();\n" +
            "      payResponse.complete('success');\n" +
            //==================================================================//
            // Note that success does not necessarily mean that the enrollment  //
            // succeeded, it just means that the result is a redirect URL.      //                                                   //
            //==================================================================//
            "      document.location.href = payResponse.details." +
              MobileProxyParameters.W3CPAY_GOTO_URL + ";\n" +
            "    } catch (err) {\n" +
            "      console.error(err);\n" +
            "      paymentRequestError(err.message);\n" +
            "    }\n" +
            "  } else {\n" +
            // The browser does not support PaymentRequest, fallback to the awkward URL handler
            "    document.forms.shoot.submit();\n" +
            "  }\n" +
            "}\n",
            null,
            "<div style='padding:0 1em'>" +
              "This proof-of-concept system provisions secure payment credentials " + 
              "to be used in the Android version of the Saturn &quot;Wallet&quot;." +
            "</div>" + 
            "<div style='display:flex;justify-content:center;padding-top:15pt'>" +
              "<table>" + 
                "<tr><td>Your name (real or made up):</td></tr>" + 
                "<tr><td>" +
                  "<form name='shoot' method='POST' action='" + THIS_SERVLET + "'>" + 
                    "<input type='text' name='" + USERNAME_SESSION_ATTR + 
                      "' value='" + DEFAULT_USER_NAME_HTML + 
                      "' size='30' maxlength='50' " + 
                      "style='background-color:#def7fc' oninput=\"setUserName()\">" +
                   "</form>" +
                 "</td></tr>" + 
              "</table>" +
            "</div>" + 
            "<div style='text-align:center'>" +
              "This name will be printed on your virtual payment cards." +
            "</div>" + 
            "<div id='" + ERROR_ID + "' " +
              "style='color:red;font-weight:bold;padding-top:1em;display:none'></div>" +
            "<img id='" + WAITING_ID + "' src='waiting.gif' " +
              "style='padding-top:1em' alt='waiting'>" +
            "<div style='display:flex;justify-content:center;padding-top:1em'>" +
              "<div id='" + BUTTON_ID + "' style='display:none' class='stdbtn' onclick=\"enroll()\">" +
                BUTTON_TEXT_HTML + 
              "</div>" +
            "</div>" + 
            "<div style='padding:4em 1em 1em 1em'>If you have not yet " + 
              "installed the &quot;Wallet&quot;, this is the time but <i>please do not " + 
              "start the application</i>, simply press " + 
              "<div style='display:inline;background:blue;color:white;" + 
              "font-weight:bold;padding:0 0.5em'>&lt;</div> " + 
              "after the installation!</i>" +
            "</div>" +
            "<div style='cursor:pointer;display:flex;justify-content:center;align-items:center'>" +
              "<img src='google-play-badge.png' style='height:25pt;padding:0 15pt' alt='image' " +
                "title='Android' onclick=\"document.location.href = '" +
                "https://play.google.com/store/apps/details?id=" +
                MobileProxyParameters.ANDROID_PACKAGE_NAME + "'\">" +
            "</div>" + 
            "</div>" + // Main window end tag
            "<div class='sitefooter'>Note: in a real configuration you would also need to " +
            "authenticate as a part of the enrollment."));
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException {
        request.setCharacterEncoding("utf-8");
        HttpSession session = request.getSession(false);
        String userName = request.getParameter(USERNAME_SESSION_ATTR);
        if (userName != null) {
            userName = userName.trim();
            if (userName.isEmpty()) {
                userName = ANONYMOUS_JAVA;
            } else if (userName.length() > NAME_MAX_LENGTH) {
                userName = userName.substring(0, NAME_MAX_LENGTH);
            }
            session.setAttribute(USERNAME_SESSION_ATTR, userName);
        }
        
        if (request.getParameter(W3C_PAYMENT_REQUEST_MODE_PARM) == null) {
            output(response,
                   getHTML(GO_HOME,
                "onload=\"document.location.href = '" + 
                    getInvocationUrl(MobileProxyParameters.SCHEME_URLHANDLER, session) + 
                    "#Intent;scheme=webpkiproxy;package=" +  
                    MobileProxyParameters.ANDROID_PACKAGE_NAME +
                    ";end';\"", 
                "<div><div class='label' style='text-align:center'>Saturn App Bootstrap</div>" +
                "<div style='padding-top:15pt'>If this is all you get there is " +
                "something wrong with the installation.</div>" +
                "</div>"));
        } else {
            output(response, "");
        }
    }
}
