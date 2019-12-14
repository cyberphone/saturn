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

public class SVGPath extends SVGObject {

    SVGValue x;
    SVGValue y;
    SVGPathValues path;
    
    public SVGPath(SVGValue x,
                   SVGValue y,
                   SVGPathValues path,
                   Double strokeWidth,
                   String strokeColor,
                   String fillColor) {
        processColor(strokeWidth, strokeColor, fillColor);
        _addAttribute(SVGAttributes.D, path);
        path.parent = this;
        this.path = path;
        this.x = x;
        this.y = y;
    }

    public SVGPath(SVGAnchor anchor,
                   SVGPathValues path,
                   Double strokeWidth,
                   String strokeColor,
                   String fillColor) {
        this(anchor.xAlignment(new SVGDoubleValue(path.maxX - path.minX)),
             anchor.yAlignment(new SVGDoubleValue(path.maxY - path.minY)),
             path,
             strokeWidth,
             strokeColor,
             fillColor);
    }
    

    @Override
    String getTag() {
        return "path";
    }

    @Override
    boolean hasBody() {
        return false;
    }
    
    public SVGPath setFilter(String filter) {
        addString(SVGAttributes.FILTER, new SVGStringValue(filter));
        return this;
    }

    @Override
    double getMaxX() {
        if (x.getDouble() + path.minX < 0) {
//            throw new RuntimeException("SVGPath X negative!");
        }
        x = new SVGAddOffset(x, SVGDocument.marginX);
        return x.getDouble() + path.maxX;
    }

    @Override
    double getMaxY() {
        if (y.getDouble() + path.minY < 0) {
  //          throw new RuntimeException("SVGPath Y negative!");
        }
        y = new SVGAddOffset(y, SVGDocument.marginY);
        return y.getDouble() + path.maxY;
    }

    public SVGPath setDashMode(double written, double empty) {
        addDashes(written, empty);
        return this;
    }

    public SVGPath setRoundLineCap() {
        _setRoundLineCap();
        return this;
    }

    public SVGPath setFillOpacity(double opacity) {
        _setFillOpacity(opacity);
        return this;
    }
}
