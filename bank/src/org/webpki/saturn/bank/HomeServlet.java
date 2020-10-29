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
package org.webpki.saturn.bank;

import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.TimeUtils;

// This servlet publishes a miniscule "home page".

public class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String authorityUrl = BankService.providerAuthorityUrl;
        StringBuilder html = new StringBuilder(
            AuthorityBaseServlet.TOP_ELEMENT +
            "<link rel='icon' href='saturn.png' sizes='192x192'><title>Saturn Bank</title>" +
            AuthorityBaseServlet.REST_ELEMENT +
            "<body>")
        .append(AuthorityBaseServlet.addLogotype("images/logotype.svg", 
                                                 BankService.bankCommonName,
                                                 false))
        .append(
            "<div class='spacepara'>This is a " +
            AuthorityBaseServlet.SATURN_LINK +
            " &quot;bank&quot; server.</div>" +
            "<div class='para'>Started: ")
        .append(TimeUtils.displayUtcTime(BankService.started))
        .append("</div>" +
            "<div class='para'>Successful transactions: ")
        .append(BankService.successfulTransactions)
        .append("</div>" +
            "<div class='para'>Rejected transactions: ")
        .append(BankService.rejectedTransactions)
        .append("</div>" +
            "<div class='para'>Authority object: " +
            "<a href='")
        .append(authorityUrl)
        .append("'>")
        .append(authorityUrl)
        .append("</a>")
        .append("</div><div class='spacepara'>Registered Merchants");
        if (BankService.PayeeAccountDb.isEmpty()) {
            html.append(": <i>None</i></div>");
        } else {
            html.append("</div>" +
                "<table class='tftable'><tr><th>ID</th><th>Common Name</th><th>Authority Object</th></tr>");
            for (PayeeCoreProperties payeeCoreProperties : BankService.PayeeAccountDb.values()) {
                String payeeAuthorityUrl = payeeCoreProperties.getPayeeAuthorityUrl();
                html.append("<tr><td style='text-align:right'>")
                    .append(payeeCoreProperties.getPayeeId())
                    .append("</td><td>")
                    .append(payeeCoreProperties.getCommonName())
                    .append("</td><td><a href='")
                    .append(payeeAuthorityUrl)
                    .append("'>")
                    .append(payeeAuthorityUrl)
                    .append("</a></td></tr>");
            }
            html.append("</table>");
        }
        html.append("</body></html>");
        HttpSupport.writeHtml(response, html);
    }
}
