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

///////////////////////////////////////////////////////////////////////////////
// This is a Node.js version of the "Acquirer" server used in the "Saturn"   //
// proof-of-concept payment authorization system.                            //
///////////////////////////////////////////////////////////////////////////////

// Node.js platform imports
const Https = require('https');
const Url   = require('url');
const Fs    = require('fs');

// WebPKI.org library imports
const Keys      = require('webpki.org').Keys;
const Jsf       = require('webpki.org').Jsf;
const JsonUtil  = require('webpki.org').JsonUtil;
const Logging   = require('webpki.org').Logging;

// Saturn common library imports
const ServerCertificateSigner = require('../nodejs-common/ServerCertificateSigner');
const ServerAsymKeySigner     = require('../nodejs-common/ServerAsymKeySigner');
const BaseProperties          = require('../nodejs-common/BaseProperties');
const PayeeAuthority          = require('../nodejs-common/PayeeAuthority');
const PayeeCoreProperties     = require('../nodejs-common/PayeeCoreProperties');
const ProviderAuthority       = require('../nodejs-common/ProviderAuthority');
const TransactionRequest      = require('../nodejs-common/TransactionRequest');
const TransactionResponse     = require('../nodejs-common/TransactionResponse');

const Config = require('./config/config');

const logger = new Logging.Logger(__filename);
logger.info('Initializing...');

/////////////////////////////////
// Initiate static data
/////////////////////////////////

function readFile(path) {
  return Fs.readFileSync(path);
}

const homePage = readFile(__dirname + '/index.html');

const AO_EXPIRY_TIME = 3600;  // Authority object expiry time in seconds

let referenceId = 194006;
function getReferenceId() {
  return '#' + (referenceId++);
}

let port = Url.parse(Config.host).port;
if (port == null) {
  port = 443;
}
let applicationPath = Url.parse(Config.host).path;
if (applicationPath == '/') {
  applicationPath = '';
}

/////////////////////////////////
// Initiate cryptographic keys
/////////////////////////////////

const options = {
  key: readFile(Config.tlsKeys.keyFile),
  cert: readFile(Config.tlsKeys.certFile)
};

const keyData = readFile(Config.ownKeys.certAndKey);

const serverCertificateSigner =
  new ServerCertificateSigner(Keys.createPrivateKeyFromPem(keyData),
                              Keys.createCertificatesFromPem(keyData));

const serverAsymKeySigner = new ServerAsymKeySigner(Keys.createPrivateKeyFromPem(keyData));

const paymentRoot = Keys.createCertificatesFromPem(readFile(Config.trustAnchors));

const encryptionKeys = [];
encryptionKeys.push(Keys.createPrivateKeyFromPem(readFile(Config.ownKeys.ecEncryptionKey)));
encryptionKeys.push(Keys.createPrivateKeyFromPem(readFile(Config.ownKeys.rsaEncryptionKey)));

/////////////////////////////////
//Initiate merchant database
/////////////////////////////////

const payeeDb = new Map();
let payees = new JsonUtil.ArrayReader(JSON.parse(readFile(Config.payeeDb).toString('utf8')));
do {
  let payeeCoreProperties = new PayeeCoreProperties(payees.getObject());
  payeeCoreProperties[BaseProperties.TIME_STAMP_JSON] = 0;  // To make it expired from the beginning
  payeeDb.set(payeeCoreProperties.urlSafeId, payeeCoreProperties);
  logger.info('Added payee: ' + payeeCoreProperties.urlSafeId);
} while (payees.hasMore());

let providerAuthority;
function updateProviderAuthority() {
  providerAuthority = ProviderAuthority.encode(Config.host + '/authority',
                                               Config.host,
                                               Config.host + '/authorize',
                                               encryptionKeys[0].getPublicKey(),
                                               AO_EXPIRY_TIME,
                                               serverCertificateSigner);
}
updateProviderAuthority();
setInterval(updateProviderAuthority, AO_EXPIRY_TIME * 500);

