/*
 *  Copyright 2015-2019 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.common;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public interface EnumDataBase {

    public int getIntValue();
    
    public static int init(Connection connection,
                           String functionName,
                           String enumName) throws SQLException {
        try (CallableStatement stmt = 
                connection.prepareCall("{? = call " + functionName + "(?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setString(2, enumName);
            stmt.execute();
            int outputValue = stmt.getInt(1);
            if (outputValue == 0) {
                throw new SQLException("Failed on enum=" + enumName);
            }
            return outputValue;
        }
    }
}
