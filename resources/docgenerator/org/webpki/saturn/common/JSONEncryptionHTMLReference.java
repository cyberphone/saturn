/*
 *  Copyright 2006-2015 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.common;

import static org.webpki.keygen2.KeyGen2Constants.*;

import java.io.IOException;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.AsymEncryptionAlgorithms;
import org.webpki.crypto.SymEncryptionAlgorithms;
import org.webpki.crypto.CertificateFilter;
import org.webpki.crypto.KeyContainerTypes;
import org.webpki.crypto.KeyUsageBits;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.MACAlgorithms;

import org.webpki.json.JSONBaseHTML;
import org.webpki.json.JSONBaseHTML.RowInterface;
import org.webpki.json.JSONBaseHTML.Types;
import org.webpki.json.JSONBaseHTML.ProtocolObject.Row.Column;
import org.webpki.json.JSONBaseHTML.ProtocolStep;

import org.webpki.json.JSONSignatureDecoder;

import org.webpki.sks.SecureKeyStore;

/**
 * Create an HTML description of the JSON Encryption Format.
 * 
 * @author Anders Rundgren
 */
public class JSONEncryptionHTMLReference extends JSONBaseHTML.Types
  {
    static JSONBaseHTML json;
    static RowInterface row;
    
    static class TargetKeyReference implements JSONBaseHTML.Extender
      {
        String sks_method;
        String json_tag;
        boolean optional_group;
        boolean array_flag;
        
        TargetKeyReference (String json_tag, boolean array_flag, String sks_method, boolean optional_group)
          {
            this.sks_method = sks_method;
            this.json_tag = json_tag;
            this.optional_group = optional_group;
            this.array_flag = array_flag;
          }
  
        @Override
        public Column execute (Column column) throws IOException
          {
            column = column
              .newRow ()
                .newColumn ()
                  .addProperty (json_tag);
            column = (array_flag ? column.addArrayLink (json_tag, 1) : column.addLink (json_tag))
                .newColumn ()
                  .setType (WEBPKI_DATA_TYPES.OBJECT)
                .newColumn ();
            if (optional_group)
              {
                column.setChoice (false, 2);
              }
            return column
                .newColumn ()
                  .addString ("<i>Optional</i>: See <code>SKS:")
                  .addString (sks_method)
                  .addString ("</code>.");
          }
      }
    
    static class LinkedObject implements JSONBaseHTML.Extender
      {
        String name;
        boolean mandatory;
        String description;
        int choice_length;
        
        LinkedObject (String name, boolean mandatory, String description, int choice_length)
          {
            this.name = name;
            this.mandatory = mandatory;
            this.description = description;
            this.choice_length = choice_length;
          }

        LinkedObject (String name, boolean mandatory, String description)
          {
            this (name, mandatory,description, 0);
          }

        @Override
        public Column execute (Column column) throws IOException
          {
            column = column
              .newRow ()
                .newColumn ()
                  .addProperty (name)
                  .addLink (name)
                .newColumn ()
                  .setType (WEBPKI_DATA_TYPES.OBJECT)
                .newColumn ();
            if (choice_length  == 0)
              {
                column.setUsage (mandatory);
              }
            else
              {
                column.setChoice (mandatory, choice_length);
              }
            return column
                .newColumn ()
                  .addString (description);
          }
      }


    static void createOption (String property, WEBPKI_DATA_TYPES type, boolean array_flag, String descrption) throws IOException
      {
        Column column = row.newRow ()
          .newColumn ()
            .addProperty (property);
        if (array_flag)
          {
            column.addArrayList (property, 1);
          }
        else
          {
            column.addSymbolicValue (property);
          }
        column = column.newColumn ().setType (type).newColumn ();
        column.setUsage (false);
        row = column.newColumn ().addString (descrption);
      }

    static void getListAttribute (StringBuffer s, String attribute)
      {
        s.append ("<li><code>")
         .append (attribute)
         .append ("</code></li>");
      }

    public static void main (String args[]) throws IOException
      {
        if (args.length != 1)
          {
            new RuntimeException ("Missing file argument");
          }
        json = new JSONBaseHTML (args, "JEF - JSON Encryption Format");
        
        json.addParagraphObject ().append ("<div style=\"margin-top:200pt;margin-bottom:200pt;text-align:center\"><span style=\"" + JSONBaseHTML.HEADER_STYLE + "\">JEF</span>" +
             "<br><span style=\"font-size:" + JSONBaseHTML.CHAPTER_FONT_SIZE + "\">&nbsp;<br>JSON Encryption Format</span></div>");
        
        json.niceSquare ("<i>Disclaimer</i>: This is a system in development. That is, the specification may change without notice.", 20);
        
        json.addTOC ();
        
        json.addParagraphObject ("Introduction").append ("This document describes a JSON ")
            .append(json.createReference (JSONBaseHTML.REF_JSON))
            .append(" formatted container for holding encrypted data.  " +
                     "The encrypted data is expressed in Base64URL ")
            .append (json.createReference (JSONBaseHTML.REF_BASE64))
            .append (". " +
                     "The scheme borrows heavily from IETF's JWE ")
             .append(json.createReference (JSONBaseHTML.REF_JWE))
             .append(" but casted in a format that matches JCS ")
             .append (json.createReference (JSONBaseHTML.REF_JCS))
             .append ("." + LINE_SEPARATOR +
                      "Finding the proper balance in a complex scheme like KeyGen2 is a combination of &quot;gut feeling&quot;, " +
                      "political considerations, available technology, foresight and market research. " +
                      "If this particular specification hit the right level only time can tell." +
                      "<table style=\"margin-top:20pt;margin-bottom:20pt;margin-left:auto;margin-right:auto;text-align:center\">" +
                      "<tr><td>&quot;<i>Perfection&nbsp;is&nbsp;achieved,&nbsp;not&nbsp;when&nbsp;there&nbsp;is&nbsp;" +
                      "nothing&nbsp;more<br>to&nbsp;add,&nbsp;but&nbsp;when&nbsp;there&nbsp;is&nbsp;nothing&nbsp;left&nbsp;to&nbsp;take&nbsp;away</i>&quot;</td></tr>" +
                      "<tr><td style=\"padding-top:4pt;font-size:8pt;text-align:right\">Antoine de Saint-Exup\u00e9ry</td></tr></table>");

        json.addParagraphObject ("Proxy Scheme").append ("Unlike certificate management protocols like CMP ")
            .append (json.createReference (JSONBaseHTML.REF_CMP))
            .append (", <i>KeyGen2 " +
                     "mandates a two-layer client architecture</i> where the " +
                     "outermost part is talking to the outside world (user and issuer), " +
                     "while an inner part does the communication with the SKS. " +
                     "That is, the client implementation acts as &quot;proxy&quot; enabling the use of a cleartext, JSON based, " +
                     "fairly high-level protocol with issuer, in spite of the fact that SKS only deals with " +
                     "low-level binary data." + LINE_SEPARATOR +
                     "Another core proxy task is minimizing network roundtrips through SKS command aggregation." + LINE_SEPARATOR +
                     "Although KeyGen2 depends on a proxy for doing the &quot;Heavy Lifting&quot;, " +
                     "E2ES (End To End Security) is achieved through the use of a <i>dynamically created shared secret</i>, " +
                     "which is only known by the SKS and the issuer. " +LINE_SEPARATOR +
                     "For a detailed description of the proxy scheme and the E2ES solution, consult the SKS architecture document ")
            .append (json.createReference (JSONBaseHTML.REF_SKS))
            .append (".");

        json.addParagraphSubObject (SECTION_HTTP_DEPENDENCIES).append ("KeyGen2 objects transferred through HTTP <b>must</b> use the Content-Type <code>application/json</code>."  + LINE_SEPARATOR +
                                    "Since KeyGen2 is to be regarded as an intrinsic part of the browser, HTTP cookies <b>must</b> be handled as for other HTTP requests.");

        json.addParagraphSubObject ("Error Handling").append ("Errors occurring on the <i>client's side</i> <b>must</b> terminate the session " +
                                    "and display an error dialog telling the user what happened." + LINE_SEPARATOR +
                                    "<i>Server-side</i> errors <b>must</b> abort the current server operation and return an appropriate " +
                                    json.globalLinkRef (SECTION_TERMINATION_MESSAGE) + " to the user. If the KeyGen2 proxy at this stage is rather expecting a KeyGen2 protocol object (see " +
                                    json.globalLinkRef (SECTION_HTTP_DEPENDENCIES) + "), the client session <b>must</b> be terminated." + LINE_SEPARATOR +
                                    "Whenever a KeyGen2 client session is aborted, the proxy <i>should</i> also abort the associated, potentially active SKS provisioning session (see <code>abortProvisioningSession</code>).");
        
        json.addParagraphSubObject ("Key Management Operations").append ("KeyGen2 provides built-in support for the SKS key management operations " +
                                    "<code>postUnlockKey</code>, <code>postDeleteKey</code>, <code>postUpdateKey</code> and <code>postCloneKeyProtection</code>." + LINE_SEPARATOR +
                                    "In the case the exact key is not known in advance, you <b>must</b> include a key discovery sequence as described in " +
                                    json.createReference (JSONBaseHTML.REF_SKS) + " <i>Appendix D, Remote Key Lookup</i>."); 

        json.addParagraphSubObject (SECTION_TERMINATION_MESSAGE).append ("When a KeyGen2 protocol sequence terminates (like when the proxy has sent a " +
                                    json.globalLinkRef (KeyGen2Messages.PROVISIONING_FINALIZATION_RESPONSE.getName ()) + " object to the server), " +
                                    "the browser <b>must</b> return to its &quot;normal&quot; state, ready for receiving a matching HTTP body containing a HTML page or similar."  + LINE_SEPARATOR +
                                    "Note that returned data <b>must</b> target the same <code>window</code> object which was used during invocation.");

        json.addParagraphSubObject (SECTION_DEFERRED_ISSUANCE).append ("To reduce costs for credential issuers, they may require users' " +
                                    "filling in forms on the web with user-related information followed by a KeyGen2 sequence terminating (see " + json.globalLinkRef (SECTION_TERMINATION_MESSAGE) + ") after " +
                                    json.globalLinkRef (KeyGen2Messages.KEY_CREATION_RESPONSE.getName ()) + 
                                    ". This mode <b>must</b> be indicated by setting " + json.globalLinkRef (KeyGen2Messages.KEY_CREATION_REQUEST.getName (), DEFERRED_ISSUANCE_JSON) + " to <code>true</code>." + LINE_SEPARATOR +
                                    "After the issuer in some way have verified the user's claimed data (and typically also the SKS <code>Device&nbsp;ID</code>), " +
                                    "the certification process is <i>resumed</i> by relaunching the " + json.globalLinkRef (KeyGen2Messages.INVOCATION_REQUEST.getName ()) +
                                    " (with " + json.globalLinkRef (KeyGen2Messages.INVOCATION_REQUEST.getName (), ACTION_JSON) + 
                                    " set to <code>" + Action.RESUME.getJSONName () + "</code>) through a URL sent to the user via mail, SMS, QR-code or NFC. The KeyGen2 proxy <b>must</b> after reception of the " +
                                    json.globalLinkRef (KeyGen2Messages.INVOCATION_REQUEST.getName ()) + " verify that there actually is an <i>open</i> SKS provisioning session having a matching " +
                                    json.globalLinkRef (KeyGen2Messages.INVOCATION_REQUEST.getName (), SERVER_SESSION_ID_JSON) + ".");

        json.addParagraphSubObject ("SOP Adherance").append ("The KeyGen2 proxy <b>must not</b> accept <code>SubmitURL</code> requests outside of the domain which returned the " + json.globalLinkRef (KeyGen2Messages.INVOCATION_REQUEST.getName ()) +
                                    " message, i.e. strictly following SOP (Same Origin Policy).");

        json.addParagraphSubObject ("JCS Profile").append ("Although KeyGen2 makes extensive use of the JSON Cleartext Signature scheme " + json.createReference (JSONBaseHTML.REF_JCS) +
                                    " a compliant implementation <b>must not</b> accept JOSE " +  json.createReference (JSONBaseHTML.REF_JWS) + 
                                    " algorithm identifiers since these are not natively supported by the target. See " + json.createReference (JSONBaseHTML.REF_SKS) + 
                                    " <i>Algorithm Support</i>." + LINE_SEPARATOR +
                                    "In addition, the JCS <code>" + JSONSignatureDecoder.KEY_ID_JSON + "</code>, <code>" +JSONSignatureDecoder.EXTENSIONS_JSON + "</code> and <code>" + JSONSignatureDecoder.PEM_URL_JSON + "</code> " +
                                    "properties <b>must not</b> be featured in KeyGen2 messages either." + LINE_SEPARATOR +
                                    "This document only refers to the required JCS properties.");

        json.addDataTypesDescription ("");
        
        json.addProtocolTableEntry ("Objects").append ("The following tables describe the KeyGen2 JSON structures in detail." + LINE_SEPARATOR +
                           "Entries written in <i>italics</i> like <a href=\"#" + GENERATED_KEYS_JSON + "\"><i>" + GENERATED_KEYS_JSON + "</i></a> " +
                           "represent sub objects, while the other entries such as <a href=\"#" + KeyGen2Messages.INVOCATION_REQUEST.getName ()  + "\">" + KeyGen2Messages.INVOCATION_REQUEST.getName () + "</a> " +
                           "consitute of the actual messages.");
        
        json.setAppendixMode ();
        
        json.sampleRun (JSONEncryptionHTMLReference.class,
                        "In the following KeyGen2 sample run the issuer requests that the client (SKS) creates an RSA 2048-bit key " +
                        "protected by a user-set PIN governed by a number of issuer-defined policies." + LINE_SEPARATOR +
                        "Finally, the issuer provides a certificate and a platform-adapted logotype." + LINE_SEPARATOR +
                        "For information regarding the cryptographic constructs, consult the SKS architecture manual.",
                        new ProtocolStep[]{new ProtocolStep ("InvocationRequest.json", "After a <i>compatible</i> browser has received this message, a dialog like the following is " +
                                                                                 "shown to user:" +
                                                                                  json.createDialog ("Credential Enrollment",
 "<tr><td colspan=\"2\">The following provider wants to create a<br>login credential for you:</td></tr>" +
 "<tr><td colspan=\"2\" style=\"text-align:center;padding-bottom:15pt\"><div style=\"display:inline-block\" class=\"dlgtext\">issuer.example.com</div></td></tr>") +
 "If the user accepts the request, the following response is sent to the server at the address specified by " + json.globalLinkRef (KeyGen2Messages.INVOCATION_REQUEST.getName (), SUBMIT_URL_JSON) + ":"),
                        new ProtocolStep ("InvocationResponse.json", "When the server has received the response above, it creates an <i>ephemeral EC key pair</i> and returns the public part to the client<br>together with other session parameters:"),
                        new ProtocolStep ("ProvisioningInitializationRequest.json", "Next the client generates a <i>matching ephemeral EC key pair</i> and sends the public part back to the server " +
 "including a client<br>session-ID, key attestation, device-certificate, etc.:"),
                        new ProtocolStep ("ProvisioningInitializationResponse.json", "After these message exchanges, the SKS and server (issuer) have established a <i>shared session-key</i>, " +
 "which is used for securing the<br>rest of the session through MAC and encryption operations." +
 "<br>&nbsp;<br>SKS API Reference: <code>createProvisioningSession</code>.<br>&nbsp;<br>" +
 "In the sample a request for creating a key is subsequently returned to the client:"),
                        new ProtocolStep ("KeyCreationRequest.json", "After the browser has received this message, a dialog like the following is " +
                                                                                  "shown to user:" +
                                                                                  json.createDialog ("PIN Code Assignment",
"<tr><td colspan=\"2\">Set and memorize a PIN for the<br>login credential:</td></tr>" +
"<tr><td colspan=\"2\" style=\"padding-top:0px\"><div class=\"dlgtext\">&#x2022; &#x2022; &#x2022; &#x2022; &#x2022; &#x2022;</div></td></tr>" +
"<tr><td colspan=\"2\">Repeat PIN:</td></tr>" +
"<tr><td colspan=\"2\" style=\"padding-top:0px;padding-bottom:15pt\"><div class=\"dlgtext\">&#x2022; &#x2022; &#x2022; &#x2022; &#x2022; &#x2022;</div></td></tr>") +
"When the user has set a PIN <i>matching the issuer's policy</i> and hit &quot;OK&quot;, the requested key pair is created and the public part of the<br>key pair is sent to the server for certification as shown " +
"in the response below." +
"<br>&nbsp;<br>SKS API References: <code>createPinPolicy</code>, <code>createKeyEntry</code>."),
                        new ProtocolStep ("KeyCreationResponse.json", "The server responds by issuing a matching certificate including an associated logotype." +
"<br>&nbsp;<br>SKS API References: <code>setCertificatePath</code>, <code>addExtension</code>."),
                        new ProtocolStep ("ProvisioningFinalizationRequest.json", "The finalization message which will only be sent to the server if the previous steps were successful." +
"<br>&nbsp;<br>SKS API Reference: <code>closeProvisioningSession</code>."),
                        new ProtocolStep ("ProvisioningFinalizationResponse.json", "Here the user is supposed to receive an issuer-specific web-page telling what to do next. " +
"See " + json.globalLinkRef (SECTION_TERMINATION_MESSAGE) + ".")});

        json.addParagraphObject ("Acknowledgements").append ("The design of the KeyGen2 protocol was &quot;inspired&quot; by several predecessors, most notably IETF's DSKPP ")
                          .append (json.createReference (JSONBaseHTML.REF_DSKPP))
                          .append ("." + LINE_SEPARATOR +
                          "Funding has been provided by <i>PrimeKey Solutions AB</i> and the <i>Swedish Innovation Board (VINNOVA)</i>.");
        
        json.addReferenceTable ();
        
        json.addDocumentHistoryLine ("2014-08-08", "0.7", "First official release");
        json.addDocumentHistoryLine ("2014-12-08", "0.71", "Aligned KeyGen2 with the updated " + json.createReference (JSONBaseHTML.REF_SKS) + " and " + 
                                     json.createReference (JSONBaseHTML.REF_JCS) + " specifications");
        json.addDocumentHistoryLine ("2015-01-12", "0.72", "Updated version to match ECDSA signature encoding change");
        json.addDocumentHistoryLine ("2016-01-25", "0.73", "Added JOSE algorithm support");

        json.addParagraphObject ("Author").append ("KeyGen2 was primarily developed by Anders Rundgren (<code>anders.rundgren.net@gmail.com</code>) as a part " +
                                     "of the OpenKeyStore project " +
                                     json.createReference (JSONBaseHTML.REF_OPENKEYSTORE)  + ".");

        preAmble (KeyGen2Messages.INVOCATION_REQUEST.getName ())
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_SESSION_ID_JSON)
              .addSymbolicValue (SERVER_SESSION_ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("The <code>" + SERVER_SESSION_ID_JSON +
                          "</code> <b>must</b> remain constant for the entire session.")
          .newExtensionRow (new SubmitURL ())
          .newRow ()
            .newColumn ()
              .addProperty (ACTION_JSON)
              .addSymbolicValue (ACTION_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("The <code>" + ACTION_JSON +
                          "</code> property gives (through a suitable GUI dialog) the user a hint of what the session in progress is about to perform. " +
                          "The valid constants are:<ul>" +
                          "<li><code>" + Action.MANAGE.getJSONName () + "</code> - Create, delete and/or update credentials</li>" +
                          "<li style=\"padding-bottom:4pt;padding-top:4pt\"><code>" + Action.RESUME.getJSONName () + "</code> - Resume operation after an interrupted ")
               .addLink (KeyGen2Messages.KEY_CREATION_RESPONSE.getName ())
               .addString (".  See ")
               .addLink (SECTION_DEFERRED_ISSUANCE)
               .addString (". A confirming client <b>must</b> after responding with ") 
               .addLink (KeyGen2Messages.INVOCATION_RESPONSE.getName ())
               .addString (" only accept a ")
               .addLink (KeyGen2Messages.PROVISIONING_FINALIZATION_REQUEST.getName ())
               .addString ("</li>" +
                           "<li><code>" + Action.UNLOCK.getJSONName () +
                           "</code> - Unlock existing keys. A conforming client should disallow ")
               .addLink (KeyGen2Messages.KEY_CREATION_REQUEST.getName ())
               .addString ("</li></ul>")
          .newRow ()
            .newColumn ()
              .addProperty (ABORT_URL_JSON)
              .addSymbolicValue (ABORT_URL_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Optional URL the provisioning client should launch the browser with if the user cancels the process.")
          .newRow ()
            .newColumn ()
              .addProperty (PRIVACY_ENABLED_JSON)
              .addSymbolicValue (PRIVACY_ENABLED_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("The <code>" + PRIVACY_ENABLED_JSON +
                          "</code> flag is used to set mode during ")
               .addLink (KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName ())
               .addString (".<br>See <code>SKS:createProvisioningSession." + PRIVACY_ENABLED_JSON +
                           "</code>." + LINE_SEPARATOR + "Note: The default value is <code>false</code>.")
          .newExtensionRow (new OptionalArrayList (PREFERREDD_LANGUAGES_JSON,
                                                   "<i>Optional</i>: List of preferred languages using ISO 639-1 two-character notation."))
          .newExtensionRow (new OptionalArrayList (KeyContainerTypes.KCT_TARGET_KEY_CONTAINERS,
                         "<i>Optional</i>: List of target key container types.  The elements may be:<ul>" +
                         getKeyContainers () +
                         "</ul>" +
                         "The key containers are listed in preference order. " +
                         "If no matching container is available the client may prompt " +
                         "the user for inserting a card or similar." + LINE_SEPARATOR + 
                         "If <code>" +
                         KeyContainerTypes.KCT_TARGET_KEY_CONTAINERS + "</code> is undefined " +
                         "the provisioning client is supposed to use the system's 'native' keystore."))
          .newRow ()
            .newColumn ()
              .addProperty (CLIENT_CAPABILITY_QUERY_JSON)
              .addArrayList (URI_LIST, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("<i>Optional</i>: List of URIs signifying client (platform) capabilities. " +
                          "The response (")
              .addPropertyLink (CLIENT_CAPABILITIES_JSON, KeyGen2Messages.INVOCATION_RESPONSE.getName ())
              .addString (") <b>must</b> contain the same URIs (in any order). " + LINE_SEPARATOR +
                         "Note that capabilities may refer to algorithms or specific extensions (see <code>SKS:addExtension</code>), as well as to non-SKS items such as ")
              .addPropertyLink (VIRTUAL_ENVIRONMENT_JSON, KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName ())
              .addString ("." + LINE_SEPARATOR +
                          "Another possible use of this feature is for signaling support for extensions " +
                          "in the protocol itself while keeping the name-space etc. intact." + LINE_SEPARATOR +
                          "<i>If requested capabilities are considered as privacy sensitive, a conforming implementation " +
                          "should ask for the user's permission to disclose them</i>." + LINE_SEPARATOR +
                          "Device-specific data like IMEI numbers <b>must not</b> be requested in the ") 
              .addPropertyLink (PRIVACY_ENABLED_JSON, KeyGen2Messages.INVOCATION_REQUEST.getName ())
              .addString (" mode." + LINE_SEPARATOR +
                          "For quering ")
              .addPropertyLink (VALUES_JSON, CLIENT_CAPABILITIES_JSON)
              .addString (" the following client attribute URIs are pre-defined:<ul>" + clientAttributes () +
                          "</ul>")
          .newExtensionRow (new OptionalSignature ());
  
        preAmble (KeyGen2Messages.INVOCATION_RESPONSE.getName ())
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_SESSION_ID_JSON)
              .addSymbolicValue (SERVER_SESSION_ID_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("Copy of <code>" + SERVER_SESSION_ID_JSON +
                          "</code> from ")
              .addLink (KeyGen2Messages.INVOCATION_REQUEST.getName ())
              .addString (".")
          .newRow ()
            .newColumn ()
              .addProperty (NONCE_JSON)
              .addSymbolicValue (NONCE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("<i>Optional</i> 1-32 byte nonce. See ")
              .addLink (KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName ())
              .addString (".")
          .newExtensionRow (new OptionalArrayObject (CLIENT_CAPABILITIES_JSON,
                                                     1,
                                                     "List of capabilities including algorithms, specific features, " +
                                                     "dynamic or static data, and preferred image sizes."));

        preAmble (KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName ())
          .newExtensionRow (new ServerSessionID ())
          .newExtensionRow (new SubmitURL ())
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_TIME_JSON)
              .addSymbolicValue (SERVER_TIME_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.DATE)
            .newColumn ()
            .newColumn ()
              .addString ("Server time which the client should verify as a &quot;sanity&quot; check.")
          .newRow ()
            .newColumn ()
              .addProperty (SESSION_KEY_ALGORITHM_JSON)
              .addValue (SecureKeyStore.ALGORITHM_SESSION_ATTEST_1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createProvisioningSession." +
                          SESSION_KEY_ALGORITHM_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (SESSION_KEY_LIMIT_JSON)
              .addUnquotedValue (SESSION_KEY_LIMIT_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.USHORT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createProvisioningSession." + SESSION_KEY_LIMIT_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (SESSION_LIFE_TIME_JSON)
              .addUnquotedValue (SESSION_LIFE_TIME_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.UINT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createProvisioningSession." + SESSION_LIFE_TIME_JSON + "</code>.")
          .newExtensionRow (new LinkedObject (SERVER_EPHEMERAL_KEY_JSON,
                                              true,
                                               "See <code>SKS:createProvisioningSession." +
                                              SERVER_EPHEMERAL_KEY_JSON + "</code>."))
          .newExtensionRow (new LinkedObject (KEY_MANAGEMENT_KEY_JSON,
                                              false,
                                              "See <code>SKS:createProvisioningSession." +
                                              KEY_MANAGEMENT_KEY_JSON + "</code>."))
          .newExtensionRow (new LinkedObject (VIRTUAL_ENVIRONMENT_JSON,
                                              false,
                          "The <code>" + VIRTUAL_ENVIRONMENT_JSON + "</code> option is intended to support BYOD " +
                          "use-cases where the provisioning process bootstraps an alternative " +
                          "environment and associated policies." + LINE_SEPARATOR +
                          "Since the exact nature of such an environment is platform dependent, it is necessary " +
                          "to find out what is actually available using the pre-defined extension URI <code>&quot;"))
              .addString (KeyGen2URIs.FEATURE.VIRTUAL_ENVIRONMENT)
              .addString ("&quot;</code>. The recommended method is adding the following to ")
              .addLink (KeyGen2Messages.INVOCATION_REQUEST.getName ())
              .addString (":" + LINE_SEPARATOR + "<code>&nbsp;&nbsp;&quot;" + CLIENT_CAPABILITY_QUERY_JSON +
                          "&quot;:&nbsp;[&quot;" +
                          KeyGen2URIs.FEATURE.VIRTUAL_ENVIRONMENT +
                          "&quot;]</code>" + LINE_SEPARATOR +
                          "A possible ")
              .addLink (KeyGen2Messages.INVOCATION_RESPONSE.getName ())
              .addString (" could be:" + LINE_SEPARATOR +
                          "<code>&nbsp;&nbsp;&quot;" + CLIENT_CAPABILITIES_JSON +
                          "&quot;:&nbsp;[{<br>&nbsp;&nbsp;&nbsp;&nbsp;" +
                          "&quot;" + TYPE_JSON + "&quot;:&nbsp;" +
                          "&quot;" +
                          KeyGen2URIs.FEATURE.VIRTUAL_ENVIRONMENT +
                          "&quot;,<br>&nbsp;&nbsp;&nbsp;&nbsp;&quot;" + VALUES_JSON +
                          "&quot;:&nbsp;[&quot;http://extreme-vm.com/type.3" +
                          "&quot;]<br>&nbsp;&nbsp;}]</code>" + LINE_SEPARATOR + 
                          "If a virtual environment is already installed only the configuration should be updated. " + LINE_SEPARATOR +
                          "Note that the <code>" +
                          VIRTUAL_ENVIRONMENT_JSON +
                          "</code> option presumes that the <code>" +
                          KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName () +
                          "</code> is <i>signed</i>.")
          .newRow ()
            .newColumn ()
              .addProperty (NONCE_JSON)
              .addSymbolicValue (NONCE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("<i>Optional</i> 1-32 byte nonce. The <code>" +
                           NONCE_JSON + "</code> value <b>must</b> be identical to the <code>" +
                           NONCE_JSON + "</code> specified in ")
               .addLink (KeyGen2Messages.INVOCATION_RESPONSE.getName ())
               .addString (". Also see <code>" + JSONSignatureDecoder.SIGNATURE_JSON + "</code>.")
          .newExtensionRow (new OptionalSignature ())
              .addString (" Note that <code>" + NONCE_JSON +
                          "</code> <b>must</b> be specified for a signed <code>" +
                          KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName () + "</code>.");

        preAmble (KeyGen2Messages.PROVISIONING_INITIALIZATION_RESPONSE.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_TIME_JSON)
              .addSymbolicValue (SERVER_TIME_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.DATE)
            .newColumn ()
            .newColumn ()
              .addString ("Server time transferred verbatim from ")
              .addLink (KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName ())
              .addString (".")
          .newRow ()
            .newColumn ()
              .addProperty (CLIENT_TIME_JSON)
              .addSymbolicValue (CLIENT_TIME_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.DATE)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createProvisioningSession." + CLIENT_TIME_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (ATTESTATION_JSON)
              .addSymbolicValue (ATTESTATION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createProvisioningSession." +
                          ATTESTATION_JSON + "</code>.")
          .newExtensionRow (new LinkedObject (CLIENT_EPHEMERAL_KEY_JSON,
                                              true,
                                              "See <code>SKS:createProvisioningSession." + CLIENT_EPHEMERAL_KEY_JSON + "</code>."))
          .newExtensionRow (new LinkedObject (DEVICE_ID_JSON,
                                              false,
                          "See <code>SKS:createProvisioningSession</code>. " +
                          "Note that this property is either required or forbidden " +
                          "depending on the value of "))
            .addPropertyLink (PRIVACY_ENABLED_JSON, KeyGen2Messages.INVOCATION_REQUEST.getName ())
            .addString (".")
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_CERT_FP_JSON)
              .addSymbolicValue (SERVER_CERT_FP_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("SHA256 fingerprint of the server's certificate during receival of the ")
              .addLink (KeyGen2Messages.PROVISIONING_INITIALIZATION_REQUEST.getName ())
              .addString (" object. " + LINE_SEPARATOR + 
                          "This property is mandatory for HTTPS connections.")
          .newExtensionRow (new LinkedObject (JSONSignatureDecoder.SIGNATURE_JSON,
                                              true,
                                              "Symmetric key signature covering the entire response. See <code>" +
                                              "SKS:signProvisioningSessionData</code>."));

        preAmble (KeyGen2Messages.CREDENTIAL_DISCOVERY_REQUEST.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newExtensionRow (new SubmitURL ())
          .newRow ()
             .newColumn ()
              .addProperty (LOOKUP_SPECIFIERS_JSON)
              .addArrayLink (LOOKUP_SPECIFIERS_JSON, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of signed credential lookup specifiers. " +
                          "See SKS appendix &quot;Remote Key Lookup&quot; for details.")
          .newExtensionRow (new OptionalSignature ());
  
        preAmble (KeyGen2Messages.CREDENTIAL_DISCOVERY_RESPONSE.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newRow ()
            .newColumn ()
              .addProperty (LOOKUP_RESULTS_JSON)
              .addArrayLink (LOOKUP_RESULTS_JSON, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of credential lookup results. " +
                          "See SKS appendix &quot;Remote Key Lookup&quot; for details.");

        preAmble (KeyGen2Messages.KEY_CREATION_REQUEST.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newExtensionRow (new SubmitURL ())
          .newRow ()
            .newColumn ()
              .addProperty (KEY_ENTRY_ALGORITHM_JSON)
              .addValue (SecureKeyStore.ALGORITHM_KEY_ATTEST_1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." +
                          KEY_ENTRY_ALGORITHM_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (DEFERRED_ISSUANCE_JSON)
              .addUnquotedValue (DEFERRED_ISSUANCE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Flag telling if the process should be suspended after ")
              .addLink (KeyGen2Messages.KEY_CREATION_RESPONSE.getName ())
              .addString (".  Default value: <code>false</code>. " +
                          "See the <code>" + ACTION_JSON + "</code> property in ")
              .addLink (KeyGen2Messages.INVOCATION_REQUEST.getName ())
              .addString (".")
          .newExtensionRow (new OptionalArrayObject (PUK_POLICY_SPECIFIERS_JSON,
                                                     1,
                                                     "List of PUK policy objects to be created. " +
                                                     "See <code>SKS:createPukPolicy</code>."))
          .newExtensionRow (new OptionalArrayObject (PIN_POLICY_SPECIFIERS_JSON,
                                                     1,
                                                     "List of PIN policy objects to be created. " +
                                                     "See <code>SKS:createPinPolicy</code>."))
          .newExtensionRow (new OptionalArrayObject (KEY_ENTRY_SPECIFIERS_JSON,
                                                     1,
                                                     "List of key entries to be created. " +
                                                     "See <code>SKS:createKeyEntry</code>."))
          .newExtensionRow (new OptionalSignature ()).setNotes (
              "Due to the stateful MAC scheme featured in SKS, " +
              "the properties beginning with <code>" + PUK_POLICY_SPECIFIERS_JSON + "</code> " +
              "and ending with <code>" + KEY_ENTRY_SPECIFIERS_JSON + "</code>, <b>must</b> " +
              "<i>be generated (by the issuer) and executed (by the SKS) in " +
              "exactly the order they are declared in this table as well " +
              "as in associated object arrays</i>.");
  
        preAmble (KeyGen2Messages.KEY_CREATION_RESPONSE.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newRow ()
            .newColumn ()
              .addProperty (GENERATED_KEYS_JSON)
              .addArrayLink (GENERATED_KEYS_JSON, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of generated keys. See <code>SKS:createKeyEntry</code>.").setNotes ("Due to the stateful MAC scheme featured in SKS, " +
                          "<code>" + GENERATED_KEYS_JSON + "</code> <b>must</b> " +
                          "<i>be encoded (by the SKS) and decoded (by the issuer) in exactly the same " +
                          "order (message wise) as they are encountered in the associated</i>  <a href=\"#" +
                           KeyGen2Messages.KEY_CREATION_REQUEST.getName () + "." + KEY_ENTRY_SPECIFIERS_JSON + "\">" + KEY_ENTRY_SPECIFIERS_JSON + "</a> "+
                           "(including those embedded by <a href=\"#" +
                           KeyGen2Messages.KEY_CREATION_REQUEST.getName () + "." + PIN_POLICY_SPECIFIERS_JSON + "\">" + PIN_POLICY_SPECIFIERS_JSON + "</a>).");

        preAmble (KeyGen2Messages.PROVISIONING_FINALIZATION_REQUEST.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newExtensionRow (new SubmitURL ())
          .newExtensionRow (new OptionalArrayObject (ISSUED_CREDENTIALS_JSON,
                                                     1,
                 "<i>Optional:</i> List of issued credentials. See <code>" +
                 "SKS:setCertificatePath</code>.")).setNotes (
                     "Due to the stateful MAC scheme featured in SKS, " +
                     "the properties beginning with <code>" + ISSUED_CREDENTIALS_JSON + "</code> " +
                     "and ending with <code>" + DELETE_KEYS_JSON + "</code>, <b>must</b> " +
                     "<i>be generated (by the issuer) and executed (by the SKS) in exactly " +
                     "the order they are declared in this table as well " +
                     "as in associated object arrays</i>.")
          .newExtensionRow (new OptionalArrayObject (UNLOCK_KEYS_JSON,
                                                     1,
                                                     "<i>Optional:</i> List of keys to be unlocked. See <code>" +
                                                     "SKS:postUnlockKey</code>."))
          .newExtensionRow (new OptionalArrayObject (DELETE_KEYS_JSON,
                                                     1,
                                                     "<i>Optional:</i> List of keys to be deleted. See <code>" +
                                                     "SKS:postDeleteKey</code>."))
          .newRow ()
            .newColumn ()
              .addProperty (NONCE_JSON)
              .addSymbolicValue (NONCE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:closeProvisioningSession</code>.")
          .newExtensionRow (new MAC ("closeProvisioningSession"))
            .addString (LINE_SEPARATOR +
                 "Due to the stateful MAC scheme featured in SKS, this " +
                 "<code>" + MAC_JSON + "</code> " +
                 "<b>must</b> be the final of a provisioning session both during encoding and decoding.")
          .newExtensionRow (new OptionalSignature ());

        preAmble (KeyGen2Messages.PROVISIONING_FINALIZATION_RESPONSE.getName ())
          .newExtensionRow (new StandardServerClientSessionIDs ())
          .newRow ()
            .newColumn ()
              .addProperty (ATTESTATION_JSON)
              .addSymbolicValue (ATTESTATION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:closeProvisioningSession</code>.");

        json.addSubItemTable (KEY_MANAGEMENT_KEY_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.PUBLIC_KEY_JSON)
              .addLink (JSONSignatureDecoder.PUBLIC_KEY_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("Actual key management key.")
          .newExtensionRow (new OptionalArrayObject (UPDATABLE_KEY_MANAGEMENT_KEYS_JSON,
                            1,
                            "<i>Optional:</i> List of the previous generation " +
                            "of key management keys."));

        json.addSubItemTable (UPDATABLE_KEY_MANAGEMENT_KEYS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.PUBLIC_KEY_JSON)
              .addLink (JSONSignatureDecoder.PUBLIC_KEY_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("Previous generation key management key." + LINE_SEPARATOR +
                          "Note that <code>SKS:updateKeyManagementKey." + KEY_MANAGEMENT_KEY_JSON + "</code>" +
                          " refers to the <i>new</i> key management key specified in the object <i>immediately above</i> (=embedding) this ")
              .addLink (UPDATABLE_KEY_MANAGEMENT_KEYS_JSON)
              .addString (" object.")
          .newRow ()
            .newColumn ()
              .addProperty (AUTHORIZATION_JSON)
              .addSymbolicValue (AUTHORIZATION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Authorization of the new key management key. See <code>SKS:updateKeyManagementKey." + AUTHORIZATION_JSON + "</code>.")
          .newExtensionRow (new OptionalArrayObject (UPDATABLE_KEY_MANAGEMENT_KEYS_JSON,
                            1,
                            "<i>Optional:</i> List of the previous generation of key management keys."));

        json.addSubItemTable (VIRTUAL_ENVIRONMENT_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (TYPE_JSON)
              .addSymbolicValue (TYPE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("Virtual environment specific type URI like <code>&quot;http://extreme-vm.com/type.3&quot;</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (CONFIGURATION_JSON)
              .addSymbolicValue (CONFIGURATION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Virtual environment specific configuration (setup) data.")
          .newRow ()
            .newColumn ()
              .addProperty (FRIENDLY_NAME_JSON)
              .addSymbolicValue (FRIENDLY_NAME_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("Virtual environment friendly name.");

        json.addSubItemTable (LOOKUP_SPECIFIERS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("Each specifier <b>must</b> have a unique ID.")
          .newRow ()
            .newColumn ()
              .addProperty (NONCE_JSON)
              .addSymbolicValue (NONCE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("<code>" + NONCE_JSON + "</code> data. " +
                          "See SKS appendix &quot;Remote Key Lookup&quot; for details.")
          .newRow ()
            .newColumn ()
              .addProperty (SEARCH_FILTER_JSON)
              .addLink (SEARCH_FILTER_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("<i>Optional</i> additional search conditions." + LINE_SEPARATOR +
                          "Note that at least one search condition <b>must</b> be specified if this option is used. The result of each condition is combined through a logical AND operation.")
          .newExtensionRow (new LinkedObject (JSONSignatureDecoder.SIGNATURE_JSON,
                            true,
                            "Signature using a key management key signature covering the lookup specifier. " +
                            "Note that the <code>" + JSONSignatureDecoder.PUBLIC_KEY_JSON + "</code> property <b>must</b> be present. " +
                            "See SKS appendix &quot;Remote Key Lookup&quot; for details."));

        createSearchFilter ();

        json.addSubItemTable (LOOKUP_RESULTS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("Each result <b>must</b> have a unique ID matching the request.")
          .newRow ()
            .newColumn ()
              .addProperty (MATCHING_CREDENTIALS_JSON)
              .addArrayLink (MATCHING_CREDENTIALS_JSON, 0)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of matching credentials.");
        
        json.addSubItemTable (MATCHING_CREDENTIALS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_SESSION_ID_JSON)
              .addSymbolicValue (SERVER_SESSION_ID_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("<code>" + SERVER_SESSION_ID_JSON + "</code> of matching credential.")
          .newRow ()
            .newColumn ()
              .addProperty (CLIENT_SESSION_ID_JSON)
              .addSymbolicValue (CLIENT_SESSION_ID_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("<code>" + CLIENT_SESSION_ID_JSON + "</code> of matching credential.")
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.CERTIFICATE_PATH_JSON)
              .addArrayList (SORTED_CERT_PATH, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Identical representation as the <code>" +
                          JSONSignatureDecoder.CERTIFICATE_PATH_JSON +
                          "</code> in ")
              .addLink (JSONSignatureDecoder.SIGNATURE_JSON)
              .addString (".")
          .newRow ()
            .newColumn ()
              .addProperty (LOCKED_JSON)
              .addUnquotedValue (LOCKED_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("If this property is <code>true</code> the key associated " +
                          "with the credential is locked due to multiple PIN errors. " +
                          "The default value is <code>false</code>.  See ")
              .addPropertyLink (UNLOCK_KEYS_JSON, KeyGen2Messages.PROVISIONING_FINALIZATION_REQUEST.getName ())
              .addString (".");

        json.addSubItemTable (PUK_POLICY_SPECIFIERS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPukPolicy." + ID_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (ENCRYPTED_PUK_JSON)
              .addSymbolicValue (ENCRYPTED_PUK_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPukPolicy." + ENCRYPTED_PUK_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (RETRY_LIMIT_JSON)
              .addUnquotedValue (RETRY_LIMIT_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.USHORT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPukPolicy." + RETRY_LIMIT_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (FORMAT_JSON)
              .addSymbolicValue (FORMAT_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPukPolicy." + FORMAT_JSON + "</code>.")
          .newExtensionRow (new MAC ("createPukPolicy"))
          .newRow ()
            .newColumn ()
              .addProperty (PIN_POLICY_SPECIFIERS_JSON)
              .addArrayLink (PIN_POLICY_SPECIFIERS_JSON, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of PIN policy objects to be created and controlled by this PUK policy. " +
                          "See <code>SKS:createPinPolicy</code>.");

        json.addSubItemTable (PIN_POLICY_SPECIFIERS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPinPolicy." + ID_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (MIN_LENGTH_JSON)
              .addUnquotedValue (MIN_LENGTH_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.USHORT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPinPolicy." + MIN_LENGTH_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (MAX_LENGTH_JSON)
              .addUnquotedValue (MAX_LENGTH_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.USHORT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPinPolicy." + MAX_LENGTH_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (RETRY_LIMIT_JSON)
              .addUnquotedValue (RETRY_LIMIT_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.USHORT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPinPolicy." + RETRY_LIMIT_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (FORMAT_JSON)
              .addSymbolicValue (FORMAT_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createPinPolicy." + FORMAT_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (USER_MODIFIABLE_JSON)
              .addUnquotedValue (USER_MODIFIABLE_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Flag with the default value <code>true</code>." +
                          "<br>See <code>SKS:createPinPolicy." + USER_MODIFIABLE_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (GROUPING_JSON)
              .addSymbolicValue (GROUPING_JSON)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Grouping specifier with the default value <code>none</code>." +
                          "<br>See <code>SKS:createPINPolicy." + GROUPING_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (INPUT_METHOD_JSON)
              .addSymbolicValue (INPUT_METHOD_JSON)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Input method specifier with the default value <code>any</code>." +
                          "<br>See <code>SKS:createPinPolicy." + INPUT_METHOD_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (PATTERN_RESTRICTIONS_JSON)
              .addArrayList (PATTERN_RESTRICTIONS_JSON, 1)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("List of pattern restrictions.  See <code>SKS:createPinPolicy." + PATTERN_RESTRICTIONS_JSON + "</code>." +
                          "<br>If this property is undefined, there are no PIN pattern restrictions.")
          .newExtensionRow (new MAC ("createPinPolicy"))
          .newRow ()
            .newColumn ()
              .addProperty (KEY_ENTRY_SPECIFIERS_JSON)
              .addArrayLink (KEY_ENTRY_SPECIFIERS_JSON, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of key entries to be created and controlled by this PIN policy." +
                          "<br>See <code>SKS:createKeyEntry</code>.");

        json.addSubItemTable (KEY_ENTRY_SPECIFIERS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + ID_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (ENCRYPTED_PIN_JSON)
              .addSymbolicValue (ENCRYPTED_PIN_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry.pinValue</code>.<br>" + "" +
              		      "Note that if this property is defined, the " +
              		      "<code>SKS:createPinPolicy.userDefined</code> " +
              		      "flag of the required embedding PIN policy is set to <code>false</code> " +
              		      "else it is set to <code>true</code>." + LINE_SEPARATOR +
              		      "Keys associated with a specific PIN policy " +
              		      "<b>must not</b> mix user-defined and preset PINs.")
          .newRow ()
            .newColumn ()
              .addProperty (ENABLE_PIN_CACHING_JSON)
              .addUnquotedValue (ENABLE_PIN_CACHING_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Flag with the default value <code>false</code>.<br>" +
                          "See <code>SKS:createKeyEntry." + ENABLE_PIN_CACHING_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (DEVICE_PIN_PROTECTION_JSON)
              .addUnquotedValue (DEVICE_PIN_PROTECTION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("Flag with the default value <code>false</code>.<br>" +
                          "See <code>SKS:createKeyEntry." + DEVICE_PIN_PROTECTION_JSON + "</code>. " + LINE_SEPARATOR +
                          "Note that this flag (if true) cannot be combined with PIN policy settings.")
          .newRow ()
            .newColumn ()
              .addProperty (APP_USAGE_JSON)
              .addSymbolicValue (APP_USAGE_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + APP_USAGE_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (KEY_ALGORITHM_JSON)
              .addSymbolicValue (KEY_ALGORITHM_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + KEY_ALGORITHM_JSON + "</code>. " +
                          "Also see SKS &quot;Algorithm Support&quot;." + LINE_SEPARATOR +
                          "The currently recognized key algorithms include:" +
                          JSONBaseHTML.enumerateStandardAlgorithms (KeyAlgorithms.values (), false, false))
          .newRow ()
            .newColumn ()
              .addProperty (KEY_PARAMETERS_JSON)
              .addSymbolicValue (KEY_PARAMETERS_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + KEY_PARAMETERS_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (ENDORSED_ALGORITHMS_JSON)
              .addArrayList (URI_LIST, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + ENDORSED_ALGORITHMS_JSON + "</code>. " +
                          "Also see SKS &quot;Algorithm Support&quot;." + LINE_SEPARATOR +
                          "Note that <i>endorsed algorithm URIs <b>must</b> be specified in strict lexical order</i>." + LINE_SEPARATOR +
                          "The currently recognized algorithms include:" +
                          JSONBaseHTML.enumerateStandardAlgorithms (MACAlgorithms.values (), true, false) +
                          JSONBaseHTML.enumerateStandardAlgorithms (AsymSignatureAlgorithms.values (), false, false) +
                          JSONBaseHTML.enumerateStandardAlgorithms (AsymEncryptionAlgorithms.values (), false, false) +
                          JSONBaseHTML.enumerateStandardAlgorithms (SymEncryptionAlgorithms.values (), true, false) +
                          "<ul><li><code>" + SecureKeyStore.ALGORITHM_ECDH_RAW + "</code></li></ul>" +
                          "<ul><li><code>" + SecureKeyStore.ALGORITHM_NONE + "</code></li></ul>")
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_SEED_JSON)
              .addSymbolicValue (SERVER_SEED_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + SERVER_SEED_JSON + "</code>. " +
                          "If this property is undefined, it is assumed to be a zero-length array.")
          .newRow ()
            .newColumn ()
              .addProperty (BIOMETRIC_PROTECTION_JSON)
              .addSymbolicValue (BIOMETRIC_PROTECTION_JSON)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + BIOMETRIC_PROTECTION_JSON + "</code>. " +
                          "The default value is <code>none</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (DELETE_PROTECTION_JSON)
              .addSymbolicValue (DELETE_PROTECTION_JSON)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + DELETE_PROTECTION_JSON + "</code>. " +
                          "The default value is <code>none</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (EXPORT_PROTECTION_JSON)
              .addSymbolicValue (EXPORT_PROTECTION_JSON)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + EXPORT_PROTECTION_JSON + "</code>. " +
                          "The default value is <code style=\"white-space:nowrap\">non-exportable</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (FRIENDLY_NAME_JSON)
              .addSymbolicValue (FRIENDLY_NAME_JSON)
            .newColumn ()
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + FRIENDLY_NAME_JSON + "</code>.")
          .newExtensionRow (new MAC ("createKeyEntry"));

        json.addSubItemTable (GENERATED_KEYS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("<code>" + ID_JSON + "</code> <b>must</b> match the identifier used in ")
              .addLink (KeyGen2Messages.KEY_CREATION_REQUEST.getName ())
              .addString (" for a specific key.")
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.PUBLIC_KEY_JSON)
              .addLink (JSONSignatureDecoder.PUBLIC_KEY_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + JSONSignatureDecoder.PUBLIC_KEY_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (ATTESTATION_JSON)
              .addSymbolicValue (ATTESTATION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:createKeyEntry." + ATTESTATION_JSON + "</code>.");

        json.addSubItemTable (ISSUED_CREDENTIALS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (ID_JSON)
              .addSymbolicValue (ID_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.ID)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:setCertificatePath." + ID_JSON + "</code>")
              .addString (".<br><code>" + ID_JSON + "</code> <b>must</b> match the identifier used in ")
              .addLink (KeyGen2Messages.KEY_CREATION_REQUEST.getName ())
              .addString (" for a specific key.")
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.CERTIFICATE_PATH_JSON)
              .addArrayList (SORTED_CERT_PATH, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See <code>SKS:setCertificatePath.certificate...</code>")
              .addString (".<br>Identical representation as the <code>" +
                          JSONSignatureDecoder.CERTIFICATE_PATH_JSON +
                          "</code> in ")
              .addLink (JSONSignatureDecoder.SIGNATURE_JSON)
              .addString (".")
          .newExtensionRow (new MAC ("setCertificatePath"))
          .newRow ()
            .newColumn ()
              .addProperty (TRUST_ANCHOR_JSON)
              .addUnquotedValue (TRUST_ANCHOR_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setUsage (false)
            .newColumn ()
              .addString ("<i>Optional:</i> Flag (with the default value <code>false</code>), " +
                          "which tells if <code>" +
                          JSONSignatureDecoder.CERTIFICATE_PATH_JSON +
                          "</code> contains a user-installable trust anchor as well." + LINE_SEPARATOR +
                          "Trust anchor installation is meant to be <i>independent</i> of SKS provisioning.")
          .newExtensionRow (new LinkedObject (IMPORT_SYMMETRIC_KEY_JSON,
                                              false,
                          "<i>Optional:</i> Import of raw symmetric key. See <code>SKS:importSymmetricKey</code>.", 2))
          .newExtensionRow (new LinkedObject (IMPORT_PRIVATE_KEY_JSON,
                                              false,
                          "<i>Optional:</i> Import of private key in PKCS #8 " +
                          json.createReference (JSONBaseHTML.REF_PKCS8) +
                          " format. See <code>SKS:importPrivateKey</code>."))
          .newExtensionRow (new TargetKeyReference (UPDATE_KEY_JSON, false, "postUpdateKey", true))
          .newExtensionRow (new TargetKeyReference (CLONE_KEY_PROTECTION_JSON, false, "postCloneKeyProtection", false))
          .newExtensionRow (new OptionalArrayObject (EXTENSIONS_JSON,
              1,
              "<i>Optional:</i> List of extension objects. See <code>" +
              "SKS:addExtension</code>."))
          .newExtensionRow (new OptionalArrayObject (ENCRYPTED_EXTENSIONS_JSON,
              1,
              "<i>Optional:</i> List of encrypted extension objects. See <code>" +
              "SKS:addExtension</code>."))
          .newExtensionRow (new OptionalArrayObject (PROPERTY_BAGS_JSON,
              1,
              "<i>Optional:</i> List of property objects. See <code>" +
              "SKS:addExtension</code>."))
          .newExtensionRow (new OptionalArrayObject (LOGOTYPES_JSON,
              1,
              "<i>Optional:</i> List of logotype objects. See <code>" +
              "SKS:addExtension</code>.")).setNotes (
                  "Due to the stateful MAC scheme featured in SKS, " +
                  "the properties beginning with <code>" + IMPORT_SYMMETRIC_KEY_JSON + "</code> " +
                  "and ending with <code>" + LOGOTYPES_JSON + "</code>, <b>must</b> " +
                  "<i>be generated (by the issuer) and executed (by the SKS) in " +
                  "exactly the order they are declared in this table as well " +
                  "as in associated object arrays</i>." + LINE_SEPARATOR +
                  "Note that that credential <code>" + ID_JSON +
                  "</code>s are not guaranteed to be " +
                  "supplied in the same order as during the associated " +
                  "<a href=\"#" + KeyGen2Messages.KEY_CREATION_REQUEST.getName () + "\">" +
                  KeyGen2Messages.KEY_CREATION_REQUEST.getName () + "</a>.");

        json.addSubItemTable (new String[]{CLONE_KEY_PROTECTION_JSON,
                                           DELETE_KEYS_JSON,
                                           UNLOCK_KEYS_JSON,
                                           UPDATE_KEY_JSON})
          .newRow ()
            .newColumn ()
              .addProperty (CertificateFilter.CF_FINGER_PRINT)
              .addSymbolicValue (CertificateFilter.CF_FINGER_PRINT)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("SHA256 fingerprint of target certificate.")
          .newRow ()
            .newColumn ()
              .addProperty (SERVER_SESSION_ID_JSON)
              .addSymbolicValue (SERVER_SESSION_ID_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("For locating the target key.")
          .newRow ()
            .newColumn ()
              .addProperty (CLIENT_SESSION_ID_JSON)
              .addSymbolicValue (CLIENT_SESSION_ID_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("For locating the target key.")
          .newRow ()
            .newColumn ()
              .addProperty (AUTHORIZATION_JSON)
              .addSymbolicValue (AUTHORIZATION_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("See &quot;Target Key Reference&quot; in the SKS reference.")
          .newExtensionRow (new MAC ("post* </code> methods<code>"));
        
        json.addSubItemTable (new String[]{ENCRYPTED_EXTENSIONS_JSON,
                                           EXTENSIONS_JSON})
          .newRow ()
            .newColumn ()
              .addProperty (TYPE_JSON)
              .addSymbolicValue (TYPE_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("Extension type URI.")
          .newRow ()
            .newColumn ()
              .addProperty (EXTENSION_DATA_JSON)
              .addSymbolicValue (EXTENSION_DATA_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Extension data.")
          .newExtensionRow (new MAC ("addExtension"));

        json.addSubItemTable (LOGOTYPES_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (TYPE_JSON)
              .addSymbolicValue (TYPE_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("Logotype type URI.")
          .newRow ()
            .newColumn ()
              .addProperty (MIME_TYPE_JSON)
              .addSymbolicValue (MIME_TYPE_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("Logotype MIME type.")
          .newRow ()
            .newColumn ()
              .addProperty (EXTENSION_DATA_JSON)
              .addSymbolicValue (EXTENSION_DATA_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Logotype image data.")
          .newExtensionRow (new MAC ("addExtension"));

        json.addSubItemTable (PROPERTY_BAGS_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (TYPE_JSON)
              .addSymbolicValue (TYPE_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("Property bag type URI. See <code>SKS:addExtension</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (PROPERTIES_JSON)
              .addArrayLink (PROPERTIES_JSON, 1)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of property values. See <code>SKS:addExtension</code>.")
          .newExtensionRow (new MAC ("addExtension"));

        json.addSubItemTable (PROPERTIES_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (NAME_JSON)
              .addSymbolicValue (NAME_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("Property name.")
          .newRow ()
            .newColumn ()
              .addProperty (VALUE_JSON)
              .addSymbolicValue (VALUE_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("Property value.")
          .newRow ()
            .newColumn ()
              .addProperty (WRITABLE_JSON)
              .addUnquotedValue (WRITABLE_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
               .setUsage (false)
            .newColumn ()
              .addString ("Writable flag. Default is <code>false</code>.  See <code>SKS:setProperty</code>.");

        json.addSubItemTable (new String[]{IMPORT_PRIVATE_KEY_JSON, IMPORT_SYMMETRIC_KEY_JSON})
          .newRow ()
            .newColumn ()
              .addProperty (ENCRYPTED_KEY_JSON)
              .addSymbolicValue (ENCRYPTED_KEY_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Encrypted key material.  See <code>SKS:import* </code> methods<code>." + ENCRYPTED_KEY_JSON + "</code>.")
          .newExtensionRow (new MAC ("import* </code>methods<code>"));

        json.addSubItemTable (CLIENT_CAPABILITIES_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (TYPE_JSON)
              .addSymbolicValue (TYPE_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.URI)
            .newColumn ()
            .newColumn ()
              .addString ("Client capability type URI.")
          .newRow ()
            .newColumn ()
              .addProperty (SUPPORTED_JSON)
              .addSymbolicValue (SUPPORTED_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.BOOLEAN)
            .newColumn ()
              .setChoice (true, 3)
            .newColumn ()
              .addString ("For non-parametric queries like algorithms this property tells if the type is supported or not. " + LINE_SEPARATOR +
                          "Unknown or unsupported query types <b>must</b> always return this attribute with the argument <code>false</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (VALUES_JSON)
              .addArrayList (VALUES_JSON, 1)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("List of attribute data associated with <code>" + TYPE_JSON + "</code>.")
          .newRow ()
            .newColumn ()
              .addProperty (IMAGE_ATTRIBUTES_JSON)
              .addLink (IMAGE_ATTRIBUTES_JSON)
            .newColumn ()
              .setType (Types.WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("List of client image preferences that the issuer may use for creating suitable ")
              .addLink (LOGOTYPES_JSON)
              .addString (".  Known logotypes include:<ul>" + getLogotypes () + "</ul>" +
                          "Logotypes should not have framing borders or extra margins " +
                          "unless these are integral parts of the actual logotype image. " + 
                          "Logotypes should render nicely on light backgrounds. " +
                          "Shadows should be avoided since the icon viewer itself may add such. " +
                          "Support for PNG files is <i>mandatory</i>.");

        json.addSubItemTable (IMAGE_ATTRIBUTES_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (MIME_TYPE_JSON)
              .addSymbolicValue (MIME_TYPE_JSON)
            .newColumn ()
            .newColumn ()
            .newColumn ()
              .addString ("Image MIME type.")
          .newRow ()
            .newColumn ()
              .addProperty (WIDTH_JSON)
              .addSymbolicValue (WIDTH_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.UINT)
            .newColumn ()
            .newColumn ()
              .addString ("Image width.")
          .newRow ()
            .newColumn ()
              .addProperty (HEIGHT_JSON)
              .addSymbolicValue (HEIGHT_JSON)
            .newColumn ()
               .setType (WEBPKI_DATA_TYPES.UINT)
            .newColumn ()
            .newColumn ()
              .addString ("Image height.");

        json.addSubItemTable (DEVICE_ID_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.CERTIFICATE_PATH_JSON)
              .addArrayList (SORTED_CERT_PATH, 1)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.BASE64)
            .newColumn ()
            .newColumn ()
              .addString ("Identical representation as the <code>" +
                          JSONSignatureDecoder.CERTIFICATE_PATH_JSON +
                          "</code> in ")
              .addLink (JSONSignatureDecoder.SIGNATURE_JSON)
              .addString (".");
        
        json.addSubItemTable (SERVER_EPHEMERAL_KEY_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.PUBLIC_KEY_JSON)
              .addLink (JSONSignatureDecoder.PUBLIC_KEY_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("<code>" + SERVER_EPHEMERAL_KEY_JSON + 
                          "</code> <b>must</b> be an EC key matching the capabilities of the SKS.");
      
        json.addSubItemTable (CLIENT_EPHEMERAL_KEY_JSON)
          .newRow ()
            .newColumn ()
              .addProperty (JSONSignatureDecoder.PUBLIC_KEY_JSON)
              .addLink (JSONSignatureDecoder.PUBLIC_KEY_JSON)
            .newColumn ()
              .setType (WEBPKI_DATA_TYPES.OBJECT)
            .newColumn ()
            .newColumn ()
              .addString ("<code>" + CLIENT_EPHEMERAL_KEY_JSON + 
                          "</code> <b>must</b> be an EC key using the same curve as <code>" + 
                          SERVER_EPHEMERAL_KEY_JSON + "</code>.");

        json.addJSONSignatureDefinitions (false, null, null, false);
        
        json.writeHTML ();
      }
  }