/////////////////////////////////
// The request processors
/////////////////////////////////

const jsonPostProcessors = {
 
  authorize : function(reader) {

    // Decode the card payment request message
    let cardPaymentRequest = new TransactionRequest(reader);

    // Verify that the request comes from one of "our" merchants
    let payee = cardPaymentRequest.getPayeeAuthorityUrl();
    let q = payee.lastIndexOf('/');
    if (q < 1 || q > payee.length - 2) {
      q = 0;
    }
    let payeeDbEntry = payeeDb.get(payee.substring(++q));
    if (payeeDbEntry === undefined) {
      throw new TypeError('Unknown merchant ID=' + payee + ', Common Name=' + payee.getCommonName());
    }
    if (!cardPaymentRequest.getPublicKey().equals(
         payeeDbEntry[BaseProperties.SIGNATURE_PARAMETERS_JSON][0][Jsf.PUBLIC_KEY_JSON])) {
      throw new TypeError('Public key doesn\'t match merchant ID=' + payee.getId());
    }
    if (!cardPaymentRequest.getAuthorizationPublicKey().equals(
        payeeDbEntry[BaseProperties.SIGNATURE_PARAMETERS_JSON][0][Jsf.PUBLIC_KEY_JSON])) {
      throw new TypeError('Public key doesn\'t match merchant ID=' + payee.getId());
    }

    // Verify the the embedded response was created by a known bank (network)
    cardPaymentRequest.verifyPayerProvider(paymentRoot);

    // This is the account we are processing
    let cardData = cardPaymentRequest.getProtectedCardData(encryptionKeys);
    let currency = cardPaymentRequest.getPaymentRequest().getCurrency();
    let amountString = cardPaymentRequest.getAmount().toFixed(currency.getDecimals());
    let testMode = cardPaymentRequest.getTestMode();
    logger.info((testMode ? 'TEST ONLY: ' : '') + 
                  'Amount=' + amountString + ' ' + currency.getSymbol() + ', Card Number=' + cardData.getAccountId() + 
                  ', Holder=' + cardData.getAccountOwner());
    if (!testMode) {
    
      /////////////////////////////////////////////////////////////
      // Insert call to payment network HERE
      /////////////////////////////////////////////////////////////

    }
    // We did it! :-)
    return TransactionResponse.encode(cardPaymentRequest,
                                      getReferenceId(),
                                      'Card payment network log data...',
                                      serverCertificateSigner);
  }

};

const jsonGetProcessors = {

  authority : function(getArgument) {
    // This call MUST NOT have any argument
    return getArgument ? null : providerAuthority;
  },

  payees : function(getArgument) {
    // This call MUST have a single REST-like argument holding a merchant id
    if (getArgument) {

      // Valid merchant id?
      let payeeInformation = payeeDb.get(getArgument);
      if (payeeInformation === undefined) {
        return null;
      }

      // If the payee authority object has less than half of its life left, renew it
      let now = new Date();
      if (payeeInformation[BaseProperties.TIME_STAMP_JSON] < now.getTime() - (AO_EXPIRY_TIME * 500)) {
        payeeInformation[BaseProperties.TIME_STAMP_JSON] = now.getTime();
        payeeInformation.payeeAuthority = PayeeAuthority.encode(Config.host + '/payees/' + getArgument,
                                                                Config.host + '/authority',
                                                                payeeInformation,
                                                                now,
                                                                AO_EXPIRY_TIME,
                                                                serverAsymKeySigner);
      }
      return payeeInformation.payeeAuthority;
    }
    // Missing merchant id
    return null;
  }
};

/////////////////////////////////
// Core HTTP server code
/////////////////////////////////

function serverError(response, message) {
  if (message === undefined || typeof message != 'string') {
    message = 'Unrecoverable error message';
  }
  message = Buffer.from(message);
  response.writeHead(500, {'Content-Type'  : 'text/plain; charset=UTF-8',
                           'Connection'    : 'close',
                           'Content-Length': message.length});
  response.write(message);
  response.end();
}

