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

'use strict';

// Saturn "AuthorizationRequest" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties     = require('./BaseProperties');
const Messages           = require('./Messages');
const Software           = require('./Software');
const PaymentRequest     = require('./PaymentRequest');
const AccountDataDecoder = require('./AccountDataDecoder');
    
const EXPECTED_CONTEXT = 'https://supercard.com/saturn/v3#pms';

const EXPECTED_METHOD  = 'https://supercard.com';

function AuthorizationRequest(rd) {
  this.root = Messages.parseBaseMessage(Messages.AUTHORIZATION_REQUEST, rd);
  this.testMode = rd.getBooleanConditional(BaseProperties.TEST_MODE_JSON);
  this.recipientUrl = rd.getString(BaseProperties.RECIPIENT_URL_JSON);
  this.payeeAuthorityUrl = rd.getString(BaseProperties.PAYEE_AUTHORITY_URL_JSON);
  let paymentMethod = rd.getString(BaseProperties.PAYMENT_METHOD_JSON);
  if (paymentMethod != EXPECTED_METHOD) {
    throw new TypeError('Unrecognized payment method: ' + paymentMethod);
  }
  this.paymentRequest = new PaymentRequest(rd.getObject(BaseProperties.PAYMENT_REQUEST_JSON));
  rd.scanItem(BaseProperties.ENCRYPTED_AUTHORIZATION_JSON);  // Already processed by the bank
  let payeeReceiveAccount = 
      new AccountDataDecoder(rd.getObject(BaseProperties.PAYEE_RECEIVE_ACCOUNT_JSON));
  payeeReceiveAccount.checkAccountTypes(AccountDataDecoder.SEPA_ACCOUNT);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.clientIpAddress = rd.getString(BaseProperties.CLIENT_IP_ADDRESS_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature(BaseProperties.REQUEST_SIGNATURE_JSON).getPublicKey();
  rd.checkForUnread();
}

module.exports = AuthorizationRequest;
