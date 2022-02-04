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
import java.sql.SQLException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.saturn.bank.BankService;

import org.webpki.saturn.common.AuthorityBaseServlet;
import org.webpki.saturn.common.HttpSupport;

/////////////////////////////////////////////////////////////////////////////////
// This is a debugger/demo servlet showing the currently enrolled users        //
/////////////////////////////////////////////////////////////////////////////////

public class UserListingServlet extends HttpServlet {
  
    private static final long serialVersionUID = 1L;
    
    static final String MAX_ROWS = "100";

    static final String SQL = "SELECT DISTINCT ClientIpAddress, ClientHost, Name, " +
                              "Id, Created, AccessCount, " + 
                              "COALESCE(LastAccess,'') FROM USERS " +
                              "ORDER BY Created DESC LIMIT " + MAX_ROWS;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            try (Connection connection = BankService.jdbcDataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(SQL);
                 ResultSet rs = stmt.executeQuery();) {
                StringBuilder html = new StringBuilder(AuthorityBaseServlet.TOP_ELEMENT +
                        "<link rel='icon' href='../saturn.png' sizes='192x192'>"+
                        "<title>Registered List</title>" +
                        AuthorityBaseServlet.REST_ELEMENT +
                        "<body><div class='header' style='margin-left:auto;margin-right:auto'>" +
                        "User List</div>" +
                        "<div style='padding-bottom:10pt'>This " +
                        AuthorityBaseServlet.SATURN_LINK +
                        " service shows the last " + MAX_ROWS +
                        " users in the payers' bank." +
                        "</div><table class='tftable'><tr>" +
                        "<th>IP Address</th>" +
                        "<th>Host</th>" +
                        "<th>User Name</th>" +
                        "<th>User ID</th>" +
                        "<th>Created</th>" +
                        "<th>Access Count</th>" +
                        "<th>Last Access</th>" +
                        "</tr>");
               while (rs.next()) {
                    html.append("<tr>");
                    for (int q = 1; q <= 7; q++) {
                        String value = rs.getString(q);
                        if (q == 2 && value == null) {
                            value = "<div style='text-align:center'>-</div>";
                        }
                        html.append("<td>").append(value).append("</td>");
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
