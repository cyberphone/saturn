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

//////////////////////////////////////////////////////////////////////////////////////
// This is a Node.js version of the "Acquirer" server used in the Web2Native Bridge //
// proof-of-concept payment system.                                                 //
//////////////////////////////////////////////////////////////////////////////////////

const Https = require('https');
const Url = require('url');
const Fs = require('fs');

const Keys = require('webpki.org').Keys;
const Jcs = require('webpki.org').Jcs;
const Big = require('webpki.org').Big;
const ByteArray = require('webpki.org').ByteArray;
const JsonUtil = require('webpki.org').JsonUtil;
const Logging = require('webpki.org').Logging;

const ServerCertificateSigner = require('../nodejs-common/ServerCertificateSigner');
const BaseProperties = require('../nodejs-common/BaseProperties');
const PayeeAuthority = require('../nodejs-common/PayeeAuthority');
const ProviderAuthority = require('../nodejs-common/ProviderAuthority');
const ErrorReturn = require('../nodejs-common/ErrorReturn');
const CardPaymentRequest = require('../nodejs-common/CardPaymentRequest');
const CardPaymentResponse = require('../nodejs-common/CardPaymentResponse');

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

const AO_EXPIRY_TIME = 120;  // Authority object expiry time in seconds

var referenceId = 194006;
function getReferenceId() {
  return '#' + (referenceId++);
}

var port = Url.parse(Config.host).port;
if (port == null) {
  port = 443;
}
var applicationPath = Url.parse(Config.host).path;
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

const paymentRoot = Keys.createCertificatesFromPem(readFile(Config.trustAnchors));

const encryptionKeys = [];
encryptionKeys.push(Keys.createPrivateKeyFromPem(readFile(Config.ownKeys.ecEncryptionKey)));
encryptionKeys.push(Keys.createPrivateKeyFromPem(readFile(Config.ownKeys.rsaEncryptionKey)));

const payeeDb = new Map();
JSON.parse(readFile(Config.payeeDb).toString('utf8')).forEach((entry) => {
  entry[BaseProperties.TIME_STAMP_JSON] = 0;  // To make it expired from the beginning
  payeeDb.set(entry[BaseProperties.PAYEE_JSON][BaseProperties.ID_JSON], entry);
});

