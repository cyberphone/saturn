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
package org.webpki.saturn.bank.admin;

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

import org.webpki.saturn.bank.BankService;

import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.HttpSupport;

/////////////////////////////////////////////////////////////////////////////////
// This is a debugger/demo servlet showing the last 50 transactions            //
/////////////////////////////////////////////////////////////////////////////////

public class TransactionListingServlet extends HttpServlet {
  
    private static final long serialVersionUID = 1L;
    
    static final String MAX_ROWS = "50";

    static final String SQL = "SELECT " +
            "LASTTRANS.Created AS `Created`, " +
            "LASTTRANS.Amount AS `Amount`, " +
            "LASTTRANS.Balance AS `Balance`, " +
            "ACCOUNTS.Id AS `Account`, " +
            "IFNULL((SELECT AccountId FROM CREDENTIALS WHERE " +
               "CREDENTIALS.Id = LASTTRANS.CredentialId),'') AS `Symbolic Account Id`, " +
            "USERS.Name As `Account Holder`, " +
            "LASTTRANS.PayeeAccount AS `Payee Account`, " +
            "COALESCE(LASTTRANS.PayeeName,'') AS `Payee Name`, " +
            "COALESCE(LASTTRANS.PayeeReference,'') AS `Payee Ref`, " +
            "LASTTRANS.TId AS `Trans Id`, " +
            "COALESCE(LASTTRANS.ReservationId,'') AS `Res Id`, " +
            "TRANSACTION_TYPES.Name AS `Type` " +
            "FROM (SELECT MAX(Id) AS TId, " +
                    "Created, " + 
                    "Amount, " +
                    "TransactionTypeId, " +
                    "Balance, " +
                    "PayeeAccount, " +
                    "PayeeName, " +
                    "PayeeReference, " +
                    "InternalAccountId, " +
                    "ReservationId, " +
                    "CredentialId FROM TRANSACTIONS " +
                "GROUP BY Id ORDER BY Id DESC LIMIT " + MAX_ROWS + ") AS LASTTRANS  " +
            "INNER JOIN ACCOUNTS ON " +
            "LASTTRANS.InternalAccountId = ACCOUNTS.Id " +
            "INNER JOIN TRANSACTION_TYPES ON " +
            "LASTTRANS.TransactionTypeId = TRANSACTION_TYPES.Id " +
            "INNER JOIN USERS ON " +
            "ACCOUNTS.UserId = USERS.Id " +
            "ORDER BY LASTTRANS.TId DESC";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            try (Connection connection = BankService.jdbcDataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(SQL);
                 ResultSet rs = stmt.executeQuery();) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int numberOfColumns = rsmd.getColumnCount();
                StringBuilder html = new StringBuilder(AuthorityBaseServlet.TOP_ELEMENT +
                        "<link rel='icon' href='../saturn.png' sizes='192x192'>"+
                        "<title>Transaction List</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body><div class='header' style='margin-left:auto;margin-right:auto'>" +
                        "Transaction List</div>" +
                        "<div style='padding-bottom:10pt'>This " +
                        AuthorityBaseServlet.SATURN_LINK +
                        " demo/debug service shows the last " + MAX_ROWS +
                        " transactions in the payers' bank. " +
                        "To make it possible start over a demo without enrolling again, " +
                        "<i>a user account is restored (and associated transactions deleted) after 30 " +
                        "minutes of inactivity</i>.</div><table class='tftable'><tr>");
                for (int q = 1; q <= numberOfColumns; q++) {
                    html.append("<th>").append(rsmd.getColumnLabel(q)).append("</th>");
                }
                html.append("</tr>");
                while (rs.next()) {
                    html.append("<tr>");
                    for (int q = 1; q <= numberOfColumns; q++) {
                         html.append(q == 2 || q == 3 ? "<td style='text-align:right'>" : "<td>")
                             .append(rs.getString(q))
                             .append("</td>");
                    }
                    html.append("</tr>");
                }
                HttpSupport.writeHtml(response, html.append("</table></body></html>"));
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
