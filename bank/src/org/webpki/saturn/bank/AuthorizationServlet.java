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

import java.math.BigDecimal;

import java.text.SimpleDateFormat;

import java.util.Locale;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.UserChallengeItem;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.UserAccountEntry;
import org.webpki.saturn.common.NonDirectPayments;
import org.webpki.saturn.common.Messages;

import org.webpki.util.ArrayUtil;
import org.webpki.util.ISODateTime;

/////////////////////////////////////////////////////////////////////////////////
// This is the Saturn core Payment Provider (Bank) authorization servlet       //
/////////////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;
    
    @Override
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws Exception {

        // Decode authorization request message
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(providerRequest);

        // Check that we actually were the intended party
        if (!BankService.serviceUrl.equals(authorizationRequest.getRecepientUrl())) {
            throw new IOException("Unexpected \"" + RECEPIENT_URL_JSON + "\" : " + authorizationRequest.getRecepientUrl());
        }

        // Verify that we understand the payment method
        AuthorizationRequest.PaymentMethodDecoder paymentMethodSpecific =
            authorizationRequest.getPaymentMethodSpecific(BankService.knownPaymentMethods);

        // Fetch the payment request object
        PaymentRequest paymentRequest = authorizationRequest.getPaymentRequest();
        NonDirectPayments nonDirectPayment = paymentRequest.getNonDirectPayment();
        boolean cardPayment = authorizationRequest.getPaymentMethod().isCardPayment();
        
        // Get the providers. Note that caching could play tricks on you!
        PayeeAuthority payeeAuthority;
        ProviderAuthority providerAuthority;
        boolean nonCached = false;
        while (true) {
            // Lookup of Payee
            urlHolder.setNonCachedMode(nonCached);
            payeeAuthority = 
                BankService.externalCalls.getPayeeAuthority(urlHolder,
                                                            authorizationRequest.getAuthorityUrl());

            // Lookup of Payee's Provider
            urlHolder.setNonCachedMode(nonCached);
            providerAuthority =
                BankService.externalCalls.getProviderAuthority(urlHolder,
                                                               payeeAuthority.getProviderAuthorityUrl());

            // Now verify that they are issued by the same entity
            if (payeeAuthority.getAttestationKey().equals(
                    providerAuthority.getHostingProvider() == null ?
                // Direct attestation of Payee
                providerAuthority.getSignatureDecoder().getCertificatePath()[0].getPublicKey()
                                                                   :
                // Indirect attestation of Payee through a designated Hosting provider
                providerAuthority.getHostingProvider().getPublicKey())) {
                break;
            }

            // No match, should we give up?
            if (nonCached) {
                throw new IOException("Payee attestation key mismatch");
            }
            
            // Edge case?  Yes, but it could happen
            nonCached = !nonCached;
        }

        // Verify that the authority objects were signed by a genuine payment partner
        providerAuthority.getSignatureDecoder().verify(cardPayment ? BankService.acquirerRoot : BankService.paymentRoot);

        // Verify Payee signature keys.  They may be one generation back as well
        PayeeCoreProperties payeeCoreProperties = payeeAuthority.getPayeeCoreProperties();
        payeeCoreProperties.verify(paymentRequest.getPayee(), authorizationRequest.getSignatureDecoder());
        payeeCoreProperties.verify(paymentRequest.getPayee(), paymentRequest.getSignatureDecoder());

        // Optionally verify the claimed Payee account
        byte[] accountHash = paymentMethodSpecific.getAccountHash();
        if (payeeCoreProperties.getAccountHashes() == null) {
            if (accountHash != null) {
                throw new IOException("Missing \"" + ACCOUNT_VERIFIER_JSON + 
                                      "\" in \"" + Messages.PAYEE_AUTHORITY.toString() + "\"");
            }
        } else {
            if (accountHash == null) {
                throw new IOException("Missing verifiable payee account");
            }
            boolean notFound = true;
            for (byte[] hash : payeeCoreProperties.getAccountHashes()) {
                if (ArrayUtil.compare(accountHash, hash)) {
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                throw new IOException("Payee account does not match \"" + ACCOUNT_VERIFIER_JSON + 
                                      "\" in \"" + Messages.PAYEE_AUTHORITY.toString() + "\"");
            }
        }

        // Decrypt and validate the encrypted Payer authorization
        AuthorizationData authorizationData = authorizationRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

        // Verify that the there is a matching Payer account
        String accountId = authorizationData.getAccountId();
        String authorizedPaymentMethod = authorizationData.getPaymentMethod();
        UserAccountEntry account = BankService.userAccountDb.get(accountId);
        if (account == null) {
            logger.severe("No such account ID: " + accountId);
            throw new IOException("No such user account ID");
        }
        if (!authorizedPaymentMethod.equals(account.getPaymentMethod())) {
            logger.severe("Wrong payment method: " + authorizedPaymentMethod + " for account ID: " + accountId);
            throw new IOException("Wrong payment method");
        }
        if (!account.getPublicKey().equals(authorizationData.getPublicKey())) {
            logger.severe("Wrong public key for account ID: " + accountId);
            throw new IOException("Wrong user public key");
        }

        // We don't accept requests that are old or ahead of time
        long diff = System.currentTimeMillis() - authorizationData.getTimeStamp().getTimeInMillis();
        if (diff > (MAX_CLIENT_CLOCK_SKEW + MAX_CLIENT_AUTH_AGE) || diff < -MAX_CLIENT_CLOCK_SKEW) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
            sdf.setTimeZone(authorizationData.getTimeStamp().getTimeZone());
            BankService.rejectedTransactions++;
            return createProviderUserResponse("Either your request is older than " + 
                                                (MAX_CLIENT_AUTH_AGE / 60000) +
                                                " minutes, or your device clock is incorrect.<p>Timestamp=" +
                                                "<span style=\"white-space:nowrap\">" + 
                                                sdf.format(authorizationData.getTimeStamp().getTime()) +
                                                "</span>.</p>",
                                              null,
                                              authorizationData);
        }
            
        // Merchant provides the client's IP address which can be used for RBA
        String clientIpAddress = authorizationRequest.getClientIpAddress();
        
        ////////////////////////////////////////////////////////////////////////////
        // We got an authentic request.  Now we need to check available funds etc.//
        // Since we don't have a real bank this part is rather simplistic :-)     //
        ////////////////////////////////////////////////////////////////////////////

        BigDecimal amount = paymentRequest.getAmount();
        // Sorry but you don't appear to have a million bucks :-)
        if (amount.compareTo(DEMO_ACCOUNT_LIMIT) >= 0) {
            BankService.rejectedTransactions++;
            return createProviderUserResponse("Your request for " + 
                                                amountInHtml(paymentRequest, amount) +
                                                " appears to be slightly out of your current capabilities...",
                                              null,
                                              authorizationData);
        }

        // RBA v0.001...
        if (amount.compareTo(DEMO_RBA_LIMIT) >= 0 &&
            (authorizationData.getOptionalUserResponseItems() == null ||
             !authorizationData.getOptionalUserResponseItems()[0].getText().equals("garbo"))) {
            BankService.rejectedTransactions++;
            return createProviderUserResponse("Transaction requests exceeding " +
                                                amountInHtml(paymentRequest, DEMO_RBA_LIMIT) +
                                                " require additional user authentication to " +
                                                "be performed. Please enter your <span style=\"color:blue\">mother's maiden name</span>." +
                                                "<p>Since <i>this is a demo</i>, " +
                                                "answer <span style=\"color:red\">garbo</span>&nbsp; :-)</p>",
                                              new UserChallengeItem[]{new UserChallengeItem(RBA_PARM_MOTHER,
                                                    amount.compareTo(DEMO_RBA_LIMIT_CT) == 0 ?
                                                         UserChallengeItem.TYPE.ALPHANUMERIC : UserChallengeItem.TYPE.ALPHANUMERIC_SECRET,
                                                                                           20,
                                                                                           null)},
                                              authorizationData);
        }

        // Pure sample data...
        // Separate credit-card and account2account payments
        AuthorizationResponse.AccountDataEncoder accountData = cardPayment ?
            new com.supercard.SupercardAccountDataEncoder(authorizationData.getAccountId(), 
                                                          "Luke Skywalker",
                                                          ISODateTime.parseDateTime("2022-12-31T00:00:00Z"),
                                                          "943")
                                                     :
            new org.payments.sepa.SEPAAccountDataEncoder("FR1420041010050500013M02606");

        // Reference to Merchant
        StringBuffer accountReference = new StringBuffer();
        int q = accountId.length() - 4;
        for (char c : accountId.toCharArray()) {
            accountReference.append((--q < 0) ? c : '*');
        }
        boolean testMode = authorizationRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ": "") +
                "Authorized Amount=" + amount.toString() + 
                ", Account ID=" + accountId + 
                ", Payment Method=" + authorizedPaymentMethod + 
                ", Client IP=" + clientIpAddress +
                ", Method Specific=" + paymentMethodSpecific.logLine());

        String optionalLogData = null;
        if (!testMode) {
            // Here we would actually update things...
            // If Payer and Payee are in the same bank it will not require any networking of course.
            // Note that card and nonDirectPayments payments only reserve an amount.
            if (!cardPayment && nonDirectPayment == null) {
                optionalLogData = "Bank payment network log data...";
            }
        }
        
        // We did it!
        BankService.successfulTransactions++;
        return AuthorizationResponse.encode(authorizationRequest,
                                            accountReference.toString(),
                                            providerAuthority.getEncryptionParameters()[0],
                                            accountData,
                                            getReferenceId(),
                                            optionalLogData,
                                            BankService.bankKey);
    }
}
