/* CompareToIntOperator Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: CompareToIntOperator.java 1378 2004-02-04 19:01:35Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class CompareToIntOperator extends Operator {
    boolean allowsNaN;
    boolean greaterOnNaN;
    Type compareType;

    public CompareToIntOperator(Type type, boolean greaterOnNaN) {
        super(Type.tInt, 0);
        compareType = type;
	this.allowsNaN = (type == Type.tFloat || type == Type.tDouble);
	this.greaterOnNaN = greaterOnNaN;
	initOperands(2);
    }

    public int getPriority() {
        return 499;
    }

    public void updateSubTypes() {
	subExpressions[0].setType(Type.tSubType(compareType));
	subExpressions[1].setType(Type.tSubType(compareType));
    }

    public void updateType() {
    }

    public boolean opEquals(Operator o) {
	return (o instanceof CompareToIntOperator);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException
    {
        subExpressions[0].dumpExpression(writer, 550);
	writer.breakOp();
	writer.print(" <=>");
	if (allowsNaN)
	    writer.print(greaterOnNaN ? "g" : "l");
	writer.print(" ");
        subExpressions[1].dumpExpression(writer, 551);
    }
}
