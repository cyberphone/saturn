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

public class SVGLine extends SVGObject {
    
    public SVGLine(SVGValue x1,
                   SVGValue y1,
                   SVGValue x2,
                   SVGValue y2,
                   Double strokeWidth,
                   String strokeColor) {
        addDouble(SVGAttributes.X1, x1);
        addDouble(SVGAttributes.Y1, y1);
        addDouble(SVGAttributes.X2, x2);
        addDouble(SVGAttributes.Y2, y2);
        addDouble(SVGAttributes.STROKE_WIDTH, SVGDoubleValue.parse(strokeWidth));
        addString(SVGAttributes.STROKE_COLOR, SVGStringValue.parse(strokeColor));
    }

    @Override
    String getTag() {
        return "line";
    }

    @Override
    boolean hasBody() {
        return false;
    }

    @Override
    double getMaxX() {
        double x1 = updateMargin(SVGDocument.marginX, SVGAttributes.X1);
        double x2 = updateMargin(SVGDocument.marginX, SVGAttributes.X2);
        return x1 > x2 ? x1 : x2;
    }

    @Override
    double getMaxY() {
        double y1 = updateMargin(SVGDocument.marginY, SVGAttributes.Y1);
        double y2 = updateMargin(SVGDocument.marginY, SVGAttributes.Y2);
        return y1 > y2 ? y1 : y2;
    }
    
    public SVGLine setDashMode(double written, double empty) {
        addDashes(written, empty);
        return this;
    }
}
