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
package org.webpki.saturn.common;

import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

//This servlet provides a base for "PayeeAuthority" and "ProviderAuthority" publishers.

public abstract class AuthorityBaseServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    protected abstract boolean isProvider();  // ProviderAuthorityObject
    
    protected boolean isPayeeHosted() {       // Payee is enabled through a hosting service?
        return false;
    }
    
    private static int LOGOTYPE_AREA = 100;  // We give logotypes the same area to play around in
    
    public static final String BORDER = 
        "border-width:1px;border-style:solid;border-color:#a9a9a9";
    
    public static final String BOX_SHADOW_OFFSET = "0.3em";

    public static final String BOX_SHADOW = "box-shadow:" +
        BOX_SHADOW_OFFSET + " " +
        BOX_SHADOW_OFFSET + " " +
        BOX_SHADOW_OFFSET + " " +
        "#d0d0d0";

    public static final String TOP_ELEMENT = 
        "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
        "<meta name='viewport' content='width=device-width, initial-scale=1'>";

    public static final String REST_ELEMENT = 
        "<style type='text/css'>" +
        " .header {font-size:1.6em;padding-bottom:1em}" +
        " .spacepara {padding:1.2em 0 0.6em 0}" +
        " .para {padding-bottom:0.6em}" +
        " .tftable {border-collapse:collapse;" + BOX_SHADOW + ";" +
           "margin-bottom:" + BOX_SHADOW_OFFSET + "}" +
        " .tftable td {white-space:nowrap;background-color:#ffffe0;" +
          "padding:0.4em 0.5em;" + BORDER + "}" +
        " .tftable th {white-space:nowrap;padding:0.4em 0.5em;" +
          "background:linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
        "text-align:center;" + BORDER +"}" +
        " .json {word-break:break-all;background-color:#f8f8f8;padding:1em;" +
                            BORDER + ";" + BOX_SHADOW + "}" +
        " body {margin:10pt;font-size:8pt;color:#000000;font-family:Verdana," +
        "'Bitstream Vera Sans','DejaVu Sans',Arial,'Liberation Sans';background-color:white}" + 
        " code {font-size:9pt}" +
        " @media (max-width:800px) {code {font-size:8pt;}}" +
        " a {color:blue;text-decoration:none}" +
        "</style></head>";
    public static final String SATURN_LINK = 
            "<a href='https://cyberphone.github.io/doc/saturn/'>Saturn</a>";

    String keyWord(String constant) {
        return "<code>&quot;" + constant + "&quot;</code>";
    }
    
    String tableRow(JSONObjectReader rd,
                    String property, 
                    String description) throws IOException {
        return tableRow(rd, property, description, false);
    }
    
    String tableRow(JSONObjectReader rd,
                    String property,
                    String description,
                    boolean optional) throws IOException {
        if (!optional || rd.hasProperty(property)) {
            rd.scanAway(property);
        }
        return "<tr><td><code>" + property + "</code></td><td>" + 
               (optional ? "<i>Optional</i>. " : "") + description + "</td></tr>";
    }
    
    public static StringBuilder addLogotype(String logotypeUrl,
                                            String commonName,
                                            boolean centered) {
        return new StringBuilder(
            "<script>\n" +
            "function adjustImage(image) {\n" +
            "  image.style.width = " +
               "Math.sqrt((" +
               LOGOTYPE_AREA + 
               " * image.offsetWidth) / image.offsetHeight) + 'em';\n" +
               "  image.style.visibility = 'visible';\n" +
            "}\n"+
            "</script>" +
            "<img style='")
        .append(centered ? "margin:0 auto;display:block;" : "")
        .append("max-width:90%;visibility:hidden' src='")
        .append(logotypeUrl)
        .append("' alt='logo' title='")
        .append(commonName)
        .append(" logotype' onload=\"adjustImage(this)\">");
    }

    public void processAuthorityRequest(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        byte[] authorityData) throws IOException, ServletException {
        if (authorityData == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain");
            response.getWriter().append("No such entry");
        } else {
            String accept = request.getHeader(HttpSupport.HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(HttpSupport.HTML_CONTENT_TYPE)) {
                // Presumably called by a browser
                JSONObjectReader rd = JSONParser.parse(authorityData);
                rd.getString(JSONDecoderCache.CONTEXT_JSON);
                rd.getString(JSONDecoderCache.QUALIFIER_JSON);
                StringBuilder html = new StringBuilder(TOP_ELEMENT +
                        "<link rel='icon' href='")
                .append(isProvider() ? "" : "../")
                .append("saturn.png' sizes='192x192'>"+
                        "<title>Saturn Authority Object</title>" +
                        REST_ELEMENT +
                        "<body><div class='header'>")
                .append(isProvider() ? 
  Messages.PROVIDER_AUTHORITY.toString() : Messages.PAYEE_AUTHORITY.toString())
                .append("</div>")
                .append(addLogotype(rd.getString(LOGOTYPE_URL_JSON),
                                    rd.getString(COMMON_NAME_JSON),
                                    false))
                .append("<div class='spacepara'>This " +
                        SATURN_LINK +
                        " <i>live object</i> is normally requested by service providers " + 
                        "for looking up partner core data. In this case " +
                        "the requester seems to be a browser which is why a " +
                        "&quot;pretty-printed&quot; HTML page is returned instead of raw JSON.</div>" + 
                        "<div class='para'>This particular (")
                .append(isProvider() ? "provider" : "payee")
                .append(") object was issued by " +
                        (isPayeeHosted() ?
                                "an external hosting provider under the supervision of " : ""))
                .append("the provider specified by the " +
                        keyWord(PROVIDER_AUTHORITY_URL_JSON) +
                        " property: <a href='" +
                        rd.getString(PROVIDER_AUTHORITY_URL_JSON) + "'>" + 
                        rd.getString(PROVIDER_AUTHORITY_URL_JSON) + 
                        "</a>.")
                .append("</div><div class='json'>")
                .append(rd.serializeToString(JSONOutputFormats.PRETTY_HTML))
                .append("</div><div class='spacepara'>Quick Reference</div>" +
                        "<div style='overflow-x:auto'><table class='tftable'>" +
                        "<tr><th>Property</th><th>Description</th></tr>")
                .append(isProvider() ?
                        tableRow(rd, HTTP_VERSIONS_JSON, "Supported HTTP versions") +
                        tableRow(rd, PROVIDER_AUTHORITY_URL_JSON, "The address of this object") +
                        tableRow(rd, COMMON_NAME_JSON, "Provider common name") +
                        tableRow(rd, HOME_PAGE_JSON, "Provider public home page") +
                        tableRow(rd, LOGOTYPE_URL_JSON, "Provider logotype (as shown above)") +
                        tableRow(rd, SERVICE_URL_JSON, "Primary service end point") +
                        tableRow(rd, SUPPORTED_PAYMENT_METHODS_JSON, 
                                "Supported client:[backend...] payment methods") +
                        tableRow(rd, EXTENSIONS_JSON, "Supported extension objects", true) +
                        tableRow(rd, SIGNATURE_PROFILES_JSON, "Signature key types " + 
                                 "and algorithms <i>recognized</i> by the provider") +
                        tableRow(rd, ENCRYPTION_PARAMETERS_JSON, "Holds one or more " +
                                "encryption keys <i>offered</i> by the provider") +
                        tableRow(rd, HOSTING_PROVIDERS_JSON, 
                                "Holds core data of payee hosting providers", true)
                           : 
                        tableRow(rd, PAYEE_AUTHORITY_URL_JSON, 
                                "The address of this object (payee \"identity\")") +
                        tableRow(rd, PROVIDER_AUTHORITY_URL_JSON, 
                                "The address of the issuing provider's authority object") +
                        tableRow(rd, LOCAL_PAYEE_ID_JSON, 
                                "Local payee id used by the payee provider") +
                        tableRow(rd, COMMON_NAME_JSON, "Payee common name") +
                        tableRow(rd, HOME_PAGE_JSON, "Payee public home page") +
                        tableRow(rd, LOGOTYPE_URL_JSON, "Payee logotype (as shown above)") +
                        tableRow(rd, ACCOUNT_VERIFIER_JSON, "For verifying claimed payee account", 
                                true) +
                        tableRow(rd, SIGNATURE_PARAMETERS_JSON, "Holds one or more payee " +
                                "signature keys and associated algorithms")
                        )
                .append(tableRow(rd, TIME_STAMP_JSON, "Object creation time"))
                .append(tableRow(rd, EXPIRES_JSON, "When the object becomes stale/invalid"))
                .append(tableRow(rd, ISSUER_SIGNATURE_JSON, isProvider() ?
                                       "X.509 provider signature" : "Hosting provider signature"))
                .append("</table></div><p><i>API Version</i>: " + Version.PROTOCOL +
                        "</p></body></html>");
                // Just to check that we didn't forgot anything...
                rd.checkForUnread();
                HttpSupport.writeHtml(response, html);
            } else {
                // Normal call from a service
                HttpSupport.writeData(response, authorityData, JSON_CONTENT_TYPE);
            }
        }
    }
}
