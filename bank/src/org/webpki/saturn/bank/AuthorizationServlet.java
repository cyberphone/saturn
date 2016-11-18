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
package org.webpki.saturn.bank;

import java.io.IOException;

import java.security.GeneralSecurityException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.ChallengeField;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ProtectedAccountData;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.UserAccountEntry;
import org.webpki.saturn.common.ProviderUserResponse;

import org.webpki.util.ISODateTime;

/////////////////////////////////////////////////////////////////////////////////
// This is the Saturn basic mode Payment Provider (Bank) authorization servlet //
/////////////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;

    @Override
    JSONObjectWriter processCall(JSONObjectReader providerRequest, UrlHolder urlHolder)
    throws IOException, GeneralSecurityException {

        // Decode authorization request message
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(providerRequest);
        PaymentRequest paymentRequest = authorizationRequest.getPaymentRequest();
        boolean cardPayment = authorizationRequest.getPayerAccountType().isCardPayment();
        
        // Verify that the authorization request is signed by a payment partner
        urlHolder.setUrl(authorizationRequest.getAuthorityUrl());
        PayeeAuthority PayeeAuthority = getPayeeAuthority(urlHolder);
        urlHolder.setUrl(null);
        AuthorizationRequest.comparePublicKeys(PayeeAuthority.getPayeePublicKey(), paymentRequest);
        PayeeAuthority.getSignatureDecoder().verify(cardPayment ? BankService.acquirerRoot : BankService.paymentRoot);

        // Decrypt the encrypted user authorization
        AuthorizationData authorizationData = authorizationRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

        // Merchant provides the client's IP address which can be used for RBA
        String clientIpAddress = authorizationRequest.getClientIpAddress();

        // Verify that the there is a matching user account
        String accountId = authorizationData.getAccountDescriptor().getAccountId();
        String accountType = authorizationData.getAccountDescriptor().getAccountType();
        UserAccountEntry account = BankService.userAccountDb.get(accountId);
        if (account == null) {
            logger.severe("No such account ID: " + accountId);
            throw new IOException("No such user account ID");
        }
        if (!account.getType().equals(accountType)) {
            logger.severe("Wrong account type: " + accountType + " for account ID: " + accountId);
            throw new IOException("Wrong user account type");
        }
        if (!account.getPublicKey().equals(authorizationData.getPublicKey())) {
            logger.severe("Wrong public key for account ID: " + accountId);
            throw new IOException("Wrong user public key");
        }
        logger.info("Authorized AccountID=" + accountId + ", AccountType=" + accountType);

        ////////////////////////////////////////////////////////////////////////////
        // We got an authentic request.  Now we need to check available funds etc.//
        // Since we don't have a real bank this part is rather simplistic :-)     //
        ////////////////////////////////////////////////////////////////////////////

        // Sorry but you don't appear to have a million bucks :-)
        if (paymentRequest.getAmount().compareTo(DEMO_ACCOUNT_LIMIT) >= 0) {
            return ProviderUserResponse.encode(BankService.bankCommonName,
                                               "Your request for " + 
                                               amountInHtml(paymentRequest, paymentRequest.getAmount()) +
                                               " appears to be slightly out of your current capabilities...",
                                               null,
                                               authorizationData.getDataEncryptionKey(),
                                               authorizationData.getDataEncryptionAlgorithm());
        }

        // RBA v0.001...
        if (paymentRequest.getAmount().compareTo(DEMO_RBA_LIMIT) >= 0 &&
            (authorizationData.getOptionalChallengeResults() == null ||
             !authorizationData.getOptionalChallengeResults()[0].getText().equals("garbo"))) {
            return ProviderUserResponse.encode(BankService.bankCommonName,
                                               "Transaction requests exceeding " +
                                               amountInHtml(paymentRequest, DEMO_RBA_LIMIT) +
                                               " requires additional user authentication to " +
                                               "be performed. Please enter your <span style=\"color:blue\">mother's maiden name</span>." +
                                               "<p>Since <i>this is a demo</i>, " +
                                               "answer <span style=\"color:red\">garbo</span>&nbsp; :-)</p>",
                                               new ChallengeField[]{new ChallengeField(RBA_PARM_MOTHER,
                                                                        ChallengeField.TYPE.ALPHANUMERIC,
                                                                    20,
                                                                    null)},
                                               authorizationData.getDataEncryptionKey(),
                                               authorizationData.getDataEncryptionAlgorithm());
        }

        // Separate credit-card and account2account payments
        JSONObjectWriter encryptedCardData = null;
        if (cardPayment) {

            // Lookup of payee's acquirer
            urlHolder.setUrl(PayeeAuthority.getProviderAuthorityUrl());
            ProviderAuthority acquirerAuthority = getProviderAuthority(urlHolder);
            urlHolder.setUrl(null);

            // Pure sample data...
            JSONObjectWriter protectedAccountData =
                ProtectedAccountData.encode(authorizationData.getAccountDescriptor(),
                                            "Luke Skywalker",
                                            ISODateTime.parseDateTime("2019-12-31T00:00:00Z").getTime(),
                                            "943");
            encryptedCardData = new JSONObjectWriter()
                .setEncryptionObject(protectedAccountData.serializeJSONObject(JSONOutputFormats.NORMALIZED),
                                     acquirerAuthority.getDataEncryptionAlgorithm(),
                                     acquirerAuthority.getEncryptionKey(true),
                                     acquirerAuthority.getKeyEncryptionAlgorithm());
        }

        StringBuffer accountReference = new StringBuffer();
        int q = accountId.length() - 4;
        for (char c : accountId.toCharArray()) {
            accountReference.append((--q < 0) ? c : '*');
        }

        // We did it!
        return AuthorizationResponse.encode(authorizationRequest,
                                            accountReference.toString(),
                                            encryptedCardData,
                                            getReferenceId(),
                                            BankService.bankKey);
    }
}
