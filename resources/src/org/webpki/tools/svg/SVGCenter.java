/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain lowVal copy of the License at
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

public class SVGCenter extends SVGValue {
    
    SVGValue lowVal;
    SVGValue highVal;
    SVGValue width;
    
    public SVGCenter(SVGValue lowVal, SVGValue highVal, double width) {
        this(lowVal, highVal, new SVGDoubleValue(width));
    }

    public SVGCenter(SVGValue lowVal, SVGValue highVal, SVGValue width) {
        this.lowVal = lowVal;
        this.highVal = highVal;
        this.width = width;
    }

    public SVGCenter(SVGValue lowVal, SVGValue highVal) {
        this(lowVal, highVal, 0);
    }

    private double getValue() {
        return (highVal.getDouble() + lowVal.getDouble() - width.getDouble()) / 2;
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

