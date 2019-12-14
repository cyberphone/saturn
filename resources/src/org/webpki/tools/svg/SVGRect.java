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

public class SVGRect extends SVGObject {
    
    public SVGRect(SVGValue x,
                   SVGValue y,
                   SVGValue width,
                   SVGValue height,
                   Double strokeWidth,
                   String strokeColor,
                   String fillColor) {
        addDouble(SVGAttributes.X, x);
        addDouble(SVGAttributes.Y, y);
        addDouble(SVGAttributes.WIDTH, width);
        addDouble(SVGAttributes.HEIGHT, height);
        processColor(strokeWidth, strokeColor, fillColor);
    }

    public SVGRect(SVGAnchor anchor,
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
        return "rect";
    }

    @Override
    boolean hasBody() {
        return false;
    }
    
    public SVGRect setRadiusY(double value) {
        addDouble(SVGAttributes.RY, new SVGDoubleValue(value));
        return this;
    }

    public SVGRect setRadiusX(double value) {
        addDouble(SVGAttributes.RX, new SVGDoubleValue(value));
        return this;
    }

    public SVGRect setFilter(String filter) {
        addString(SVGAttributes.FILTER, new SVGStringValue(filter));
        return this;
    }

    @Override
    public SVGValue getPrimaryX() {
        return getAttribute(SVGAttributes.X);
    }

    @Override
    public SVGValue getPrimaryY() {
        return getAttribute(SVGAttributes.Y);
    }

    @Override
    public SVGValue getPrimaryWidth() {
        return getAttribute(SVGAttributes.WIDTH);
    }

    @Override
    public SVGValue getPrimaryHeight() {
        return getAttribute(SVGAttributes.HEIGHT);
    }

    public SVGRect setShader (SVGShaderTemplate shading) {
        SVGValue width = getAttribute(SVGAttributes.WIDTH);
        SVGValue height = getAttribute(SVGAttributes.HEIGHT);
        double xOffset = shading.xOffset;
        double yOffset = shading.yOffset;
        if (getAttribute(SVGAttributes.STROKE_WIDTH) != null) {
            double strokeWidth = getAttribute(SVGAttributes.STROKE_WIDTH).getDouble();
            height = new SVGAddOffset(height, strokeWidth);
            width = new SVGAddOffset(width, strokeWidth);
            xOffset -= strokeWidth / 2;
            yOffset -= strokeWidth / 2;
        }
        SVGRect temp = new SVGRect(new SVGAddOffset(getAttribute(SVGAttributes.X), xOffset),
                                   new SVGAddOffset(getAttribute(SVGAttributes.Y), yOffset),
                                   width,
                                   height,
                                   null,
                                   null,
                                   shading.fillColor);
        if (getAttribute(SVGAttributes.RX) != null) {
            temp.setRadiusX(getAttribute(SVGAttributes.RX).getDouble());
        }
        if (getAttribute(SVGAttributes.RY) != null) {
            temp.setRadiusY(getAttribute(SVGAttributes.RY).getDouble());
        }
        if (shading.filter != null) {
            temp.setFilter(shading.filter);
        }
        beforeDependencyElements.add(temp);
        return this;
    }

    @Override
    double getMaxX() {
        return updateMargin(SVGDocument.marginX, SVGAttributes.X) + getAttribute(SVGAttributes.WIDTH).getDouble();
    }

    @Override
    double getMaxY() {
        return updateMargin(SVGDocument.marginY, SVGAttributes.Y) + getAttribute(SVGAttributes.HEIGHT).getDouble();
    }

    public SVGRect setDashMode(double written, double empty) {
        addDashes(written, empty);
        return this;
    }
    
    public SVGRect setLink(String url, String toolTip, Boolean replace) {
        _setLink(url, toolTip, replace);
        return this;
    }

    public SVGRect endLink() {
        _endLink();
        return this;
    }

    public SVGRect addLeftText(double leftMargin,
                               double topMargin,
                               String fontFamily,
                               double fontSize,
                               String text) {
        afterDependencyElements.add(new SVGText(new SVGAddOffset(getAttribute(SVGAttributes.X),
                                                leftMargin),
                                    new SVGAddOffset(getAttribute(SVGAttributes.Y),
                                                                  topMargin),
                                    fontFamily,
                                    fontSize,
                                    SVGText.TEXT_ANCHOR.START,
                                    text));
        return this;
    }

    public SVGRect addCenterText(double topMargin,
                                 String fontFamily,
                                 double fontSize,
                                 String text) {
        afterDependencyElements.add(new SVGText(
                new SVGAddDouble(getAttribute(SVGAttributes.X),
                                 new SVGDivConstant(getAttribute(SVGAttributes.WIDTH), 2)),
                new SVGAddOffset(new SVGAddDouble(getAttribute(SVGAttributes.Y),
                                 new SVGDivConstant(getAttribute(SVGAttributes.HEIGHT), 2)), topMargin),
                                                fontFamily,
                                                fontSize,
                                                SVGText.TEXT_ANCHOR.MIDDLE,
                                                text).setDy("0.35em"));
        return this;
    }
}
