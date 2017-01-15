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

// Saturn "AuthorizationRequest" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties    = require('./BaseProperties');
const Messages          = require('./Messages');
const Software          = require('./Software');
const PaymentRequest    = require('./PaymentRequest');
const PayerAccountTypes = require('./PayerAccountTypes');
const AccountDescriptor = require('./AccountDescriptor');
    
const SOFTWARE_NAME    = "WebPKI.org - Payee";
const SOFTWARE_VERSION = "1.00";

function AuthorizationRequest(rd) {
  this.root = Messages.parseBaseMessage(Messages.AUTHORIZATION_REQUEST, rd);
  this.testMode = rd.getBooleanConditional(BaseProperties.TEST_MODE_JSON);
  this.authorityUrl = rd.getString(BaseProperties.AUTHORITY_URL_JSON);
  this.payerAccountType = PayerAccountTypes.fromTypeUri(rd.getString(BaseProperties.ACCOUNT_TYPE_JSON));
  this.paymentRequest = new PaymentRequest(rd.getObject(BaseProperties.PAYMENT_REQUEST_JSON));
  this.encryptedAuthorizationData = rd.getObject(BaseProperties.ENCRYPTED_AUTHORIZATION_JSON).getEncryptionObject();
  if (rd.hasProperty(BaseProperties.PAYEE_ACCOUNT_JSON)) {
    this.payeeAccount = new AccountDescriptor(rd.getObject(BaseProperties.PAYEE_ACCOUNT_JSON));
  }
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.clientIpAddress = rd.getString(BaseProperties.CLIENT_IP_ADDRESS_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature().getPublicKey();
  AuthorizationRequest.comparePublicKeys(this.publicKey, this.paymentRequest);
  rd.checkForUnread();
}

AuthorizationRequest.comparePublicKeys = function(publicKey, paymentRequest) {
  if (!publicKey.equals(paymentRequest.getPublicKey())) {
    throw new TypeError('Outer and inner public key differ');
  }
};

/*
    public boolean getTestMode() {
        return testMode;
    }

    public PayerAccountTypes getPayerAccountType() {
        return payerAccountType;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public AccountDescriptor getAccountDescriptor() {
        return payeeAccount;
    }

    public GregorianCalendar getExpires() {
        return expires;
    }

    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    public String getAuthorityUrl() {
        return authorityUrl;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public static JSONObjectWriter encode(Boolean testMode,
                                          String authorityUrl,
                                          PayerAccountTypes payerAccountType,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          AccountDescriptor payeeAccount,
                                          String referenceId,
                                          Date expires,
                                          ServerAsymKeySigner signer) throws IOException {
        JSONObjectWriter wr = Messages.createBaseMessage(Messages.AUTHORIZATION_REQUEST);
        if (testMode != null) {
            wr.setBoolean(TEST_MODE_JSON, testMode);
        }
        wr.setString(AUTHORITY_URL_JSON, authorityUrl)
          .setString(ACCOUNT_TYPE_JSON, payerAccountType.getTypeUri())
          .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
          .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedAuthorizationData);
        if (payeeAccount != null) {
            wr.setObject(PAYEE_ACCOUNT_JSON, payeeAccount.writeObject());
        }
        wr.setString(REFERENCE_ID_JSON, referenceId)
          .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress);
        if (expires != null) {
            wr.setDateTime(EXPIRES_JSON, expires, true);
        }
        return wr
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }

    public static void comparePublicKeys(PublicKey publicKey, PaymentRequest paymentRequest) throws IOException {
        if (!publicKey.equals(paymentRequest.getPublicKey())) {
            throw new IOException("Outer and inner public keys differ");
        }
    }

    public AuthorizationData getDecryptedAuthorizationData(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        AuthorizationData authorizationData =
            new AuthorizationData(JSONParser.parse(encryptedAuthorizationData.getDecryptedData(decryptionKeys)));
        if (!ArrayUtil.compare(authorizationData.getRequestHash(), paymentRequest.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
        }
        return authorizationData;
    }

*/

module.exports = AuthorizationRequest;
