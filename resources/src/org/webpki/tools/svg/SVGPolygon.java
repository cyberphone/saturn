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

public class SVGPolygon extends SVGObject {

    double[] coordinates;
    SVGValue x;
    SVGValue y;
    
    class SVGPointsValue extends SVGValue {
 
        @Override
        public String getStringRepresentation() {
            int i = 0;
            StringBuilder result = new StringBuilder();
            while (i < coordinates.length) {
                if (i > 0) {
                    result.append(' ');
                }
                result.append(niceDouble(x.getDouble() + coordinates[i++]))
                      .append(',')
                      .append(niceDouble(y.getDouble() + coordinates[i++]));
            }
            return result.toString();
        }
    }
    
    public SVGPolygon(SVGValue x,
                      SVGValue y,
                      double[] coordinates,
                      Double strokeWidth,
                      String strokeColor,
                      String fillColor) {
        if ((coordinates.length & 1) != 0) {
            throw new RuntimeException("Wrong number of points");
        }
        _addAttribute(SVGAttributes.POINTS, new SVGPointsValue());
        processColor(strokeWidth, strokeColor, fillColor);
        this.coordinates = coordinates;
        for (int q = 0; q < coordinates.length; q++) {
            if (coordinates[q] < 0) {
                throw new RuntimeException("Polygon data must be >= 0");
            }
        }
        this.x = x;
        this.y = y;
      }
    
    private static double getMax(int start, double[] coordinates) {
        double max = coordinates[start];
        for (int q = start; q < coordinates.length; q += 2) {
            if (coordinates[q] > max) {
                max = coordinates[q];
            }
        }
        return max;
    }
    
    private double getMax(int start) {
        return SVGPolygon.getMax(start, coordinates);
    }

    public SVGPolygon(SVGAnchor anchor,
                      double[] coordinates,
                      Double strokeWidth,
                      String strokeColor,
                      String fillColor) {
        this(anchor.xAlignment(new SVGDoubleValue(SVGPolygon.getMax(0, coordinates))),
             anchor.yAlignment(new SVGDoubleValue(SVGPolygon.getMax(1, coordinates))),
             coordinates,
             strokeWidth,
             strokeColor,
             fillColor);
    }

    @Override
    String getTag() {
        return "polygon";
    }

    @Override
    boolean hasBody() {
        return false;
    }

    @Override
    double getMaxX() {
        x = new SVGAddOffset(x, SVGDocument.marginX);
        return x.getDouble() + getMax(0);
    }

    @Override
    double getMaxY() {
        y = new SVGAddOffset(y, SVGDocument.marginY);
        return y.getDouble() + getMax(1);
    }

    public SVGPolygon setShader(SVGShaderTemplate shading) {
        double xOffset = shading.xOffset;
        double yOffset = shading.yOffset;
        if (getAttribute(SVGAttributes.STROKE_WIDTH) != null) {
            double strokeWidth = getAttribute(SVGAttributes.STROKE_WIDTH).getDouble();
            xOffset -= strokeWidth / 2;
            yOffset -= strokeWidth / 2;
        }
        SVGPolygon temp = new SVGPolygon(new SVGAddOffset(x, xOffset),
                                         new SVGAddOffset(y, yOffset),
                                         coordinates,
                                         null,
                                         null,
                                         shading.fillColor);

        if (shading.filter != null) {
            temp.setFilter(shading.filter);
        }
        beforeDependencyElements.add(temp);
        return this;
    }

    public SVGPolygon setFilter(String filter) {
        addString(SVGAttributes.FILTER, new SVGStringValue(filter));
        return this;
    }    
}
