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

public class SVGAddDouble extends SVGValue {
    
    SVGValue a;
    SVGValue b;
    double offset;
    
    public SVGAddDouble(SVGValue a, SVGValue b) {
        this(a, b, 0);
    }
    
    public SVGAddDouble(SVGValue a, SVGValue b, double offset) {
        this.a = a;
        this.b = b;
        this.offset = offset;
    }

    private double getValue() {
        return a.getDouble() + b.getDouble() + offset;
    }

    @Override
    public String getStringRepresentation() {
        return niceDouble(getValue());
    }

    @Override
    public double getDouble() {
        return getValue();
    }
}

