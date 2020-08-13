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

// Configuration parameters fo the "Acquirer" server

const Url = require('url');

let config = {};

config.logging = false;

config.host = 'https://localhost:8888';

config.tlsKeys = {
  keyFile:  __dirname + '/tlskeys/' + Url.parse(config.host).hostname + '.key.pem',
  certFile: __dirname + '/tlskeys/' + Url.parse(config.host).hostname + '.cert.pem'
};

config.ownKeys = {
  certAndKey      :  __dirname + '/ownkeys/acquirer.cert-and-key.pem',
  rsaEncryptionKey: __dirname + '/ownkeys/acquirer.rsa-enc-key.pem',
  ecEncryptionKey : __dirname + '/ownkeys/acquirer.ec-enc-key.pem'
};

config.trustAnchors = __dirname + '/trustanchors/paymentnetworks.pem';

config.payeeDb = __dirname + '/../../resources/credentials/merchants.webpay-acquirer.json';

module.exports = config;
