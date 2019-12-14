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
package org.webpki.tools.svg;

public abstract class SVGValue {
    
    public abstract String getStringRepresentation();
    
    public double getDouble() {
        throw new RuntimeException("getDouble() invalid for this type " + this.getClass().getCanonicalName());
    }

    public String getString() {
        throw new RuntimeException("getString() invalid for this type " + this.getClass().getCanonicalName());
    }

    String niceDouble(double value) {
        long lval = (long)(value * 100);
        value = ((double)lval) / 100;
        if (value == (long)value) {
            return Long.toString((long)value);
        }
        return Double.toString(value);
    }
}

