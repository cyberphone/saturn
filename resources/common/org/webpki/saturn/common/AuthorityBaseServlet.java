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
import org.webpki.json.JSONSignatureDecoder;

//This servlet provides a base for "PayeeAuthority" and "ProviderAuthority" publishers.

public abstract class AuthorityBaseServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    protected abstract boolean isProvider();
    
    public static final String BORDER = "border-width:1px;border-style:solid;border-color:#a9a9a9";
    public static final String BOX_SHADDOW = "box-shadow:3pt 3pt 3pt #D0D0D0";
    public static final String TOP_ELEMENT = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">";
    public static final String REST_ELEMENT = 
        "<style type=\"text/css\">" +
        " .header {text-align:center;font-size:10pt;font-weight:bold;padding:15pt}" +
        " td {padding-bottom:8pt}" +
        " .tftable {border-collapse:collapse;" + BOX_SHADDOW + "}" +
        " .tftable td {background-color:#FFFFE0;padding:3pt 4pt;" + BORDER + "}" +
        " .tftable th {padding:4pt 3pt;background:linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
        "text-align:center;" + BORDER +"}" +
        " body {margin:10pt;font-size:8pt;color:#000000;font-family:Verdana," +
        "'Bitstream Vera Sans','DejaVu Sans',Arial,'Liberation Sans';background-color:white}" + 
        " code {font-size:9pt}" +
        " a {color:blue;text-decoration:none}" +
        "</style></head>";
    public static final String SATURN_LINK = "<a href=\"https://cyberphone.github.io/doc/saturn/\" target=\"_blank\">Saturn</a>";

    String keyWord(String constant) {
        return "<code>&quot;" + constant + "&quot;</code>";
    }
    
    String tableRow(JSONObjectReader rd, String property, String description) throws IOException {
        return tableRow(rd, property, description, false);
    }
    
    String tableRow(JSONObjectReader rd, String property, String description, boolean optional) throws IOException {
        if (!optional || rd.hasProperty(property)) {
            rd.scanAway(property);
        }
        return "<tr><td><code>" + property + "</code></td><td>" + (optional ? "<i>Optional</i>. " : "") + description + "</td></tr>";
    }

    public void processAuthorityRequest(HttpServletRequest request, HttpServletResponse response, byte[] authorityData) throws IOException, ServletException {
        if (authorityData == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            String accept = request.getHeader(HttpSupport.HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(HttpSupport.HTML_CONTENT_TYPE)) {
                // Presumably called by a browser
                JSONObjectReader rd = JSONParser.parse(authorityData);
                rd.getString(JSONDecoderCache.CONTEXT_JSON);
                rd.getString(JSONDecoderCache.QUALIFIER_JSON);
                response.setContentType(HttpSupport.HTML_CONTENT_TYPE + "; charset=utf-8");
                StringBuffer html = new StringBuffer(TOP_ELEMENT +
                            "<link rel=\"icon\" href=\"")
                    .append(isProvider() ? "" : "../")
                    .append("saturn.png\" sizes=\"192x192\">"+
                            "<title>Saturn Authority Object</title>" +
                            REST_ELEMENT +
                            "<body><table style=\"margin-left:auto;margin-right:auto\"><tr><td class=\"header\">")
                    .append(isProvider() ? Messages.PROVIDER_AUTHORITY.toString() : Messages.PAYEE_AUTHORITY.toString())
                    .append("</td></tr>" +
                            "<tr><td>This " +
                            SATURN_LINK +
                            " <i>live object</i> is normally requested by service providers for looking up partner core data. In this case " +
                            "the requester seems to be a browser which is why a &quot;pretty-printed&quot; HTML page is returned instead of raw JSON.")
                    .append(isProvider() ? "" : "</td></tr><tr><td>" +
                            "This particular (payee) object was issued by the provider specified by the " +
                            keyWord(PROVIDER_AUTHORITY_URL_JSON) +
                            " property: <a href=\"" +
                            rd.getString(PROVIDER_AUTHORITY_URL_JSON) + "\" target=\"_blank\">" + 
                            rd.getString(PROVIDER_AUTHORITY_URL_JSON) + 
                            "</a>.")
                    .append("</td></tr>" +
                            "<tr><td><div style=\"word-break:break-all;background-color:#F8F8F8;padding:10pt;" +
                            BORDER + ";" + BOX_SHADDOW + "\">")
                    .append(rd.serializeToString(JSONOutputFormats.PRETTY_HTML))
                    .append("</div></td></tr><tr><td style=\"padding:4pt 0pt 4pt 0pt\">Short reference:</td></tr>" +
                            "<tr><td><table class=\"tftable\"><tr><th>Property</th><th>Description</th></tr>")
                    .append(isProvider() ?
                            tableRow(rd, HTTP_VERSION_JSON, "Preferred HTTP version (&#x2265; HTTP/1.1)") +
                            tableRow(rd, AUTHORITY_URL_JSON, "The address of this object") +
                            tableRow(rd, HOME_PAGE_JSON, "Provider public home page") +
                            tableRow(rd, SERVICE_URL_JSON, "Primary service end point") +
                            tableRow(rd, EXTENSIONS_JSON, "Supported extension objects", true) +
                            tableRow(rd, PAYMENT_METHODS_JSON, "Supported payment methods") +
                            tableRow(rd, SIGNATURE_PROFILES_JSON, "Signature key types and algorithms <i>recognized</i> by the provider") +
                            tableRow(rd, ENCRYPTION_PARAMETERS_JSON, "Holds one or more encryption keys <i>offered</i> by the provider") +
                            tableRow(rd, HOSTING_PROVIDER_JSON, "Holds core data of a payee hosting provider", true)
                               : 
                            tableRow(rd, AUTHORITY_URL_JSON, "The address of this object") +
                            tableRow(rd, HOME_PAGE_JSON, "Payee public home page") +
                            tableRow(rd, PROVIDER_AUTHORITY_URL_JSON, "The address of the issuing provider's authority object") +
                            tableRow(rd, COMMON_NAME_JSON, "Payee common name") +
                            tableRow(rd, ID_JSON, "Local payee id used by the payee provider") +
                            tableRow(rd, SIGNATURE_PARAMETERS_JSON, "Holds one or more payee signature keys and associated algorithms")
                            )
                    .append(tableRow(rd, TIME_STAMP_JSON, "Object creation time"))
                    .append(tableRow(rd, EXPIRES_JSON, "When the object becomes stale/invalid"))
                    .append(tableRow(rd, JSONSignatureDecoder.SIGNATURE_JSON, isProvider() ?
                                                    "X.509 provider attestation signature" : "Hosting provider attestation signature"))
                    .append("</table></td></tr></table></body></html>");
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