var providerAuthority;
function updateProviderAuthority() {
  providerAuthority = ProviderAuthority.encode(Config.host + '/authority',
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
    var cardPaymentRequest = new CardPaymentRequest(reader);

    // Verify that the request comes from one of "our" merchants
    var payee = cardPaymentRequest.getPayee();
    var payeeDbEntry = payeeDb.get(payee[BaseProperties.ID_JSON]);
    if (payeeDbEntry === undefined ||
        payeeDbEntry[BaseProperties.PAYEE_JSON][BaseProperties.COMMON_NAME_JSON] !=
          payee[BaseProperties.COMMON_NAME_JSON]) {
      throw new TypeError('Unkown merchant ID=' + payee.getId() + ', Common Name=' + payee.getCommonName());
    }
    
@BYGG OM DB!
    if (!cardPaymentRequest.getPublicKey().equals(Keys.encodePublicKey(payeeDbEntry[Jcs.PUBLIC_KEY_JSON]))) {
      throw new TypeError('Outer and inner public key differ');
    }
    
    // Verify the the embedded response was created by a known bank (network)
    cardPaymentRequest.verifyPayerProvider(paymentRoot);

    cardPaymentRequest.getProtectedAccountData(encryptionKeys);
/*
    // Get the embedded authorization from the payer's payment provider (bank)
    var embeddedResponse = cardPaymentRequest.getEmbeddedResponse();

    // Verify that the provider's signature belongs to a valid payment provider trust network
    embeddedResponse.getSignatureDecoder().verifyTrust(paymentRoot);
    var payeeBank = embeddedResponse.getSignatureDecoder().getCertificatePath()[0].getSubject();

    // Get the the account data we sent encrypted through the merchant
    var protectedAccountData = embeddedResponse.getProtectedAccountData(encryptionKeys);
    if (Config.logging) {
      logger.info('Account:\n' + protectedAccountData.toString());
    }

    // The original request contains some required data like currency
    var paymentRequest = embeddedResponse.getPaymentRequest();
    var payee = paymentRequest.getPayee();

    // Verify that the merchant is one of our customers.
    // Rudimentary customer "database": a single customer!
    // Note that since a payee (merchant) is vouched for by a bank it is the combination
    // of a payee ID and the name of the certifying bank that comprise a valid customer.
    // The payee's public key is only of interest to the certifying bank.
    if (payee.getId() != '86344') {
      throw new TypeError('Unknown merchant: ID=' + payee.getId() + ', Common Name=' + payee.getCommonName());
    }
    if (payeeBank != 'CN=mybank.com,2.5.4.5=#130434353031,C=FR') {
      throw new TypeError('Merchant: ID=' + payee.getId() + ' does not match bank: ' + payeeBank);
    }
*/
    // We got an authentic request.  Now we need to check available funds etc.
    // Since we don't have a real acquirer this part is rather simplistic :-)
    return CardPaymentResponse.encode(cardPaymentRequest,
                                      getReferenceId(),
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
      var payeeInformation = payeeDb.get(getArgument);
      if (payeeInformation !== undefined) {

        // If the payee authority object has less than half of its life left, renew it
        var now = new Date();
        if (payeeInformation[BaseProperties.TIME_STAMP_JSON] < now.getTime() - (AO_EXPIRY_TIME * 500)) {
          payeeInformation[BaseProperties.TIME_STAMP_JSON] = now.getTime();
          payeeInformation.payeeAuthority = PayeeAuthority.encode(Config.host + '/payees/' + getArgument,
                                                                  Config.host + '/authority',
                                                                  payeeInformation,
                                                                  now,
                                                                  AO_EXPIRY_TIME,
                                                                  serverCertificateSigner);
        }
        return payeeInformation.payeeAuthority;
      }
    }

    // Missing or unknown merchant id
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
  message = new Buffer(message);
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

function returnJsonData(request, response, jsonWriter) {
  if (jsonWriter) {
    var output = jsonWriter.getNormalizedData();
    response.writeHead(200, {'Content-Type'  : BaseProperties.JSON_CONTENT_TYPE,
                             'Connection'    : 'close',
                             'Content-Length': output.length});
    response.write(new Buffer(output));
    response.end();
    successLog('Returned data', request, jsonWriter);
  } else {
    noSuchFileResponse(response, request);
  }
}

function noSuchFileResponse(response, request) {
    var message = 'No such file: ' + request.url;
    response.writeHead(404, {'Connection'    : 'close',
                             'Content-Type'  : 'text/plain',
                             'Content-Length': message.length});
    response.write(message);
    response.end();
 }

Https.createServer(options, (request, response) => {
  var pathname = Url.parse(request.url).pathname;
  if (pathname.startsWith(applicationPath + '/')) {
    pathname = pathname.substring(applicationPath.length + 1);
  }
  if (request.method == 'GET') {
    // Find possible REST-like argument list
    var i = pathname.indexOf('/');
    var getPath = pathname;
    var getArgument = null;
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
        returnJsonData(request, response, jsonGetProcessors[getPath](getArgument));
      } catch (e) {
        logger.error(e.stack)
        serverError(response, e.message);
      }
    } else if (pathname == '') {
      response.writeHead(200, {'Content-Type': 'text/html'});
      response.write(homePage);
      response.end();
    } else {
      noSuchFileResponse(response, request);
    }
    return;
  }
  if (request.method != 'POST') {
    serverError(response, '"POST" method expected');
    return;
  }
  if (pathname in jsonPostProcessors) {
    var chunks = [];
    request.on('data', (chunk) => {
      chunks.push(chunk);
    });
    request.on('end', () => {
      try {
        if (request.headers['content-type'] != BaseProperties.JSON_CONTENT_TYPE) {
          serverError(response, 'Content type must be: ' + BaseProperties.JSON_CONTENT_TYPE);
          return;
        }
        var jsonReader = new JsonUtil.ObjectReader(JSON.parse(Buffer.concat(chunks).toString('utf8')));
        successLog('Received data', request, jsonReader);
        returnJsonData(request, response, jsonPostProcessors[pathname](jsonReader));
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
