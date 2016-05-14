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

package org.webpki.saturn.keyprovider;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.cert.X509Certificate;

import java.security.interfaces.RSAPublicKey;

import java.net.URLEncoder;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.keygen2.ServerState;
import org.webpki.keygen2.KeySpecifier;
import org.webpki.keygen2.KeyGen2URIs;
import org.webpki.keygen2.InvocationResponseDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseDecoder;
import org.webpki.keygen2.CredentialDiscoveryResponseDecoder;
import org.webpki.keygen2.KeyCreationResponseDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseDecoder;
import org.webpki.keygen2.InvocationRequestEncoder;
import org.webpki.keygen2.ProvisioningInitializationRequestEncoder;
import org.webpki.keygen2.CredentialDiscoveryRequestEncoder;
import org.webpki.keygen2.KeyCreationRequestEncoder;
import org.webpki.keygen2.ProvisioningFinalizationRequestEncoder;

import org.webpki.sks.Grouping;
import org.webpki.sks.AppUsage;
import org.webpki.sks.PassphraseFormat;

import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Encryption;

import org.webpki.webutil.ServletUtil;

import org.webpki.json.JSONEncoder;
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

// A KeyGen2 protocol runner that setups pre-configured wallet keys.

public class KeyProviderServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger (KeyProviderServlet.class.getCanonicalName());
    
    static String success_image_and_message;
    
    void returnKeyGen2Error(HttpServletResponse response, String errorMessage) throws IOException, ServletException {
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Server errors are returned as HTTP redirects taking the client out of its KeyGen2 mode
        ////////////////////////////////////////////////////////////////////////////////////////////
        response.sendRedirect(KeyProviderService.keygen2EnrollmentUrl + 
                              "?" +
                              KeyProviderInitServlet.ERROR_TAG +
                              "=" +
                              URLEncoder.encode(errorMessage, "UTF-8"));
    }
    
    void keygen2JSONBody(HttpServletResponse response, JSONEncoder object) throws IOException {
        byte[] jsonData = object.serializeJSONDocument(JSONOutputFormats.PRETTY_PRINT);
        if (KeyProviderService.isDebug()) {
            log.info("Sent message\n" + new String(jsonData, "UTF-8"));
        }
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(jsonData);
    }

    void requestKeyGen2KeyCreation(HttpServletResponse response, ServerState keygen2State)
            throws IOException {
        ServerState.PINPolicy pinPolicy = 
            keygen2State.createPINPolicy(PassphraseFormat.NUMERIC,
                                         4,
                                         8,
                                         3,
                                         null);
        pinPolicy.setGrouping(Grouping.SHARED);
    
        for (KeyProviderService.PaymentCredential paymentCredential : KeyProviderService.paymentCredentials) {
            ServerState.Key key = keygen2State.createKey(AppUsage.SIGNATURE,
                                                         new KeySpecifier(KeyAlgorithms.NIST_P_256),
                                                         pinPolicy);
            AsymSignatureAlgorithms signAlg =
                paymentCredential.signatureKey.getPublicKey() instanceof RSAPublicKey ?
                    AsymSignatureAlgorithms.RSA_SHA256 : AsymSignatureAlgorithms.ECDSA_SHA256;
            key.setEndorsedAlgorithms(new String[]{signAlg.getAlgorithmId(AlgorithmPreferences.SKS)});
            key.setCertificatePath(paymentCredential.signatureKey.getCertificatePath());
            key.setPrivateKey(paymentCredential.signatureKey.getPrivateKey().getEncoded());
            key.setFriendlyName("Account " + paymentCredential.accountId);

            key.addExtension(BaseProperties.SATURN_WEB_PAY_CONTEXT_URI,
                             new JSONObjectWriter()
                .setObject(BaseProperties.ACCOUNT_JSON, 
                           new AccountDescriptor(paymentCredential.accountType,
                                                 paymentCredential.accountId).writeObject())
                .setBoolean(BaseProperties.CARD_FORMAT_ACCOUNT_ID_JSON,
                            paymentCredential.cardFormatted)
                .setString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON,
                            paymentCredential.authorityUrl)
                .setString(BaseProperties.SIGNATURE_ALGORITHM_JSON,
                           signAlg.getAlgorithmId(AlgorithmPreferences.JOSE))
                .setObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON, new JSONObjectWriter()
                    .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON,
                               Encryption.JOSE_A128CBC_HS256_ALG_ID)
                    .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON,
                               paymentCredential.encryptionKey instanceof RSAPublicKey ?
                                   Encryption.JOSE_RSA_OAEP_256_ALG_ID 
                                                          : 
                                   Encryption.JOSE_ECDH_ES_ALG_ID)
                    .setPublicKey(paymentCredential.encryptionKey, AlgorithmPreferences.JOSE))
                             .serializeJSONObject(JSONOutputFormats.NORMALIZED));

           key.addLogotype(KeyGen2URIs.LOGOTYPES.CARD, paymentCredential.cardImage);
        }
    
        keygen2JSONBody(response, 
                        new KeyCreationRequestEncoder(keygen2State,
                                                      KeyProviderService.keygen2EnrollmentUrl));
      }

    String certificateData(X509Certificate certificate) {
        return ", Subject='" + certificate.getSubjectX500Principal().getName() +
               "', Serial=" + certificate.getSerialNumber();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
           throws IOException, ServletException {
        executeRequest(request, response, null, false);
    }

    void executeRequest(HttpServletRequest request,
                        HttpServletResponse response,
                        String versionMacro,
                        boolean init)
         throws IOException, ServletException {
        String keygen2EnrollmentUrl = KeyProviderService.keygen2EnrollmentUrl;
        HttpSession session = request.getSession(false);
        try {
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Check that the request is properly authenticated
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (session == null) {
                throw new IOException("Session timed out");
             }
            ServerState keygen2State =
                (ServerState) session.getAttribute(KeyProviderInitServlet.KEYGEN2_SESSION_ATTR);
            if (keygen2State == null) {
                throw new IOException("Server state missing");
            }
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Check if it is the first (trigger) message from the client
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (init) {
                if (KeyProviderService.grantedVersions != null) {
                    boolean found = false;;
                    for (String version : KeyProviderService.grantedVersions) {
                        if (version.equals(versionMacro)) {
                            found = true;
                            break;
                          }
                    }
                    if (!found) {
                        throw new IOException("Wrong version of WebPKI, you need to update");
                    }
                }
                InvocationRequestEncoder invocationRequest =
                    new InvocationRequestEncoder(keygen2State,
                                                 keygen2EnrollmentUrl,
                                                 null);
                invocationRequest.setAbortUrl(keygen2EnrollmentUrl +
                                                  "?" +
                                                  KeyProviderInitServlet.ABORT_TAG +
                                                  "=true");
                keygen2State.addImageAttributesQuery(KeyGen2URIs.LOGOTYPES.LIST);
                keygen2JSONBody(response, invocationRequest);
                return;
              }

            ////////////////////////////////////////////////////////////////////////////////////////////
            // It should be a genuine KeyGen2 response.  Note that the order is verified!
            ////////////////////////////////////////////////////////////////////////////////////////////
            byte[] jsonData = ServletUtil.getData(request);
            if (!request.getContentType().equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Wrong \"Content-Type\": " + request.getContentType());
            }
            if (KeyProviderService.isDebug()) {
                log.info("Received message:\n" + new String(jsonData, "UTF-8"));
            }
            JSONDecoder jsonObject = KeyProviderService.keygen2JSONCache.parse(jsonData);
            switch (keygen2State.getProtocolPhase()) {
                case INVOCATION:
                  InvocationResponseDecoder invocationResponse = (InvocationResponseDecoder) jsonObject;
                  keygen2State.update(invocationResponse);

                  // Now we really start doing something
                  ProvisioningInitializationRequestEncoder provisioningInitRequest =
                      new ProvisioningInitializationRequestEncoder(keygen2State,
                                                                   keygen2EnrollmentUrl,
                                                                   1000,
                                                                   (short)50);
                  provisioningInitRequest.setKeyManagementKey(
                          KeyProviderService.keyManagemenentKey.getPublicKey());
                  keygen2JSONBody(response, provisioningInitRequest);
                  return;

                case PROVISIONING_INITIALIZATION:
                  ProvisioningInitializationResponseDecoder provisioningInitResponse = (ProvisioningInitializationResponseDecoder) jsonObject;
                  keygen2State.update(provisioningInitResponse, KeyProviderService.tlsCertificate);

                  log.info("Device Certificate=" + certificateData(keygen2State.getDeviceCertificate()));
                  CredentialDiscoveryRequestEncoder credentiaDiscoveryRequest =
                      new CredentialDiscoveryRequestEncoder(keygen2State, keygen2EnrollmentUrl);
                  credentiaDiscoveryRequest.addLookupDescriptor(
                      KeyProviderService.keyManagemenentKey.getPublicKey());
                  keygen2JSONBody(response, credentiaDiscoveryRequest);
                  return;

                case CREDENTIAL_DISCOVERY:
                  CredentialDiscoveryResponseDecoder credentiaDiscoveryResponse = (CredentialDiscoveryResponseDecoder) jsonObject;
                  keygen2State.update(credentiaDiscoveryResponse);
                  for (CredentialDiscoveryResponseDecoder.LookupResult lookupResult : credentiaDiscoveryResponse.getLookupResults()) {
                      for (CredentialDiscoveryResponseDecoder.MatchingCredential matchingCredential : lookupResult.getMatchingCredentials()) {
                          X509Certificate endEntityCertificate = matchingCredential.getCertificatePath()[0];
                          keygen2State.addPostDeleteKey(matchingCredential.getClientSessionId(), 
                                                        matchingCredential.getServerSessionId(),
                                                        endEntityCertificate,
                                                        KeyProviderService.keyManagemenentKey.getPublicKey());
                          log.info("Deleting key=" + certificateData(endEntityCertificate));
                      }
                  }
                  requestKeyGen2KeyCreation(response, keygen2State);
                  return;

                case KEY_CREATION:
                  KeyCreationResponseDecoder keyCreationResponse = (KeyCreationResponseDecoder) jsonObject;
                  keygen2State.update(keyCreationResponse);
                  keygen2JSONBody(response,
                                  new ProvisioningFinalizationRequestEncoder(keygen2State,
                                                                             keygen2EnrollmentUrl));
                  return;

                case PROVISIONING_FINALIZATION:
                  ProvisioningFinalizationResponseDecoder provisioningFinalResponse =
                      (ProvisioningFinalizationResponseDecoder) jsonObject;
                  keygen2State.update(provisioningFinalResponse);
                  log.info("Successful KeyGen2 run");

                  ////////////////////////////////////////////////////////////////////////////////////////////
                  // We are done, return an HTTP redirect taking the client out of its KeyGen2 mode
                  ////////////////////////////////////////////////////////////////////////////////////////////
                  response.sendRedirect(keygen2EnrollmentUrl);
                  return;

                default:
                  throw new IOException("Unxepected state");
            }
        } catch (Exception e) {
            if (session != null) {
                session.invalidate();
            }
            log.log(Level.SEVERE, "KeyGen2 failure", e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter printerWriter = new PrintWriter(baos);
            e.printStackTrace(printerWriter);
            printerWriter.flush();
            returnKeyGen2Error(response, baos.toString("UTF-8"));
        }
    }

    boolean foundData(HttpServletRequest request, StringBuffer result, String tag) {
        String value = request.getParameter(tag);
        if (value == null) {
            return false;
        }
        result.append(value);
        return true;
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
           throws IOException, ServletException {
        if (request.getParameter(KeyProviderInitServlet.INIT_TAG) != null) {
            executeRequest(request,
                           response,
                           request.getParameter(KeyProviderInitServlet.ANDROID_WEBPKI_VERSION_TAG),
                           true);
            return;
        }
        StringBuffer html = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">");
        StringBuffer result = new StringBuffer();
        if (foundData(request, result, KeyProviderInitServlet.ERROR_TAG)) {
            html.append("<table><tr><td><b>Failure Report:</b></td></tr><tr><td><pre><font color=\"red\">")
                .append(result)
                .append("</font></pre></td></tr></table>");
        } else if (foundData(request, result, KeyProviderInitServlet.PARAM_TAG)) {
            html.append(result);
        } else if (foundData(request, result, KeyProviderInitServlet.ABORT_TAG)) {
            log.info("KeyGen2 run aborted by the user");
            html.append("<b>Aborted by the user!</b>");
        } else {
            HttpSession session = request.getSession(false);
            if (session == null) {
                html.append("<b>You need to restart the session</b>");
            } else {
                session.invalidate();
                html.append(KeyProviderService.successImageAndMessage);
            }
        }
        KeyProviderInitServlet.output(response, 
                                      KeyProviderInitServlet.getHTML(null,
                                                                     null,
                                                                     html.append("</td></tr>").toString()));
    }

}