function successLog(returnOrReceived, request, jsonReaderOrWriter) {
  if (Config.logging) {
    logger.info(returnOrReceived + ' [' +
                request.socket.remoteAddress + '] [' + request.url + ']:\n' +
                jsonReaderOrWriter.toString());
  }
}

function writeData(response, data, contentType) {
  response.writeHead(200, {'Content-Type'  : contentType,
                           'Server'        : 'Node.js',
                           'Connection'    : 'close',
                           'Pragma'        : 'No-Cache',
                           'Expires'       : 'Thu, 01 Jan 1970 00:00:00 GMT',
                           'Content-Length': data.length});
  response.write(data);
  response.end();
}

function returnJsonData(request, response, jsonWriter) {
  writeData(response, Buffer.from(jsonWriter.getNormalizedData()), BaseProperties.JSON_CONTENT_TYPE);
  successLog('Returned data', request, jsonWriter);
}

function noSuchFileResponse(response, request) {
    let message = 'No such file: ' + request.url;
    response.writeHead(404, {'Connection'    : 'close',
                             'Content-Type'  : 'text/plain',
                             'Content-Length': message.length});
    response.write(message);
    response.end();
 }

function writeHtml(response, htmlData) {
  writeData(response, htmlData, 'text/html; charset=utf-8');
}

Https.createServer(options, (request, response) => {
  let pathname = Url.parse(request.url).pathname;
  if (pathname.startsWith(applicationPath + '/')) {
    pathname = pathname.substring(applicationPath.length + 1);
  }
  if (request.method == 'GET') {
    // Find possible REST-like argument list
    let i = pathname.indexOf('/');
    let getPath = pathname;
    let getArgument = null;
    if (i > 0) {
      getPath = pathname.substring(0, i);
      getArgument = pathname.substring(i + 1);
      if (getArgument.length == 0) {
        // Syntactic error in our implementation
        getPath = pathname;
      }
    }
    if (getPath in jsonGetProcessors) {
      try {
        let jsonWriter = jsonGetProcessors[getPath](getArgument);
        if (jsonWriter) {
          let accept = request.headers['accept'];
          if (!accept || accept == BaseProperties.JSON_CONTENT_TYPE) {
            returnJsonData(request, response, jsonWriter);
          } else {
            writeHtml(response, '<!DOCTYPE html><html><head><title>Acquirer Server (nodejs)</title>' +
                                '<meta http-equiv=Content-Type content="text/html; charset=utf-8">' +
                                '<meta name="viewport" content="width=device-width, initial-scale=1">' +
                                '</head><body><pre>' + jsonWriter.toString() + '</pre></body></html>');
          }
        } else {
          noSuchFileResponse(response, request);
        }
      } catch (e) {
        logger.error(e.stack)
        serverError(response, e.message);
      }
    } else if (pathname == '') {
      writeHtml(response, homePage);
    } else {
      noSuchFileResponse(response, request);
    }
  } else if (request.method != 'POST') {
    serverError(response, '"POST" method expected');
  } else if (pathname in jsonPostProcessors) {
    let chunks = [];
    request.on('data', (chunk) => {
      chunks.push(chunk);
    });
    request.on('end', () => {
      try {
        if (request.headers['content-type'] != BaseProperties.JSON_CONTENT_TYPE) {
          serverError(response, 'Content type must be: ' + BaseProperties.JSON_CONTENT_TYPE);
        } else {
          let jsonReader = JsonUtil.ObjectReader.parse(Buffer.concat(chunks));
          successLog('Received data', request, jsonReader);
          returnJsonData(request, response, jsonPostProcessors[pathname](jsonReader));
        }
      } catch (e) {
        logger.error(e.stack)
        serverError(response, e.message);
      }
    });
  } else {
    noSuchFileResponse(response, request);
  }
}).listen(port, 10);

logger.info('Acquirer server running at ' + Config.host + ', ^C to shutdown');
