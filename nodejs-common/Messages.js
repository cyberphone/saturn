/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'),
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

    TRANSACTION_REQUEST    :  'TransactionRequest',            // Payee provider to Acquirer
    TRANSACTION_RESPONSE   :  'TransactionResponse',           // Response to the former

    AUTHORIZATION_REQUEST  :  'AuthorizationRequest',          // Payee provider to Payer provider
    AUTHORIZATION_RESPONSE :  'AuthorizationResponse',         // Response to the former

    PROVIDER_AUTHORITY     :  'ProviderAuthority',             // Published provider entity data
    
    PAYEE_AUTHORITY        :  'PayeeAuthority',    

    CONTEXT_JSON           : '@context',
    QUALIFIER_JSON         : '@qualifier',

    createBaseMessage : function(message) {
      return new JsonUtil.ObjectWriter()
        .setString(Messages.CONTEXT_JSON, BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)
        .setString(Messages.QUALIFIER_JSON, message);
    },
    
    parseBaseMessage : function(expectedMessage, jsonReader) {
      if (jsonReader.getString(Messages.CONTEXT_JSON) != BaseProperties.SATURN_WEB_PAY_CONTEXT_URI) {
        throw new TypeError('Unknown context: ' + jsonReader.getString(Messages.CONTEXT_JSON));
      }
      if (jsonReader.getString(Messages.QUALIFIER_JSON) != expectedMessage) {
        throw new TypeError('Unexpected qualifier: ' + jsonReader.getString(Messages.QUALIFIER_JSON));
      }
      return jsonReader;
    },
    
    getLowerCamelCase : function(message) {
      return message.substring(0, 1).toLowerCase() + message.substring(1);
    },
    
    getEmbeddedMessage : function(message, jsonReader) {
      return jsonReader.getObject(Messages.getLowerCamelCase(message));
    }

};

module.exports = Messages;

