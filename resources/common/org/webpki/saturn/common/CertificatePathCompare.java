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

import java.security.cert.X509Certificate;

public class CertificatePathCompare {
    
    private CertificatePathCompare() {}
    
    private static void assertTrue(boolean assertion) throws IOException {
        if (!assertion) {
            throw new IOException("Outer and inner certificate paths differ");
        }
    }

    public static void compareCertificatePaths(X509Certificate[] outer, X509Certificate[] inner) throws IOException {
        assertTrue(inner.length == outer.length);
        for (int q = 0; q < inner.length; q++) {
            assertTrue(outer[q].equals(inner[q]));
        }
    }
}
