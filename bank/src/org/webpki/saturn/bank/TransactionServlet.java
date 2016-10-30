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
import java.util.HashMap;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.util.ISODateTime;
import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.FinalizeCreditResponse;
import org.webpki.saturn.common.FinalizeTransactionRequest;
import org.webpki.saturn.common.FinalizeTransactionResponse;
import org.webpki.saturn.common.ChallengeField;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ProtectedAccountData;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;
import org.webpki.saturn.common.UserAccountEntry;
import org.webpki.saturn.common.ProviderUserResponse;

//////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "Native" mode Payment Provider (Bank) transaction servlet //
//////////////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;

    static HashMap<String,Integer> requestTypes = new HashMap<String,Integer>();
    
    static final int REQTYPE_PAYEE_INITIAL          = 0;
    static final int REQTYPE_TRANSACTION            = 1;
    static final int REQTYPE_PAYEE_FINALIZE_CREDIT  = 2;
    static final int REQTYPE_PAYEE_FINALIZE_CARDPAY = 3;
    static final int REQTYPE_FINALIZE_TRANSACTION   = 4;
    
    static {
        requestTypes.put(Messages.BASIC_CREDIT_REQUEST.toString(), REQTYPE_PAYEE_INITIAL);
        requestTypes.put(Messages.RESERVE_CREDIT_REQUEST.toString(), REQTYPE_PAYEE_INITIAL);
        requestTypes.put(Messages.RESERVE_CARDPAY_REQUEST.toString(), REQTYPE_PAYEE_INITIAL);

        requestTypes.put(Messages.FINALIZE_CREDIT_REQUEST.toString(), REQTYPE_PAYEE_FINALIZE_CREDIT);

        requestTypes.put(Messages.FINALIZE_CARDPAY_REQUEST.toString(), REQTYPE_PAYEE_FINALIZE_CARDPAY);

        requestTypes.put(Messages.TRANSACTION_REQUEST.toString(), REQTYPE_TRANSACTION);

        requestTypes.put(Messages.FINALIZE_TRANSACTION_REQUEST.toString(), REQTYPE_FINALIZE_TRANSACTION);
    }
    
    JSONObjectWriter processTransactionRequest(JSONObjectReader request, UrlHolder urlHolder)
    throws IOException, GeneralSecurityException {

        // Decode transaction request message
        TransactionRequest transactionRequest = new TransactionRequest(request);
        
        // Verify that the transaction request is signed by a payment partner
        transactionRequest.getSignatureDecoder().verify(BankService.paymentRoot);

        // Get the embedded by the user and merchant attested payment request
        ReserveOrBasicRequest reserveOrBasicRequest = transactionRequest.getReserveOrBasicRequest();
        boolean cardPayment = reserveOrBasicRequest.getMessage().isCardPayment();

        // Merchant provides the client's IP address which can be used for RBA
        String clientIpAddress = reserveOrBasicRequest.getClientIpAddress();

        // Decrypt the encrypted user authorization
        AuthorizationData authorizationData =
               reserveOrBasicRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

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

        // Get the embedded (counter-signed) payment request
        PaymentRequest paymentRequest = reserveOrBasicRequest.getPaymentRequest();

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

            // Lookup of payee's acquirer.  You would typically cache such information
            urlHolder.setUrl(reserveOrBasicRequest.getAcquirerAuthorityUrl());
            ProviderAuthority acquirerAuthority = getProviderAuthority(urlHolder);

            // Pure sample data...
            JSONObjectWriter protectedAccountData =
                ProtectedAccountData.encode(authorizationData.getAccountDescriptor(),
                                            "Luke Skywalker",
                                            ISODateTime.parseDateTime("2019-12-31T00:00:00Z").getTime(),
                                            "943");
            encryptedCardData = new JSONObjectWriter()
                .setEncryptionObject(protectedAccountData.serializeJSONObject(JSONOutputFormats.NORMALIZED),
                                     acquirerAuthority.getDataEncryptionAlgorithm(),
                                     acquirerAuthority.getEncryptionPublicKey(),
                                     acquirerAuthority.getKeyEncryptionAlgorithm());
        }

        StringBuffer accountReference = new StringBuffer();
        int q = accountId.length() - 4;
        for (char c : accountId.toCharArray()) {
            accountReference.append((--q < 0) ? c : '*');
        }
        
        return TransactionResponse.encode(transactionRequest,
                                          accountReference.toString(),
                                          encryptedCardData,
                                          getReferenceId(),
                                          BankService.bankKey);
    }

    JSONObjectWriter processReserveOrBasicRequest(JSONObjectReader request, UrlHolder urlHolder)
    throws IOException, GeneralSecurityException {
        // Read the by the user and merchant attested payment request
        ReserveOrBasicRequest attestedPaymentRequest = new ReserveOrBasicRequest(request);

        // The merchant is the only entity who can provide the client's IP address
        String clientIpAddress = attestedPaymentRequest.getClientIpAddress();

        // Client IP could be used for risk-based authentication
        logger.info("Client address: " + clientIpAddress);

        // Get the embedded (counter-signed) payment request
        PaymentRequest paymentRequest = attestedPaymentRequest.getPaymentRequest();

        // Verify that the merchant's signature belongs to a for us known merchant
        PayeeCoreProperties merchantProperties = BankService.merchantAccountDb.get(paymentRequest.getPayee().getId());
        if (merchantProperties == null) {
            throw new IOException("Unknown merchant: " + paymentRequest.getPayee().writeObject().toString());
        }
        if (!merchantProperties.getPublicKey().equals(paymentRequest.getPublicKey())) {
            throw new IOException("Public key doesn't match merchant: " + paymentRequest.getPayee().writeObject().toString());
        }

        // Lookup of payer's bank.  You would typically cache such information
        urlHolder.setUrl(attestedPaymentRequest.getProviderAuthorityUrl());
        ProviderAuthority providerAuthority = getProviderAuthority(urlHolder);

        // We need to separate credit-card and account-2-account payments
        boolean acquirerBased = attestedPaymentRequest.getPayerAccountType().isCardPayment();
        logger.info("Kind of operation: " + (acquirerBased ? "credit-card" : "account-2-account"));

        ////////////////////////////////////////////////////////////////////////////
        // We got an authentic request.  Now we need to get an attestation by     //
        // the payer's bank that user is authentic and have the required funds... //
        ////////////////////////////////////////////////////////////////////////////
    
        urlHolder.setUrl(providerAuthority.getTransactionUrl());

        // Customer bank: Can we please do a payment now?
        JSONObjectWriter transactionRequest = TransactionRequest.encode(attestedPaymentRequest,
                                                                        BankService.authorityUrl,
                                                                        getReferenceId(),
                                                                        BankService.bankKey);

        // Decode response
        JSONObjectReader response = postData(urlHolder, transactionRequest);
        if (response.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.PROVIDER_USER_RESPONSE.toString())) {
            // Parse for syntax only
            new ProviderUserResponse(response);
            return new JSONObjectWriter(response);
        }

        // Success
        return ReserveOrBasicResponse.encode(new TransactionResponse(response),
                                             BankService.bankKey);
    }

    JSONObjectWriter processFinalizeCreditRequest(JSONObjectReader payeeRequest, UrlHolder urlHolder) throws IOException, GeneralSecurityException {

        // Decode the finalize credit request
        FinalizeRequest finalizeRequest = new FinalizeRequest(payeeRequest);

        // Lookup of payer's bank.  You would typically cache such information
        urlHolder.setUrl(finalizeRequest.getProviderAuthorityUrl());
        ProviderAuthority providerAuthority = getProviderAuthority(urlHolder);

        // This message is the one which finally actually lifts money
        urlHolder.setUrl(providerAuthority.getTransactionUrl());
        FinalizeTransactionResponse finalizeTransactionResponse = 
            new FinalizeTransactionResponse(postData(urlHolder,
                                                     FinalizeTransactionRequest.encode(finalizeRequest,
                                                                                       getReferenceId(),
                                                                                       BankService.bankKey)));

        // It appears that we succeeded
        return FinalizeCreditResponse.encode(finalizeTransactionResponse,
                                             getReferenceId(),
                                             BankService.bankKey);
    }

    JSONObjectWriter processFinalizeCardpayRequest(JSONObjectReader payeeRequest, UrlHolder urlHolder) throws IOException, GeneralSecurityException {

        // Decode the finalize cardpay request
        FinalizeRequest finalizeRequest = new FinalizeRequest(payeeRequest);
        
        logger.info("Card data: " + finalizeRequest.getProtectedAccountData(BankService.decryptionKeys));

        // Here we are supposed to talk to the card payment network....

        // It appears that we succeeded
        return FinalizeCardpayResponse.encode(finalizeRequest,
                                              getReferenceId(),
                                              BankService.bankKey);
    }

    JSONObjectWriter processFinalizeTransactionRequest(JSONObjectReader payeeRequest, UrlHolder urlHolder) throws IOException, GeneralSecurityException {

        // Decode the finalize transaction request
        FinalizeTransactionRequest finalizeTransactionRequest = new FinalizeTransactionRequest(payeeRequest);

        //////////////////////////////////////////////////////////////////////////////
        // Since we don't have a real bank we simply return success...              //
        //////////////////////////////////////////////////////////////////////////////
        return FinalizeTransactionResponse.encode(finalizeTransactionRequest,
                                                  getReferenceId(),
                                                  BankService.bankKey);
    }

    @Override
    JSONObjectWriter processCall(JSONObjectReader providerRequest, UrlHolder urlHolder)
    throws IOException, GeneralSecurityException {
        Integer requestType = requestTypes.get(providerRequest.getString(JSONDecoderCache.QUALIFIER_JSON));
        if (requestType == null) {
            throw new IOException("Unexpected \"" + JSONDecoderCache.QUALIFIER_JSON + "\" :" + 
                                      providerRequest.getString(JSONDecoderCache.QUALIFIER_JSON));
        }
        switch (requestType) {
            case REQTYPE_PAYEE_INITIAL:
                return processReserveOrBasicRequest(providerRequest, urlHolder);

            case REQTYPE_TRANSACTION:
                return processTransactionRequest(providerRequest, urlHolder);

            case REQTYPE_PAYEE_FINALIZE_CREDIT:
                return processFinalizeCreditRequest(providerRequest, urlHolder);
                
            case REQTYPE_PAYEE_FINALIZE_CARDPAY:
                return processFinalizeCardpayRequest(providerRequest, urlHolder);
 
            case REQTYPE_FINALIZE_TRANSACTION:
                return processFinalizeTransactionRequest(providerRequest, urlHolder);
        }
        return null;
    }
}
