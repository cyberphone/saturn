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

import javax.servlet.http.HttpServletRequest;


// The purpose of this class is to enable URL information in exceptions

public class UrlHolder {
    String callerAddress;
    boolean nonCached;
    
    public UrlHolder(HttpServletRequest request) {
        callerAddress = " [Origin=" + request.getRemoteAddr() + ", Context=" + request.getContextPath() + "] ";
    }

    private String url;

    public String getUrl() {
        return url;
    }

    public void setNonCachedMode(boolean nonCached) {
        this.nonCached = nonCached;
    }

    public boolean nonCachedMode() {
        boolean temp = nonCached;
        nonCached = false;
        return temp;
    }

    public UrlHolder setUrl(String url) {
        this.url = url;
        return this;
    }
    
    public String getCallerAddress() {
        return callerAddress;
    }
}
    
