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

public class SVGEllipse extends SVGObject {
    
    public SVGEllipse(SVGValue x,
                      SVGValue y,
                      SVGValue width,
                      SVGValue height,
                      Double strokeWidth,
                      String strokeColor,
                      String fillColor) {
        SVGValue rx = new SVGDivConstant(width, 2);
        SVGValue ry = new SVGDivConstant(height, 2);
        addDouble(SVGAttributes.CX, new SVGAddDouble(x, rx));
        addDouble(SVGAttributes.CY, new SVGAddDouble(y, ry));
        addDouble(SVGAttributes.RX, rx);
        addDouble(SVGAttributes.RY, ry);
        processColor(strokeWidth, strokeColor, fillColor);
      }

    public SVGEllipse(SVGAnchor anchor,
                      SVGValue width,
                      SVGValue height,
                      Double strokeWidth,
                      String strokeColor,
                      String fillColor) {
        this(anchor.xAlignment(width),
             anchor.yAlignment(height),
             width,
             height,
             strokeWidth,
             strokeColor,
             fillColor);
    }

    @Override
    String getTag() {
        return "ellipse";
    }

    @Override
    boolean hasBody() {
        return false;
    }
    
    public SVGEllipse setFilter(String filter) {
        addString(SVGAttributes.FILTER, new SVGStringValue(filter));
        return this;
    }

    @Override
    public SVGValue getPrimaryX() {
        return new SVGSubtractDouble(getAttribute(SVGAttributes.CX), getAttribute(SVGAttributes.RX));
    }

    @Override
    public SVGValue getPrimaryY() {
        return new SVGSubtractDouble(getAttribute(SVGAttributes.CY), getAttribute(SVGAttributes.RY));
    }

    @Override
    public SVGValue getPrimaryWidth() {
        return new SVGAddDouble(getAttribute(SVGAttributes.RX), getAttribute(SVGAttributes.RX));
    }

    @Override
    public SVGValue getPrimaryHeight() {
        return new SVGAddDouble(getAttribute(SVGAttributes.RY), getAttribute(SVGAttributes.RY));
    }

    public SVGEllipse setShader (SVGShaderTemplate shading) {
        SVGValue rx = getAttribute(SVGAttributes.RX);
        SVGValue width = new SVGAddDouble(rx, rx);
        SVGValue ry = getAttribute(SVGAttributes.RY);
        SVGValue height = new SVGAddDouble(ry, ry);
        double xOffset = shading.xOffset;
        double yOffset = shading.yOffset;
        if (getAttribute(SVGAttributes.STROKE_WIDTH) != null) {
            double strokeWidth = getAttribute(SVGAttributes.STROKE_WIDTH).getDouble();
            width = new SVGAddOffset(width, strokeWidth);
            xOffset -= strokeWidth;
            yOffset -= strokeWidth;
        }
        SVGEllipse temp = new SVGEllipse(new SVGSubtractDouble(new SVGAddOffset(getAttribute(SVGAttributes.CX), xOffset), rx),
                                         new SVGSubtractDouble(new SVGAddOffset(getAttribute(SVGAttributes.CY), yOffset), ry),
                                         width,
                                         height,
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
        return updateMargin(SVGDocument.marginX, SVGAttributes.CX) + getAttribute(SVGAttributes.RX).getDouble();
    }

    @Override
    double getMaxY() {
        return updateMargin(SVGDocument.marginY, SVGAttributes.CY) + getAttribute(SVGAttributes.RY).getDouble();
    }

    public SVGEllipse setDashMode(double written, double empty) {
        addDashes(written, empty);
        return this;
    }
}
