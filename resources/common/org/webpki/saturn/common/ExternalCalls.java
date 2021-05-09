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
package org.webpki.saturn.common;

import java.io.IOException;

import java.net.URL;

import java.security.GeneralSecurityException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.net.HTTPSWrapper;

//////////////////////////////////////////////////////////////////////////
// This class deals with external POST and GET operations               //
//////////////////////////////////////////////////////////////////////////

public class ExternalCalls {
    
    boolean logging;
    Logger logger;
    Integer portMapper;
    
    public ExternalCalls(boolean logging, Logger logger, Integer portMapper) {
        this.logging = logging;
        this.logger = logger;
        this.portMapper = portMapper;
    }

    static final int TIMEOUT_FOR_REQUEST           = 10000;
    
    // Authority object caches

    Map<String,PayeeAuthorityDecoder> payeeAuthorityObjects = 
            Collections.synchronizedMap(new LinkedHashMap<>());

    Map<String,ProviderAuthorityDecoder> providerAuthorityObjects = 
            Collections.synchronizedMap(new LinkedHashMap<>());

    String portFilter(String url) throws IOException {
        // Our JBoss installation has some port mapping issues...
        if (portMapper == null) {
            return url;
        }
        URL url2 = new URL(url);
        return new URL(url2.getProtocol(),
                       url2.getHost(),
                       portMapper,
                       url2.getFile()).toExternalForm(); 
    }

    JSONObjectReader fetchJsonReturnData(HTTPSWrapper wrap, UrlHolder urlHolder) throws IOException {
        if (wrap.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new IOException("HTTP error " + wrap.getResponseCode() + " " + wrap.getResponseMessage() + ": " +
                                  (wrap.getData() == null ? "No other information available" : wrap.getDataUTF8()));
        }
        // We expect JSON, yes
        if (!wrap.getRawContentType().equals(BaseProperties.JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + BaseProperties.JSON_CONTENT_TYPE + 
                                  "\" , found: " + wrap.getRawContentType());
        }
        JSONObjectReader result = JSONParser.parse(wrap.getData());
        if (logging) {
            logger.info("Call to " + urlHolder.getUrl() + urlHolder.getCallerAddress() +
                        "returned:\n" + result);
        }
        return result;
    }

    public JSONObjectReader postJsonData(UrlHolder urlHolder, String url, JSONObjectWriter request) throws IOException {
        if (logging) {
            logger.info("About to call " + url + urlHolder.getCallerAddress() +
                        "with data:\n" + request);
        }
        urlHolder.setUrl(url);
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader(HttpSupport.HTTP_CONTENT_TYPE_HEADER, BaseProperties.JSON_CONTENT_TYPE);
        wrap.setHeader(HttpSupport.HTTP_ACCEPT_HEADER, BaseProperties.JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(portFilter(url), request.serializeToBytes(JSONOutputFormats.NORMALIZED));
        return fetchJsonReturnData(wrap, urlHolder);
    }

    JSONObjectReader getJsonData(UrlHolder urlHolder) throws IOException {
        if (logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.getCallerAddress());
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader(HttpSupport.HTTP_ACCEPT_HEADER, BaseProperties.JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makeGetRequest(portFilter(urlHolder.getUrl()));
        return fetchJsonReturnData(wrap, urlHolder);
    }

    public ProviderAuthorityDecoder getProviderAuthority(UrlHolder urlHolder, String url) 
            throws IOException, GeneralSecurityException {
        urlHolder.setUrl(url);
        ProviderAuthorityDecoder providerAuthority = providerAuthorityObjects.get(url);
        if (urlHolder.nonCachedMode() || // Note: clears nonCached flag as well
                providerAuthority == null || providerAuthority.expiresInMillis < System.currentTimeMillis()) {
            providerAuthority = new ProviderAuthorityDecoder(getJsonData(urlHolder), url);
            providerAuthorityObjects.put(url, providerAuthority);
            if (logging) {
                logger.info("Updated cache " + url);
            }
        } else {
            providerAuthority.cached = true;
            if (logging) {
                logger.info("Fetched from cache " + url);
            }
        }
        urlHolder.setUrl(null);
        return providerAuthority;
    }

    public PayeeAuthorityDecoder getPayeeAuthority(UrlHolder urlHolder, String url) 
            throws IOException, GeneralSecurityException {
        urlHolder.setUrl(url);
        PayeeAuthorityDecoder payeeAuthority = payeeAuthorityObjects.get(url);
        if (urlHolder.nonCachedMode() || // Note: clears nonCached flag as well
                payeeAuthority == null || payeeAuthority.expiresInMillis < System.currentTimeMillis()) {
            payeeAuthority = new PayeeAuthorityDecoder(getJsonData(urlHolder), url);
            payeeAuthorityObjects.put(url, payeeAuthority);
            if (logging) {
                logger.info("Updated cache " + url);
            }
        } else {
            payeeAuthority.cached = true;
            if (logging) {
                logger.info("Fetched from cache " + url);
            }
        }
        urlHolder.setUrl(null);
        return payeeAuthority;
    }
}
