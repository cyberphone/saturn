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

import java.util.ArrayList;

public class SVGPathValues extends SVGValue {

    double maxX =-10000;
    double maxY =-10000;
    
    double minX = 10000;
    double minY = 10000;

    double currX;
    double currY;
    
    SVGPath parent;
    
    public SVGPathValues() {
        
    }
    
    public SVGPathValues(SVGPathValues masterPath) {
        minX = masterPath.minX;
        minY = masterPath.minY;
        maxX = masterPath.maxX;
        maxY = masterPath.maxY;
    }
    
    class Coordinate implements Cloneable {
        boolean absolute;
        boolean actualCoordinate;
        double xValue;
        double yValue;
        
        Coordinate (boolean absolute, boolean actualCoordinate, double xValue, double yValue) {
            this.absolute = absolute;
            this.xValue = xValue;
            this.yValue = yValue;
            if (actualCoordinate) {
                if (absolute) {
                    currX = xValue;
                    currY = yValue;
                } else {
                    currX += xValue;
                    currY += yValue;
                }
                if (currX > maxX) {
                    maxX = currX;
                } else if (currX < minX) {
                    minX = currX;
                }
                if (currY > maxY) {
                    maxY = currY;
                } else if (currY < minY) {
                    minY = currY;
                }
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    class SubCommand implements Cloneable {
        char command;
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        
        SubCommand (char command) {
            this.command = command;
        }
        
        SubCommand addCoordinate(boolean absolute, double x, double y) {
            coordinates.add(new Coordinate(absolute, true, x, y));
            return this;
        }

        SubCommand addNonCoordinate(boolean absolute, double x, double y) {
            coordinates.add(new Coordinate(absolute, false, x, y));
            return this;
        }

        public SubCommand copy() throws CloneNotSupportedException {
            SubCommand newSC = new SubCommand(command);
            for (Coordinate co : coordinates) {
                newSC.coordinates.add((Coordinate) co.clone());
            }
            return newSC;
        }
    }

    ArrayList<SubCommand> commands = new ArrayList<>();
    
    @Override
    public String getStringRepresentation() {
        StringBuilder result = new StringBuilder();
        char last = 0;
        for (SubCommand subCommand : commands) {
            if (last != subCommand.command) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(last = subCommand.command);
            }
            for (Coordinate coordinate : subCommand.coordinates) {
                double xValue = coordinate.xValue;
                double yValue = coordinate.yValue;
                if (coordinate.absolute) {
                    xValue += parent.x.getDouble();
                    yValue += parent.y.getDouble();
                }
                result.append(' ')
                      .append(niceDouble(xValue))
                      .append(',')
                      .append(niceDouble(yValue));
            }
        }
        return result.toString();
    }
    
    void addSubCommand(SubCommand subCommand) {
        commands.add(subCommand);
    }
    
    public SVGPathValues moveRelative(double x, double y) {
        addSubCommand(new SubCommand('m').addCoordinate(false, x, y));
        return this;
    }

    public SVGPathValues moveAbsolute(double x, double y) {
        addSubCommand(new SubCommand('M').addCoordinate(true, x, y));
        return this;
    }

    public SVGPathValues lineToRelative(double x, double y) {
        addSubCommand(new SubCommand('l').addCoordinate(false, x, y));
        return this;
    }

    public SVGPathValues lineToAbsolute(double x, double y) {
        addSubCommand(new SubCommand('L').addCoordinate(true, x, y));
        return this;
    }

    public SVGPathValues cubicBezierRelative(double c1x, double c1y, double c2x,double c2y, double x, double y) {
        addSubCommand(new SubCommand('c').addNonCoordinate(false, c1x, c1y)
                                         .addNonCoordinate(false, c2x, c2y)
                                         .addCoordinate(false, x, y));
        return this;
    }

    public SVGPathValues cubicBezierAbsolute(double c1x, double c1y, double c2x,double c2y, double x, double y) {
        addSubCommand(new SubCommand('C').addNonCoordinate(true, c1x, c1y)
                                         .addNonCoordinate(true, c2x, c2y)
                                         .addCoordinate(true, x, y));
        return this;
    }

    public SVGPathValues endPath() {
        addSubCommand(new SubCommand('z'));
        return this;
    }
}
