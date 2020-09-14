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

import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.ReceiptEncoder;

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

    static final String RECEIPT_UPDATE_SQL = "UPDATE ORDERS SET Receipt=? WHERE ID=?";

    static void updateReceiptInformation(String orderId, ReceiptEncoder receiptEncoder) 
    throws SQLException, IOException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_UPDATE_SQL);) {
            stmt.setString(1, receiptEncoder.getReceiptDocument()
                    .serializeToString(JSONOutputFormats.NORMALIZED));
            stmt.setString(2, orderId);
            stmt.executeUpdate();
        }
    }
    
    static class ReceiptInfo {
        int status;
        String pathData;
    }

    static final String RECEIPT_FETCH_SQL = 
            "SELECT ReceiptStatus, ReceiptPathData FROM ORDERS WHERE Id=?";

    static ReceiptInfo getReceiptStatus(String orderId) 
    throws SQLException, IOException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_FETCH_SQL);) {
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

    static void createReceipt(ResultData resultData) throws IOException {
        try {
            try (Connection connection = MerchantService.jdbcDataSource.getConnection();
                 CallableStatement stmt = connection.prepareCall("{call CreateReceiptSP(?,?,?)}");) {
                stmt.setString(1, resultData.orderId);
                stmt.setBigDecimal(2, resultData.amount);
                stmt.setString(3, resultData.currency.toString());
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
