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

import io.interbanking.IBRequest;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.UrlHolder;

/////////////////////////////////////////////////////////////////////////////////
// This is a servlet that serves a hypothetical interbanking payment network.  //
// It is entirely independent of Saturn but there is some minor code reuse.    //
/////////////////////////////////////////////////////////////////////////////////

public class InterbankingServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;


    @Override
    JSONObjectWriter processCall(UrlHolder urlHolder,
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {
        IBRequest ibRequest = new IBRequest(providerRequest);
        JSONObjectWriter ibResponse;
        switch (ibRequest.getOperation()) {

            case CREDIT_CARD_TRANSACT:
                ibRequest.verifyCallerAuthenticity(BankService.acquirerRoot);
                ibResponse = null;
                break;

            default:
                throw new IOException("Not implemented: " + ibRequest.getOperation().toString());
        }
        return ibResponse;
    }
}
