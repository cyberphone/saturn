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
import javax.servlet.ServletOutputStream;
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
    
    String keyWord(String constant) {
        return "<code>&quot;" + constant + "&quot;</code>";
    }
    
    String list(JSONObjectReader rd, String property, String description) throws IOException {
        return list(rd, property, description, false);
    }
    
    String list(JSONObjectReader rd, String property, String description, boolean optional) throws IOException {
        if (!optional || rd.hasProperty(property)) {
            rd.scanAway(property);
        }
        return "<li>" + keyWord(property) + ": " + (optional ? "<i>Optional</i>. " : "") + description + "</li>";
    }

    public void processAuthorityRequest(HttpServletRequest request, HttpServletResponse response, byte[] authorityData) throws IOException, ServletException {
        if (authorityData == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            String accept = request.getHeader(HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(HTML_CONTENT_TYPE)) {
                // Presumably called by a browser
                JSONObjectReader rd = JSONParser.parse(authorityData);
                rd.getString(JSONDecoderCache.CONTEXT_JSON);
                rd.getString(JSONDecoderCache.QUALIFIER_JSON);
                response.setContentType(HTML_CONTENT_TYPE + "; charset=utf-8");
                authorityData = new StringBuffer("<!DOCTYPE html>" +
                            "<html><head><meta charset=\"UTF-8\"><link rel=\"icon\" href=\"")
                    .append(isProvider() ? "" : "../")
                    .append("saturn.png\" sizes=\"192x192\">"+
                            "<title>Saturn Authority Object</title><style type=\"text/css\">" +
                            " body {margin:10pt;font-size:8pt;color:#000000;font-family:Verdana,'Bitstream Vera Sans'," +
                             "'DejaVu Sans',Arial,'Liberation Sans';background-color:white} " +
                             " code {font-size:9pt} " +
                             " a {color:blue;text-decoration:none} " +
                             " li {padding-bottom:3pt} " +
                            "</style></head><body><table style=\"margin-left:auto;margin-right:auto\"><tr><td style=\"text-align:center;font-size:10pt;font-weight:bold;padding:15pt\">")
                    .append(isProvider() ? Messages.PROVIDER_AUTHORITY.toString() : Messages.PAYEE_AUTHORITY.toString())
                    .append("</td></tr>" +
                            "<tr><td style=\"padding-bottom:8pt\">" +
                            "This <a href=\"https://cyberphone.github.io/doc/saturn/\" target=\"_blank\">Saturn</a> " +
                            "<i>live object</i> is normally requested by service providers for looking up partner core data. In this case " +
                            "the requester seems to be a browser which is why a &quot;pretty-printed&quot; HTML page is returned instead of raw JSON.")
                    .append(isProvider() ? "" : "</td></tr><tr><td style=\"padding-bottom:8pt\">" +
                            "This particular (payee) object was issued by the provider specified by the " +
                            keyWord(PROVIDER_AUTHORITY_URL_JSON) +
                            " property: <a href=\"" +
                            rd.getString(PROVIDER_AUTHORITY_URL_JSON) + "\" target=\"_blank\">" + 
                            rd.getString(PROVIDER_AUTHORITY_URL_JSON) + 
                            "</a>.")
                    .append("</td></tr>" +
                            "<tr><td><div style=\"word-break:break-all;background:#F8F8F8;" +
                            "border-width:1px;border-style:solid;border-color:grey;padding:10pt;box-shadow:3pt 3pt 3pt #D0D0D0\">")
                    .append(rd.serializeToString(JSONOutputFormats.PRETTY_HTML))
                    .append("</div></td></tr><tr><td style=\"padding-top:10pt\">Properties:<ul>")
                    .append(isProvider() ?
                            list(rd, HTTP_VERSION_JSON, "Preferred HTTP version (&#x2265; HTTP/1.1)") +
                            list(rd, AUTHORITY_URL_JSON, "The address of this object") +
                            list(rd, SERVICE_URL_JSON, "Primary service end point") +
                            list(rd, EXTENSIONS_JSON, "Supported extension objects", true) +
                            list(rd, PROVIDER_ACCOUNT_TYPES_JSON, "Supported account types", true) +
                            list(rd, SIGNATURE_PROFILES_JSON, "Signature key types and algorithms <i>recognized</i> by the provider") +
                            list(rd, ENCRYPTION_PARAMETERS_JSON, "Holds one or more encryption keys <i>offered</i> by the provider")
                               : 
                            list(rd, AUTHORITY_URL_JSON, "The address of this object") +
                            list(rd, PROVIDER_AUTHORITY_URL_JSON, "The address of the issuing provider's authority object") +
                            list(rd, COMMON_NAME_JSON, "Payee common name") +
                            list(rd, ID_JSON, "Local payee id used by the payee provider") +
                            list(rd, SIGNATURE_PARAMETERS_JSON, "Holds one or more payee signature keys and associated algorithms")
                            )
                    .append(list(rd, TIME_STAMP_JSON, "Object creation time"))
                    .append(list(rd, EXPIRES_JSON, "When the object becomes stale/invalid"))
                    .append(list(rd, JSONSignatureDecoder.SIGNATURE_JSON, "X.509 provider attestation signature"))
                    .append("</ul></td></tr></table></body></html>").toString().getBytes("UTF-8");
                // Just to check that we didn't forgot anything...
                rd.checkForUnread();
            } else {
                // Normal call from a service
                response.setContentType(JSON_CONTENT_TYPE);
            }
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            // Chunked data seems unnecessary here
            response.setContentLength(authorityData.length);
            ServletOutputStream servletOutputStream = response.getOutputStream();
            servletOutputStream.write(authorityData);
            servletOutputStream.flush();
        }
    }
}
