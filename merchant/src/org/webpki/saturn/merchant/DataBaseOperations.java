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

    static final String RECEIPT_INSERT_SQL = 
            "INSERT INTO RECEIPTS(SequenceId, Receipt) VALUES(?, ?)";

    static void storeReceipt(ReceiptEncoder receiptEncoder) 
    throws SQLException, IOException {
        String receiptUrl = receiptEncoder.getReceiptUrl();
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_INSERT_SQL);) {
            stmt.setString(1, receiptUrl.substring(receiptUrl.lastIndexOf("/") + 1));
            stmt.setString(2, receiptEncoder.getReceiptDocument()
                                  .serializeToString(JSONOutputFormats.NORMALIZED));
            stmt.executeUpdate();
        }
    }

    static final String RECEIPT_FETCH_SQL = "SELECT Receipt FROM RECEIPTS WHERE SequenceId=?";

    static String fetchReceipt(String sequenceId) 
    throws SQLException, IOException {
        try (Connection connection = MerchantService.jdbcDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(RECEIPT_FETCH_SQL);) {
            stmt.setString(1, sequenceId);
            try (ResultSet rs = stmt.executeQuery();) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
