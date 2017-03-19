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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.BaseProperties;

//This servlet provides a base for "PayeeAuthority" and "ProviderAuthority" publishers.

public abstract class AuthorityBaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    protected abstract boolean isProvider();
    
    public void processAuthorityRequest(HttpServletRequest request, HttpServletResponse response, byte[] authorityData) throws IOException, ServletException {
        if (authorityData == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            String accept = request.getHeader(BaseProperties.HTTP_ACCEPT_HEADER);
            if (accept != null && accept.contains(BaseProperties.HTML_CONTENT_TYPE)) {
                // Presumably called by a browser
                JSONObjectReader rd = JSONParser.parse(authorityData);
                response.setContentType(BaseProperties.HTML_CONTENT_TYPE + "; charset=utf-8");
                authorityData = new StringBuffer("<!DOCTYPE html>" +
                            "<html><head><meta charset=\"UTF-8\"><link rel=\"icon\" href=\"")
                    .append(isProvider() ? "" : "../")
                    .append("saturn.png\" sizes=\"192x192\">"+
                            "<title>Saturn Authority Object</title><style type=\"text/css\">" +
                            " body {font-size:8pt;color:#000000;font-family:Verdana,'Bitstream Vera Sans'," +
                             "'DejaVu Sans',Arial,'Liberation Sans';background-color:white} " +
                             " code {font-size:9pt} " +
                            "</style></head><body><table style=\"margin-left:auto;margin-right:auto;width:800pt\"><tr><td style=\"text-align:center;font-size:10pt;font-weight:bold;padding:15pt\">")
                    .append(isProvider() ? Messages.PROVIDER_AUTHORITY.toString() : Messages.PAYEE_AUTHORITY.toString())
                    .append("</td></tr>" +
                            "<tr><td style=\"padding-bottom:8pt\">" +
                            "This Saturn <i>live object</i> is normally requested by a service provider for looking up partner core data. In this case " +
                            "the requester seems to be a browser which is why a &quot;pretty-printed&quot; HTML page is returned instead of raw JSON.")
                    .append(isProvider() ? "" : "</td></tr><tr><td style=\"padding-bottom:8pt\">" +
                            "This particular object is issued by the provider specified by the <code>&quot;" + 
                            BaseProperties.PROVIDER_AUTHORITY_URL_JSON + "&quot;</code> property: <a href=\"" +
                            rd.getString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON) + "\" target=\"_blank\">" + 
                            rd.getString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON) + 
                            "</a>.")
                    .append("</td></tr>" +
                            "<tr><td><div style=\"word-break:break-all;background:#F8F8F8;" +
                            "border-width:1px;border-style:solid;border-color:grey;padding:10pt;box-shadow:3pt 3pt 3pt #D0D0D0\">")
                    .append(rd.serializeToString(JSONOutputFormats.PRETTY_HTML))
                    .append("</div></td></tr></body></html>").toString().getBytes("UTF-8");
            } else {
                // Normal call from a service
                response.setContentType(BaseProperties.JSON_CONTENT_TYPE);
            }
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().write(authorityData);
        }
    }
}
