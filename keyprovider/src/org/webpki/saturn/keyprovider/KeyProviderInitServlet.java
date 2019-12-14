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

import org.webpki.net.MobileProxyParameters;

// Initiation code for KeyGen2

public class KeyProviderInitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger(KeyProviderInitServlet.class.getCanonicalName());

    static final String KEYGEN2_SESSION_ATTR           = "keygen2";
    static final String USERNAME_SESSION_ATTR          = "userName";
    static final String W3C_PAYMENT_REQUEST_MODE_PARM  = "w3cmode";  // POST is used in two ways...
    
    static final int NAME_MAX_LENGTH                   = 50;  // Reflected in the DB

    static final String INIT_TAG  = "init";     // Note: This is currently also a part of the KeyGen2 client!
    static final String ABORT_TAG = "abort";
    static final String PARAM_TAG = "msg";
    static final String ERROR_TAG = "err";
    
    static final String BUTTON_ID = "gokg2";
    
    static final String DEFAULT_USER_NAME_HTML = "Luke Skywalker &#x1f984;";    // Unicorn emoji
    
    static final String BUTTON_TEXT_HTML       = "Start Enrollment &#x1f680;";  // Rocket emoji
    
    static final String ANONYMOUS_JAVA         = "Anonymous " + 
                 new String(Character.toChars(Integer.parseInt("1f47d", 16)));  // E.T. emoji
    
    static final int MINIMUM_CHROME_VERSION    = 75;

    static final String GO_HOME =              
            "history.pushState(null, null, 'init');\n" +
            "window.addEventListener('popstate', function(event) {\n" +
            "    history.pushState(null, null, 'init');\n" +
            "});\n";

    static final String HTML_INIT = 
            "<!DOCTYPE html><html>" + 
            "<head>" + 
            "<link rel=\"icon\" href=\"saturn.png\" sizes=\"192x192\">" + 
            "<title>Payment Credential Enrollment</title>" + 
            "<meta charset=\"utf-8\">" + 
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" + 
            "<style type=\"text/css\">" + 
            ".displayContainer {" + 
            "    display: block;" + 
            "    height: 100%;" + 
            "    width: 100%;" + 
            "    align-items: center;" + 
            "    display: flex;" + 
            "    flex-direction: column;" + 
            "    justify-content: center;" + 
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
            "    font-size:10pt;" + 
            "    color:#000000;" + 
            "    font-family:verdana,arial;" + 
            "    background-color: white;" + 
            "    height: 100%;" + 
            "    margin: 0;" + 
            "    width: 100%;" + 
            "}" + 

            "html {" + 
            "    height: 100%;" + 
            "    width: 100%;" + 
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
            s.append("<script type=\"text/javascript\">").append(javascript)
                    .append("</script>");
        }
        s.append("</head><body");
        if (bodyscript != null) {
            s.append(' ').append(bodyscript);
        }
        s.append(
                "><div style=\"cursor:pointer;position:absolute;top:15pt;left:15pt;z-index:5;width:100pt\"" +
                " onclick=\"document.location.href='http://cyberphone.github.io/doc/saturn'\" title=\"Home of Saturn\">")
         .append (KeyProviderService.saturnLogotype)
         .append ("</div><div class=\"displayContainer\">")
                .append(box).append("</div></body></html>");
        return s.toString();
    }
  
    static void output(HttpServletResponse response, String html) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        byte[] data = html.getBytes("utf-8");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
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
               "&" + MobileProxyParameters.PUP_VERSIONS   + "=" + KeyProviderService.grantedVersions;
   }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userAgent = request.getHeader("User-Agent");
        boolean notOk = true;
        if (userAgent.contains("Android ")) {
            int i = userAgent.indexOf(" Chrome/");
            if (i > 0) {
                String chromeVersion = userAgent.substring(i + 8, userAgent.indexOf('.', i));
                if (Integer.parseInt(chromeVersion) >= MINIMUM_CHROME_VERSION) {
                    notOk = false;
                }
            }
        }
        if (notOk) {
            output(response, 
                    getHTML(null,
                            null,
                "<div class=\"label\">This proof-of-concept system only supports " +
                  "Android and using the \"Chrome\" browser (min version: " + 
                  MINIMUM_CHROME_VERSION + ")" +
                "</div>"));
            return;
        }
        output(response, 
               getHTML(GO_HOME +
            (KeyProviderService.useW3cPaymentRequest ?
            "function paymentRequestError(msg) {\n" +
            "  console.info('Payment request error:' + msg);\n" +
            "  document.getElementById('" + BUTTON_ID + 
            "').outerHTML = '<div style=\"color:red;font-weight:bold\">' + " +
              "msg + '</div>';\n" +
            "}\n\n" +

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
            "  if (window.PaymentRequest) {\n" +
            //==================================================================//
            // It may take a second or two to get PaymentRequest up and         //
            // running.  Indicate that to the user.                             //
            //==================================================================//
            "    document.getElementById('" + BUTTON_ID + "').outerHTML = " +
              "'<img id=\"" + BUTTON_ID + "\" src=\"waiting.gif\">';\n" +
            //==================================================================//
            // The following code may seem strange but the Web application      //
            // does not create an HttpSession so we do this immediately after   //
            // the user hit the "Enroll" button.  Using fetch() this becomes    //
            // invisible UI wise. The POST provides the current FORM data which //
            // is added to the HttpSession to be created on the server.         //
            //==================================================================//
            "    var formData = new URLSearchParams();\n" +
            "    formData.append('" + USERNAME_SESSION_ATTR +
              "', document.forms.shoot.elements." + USERNAME_SESSION_ATTR + ".value);\n" +
            "    formData.append('" + W3C_PAYMENT_REQUEST_MODE_PARM + "', 1);\n" +
            "    try {\n" +
            "      const httpResponse = await fetch('init', {\n" +
            "        method: 'POST',\n" +
            "        body: formData\n" +
            "      });\n" +
            "      if (httpResponse.status == " + HttpServletResponse.SC_OK + ") {\n" +
            "        const invocationUrl = await httpResponse.text();\n" +
            //==================================================================//
            // Success! Now we can now hook into the W3C PaymentRequest using   //
            // "dummy" payment data.                                            //
            //==================================================================//
            "        const details = {total:{label:'total',amount:{currency:'USD',value:'1.00'}}};\n" +
            "        const supportedInstruments = [{\n" +
            "          supportedMethods: '" + KeyProviderService.w3cPaymentRequestUrl + "',\n" +
// Test data
//            "          supportedMethods: 'weird-pay',\n" +
            "          data: {url: invocationUrl}\n" +
// Test data
//            "          supportedMethods: 'basic-card',\n" +
//            "          data: {supportedNetworks: ['visa', 'mastercard']}\n" +
            "        }];\n" +
            "        const payRequest = new PaymentRequest(supportedInstruments, details);\n" +
            "        if (await payRequest.canMakePayment()) {\n" +
            "          const payResponse = await payRequest.show();\n" +
            "          payResponse.complete('success');\n" +
            //==================================================================//
            // Note that success does not necessarily mean that the enrollment  //
            // succeeded, it just means that the result is a redirect URL.      //                                                   //
            //==================================================================//
            "          document.location.href = payResponse.details." +
              MobileProxyParameters.W3CPAY_GOTO_URL + ";\n" +
            "        } else {\n" +
            "          paymentRequestError('App does not seem to be installed');\n" +
            "        }\n" +
            "      } else {\n" +
            "        paymentRequestError('Server error, try again');\n" +
            "      }\n" +
            "    } catch (err) {\n" +
            "      console.error(err);\n" +
            "      paymentRequestError(err.message);\n" +
            "    }\n" +
            "  } else {\n" +
            // The browser does not support PaymentRequest, fallback to the awkward URL handler
            "    document.forms.shoot.submit();\n" +
            "  }\n" +
            "}"
                 :
            "function enroll() {\n" +
            "  document.forms.shoot.submit();\n" +
            "}"),
            null,
            "<form name=\"shoot\" method=\"POST\" action=\"init\">" + 
            "<div>" +
              "This proof-of-concept system provisions secure payment credentials<br>" + 
              "to be used in the Android version of the Saturn &quot;Wallet&quot;." +
            "</div>" + 
            "<div style=\"display:flex;justify-content:center;padding-top:15pt\">" +
              "<table>" + 
                "<tr><td>Your name (real or made up):</td></tr>" + 
                "<tr><td><input type=\"text\" name=\"" + USERNAME_SESSION_ATTR + 
                  "\" value=\"" + DEFAULT_USER_NAME_HTML + 
                  "\" size=\"30\" maxlength=\"50\" " + 
                  "style=\"background-color:#def7fc\"></td></tr>" + 
              "</table>" +
            "</div>" + 
            "<div style=\"text-align:center\">" +
              "This name will be printed on your virtual payment cards." +
            "</div>" + 
            "<div style=\"display:flex;justify-content:center;padding-top:15pt\">" +
              "<div id=\"" + BUTTON_ID + "\" class=\"stdbtn\" onclick=\"enroll()\">" +
                BUTTON_TEXT_HTML + 
              "</div>" +
            "</div>" + 
            "<div style=\"padding-top:40pt;padding-bottom:10pt\">If you have not " +
              "installed WebPKI, now is the time to do it!</div>" +
            "<div style=\"cursor:pointer;display:flex;justify-content:center;align-items:center\">" +
              "<img src=\"google-play-badge.png\" style=\"height:25pt;padding:0 15pt\" alt=\"image\" " +
                "title=\"Android\" onclick=\"document.location.href = " +
                "'https://play.google.com/store/apps/details?id=" +
                MobileProxyParameters.ANDROID_PACKAGE_NAME + "'\">" +
            "</div>" + 
            "</form>" + 
            "</div>" + // Main window end tag
            "<div class=\"sitefooter\">Note: in a real configuration you would also need to " +
            "authenticate as a part of the enrollment."));
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setCharacterEncoding("utf-8");
        HttpSession session = request.getSession(true);
        String userName = request.getParameter(USERNAME_SESSION_ATTR);
        if (userName == null || (userName = userName.trim()).isEmpty()) {
            userName = ANONYMOUS_JAVA;
        }
        if (userName.length() > NAME_MAX_LENGTH) {
            userName = userName.substring(0, NAME_MAX_LENGTH);
        }
        session.setAttribute(KEYGEN2_SESSION_ATTR,
                new ServerState(new KeyGen2SoftHSM(KeyProviderService.keyManagementKey), 
                                KeyProviderService.keygen2RunUrl,
                                KeyProviderService.serverCertificate,
                                null));
        session.setAttribute(USERNAME_SESSION_ATTR, userName);
        if (request.getParameter(W3C_PAYMENT_REQUEST_MODE_PARM) == null) {
            output(response,
                   getHTML(GO_HOME,
                "onload=\"document.location.href = '" + 
                    getInvocationUrl(MobileProxyParameters.SCHEME_URLHANDLER, session) + 
                    "#Intent;scheme=webpkiproxy;package=" +  
                    MobileProxyParameters.ANDROID_PACKAGE_NAME +
                    ";end';\"", 
                "<div><div class=\"label\" style=\"text-align:center\">Saturn App Bootstrap</div>" +
                "<div style=\"padding-top:15pt\">If this is all you get there is " +
                "something wrong with the installation.</div>" +
                "</div>"));
        } else {
/*
            // This code makes the PaymentRequest "gesture" requirement open
            // Chrome's payment dialog which is very confusing for users.
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
*/
            String invocationUrl = getInvocationUrl(MobileProxyParameters.SCHEME_W3CPAY, session);
            logger.info("POST return=" + invocationUrl);
            output(response, invocationUrl);
        }
    }
}
