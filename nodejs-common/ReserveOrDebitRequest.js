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

// ReserveOrDebitRequest object

const JsonUtil = require('webpki.org').JsonUtil;
const EncryptedData = require('webpki.org').EncryptedData;
const ByteArray = require('webpki.org').ByteArray;

const BaseProperties = require('./BaseProperties');
const Messages = require('./Messages');
const AccountDescriptor = require('./AccountDescriptor');
const PaymentRequest = require('./PaymentRequest');
const Software = require('./Software');
 
function ReserveOrDebitRequest(rd) {
  this.directDebit = rd.getString(Messages.QUALIFIER_JSON) == Messages.DIRECT_DEBIT_REQUEST;
  Messages.parseBaseMessage(this.directDebit ?
               Messages.DIRECT_DEBIT_REQUEST : Messages.RESERVE_FUNDS_REQUEST, rd);
  this.accountType = PayerAccountTypes.fromTypeUri(rd.getString(BaseProperties.ACCOUNT_TYPE_JSON));
  this.encryptedAuthorizationData = EncryptedData.parse(rd.getObject(BaseProperties.AUTHORIZATION_DATA_JSON));
  this.clientIpAddress = rd.getString(BaseProperties.CLIENT_IP_ADDRESS_JSON);
  this.paymentRequest = new PaymentRequest(rd.getObject(BaseProperties.PAYMENT_REQUEST_JSON));
  if (!this.directDebit) {
    this.expires = rd.getDateTime(BaseProperties.EXPIRES_JSON);
  }
  this.accounts = [];
  if (!directDebit && rd.hasProperty(BaseProperties.ACQUIRER_AUTHORITY_URL_JSON)) {
    this.acquirerAuthorityUrl = rd.getString(BaseProperties.ACQUIRER_AUTHORITY_URL_JSON);
  } else {
    var ar = rd.getArray(BaseProperties.PAYEE_ACCOUNTS_JSON);
    do {
      this.accounts.push(new AccountDescriptor(ar.getObject()));
    } while (ar.hasMore());
  }
  this.dateTime = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.outerPublicKey = rd.getSignature().getPublicKey();
  rd.checkForUnread();
}

ReserveOrDebitRequest.prototype.getPayeeAccountDescriptors = function() {
  return this.accounts;
};

ReserveOrDebitRequest.prototype.getExpires = function() {
  return this.expires;
};

ReserveOrDebitRequest.prototype.isDirectDebit = function() {
  return this.directDebit;
};

ReserveOrDebitRequest.prototype.getAcquirerAuthorityUrl = function() {
  return this.acquirerAuthorityUrl;
};
 
ReserveOrDebitRequest.prototype.getClientIpAddress = function() {
  return this.clientIpAddress;
};

ReserveOrDebitRequest.prototype.getPaymentRequest = function() {
  return this.paymentRequest;
};

ReserveOrDebitRequest.encode = function(directDebit,
                                        accountType,
                                        encryptedAuthorizationData,
                                        clientIpAddress,
                                        paymentRequest,
                                        acquirerAuthorityUrl,
                                        accounts,
                                        expires,
                                        signer) {
  var wr = Messages.createBaseMessage(directDebit ?
                    Messages.DIRECT_DEBIT_REQUEST : Messages.RESERVE_FUNDS_REQUEST)
    .setString(BaseProperties.ACCOUNT_TYPE_JSON, accountType.getTypeUri())
    .setObject(BaseProperties.AUTHORIZATION_DATA_JSON, encryptedAuthorizationData)
    .setString(BaseProperties.CLIENT_IP_ADDRESS_JSON, clientIpAddress)
    .setObject(BaseProperties.PAYMENT_REQUEST_JSON, paymentRequest.root);
  if (directDebit || acquirerAuthorityUrl == null) {
    var aw = wr.setArray(BaseProperties.PAYEE_ACCOUNTS_JSON);
    for (var q = 0; q < accounts.length; q++) {
      aw.setObject(accounts[q].writeObject());
    }
  } else {
    ReserveOrDebitRequest.zeroTest(BaseProperties.PAYEE_ACCOUNTS_JSON, accounts);
    wr.setString(BaseProperties.ACQUIRER_AUTHORITY_URL_JSON, acquirerAuthorityUrl);
  }
  if (directDebit) {
    ReserveOrDebitRequest.zeroTest(BaseProperties.EXPIRES_JSON, expires);
    ReserveOrDebitRequest.zeroTest(BaseProperties.ACQUIRER_AUTHORITY_URL_JSON, acquirerAuthorityUrl);
  } else {
    wr.setDateTime(BaseProperties.EXPIRES_JSON, expires);
  }
  wr.setDateTime(BaseProperties.TIME_STAMP_JSON, new Date())
    .setObject(BaseProperties.SOFTWARE_JSON, Software.encode(PaymentRequest.SOFTWARE_NAME,
                                                             PaymentRequest.SOFTWARE_VERSION))
    .setSignature(signer);
  return wr;
};

ReserveOrDebitRequest.zeroTest = function(name, object) {
  if (object != null) {
    throw new TypeError('Argument error, parameter "' + name + '" must be "null"');
  }
};

ReserveOrDebitRequest.comparePublicKeys = function(publicKey, paymentRequest) {
  if (!publicKey.equals(paymentRequest.getPublicKey())) {
    throw new TypeError('Outer and inner public key differ');
  }
};

ReserveOrDebitRequest.prototype.getDecryptedAuthorizationData = function(decryptionKeys) {
  var authorizationData =
    new AuthorizationData(this.encryptedAuthorizationData.getDecryptedData(decryptionKeys));
  comparePublicKeys (this.outerPublicKey, this.paymentRequest);
  if (!ByteArray.equals(authorizationData.getRequestHash(), this.paymentRequest.getRequestHash())) {
    throw new TypeError('Non-matching "' + BaseProperties.REQUEST_HASH_JSON + '" value');
  }
  return authorizationData;
};


module.exports = ReserveOrDebitRequest;
