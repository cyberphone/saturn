/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.common;

import java.io.IOException;

import java.security.GeneralSecurityException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

// For transferring status for which is related to the payer's account

public class ErrorReturn implements BaseProperties {

    public enum ERRORS {
        INSUFFICIENT_FUNDS  (0, "Insufficient Funds"),
        EXPIRED_CREDENTIAL  (1, "Expired Credential"),
        EXPIRED_RESERVATION (2, "Expired Fund Reservation"),
        BLOCKED_ACCOUNT     (3, "Account is blocked"),
        NOT_AUTHORIZED      (4, "Not Authorized"),
        OTHER_ERROR         (5, "Other Error");
        
        int errorCode;
        String clearText;

        ERRORS(int errorCode, String clearText) {
            this.errorCode = errorCode;
            this.clearText = clearText;
        }
    }

    private ERRORS error;
    public ERRORS getError() {
        return error;
    }

    public String getClearText() {
        return error.clearText;
    }

    public int getErrorCode() {
        return error.errorCode;
    }

    private String optionalDescription;
    public String getOptionalDescription() {
        return optionalDescription;
    }

    JSONObjectWriter write(JSONObjectWriter wr) throws IOException, GeneralSecurityException {
        wr.setInt(ERROR_CODE_JSON, error.errorCode);
        return optionalDescription == null ? wr : wr.setString(DESCRIPTION_JSON, optionalDescription);
    }

    ErrorReturn(JSONObjectReader rd) throws IOException {
        this.optionalDescription = rd.getStringConditional(DESCRIPTION_JSON);
        int errorCode = rd.getInt(ERROR_CODE_JSON);
        for (ERRORS error : ERRORS.values()) {
            if (error.errorCode == errorCode) {
                this.error = error;
                rd.checkForUnread();
                return;
            }
        }
        throw new IOException("Unknown \"" + ERROR_CODE_JSON + "\": " + errorCode);
    }

    public ErrorReturn(ERRORS error, String optionalDescription) {
        this.error = error;
        this.optionalDescription = optionalDescription;
    }

    public ErrorReturn(ERRORS error) {
        this(error, null);
    }
}
