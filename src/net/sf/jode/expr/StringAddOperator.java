/* StringAddOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: StringAddOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class StringAddOperator extends Operator {
    protected Type operandType;

    public StringAddOperator() {
        super(Type.tString, ADD_OP);
	initOperands(2);
    }

    public int getPriority() {
        return 610;
    }

    public boolean opEquals(Operator o) {
	return (o instanceof StringAddOperator);
    }

    public void updateSubTypes() {
	/* A string add allows everything */
    }

    public void updateType() {
    }
    

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {

	if (!subExpressions[0].getType().isOfType(Type.tString)
	    && !subExpressions[1].getType().isOfType(Type.tString)) {
	    writer.print("\"\"");
	    writer.breakOp();
	    writer.print(getOperatorString());
	}

	subExpressions[0].dumpExpression(writer, 610);
	writer.breakOp();
	writer.print(getOperatorString());
	subExpressions[1].dumpExpression(writer, 611);
    }
}

