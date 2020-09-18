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
package org.webpki.saturn.merchant;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.GregorianCalendar;
import java.util.logging.Logger;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.AuthorizationResponseDecoder;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.ProviderResponseDecoder;
import org.webpki.saturn.common.ReceiptDecoder;
import org.webpki.saturn.common.ReceiptEncoder;
import org.webpki.saturn.common.TransactionResponseDecoder;

public class DataBaseOperations {

    static Logger logger = Logger.getLogger(DataBaseOperations.class.getCanonicalName());
    
    static void testConnection() throws SQLException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();) { }
    }
    
    static synchronized String createOrderId(String random) throws IOException {
        try {
            try (Connection connection = MerchantService.jdbcDataSource.getConnection();
                 CallableStatement stmt = connection.prepareCall("{call CreateOrderIdSP(?,?)}");) {
                stmt.registerOutParameter(1, java.sql.Types.CHAR);
                stmt.setString(2, random);
                stmt.execute();
                return stmt.getString(1);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static class OrderInfo {
        ReceiptDecoder.Status status;
        String pathData;
        GregorianCalendar timeStamp;
    }

    static final String ORDER_STATUS_SQL = 
            "SELECT Status, ReceiptPathData, Created FROM ORDERS WHERE Id=?";

    static OrderInfo getOrderStatus(String orderId) 
    throws SQLException, IOException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(ORDER_STATUS_SQL);) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery();) {
                if (rs.next()) {
                    OrderInfo orderInfo = new OrderInfo();
                    orderInfo.status = ReceiptDecoder.Status.valueOf(rs.getString(1));
                    orderInfo.pathData = rs.getString(2);
                    orderInfo.timeStamp = new GregorianCalendar();
                    orderInfo.timeStamp.setTime(rs.getTimestamp(3));
                    return orderInfo;
                }
                return null;
            }
        }
    }

/*
        CREATE PROCEDURE SaveTransactionSP (IN p_Id CHAR(16) CHARACTER SET latin1,
                                            IN p_CommonName VARCHAR(30),
                                            IN p_AuthorityUrl VARCHAR(100),
                                            IN p_Authorization TEXT)
*/

    static void saveTransaction(ResultData resultData) throws IOException {
        try {
            try (Connection connection = MerchantService.jdbcDataSource.getConnection();
                 CallableStatement stmt = connection.prepareCall("{call SaveTransactionSP(?,?,?,?)}");) {
                stmt.setString(1, resultData.authorization.getPayeeReferenceId());
                stmt.setString(2, resultData.providerCommonName);
                stmt.setString(3, resultData.providerAuthorityUrl);
                stmt.setString(4, resultData.authorization.getJsonString());
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static final String RECEIPT_FETCH_CORE_SQL = 
            "SELECT Authorization, CommonName, AuthorityUrl FROM PAYMENTS WHERE Id=?";

    static ReceiptEncoder getReceiptData(String orderId, GregorianCalendar timeStamp) 
    throws IOException, SQLException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_FETCH_CORE_SQL);) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery();) {
                if (!rs.next()) {
                    throw new IOException("Missing core data");
                }
                JSONObjectReader json = JSONParser.parse(rs.getString(1));
                ProviderResponseDecoder prd = 
                        json.getString(JSONDecoderCache.QUALIFIER_JSON)
                            .equals(Messages.AUTHORIZATION_RESPONSE.toString()) ?
                                    new AuthorizationResponseDecoder(json) :
                                    new TransactionResponseDecoder(json);
                ReceiptEncoder receiptEncoder = new ReceiptEncoder(orderId,
                                                                   timeStamp,
                                                                   prd.getPayeeCommonName(),
                                                                   prd.getAmount(),
                                                                   prd.getCurrency(),
                                                                   prd.getPaymentMethodName(),
                                                                   prd.getAccountReference(),
                                                                   prd.getPayeeAuthorityUrl(),
                                                                   rs.getString(2),
                                                                   rs.getString(3),
                                                                   prd.getTimeStamp(),
                                                                   prd.getProviderReferenceId());
                return receiptEncoder;
            }
        }
    }
}
