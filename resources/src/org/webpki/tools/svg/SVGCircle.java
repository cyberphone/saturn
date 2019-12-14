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

public class SVGCircle extends SVGObject {
    
    public SVGCircle(SVGValue x,
                     SVGValue y,
                     SVGValue width,
                     Double strokeWidth,
                     String strokeColor,
                     String fillColor) {
        SVGValue radius = new SVGDivConstant(width, 2);
        addDouble(SVGAttributes.CX, new SVGAddDouble(x, radius));
        addDouble(SVGAttributes.CY, new SVGAddDouble(y, radius));
        addDouble(SVGAttributes.R, radius);
        processColor(strokeWidth, strokeColor, fillColor);
      }

    public SVGCircle(SVGAnchor anchor,
                     SVGValue width,
                     Double strokeWidth,
                     String strokeColor,
                     String fillColor) {
        this(anchor.xAlignment(width),
             anchor.yAlignment(width),
             width,
             strokeWidth,
             strokeColor,
             fillColor);
    }

    @Override
    String getTag() {
        return "circle";
    }

    @Override
    boolean hasBody() {
        return false;
    }
    
    public SVGCircle setFilter(String filter) {
        addString(SVGAttributes.FILTER, new SVGStringValue(filter));
        return this;
    }

    @Override
    public SVGValue getPrimaryX() {
        return new SVGSubtractDouble(getAttribute(SVGAttributes.CX), getAttribute(SVGAttributes.R));
    }

    @Override
    public SVGValue getPrimaryY() {
        return new SVGSubtractDouble(getAttribute(SVGAttributes.CY), getAttribute(SVGAttributes.R));
    }

    @Override
    public SVGValue getPrimaryWidth() {
        return new SVGAddDouble(getAttribute(SVGAttributes.R), getAttribute(SVGAttributes.R));
    }

    @Override
    public SVGValue getPrimaryHeight() {
        return getPrimaryWidth();
    }

    public SVGCircle setShader (SVGShaderTemplate shading) {
        SVGValue radius = getAttribute(SVGAttributes.R);
        SVGValue width = new SVGAddDouble(radius, radius);
        double xOffset = shading.xOffset;
        double yOffset = shading.yOffset;
        if (getAttribute(SVGAttributes.STROKE_WIDTH) != null) {
            double strokeWidth = getAttribute(SVGAttributes.STROKE_WIDTH).getDouble();
            width = new SVGAddOffset(width, strokeWidth);
            xOffset -= strokeWidth;
            yOffset -= strokeWidth;
        }
        SVGCircle temp = new SVGCircle(new SVGSubtractDouble(new SVGAddOffset(getAttribute(SVGAttributes.CX), xOffset), radius),
                                       new SVGSubtractDouble(new SVGAddOffset(getAttribute(SVGAttributes.CY), yOffset), radius),
                                       width,
                                       null,
                                       null,
                                       shading.fillColor);
        if (shading.filter != null) {
            temp.setFilter(shading.filter);
        }
        beforeDependencyElements.add(temp);
        return this;
    }

    @Override
    double getMaxX() {
        return updateMargin(SVGDocument.marginX, SVGAttributes.CX) + getAttribute(SVGAttributes.R).getDouble();
    }

    @Override
    double getMaxY() {
        return updateMargin(SVGDocument.marginY, SVGAttributes.CY) + getAttribute(SVGAttributes.R).getDouble();
    }

    public SVGCircle setLink(String url, String toolTip, boolean replace) {
        _setLink(url, toolTip, replace);
        return this;
    }
}
