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
  this.recepientUrl = rd.getString(BaseProperties.RECEPIENT_URL_JSON);
  this.authorityUrl = rd.getString(BaseProperties.AUTHORITY_URL_JSON);
  this.payerAccountType = PayerAccountTypes.fromTypeUri(rd.getString(BaseProperties.ACCOUNT_TYPE_JSON));
  this.paymentRequest = new PaymentRequest(rd.getObject(BaseProperties.PAYMENT_REQUEST_JSON));
  this.encryptedAuthorizationData = rd.getObject(BaseProperties.ENCRYPTED_AUTHORIZATION_JSON).getEncryptionObject();
  this.paymentMethodSpecific = rd.getObject(BaseProperties.PAYMENT_METHOD_SPECIFIC_JSON);
  rd.scanItem(BaseProperties.PAYMENT_METHOD_SPECIFIC_JSON);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.clientIpAddress = rd.getString(BaseProperties.CLIENT_IP_ADDRESS_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature().getPublicKey();
  rd.checkForUnread();
}

module.exports = AuthorizationRequest;
