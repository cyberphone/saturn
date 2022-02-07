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
package org.webpki.saturn.merchant;

import java.io.IOException;
import java.io.Serializable;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.security.KeyPair;

import java.util.GregorianCalendar;

import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.ContentEncryptionAlgorithms;

import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.AuthorizationDataDecoder;
import org.webpki.saturn.common.AuthorizationDataEncoder;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.ClientPlatform;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.EncryptedMessage;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.PaymentRequestEncoder;
import org.webpki.saturn.common.ProviderUserResponseEncoder;
import org.webpki.saturn.common.TimeUtils;
import org.webpki.saturn.common.UserAuthorizationMethods;
import org.webpki.saturn.common.UserChallengeItem;
import org.webpki.saturn.common.UserResponseItem;

import org.webpki.util.ArrayUtil;

public class DebugData implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public JSONObjectReader InvokeWallet;
    
    public JSONObjectReader walletResponse;

    public JSONObjectReader providerAuthority;

    public boolean basicCredit;
    
    public boolean softAuthorizationError;
    
    public JSONObjectReader payeeAuthority;
    
    public JSONObjectReader payeeProviderAuthority;

    public JSONObjectReader authorizationRequest;
    
    public JSONObjectReader authorizationResponse;

    public JSONObjectReader transactionRequest;
    
    public JSONObjectReader transactionResponse;

    // Native mode
    public JSONObjectReader reserveOrBasicRequest;
    
    public JSONObjectReader reserveOrBasicResponse;
    
    public boolean acquirerMode;
    
    public boolean hybridMode;
    
    public JSONObjectReader acquirerAuthority;
    
    public JSONObjectReader finalizeRequest;

    public JSONObjectReader finalizeResponse;

    public boolean softFinalizeError;

    public boolean gasStation;

    public JSONObjectReader refundRequest;

    public JSONObjectReader refundResponse;
    
    // Debug mode samples
    static JSONObjectReader userAuthzSample;

    static JSONObjectReader userChallAuthzSample;
    
    static EncryptedMessage encryptedMessageSample;

    static JSONObjectReader providerUserResponseSample;
    
    static KeyPair keyPair;
    
    static UserResponseItem[] userResponseItems;
    
    static final byte[] WALLET_SESSION_ENCRYPTION_KEY = 
        { (byte) 0xF4, (byte) 0xC7, (byte) 0x4F, (byte) 0x33,
          (byte) 0x98, (byte) 0xC4, (byte) 0x9C, (byte) 0xF4,
          (byte) 0x6D, (byte) 0x93, (byte) 0xEC, (byte) 0x98,
          (byte) 0x18, (byte) 0x83, (byte) 0x26, (byte) 0x61,
          (byte) 0xA4, (byte) 0x0B, (byte) 0xAE, (byte) 0x4D,
          (byte) 0x20, (byte) 0x4D, (byte) 0x75, (byte) 0x50,
          (byte) 0x36, (byte) 0x14, (byte) 0x10, (byte) 0x20,
          (byte) 0x74, (byte) 0x34, (byte) 0x69, (byte) 0x09 };
    
    static JSONObjectReader createUserAuthorizationSample() 
            throws IOException, GeneralSecurityException {
        GregorianCalendar then = TimeUtils.inSeconds(-27);
        GregorianCalendar authTime = TimeUtils.inSeconds(23);
        GregorianCalendar expires = TimeUtils.inMinutes(30);
        JSONObjectWriter paymentRequest = 
            PaymentRequestEncoder.encode("Space Shop",
                                         new BigDecimal("100.00"),
                                         Currencies.EUR,
                                         null,
                                         "#100006878", 
                                         then,
                                         expires);
        return new JSONObjectReader(AuthorizationDataEncoder.encode(
                                 new PaymentRequestDecoder(new JSONObjectReader(paymentRequest)),
                                 HashAlgorithms.SHA256,
                                 "https://payments.bigbank.com/payees/86344",
                                 "spaceshop.com", 
                                 PaymentMethods.BANK_DIRECT.getPaymentMethodUrl(),
                                 "54674448", 
                                 "FR7630002111110020050012733", 
                                  WALLET_SESSION_ENCRYPTION_KEY, 
                                 ContentEncryptionAlgorithms.A256GCM, 
                                 userResponseItems,
                                 UserAuthorizationMethods.PIN,
                                 userResponseItems == null ? then : authTime,
                                 "WebPKI Suite/Saturn",
                                 MerchantService.androidWebPkiVersions.substring(0, 
                                                         MerchantService.androidWebPkiVersions.indexOf('-')),
                                 new ClientPlatform("Android", "10", "Huawei"),
                                 new JSONAsymKeySigner(keyPair.getPrivate())
                                     .setPublicKey(keyPair.getPublic())));
    }

    static {
        try {
            keyPair = JSONParser.parse(ArrayUtil.getByteArrayFromInputStream(
                    DebugData.class.getResourceAsStream("sampleauthorizationkey.jwk"))).getKeyPair();
            userAuthzSample = createUserAuthorizationSample();
            
            userResponseItems = new UserResponseItem[]{new UserResponseItem("mother", "smith")};
            
            userChallAuthzSample = createUserAuthorizationSample();
            
            AuthorizationDataDecoder authorizationData = 
                    new AuthorizationDataDecoder(userAuthzSample, new JSONCryptoHelper.Options());
            
            providerUserResponseSample = new JSONObjectReader(ProviderUserResponseEncoder.encode(
                    "My Bank",
                    "Transaction requests exceeding " +
                      "<span style='font-weight:bold;white-space:nowrap'>&#x20ac;&#x2009;1,000</span>" +
                      " require additional user authentication to " +
                      "be performed. Please enter your " +
                      "<span style='color:blue'>mother's maiden name</span>.",
                    new UserChallengeItem[] {
                        new UserChallengeItem("mother",
                                              UserChallengeItem.TYPE.ALPHANUMERIC,
                                              null)},
                    authorizationData.getDataEncryptionKey(),
                    authorizationData.getContentEncryptionAlgorithm()));

            encryptedMessageSample = new EncryptedMessage(JSONParser.parse(
                providerUserResponseSample.getObject(BaseProperties.ENCRYPTED_MESSAGE_JSON)
                    .getEncryptionObject(new JSONCryptoHelper.Options()
                            .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.PLAIN_ENCRYPTION))
                        .getDecryptedData(
                    userAuthzSample.getObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON)
                        .getBinary(BaseProperties.ENCRYPTION_KEY_JSON))));            
        } catch (Exception e) {
            new RuntimeException(e);
        }
    }
}
