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
package org.webpki.saturn.merchant.admin;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.HttpSupport;

import org.webpki.saturn.merchant.MerchantService;

/////////////////////////////////////////////////////////////////////////////////
// This is a debugger/demo servlet showing the last 50 transactions            //
/////////////////////////////////////////////////////////////////////////////////

public class TransactionListingServlet extends HttpServlet {
  
    private static final long serialVersionUID = 1L;
    
    static final String MAX_ROWS = "50";

    static final String SQL = "SELECT Id, Status, ReceiptPathData FROM ORDERS "  +
                              "ORDER BY Id DESC LIMIT " + MAX_ROWS;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            try (Connection connection = MerchantService.jdbcDataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(SQL);
                 ResultSet rs = stmt.executeQuery();) {
                StringBuilder html = new StringBuilder(AuthorityBaseServlet.TOP_ELEMENT +
                        "<link rel='icon' href='../saturn.png' sizes='192x192'>"+
                        "<title>Transaction List</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body><div class='header' style='margin-left:auto;margin-right:auto'>" +
                        "Transaction List</div>" +
                        "<div style='padding-bottom:10pt'>This " +
                        AuthorityBaseServlet.SATURN_LINK +
                        " demo/debug service shows the last " + MAX_ROWS +
                        " merchant transactions.</div>" +
                        "<table style='margin-left:auto;margin-right:auto' class='tftable'>" +
                        "<tr><th>Id</th><th>Status</th></tr>");
                while (rs.next()) {
                    String orderId = rs.getString(1);
                    html.append("<tr><td><a href='")
                        .append(MerchantService.receiptBaseUrl)
                        .append(orderId)
                        .append(rs.getString(3))
                        .append("'>")
                        .append(orderId)
                        .append("</a></td><td style='text-align:center'>")
                        .append(rs.getString(2))
                        .append("</td></tr>");
                }
                HttpSupport.writeHtml(response, html.append("</table></body></html>"));
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
