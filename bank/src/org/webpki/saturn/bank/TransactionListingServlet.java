/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.HttpSupport;

/////////////////////////////////////////////////////////////////////////////////
// This is just a debugger/demo servlet showing the last 50 transactions       //
/////////////////////////////////////////////////////////////////////////////////

public class TransactionListingServlet extends HttpServlet {
  
    private static final long serialVersionUID = 1L;

    static final String SQL = "SELECT " +
            "TRANSACTIONS.Created AS `Created`, " +
            "TRANSACTIONS.Amount AS `Amount`, " +
            "ACCOUNTS.Id AS `Account`, " +
            "TRANSACTIONS.Balance AS `Balance`, " +
            "USERS.Name As `Account Holder`, " +
            "COALESCE(TRANSACTIONS.Originator,'') AS Originator, " +
            "COALESCE(TRANSACTIONS.ExtReference,'') AS ExtRef, " +
            "TRANSACTIONS.CredentialId AS Credential, " +
            "TRANSACTIONS.Id AS `Id`, " +
            "COALESCE(TRANSACTIONS.ReserveId,'') AS `ReserveId`, " +
            "TRANSACTION_TYPES.SymbolicName AS `Type` " +
            "FROM TRANSACTIONS " +
            "INNER JOIN ACCOUNTS ON " +
            "TRANSACTIONS.AccountId = ACCOUNTS.Id " +
            "INNER JOIN TRANSACTION_TYPES ON " +
            "TRANSACTIONS.TransactionType = TRANSACTION_TYPES.Id " +
            "INNER JOIN USERS ON " +
            "ACCOUNTS.UserId = USERS.Id " +
            "GROUP BY TRANSACTIONS.Id";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Connection connection = null; 
        try {
            connection = BankService.jdbcDataSource.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(SQL);) {
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int numberOfColumns = rsmd.getColumnCount();
                StringBuilder html = new StringBuilder(AuthorityBaseServlet.TOP_ELEMENT +
                        "<link rel=\"icon\" href=\"saturn.png\" sizes=\"192x192\">"+
                        "<title>Transaction List</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body><div class=\"header\" style=\"margin-left:auto;margin-right:auto\">" +
                        "Transaction List</div>" +
                        "<div style=\"padding-bottom:10pt\">This " +
                        AuthorityBaseServlet.SATURN_LINK +
                        " demo/debug service shows the last 50 transactions in the payers' bank. " +
                        "To make it possible start over a demo without enrolling again, " +
                        "<i>a user account is restored (and associated transactions deleted) after 30 " +
                        "minutes of inactivity</i>.</div><table class=\"tftable\"><tr>");
                for (int q = 1; q <= numberOfColumns; q++) {
                    html.append("<th>").append(rsmd.getColumnLabel(q)).append("</th>");
                }
                html.append("</tr>");
                int i = 0;
                while (rs.next() && i++ < 50) {
                    html.append("<tr>");
                    for (int q = 1; q <= numberOfColumns; q++) {
                         html.append(q == 2 || q == 4 ? "<td style=\"text-align:right\">" : "<td>")
                             .append(rs.getString(q))
                             .append("</td>");
                    }
                    html.append("</tr>");
                }
                HttpSupport.writeHtml(response, html.append("</table></body></html>"));
            }
            connection.close();
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e1) {
                }
            }
            throw new IOException(e);
        }
    }
}
