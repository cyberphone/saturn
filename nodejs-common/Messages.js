/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'),
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

// Messages used in the Web2Native Bridge PoC

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');

const Messages = {

    WALLET_INITIALIZED     : 'WalletInitialized',       // Wallet to payee web page message
    WALLET_REQUEST         : 'WalletRequest',           // Payee payment request + other data
    PAYER_AUTHORIZATION    : 'PayerAuthorization',      // Created by the Wallet

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // One-step payment operation in Account2Account mode
    DIRECT_DEBIT_REQUEST   : 'DirectDebitRequest',      // Payee request to provider
    DIRECT_DEBIT_RESPONSE  : 'DirectDebitResponse',     // Provider response to the above
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Two-step payment operation in Account2Account or Acquirer mode
    //
    // First step - Payee to Provider
    RESERVE_FUNDS_REQUEST  : 'ReserveFundsRequest',     // Reserve funds at provider
    RESERVE_FUNDS_RESPONSE : 'ReserveFundsResponse',    // Provider response to request
    //
    // Second step - Payee to Provider (Account2Account mode) or Acquirer (Acquirer mode)
    FINALIZE_REQUEST       : 'FinalizeRequest',         // Perform the actual payment operation
    FINALIZE_RESPONSE      : 'FinalizeResponse',        // Provider or Acquirer response to request
    ///////////////////////////////////////////////////////////////////////////////////////////////

    AUTHORITY              : 'Authority',               // Published entity data
    
    CONTEXT_JSON           : '@context',
    QUALIFIER_JSON         : '@qualifier',

    createBaseMessage : function(message) {
      return new JsonUtil.ObjectWriter()
        .setString(Messages.CONTEXT_JSON, BaseProperties.W2NB_WEB_PAY_CONTEXT_URI)
        .setString(Messages.QUALIFIER_JSON, message);
    },
    
    parseBaseMessage : function(expectedMessage, jsonReader) {
      if (jsonReader.getString(Messages.CONTEXT_JSON) != BaseProperties.W2NB_WEB_PAY_CONTEXT_URI) {
        throw new TypeError('Unknown context: ' + jsonReader.getString(Messages.CONTEXT_JSON));
      }
      if (jsonReader.getString(Messages.QUALIFIER_JSON) != expectedMessage) {
        throw new TypeError('Unexpected qualifier: ' + jsonReader.getString(Messages.QUALIFIER_JSON));
      }
      return jsonReader;
    }

};

module.exports = Messages;

