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

import java.util.LinkedHashMap;
import java.util.ArrayList;

public abstract class SVGObject {
    
    class SVGDashes extends SVGValue {

        double written;
        double empty;
       
        public SVGDashes(double written, double empty) {
            this.written = written;
            this.empty = empty;
        }
        
        @Override
        public String getStringRepresentation() {
            return niceDouble(written) + "," + niceDouble(empty);
        }
    };

    private LinkedHashMap<SVGAttributes,SVGValue> _attributes = new LinkedHashMap<>();
    
    ArrayList<SVGObject> beforeDependencyElements = new ArrayList<>();

    ArrayList<SVGObject> afterDependencyElements = new ArrayList<>();
    
    boolean invisible;
    
    String linkUrl;
    
    Boolean linkReplace;
    
    boolean endLink;
    
    String linkToolTip;
    
    abstract String getTag();
    
    abstract double getMaxX();

    abstract double getMaxY();

    abstract boolean hasBody();
    
    String getBody() {
        throw new RuntimeException("Unexptected call to getBody() by " + this.getClass().getCanonicalName());
    }
    
    LinkedHashMap<SVGAttributes,SVGValue> getAttributes() {
        return _attributes;
    }
    
    SVGValue getAttribute(SVGAttributes attribute) {
        return _attributes.get(attribute);
    }
    
    void _addAttribute(SVGAttributes svgAttribute, SVGValue svgValue) {
        if (_attributes.put(svgAttribute, svgValue) != null) {
            throw new RuntimeException("Trying to assign attribute multiple times: " + svgAttribute);
        }
    }
    
    void addDouble(SVGAttributes svgAttribute, SVGValue svgValue) {
        if (svgValue != null) {
            svgValue.getDouble(); // For type checking
            _addAttribute(svgAttribute, svgValue);
        }
    }


    public SVGObject addString(SVGAttributes svgAttribute, SVGValue svgValue) {
        if (svgValue != null) {
            svgValue.getString(); // For type checking
            _addAttribute(svgAttribute, svgValue);
        }
        return this;
    }

    void addDashes(double written, double empty) {
        _addAttribute(SVGAttributes.STROKE_DASHES, new SVGDashes(written, empty));
    }

    public LinkedHashMap<SVGAttributes,SVGValue> getSVGAttributes() {
         return _attributes;
    }

    public SVGValue getPrimaryX() {
        throw new RuntimeException ("Unimplemented: getPrimaryX()");
    }

    public SVGValue getPrimaryY() {
        throw new RuntimeException ("Unimplemented: getPrimaryY()");
    }

    public SVGValue getPrimaryWidth() {
        throw new RuntimeException ("Unimplemented: getPrimaryWidth()");
    }

    public SVGValue getPrimaryHeight() {
        throw new RuntimeException ("Unimplemented: getPrimaryHeight()");
    }
    
    public double updateMargin(double margin, SVGAttributes svgAttribute) {
        double value = getAttribute(svgAttribute).getDouble() + margin;
        _attributes.put(svgAttribute, new SVGDoubleValue(value));
        return value;
    }
    
    void processColor(Double strokeWidth, String strokeColor, String fillColor) {
        if (strokeWidth == null ^ strokeColor == null) {
            throw new RuntimeException("You must either specify color+stroke or nulls");
        }
        if (strokeColor != null) {
            addString(SVGAttributes.STROKE_COLOR, new SVGStringValue(strokeColor));
            addDouble(SVGAttributes.STROKE_WIDTH, new SVGDoubleValue(strokeWidth));
        }
        addString(SVGAttributes.FILL_COLOR, fillColor == null ? 
                                   new SVGStringValue("none") : new SVGStringValue(fillColor));
    }
    
    void _setLink(String url, String toolTip, Boolean replace) {
        linkUrl = url;
        linkReplace = replace;
        linkToolTip = toolTip;
        SVGDocument.linksUsed = true;
    }
    
    void _endLink() {
        endLink = true;
    }

    void _setRoundLineCap() {
        _attributes.put(SVGAttributes.STROKE_LINECAP, new SVGStringValue("round"));
    }
    
    public void addAfterObject(SVGObject svgObject) {
        afterDependencyElements.add(svgObject);
    }

    public void _setFillOpacity(double value) {
        _attributes.put(SVGAttributes.FILL_OPACITY, new SVGDoubleValue(value));
    }

    public void setVisibility(boolean visibility) {
        invisible = !visibility;
    }
}
