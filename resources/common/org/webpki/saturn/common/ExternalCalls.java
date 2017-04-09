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
package org.webpki.saturn.common;

import java.io.IOException;

import java.net.URL;

import java.util.Collections;
import java.util.GregorianCalendar;
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

    static final int TIMEOUT_FOR_REQUEST           = 5000;
    
    // Authority object caches
    Map<String,PayeeAuthority> payeeAuthorityObjects = Collections.synchronizedMap(new LinkedHashMap<String,PayeeAuthority>());

    Map<String,ProviderAuthority> providerAuthorityObjects = Collections.synchronizedMap(new LinkedHashMap<String,ProviderAuthority>());

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
            throw new IOException("Content-Type must be \"" + BaseProperties.JSON_CONTENT_TYPE + "\" , found: " + wrap.getRawContentType());
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
        JSONObjectReader json = fetchJsonReturnData(wrap, urlHolder);
        urlHolder.setUrl(null);
        return json;
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

    public ProviderAuthority getProviderAuthority(UrlHolder urlHolder, String url) throws IOException {
        urlHolder.setUrl(url);
        ProviderAuthority providerAuthority = providerAuthorityObjects.get(url);
        if (urlHolder.nonCachedMode() || // Note: clears nonCached flag as well
                providerAuthority == null || providerAuthority.getExpires().before(new GregorianCalendar())) {
            providerAuthority = new ProviderAuthority(getJsonData(urlHolder), url);
            providerAuthorityObjects.put(url, providerAuthority);
            if (logging) {
                logger.info("Updated cache " + url);
            }
        } else {
            if (logging) {
                logger.info("Fetched from cache " + url);
            }
        }
        urlHolder.setUrl(null);
        return providerAuthority;
    }

    public PayeeAuthority getPayeeAuthority(UrlHolder urlHolder, String url) throws IOException {
        urlHolder.setUrl(url);
        PayeeAuthority payeeAuthority = payeeAuthorityObjects.get(url);
        if (urlHolder.nonCachedMode() || // Note: clears nonCached flag as well
                payeeAuthority == null || payeeAuthority.getExpires().before(new GregorianCalendar())) {
            payeeAuthority = new PayeeAuthority(getJsonData(urlHolder), url);
            payeeAuthorityObjects.put(url, payeeAuthority);
            if (logging) {
                logger.info("Updated cache " + url);
            }
        } else {
            if (logging) {
                logger.info("Fetched from cache " + url);
            }
        }
        urlHolder.setUrl(null);
        return payeeAuthority;
    }
}
