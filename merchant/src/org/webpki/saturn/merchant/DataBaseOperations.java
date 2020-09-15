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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Logger;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.AuthorizationResponseDecoder;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.ProviderResponseDecoder;
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

    static class ReceiptInfo {
        int status;
        String pathData;
    }

    static final String RECEIPT_STATUS_SQL = 
            "SELECT ReceiptStatus, ReceiptPathData FROM ORDERS WHERE Id=?";

    static ReceiptInfo getReceiptStatus(String orderId) 
    throws SQLException, IOException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_STATUS_SQL);) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery();) {
                if (rs.next()) {
                    ReceiptInfo receiptInfo = new ReceiptInfo();
                    receiptInfo.status = rs.getInt(1);
                    receiptInfo.pathData = rs.getString(2);
                    return receiptInfo;
                }
                return null;
            }
        }
    }

    /*
        CREATE PROCEDURE SaveTransactionSP (IN p_Id CHAR(16) CHARACTER SET latin1,
                                            IN p_ProviderAuthorityUrl VARCHAR(100),
                                            IN p_Authorization TEXT)
    */

    static void saveTransaction(ResultData resultData) throws IOException {
        try {
            try (Connection connection = MerchantService.jdbcDataSource.getConnection();
                 CallableStatement stmt = connection.prepareCall("{call SaveTransactionSP(?,?,?)}");) {
                stmt.setString(1, resultData.authorization.getPayeeReferenceId());
                stmt.setString(2, resultData.providerAuthorityUrl);
                stmt.setString(3, resultData.authorization.getJsonString());
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static final String RECEIPT_FETCH_CORE_SQL = 
            "SELECT ProviderAuthorityUrl," +
                   "Authorization FROM PAYMENTS WHERE Id=?";

    static ReceiptEncoder getReceiptData(String orderId) 
    throws IOException, SQLException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_FETCH_CORE_SQL);) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery();) {
                if (!rs.next()) {
                    throw new IOException("Missing core data");
                }
                JSONObjectReader json = JSONParser.parse(rs.getString(2));
                ProviderResponseDecoder prd = 
                        json.getString(JSONDecoderCache.QUALIFIER_JSON)
                            .equals(Messages.AUTHORIZATION_RESPONSE.toString()) ?
                                    new AuthorizationResponseDecoder(json) :
                                    new TransactionResponseDecoder(json);
                ReceiptEncoder receiptEncoder = new ReceiptEncoder(orderId,
                                                                   prd.getCommonName(),
                                                                   prd.getAmount(),
                                                                   prd.getCurrency(),
                                                                   prd.getPaymentMethodName(),
                                                                   prd.getAccountReference(),
                                                                   rs.getString(1),
                                                                   prd.getPayeeAuthorityUrl(),
                                                                   prd.getTimeStamp(),
                                                                   prd.getProviderReferenceId());
                return receiptEncoder;
            }
        }
    }
}
