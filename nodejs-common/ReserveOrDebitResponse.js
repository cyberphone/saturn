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

// ReserveOrDebitResponse object

const JsonUtil = require('webpki.org').JsonUtil;
const EncryptedData = require('webpki.org').EncryptedData;

const BaseProperties = require('./BaseProperties');
const Messages = require('./Messages');
const AccountDescriptor = require('./AccountDescriptor');
const ProtectedAccountData = require('./ProtectedAccountData');
const PaymentRequest = require('./PaymentRequest');
const Software = require('./Software');

function ReserveOrDebitResponse(rd) {
  this.directDebit = rd.getString(Messages.QUALIFIER_JSON) == Messages.DIRECT_DEBIT_RESPONSE;
  this.root = Messages.parseBaseMessage(this.directDebit ?
                          Messages.DIRECT_DEBIT_RESPONSE : Messages.RESERVE_FUNDS_RESPONSE, rd);
  if (rd.hasProperty(BaseProperties.ERROR_CODE_JSON)) {
    this.errorReturn = new ErrorReturn(rd);
    return;
  }
  this.paymentRequest = new PaymentRequest(rd.getObject(BaseProperties.PAYMENT_REQUEST_JSON));
  this.accountType = rd.getString(BaseProperties.ACCOUNT_TYPE_JSON);
  this.accountReference = rd.getString(BaseProperties.ACCOUNT_REFERENCE_JSON);
  if (rd.hasProperty(BaseProperties.PROTECTED_ACCOUNT_DATA_JSON)) {
    this.encryptedData = EncryptedData.parse(rd.getObject(BaseProperties.PROTECTED_ACCOUNT_DATA_JSON));
    if (this.directDebit) {
      throw new TypeError('"' + PROTECTED_ACCOUNT_DATA_JSON + '" not applicable for directDebit');
    }
  } else {
    this.account = new AccountDescriptor(rd.getObject(BaseProperties.PAYEE_ACCOUNT_JSON));
  }
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  if (!this.directDebit) {
    this.expires = rd.getDateTime(BaseProperties.EXPIRES_JSON);
  }
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.signatureDecoder = rd.getSignature();
  rd.checkForUnread();
}

ReserveOrDebitResponse.prototype.success = function() {
  return this.errorReturn == null;
};

ReserveOrDebitResponse.prototype.getErrorReturn = function() {
  return this.errorReturn;
};

ReserveOrDebitResponse.prototype.getPayeeAccountDescriptor = function() {
  return this.account;
};

ReserveOrDebitResponse.prototype.isAccount2Account = function() {
  return this.encryptedData == null;
};

ReserveOrDebitResponse.prototype.getProtectedAccountData = function(decryptionKeys) {
  return new ProtectedAccountData(this.encryptedData.getDecryptedData(decryptionKeys));
};

ReserveOrDebitResponse.prototype.isDirectDebit = function() {
  return this.directDebit;
};

ReserveOrDebitResponse.prototype.getPaymentRequest = function() {
  return this.paymentRequest;
};

ReserveOrDebitResponse.prototype.getAccountType = function() {
  return this.accountType;
};

ReserveOrDebitResponse.prototype.getAccountReference = function() {
  return this.accountReference;
};

ReserveOrDebitResponse.prototype.getReferenceId = function() {
  return this.referenceId;
};

ReserveOrDebitResponse.prototype.getTimeStamp = function() {
  return this.timeStamp;
};

ReserveOrDebitResponse.prototype.getExpires = function() {
  return this.expires;
};

ReserveOrDebitResponse.prototype.getSoftware = function() {
  return this.software;
};

ReserveOrDebitResponse.prototype.getSignatureDecoder = function() {
  return this.signatureDecoder;
};

ReserveOrDebitResponse.SOFTWARE_NAME    = 'WebPKI.org - Bank';
ReserveOrDebitResponse.SOFTWARE_VERSION = '1.00';

/*

  private static JSONObjectWriter header(boolean directDebit) throws IOException {
  return Messages.createBaseMessage(directDebit ?
       Messages.DIRECT_DEBIT_RESPONSE : Messages.RESERVE_FUNDS_RESPONSE);
  }

  public static JSONObjectWriter encode(boolean directDebit,
            ErrorReturn errorReturn) throws IOException, GeneralSecurityException {
  return errorReturn.write(header(directDebit));
  }

  public static JSONObjectWriter encode(ReserveOrDebitRequest request,
            PaymentRequest paymentRequest,
            AccountDescriptor accountDescriptor,
            JSONObjectWriter encryptedAccountData,
            AccountDescriptor payeeAccount, 
            String referenceId,
            JSONX509Signer signer) throws IOException {
  StringBuffer accountReference = new StringBuffer();
  int q = accountDescriptor.getAccountId().length() - 4;
  for (char c : accountDescriptor.getAccountId().toCharArray()) {
    accountReference.append((--q < 0) ? c : '*');
  }
  boolean directDebit = request.isDirectDebit();
  JSONObjectWriter wr = header(directDebit)
    .setObject(BaseProperties.PAYMENT_REQUEST_JSON, paymentRequest.root)
    .setString(BaseProperties.ACCOUNT_TYPE_JSON, accountDescriptor.getAccountType())
    .setString(BaseProperties.ACCOUNT_REFERENCE_JSON, accountReference.toString());
  if (encryptedAccountData == null) {
    wr.setObject(BaseProperties.PAYEE_ACCOUNT_JSON, payeeAccount.writeObject());
  } else {
    if (directDebit) {
    throw new IOException('\""+ PROTECTED_ACCOUNT_DATA_JSON + "\' not applicable for directDebit');
    }
    ReserveOrDebitRequest.zeroTest(BaseProperties.PAYEE_ACCOUNT_JSON, payeeAccount);
    wr.setObject(BaseProperties.PROTECTED_ACCOUNT_DATA_JSON, encryptedAccountData);
  }
  wr.setString(BaseProperties.REFERENCE_ID_JSON, referenceId);
  if (!directDebit) {
    wr.setDateTime(BaseProperties.EXPIRES_JSON, request.expires.getTime());
  }
  return wr.setDateTime(BaseProperties.TIME_STAMP_JSON, new Date())
     .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
     .setSignature (signer);
  }

*/
  
module.exports = ReserveOrDebitResponse;
