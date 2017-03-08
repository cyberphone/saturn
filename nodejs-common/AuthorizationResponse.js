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

'use strict';

// Saturn "AuthorizationResponse" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties       = require('./BaseProperties');
const Messages             = require('./Messages');
const Software             = require('./Software');
const AuthorizationRequest = require('./AuthorizationRequest');
    
const SOFTWARE_NAME    = "WebPKI.org - Bank";
const SOFTWARE_VERSION = "1.00";

function AuthorizationResponse(rd) {
  this.root = Messages.parseBaseMessage(Messages.AUTHORIZATION_RESPONSE, rd);
  this.authorizationRequest = new AuthorizationRequest(rd.getObject(BaseProperties.EMBEDDED_JSON));
  this.accountReference = rd.getString(BaseProperties.ACCOUNT_REFERENCE_JSON);
  this.encryptedAccountData = rd.getObject(BaseProperties.ENCRYPTED_ACCOUNT_DATA_JSON).getEncryptionObject().require(true);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.optionalLogData = rd.getStringConditional(BaseProperties.LOG_DATA_JSON);
  this.dateTime = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.signatureDecoder = rd.getSignature();
  this.signatureDecoder.getCertificatePath();
  rd.checkForUnread();
}

AuthorizationResponse.prototype.getReferenceId = function() {
  return this.referenceId;
}

AuthorizationResponse.prototype.getAccountReference = function() {
  return this.accountReference;
}

AuthorizationResponse.prototype.getSignatureDecoder = function() {
  return this.signatureDecoder;
}

AuthorizationResponse.prototype.getAuthorizationRequest = function() {
  return this.authorizationRequest;
}
/*
    public static JSONObjectWriter encode(AuthorizationRequest authorizationRequest,
                                          String accountReference,
                                          ProviderAuthority providerAuthority,
                                          AccountDescriptor accountDescriptor,
                                          CardSpecificData cardSpecificData,
                                          String referenceId,
                                          ServerX509Signer signer) throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.AUTHORIZATION_RESPONSE)
            .setObject(EMBEDDED_JSON, authorizationRequest.root)
            .setString(ACCOUNT_REFERENCE_JSON, accountReference)
            .setObject(ENCRYPTED_ACCOUNT_DATA_JSON, 
                       JSONObjectWriter
                           .createEncryptionObject(ProtectedAccountData.encode(accountDescriptor, cardSpecificData)
                                                       .serializeToBytes(JSONOutputFormats.NORMALIZED),
                                                   providerAuthority.getDataEncryptionAlgorithm(),
                                                   providerAuthority.getEncryptionKey(),
                                                   providerAuthority.getKeyEncryptionAlgorithm()))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }
*/

module.exports = AuthorizationResponse;
