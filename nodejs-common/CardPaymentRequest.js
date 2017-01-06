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

// Saturn "CardPaymentRequest" object

const JsonUtil = require('webpki.org').JsonUtil;
const ByteArray = require('webpki.org').ByteArray;

const BaseProperties        = require('./BaseProperties');
const Messages              = require('./Messages');
const Software              = require('./Software');
const AuthorizationResponse = require('./AuthorizationResponse');
const AuthorizationRequest  = require('./AuthorizationRequest');
const ProtectedAccountData  = require('./ProtectedAccountData');

function CardPaymentRequest(rd) {
  this.root = Messages.parseBaseMessage(Messages.CARD_PAYMENT_REQUEST, rd);
  this.authorizationResponse = new AuthorizationResponse(rd.getObject(BaseProperties.EMBEDDED_JSON));
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature().getPublicKey();
  AuthorizationRequest.comparePublicKeys(this.publicKey,
                                         this.authorizationResponse.authorizationRequest.paymentRequest);
  if (!this.authorizationResponse.authorizationRequest.payerAccountType.isCardPayment()) {
    throw new TypeError('Payment method is not card: ' + 
        this.authorizationResponse.authorizationRequest.payerAccountType.getTypeUri());
  }
  rd.checkForUnread();
}

CardPaymentRequest.prototype.getTimeStamp = function() {
  return this.timeStamp;
};

CardPaymentRequest.prototype.getReferenceId = function() {
  return this.referenceId;
};

CardPaymentRequest.prototype.getPayee = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest.payee;
};

CardPaymentRequest.prototype.getPublicKey = function() {
  return this.publicKey;
};

CardPaymentRequest.prototype.getAuthorizationResponse = function() {
  return this.authorizationResponse;
};

CardPaymentRequest.prototype.getPaymentRequest = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest;
};

CardPaymentRequest.prototype.verifyPayerProvider = function(paymentRoot) {
  this.authorizationResponse.signatureDecoder.verifyTrust(paymentRoot);
};

CardPaymentRequest.prototype.getProtectedAccountData = function(decryptionKeys) {
  return new ProtectedAccountData(new JsonUtil.ObjectReader(JSON.parse(ByteArray.utf8ToString(
                                    this.authorizationResponse
                                      .encryptedAccountData
                                        .getDecryptedData(decryptionKeys)))),
                                  this.authorizationResponse.authorizationRequest.payerAccountType);
};

/*
    public static JSONObjectWriter encode(AuthorizationResponse authorizationResponse,
                                          String referenceId,
                                          ServerAsymKeySigner signer) throws IOException {
        return Messages.createBaseMessage(Messages.CARD_PAYMENT_REQUEST)
            .setObject(EMBEDDED_JSON, authorizationResponse.root)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(AuthorizationRequest.SOFTWARE_NAME, 
                                                      AuthorizationRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }

    public ProtectedAccountData getProtectedAccountData(Vector<DecryptionKeyHolder> decryptionKeys, boolean cardAccount)
    throws IOException, GeneralSecurityException {
        return new ProtectedAccountData(JSONParser.parse(authorizationResponse
                                                             .encryptedAccountData
                                                                 .getDecryptedData(decryptionKeys)), cardAccount);
    }

    public void verifyUserBank(JSONX509Verifier paymentRoot) throws IOException {
        authorizationResponse.signatureDecoder.verify(paymentRoot);
    }
*/

module.exports = CardPaymentRequest;
