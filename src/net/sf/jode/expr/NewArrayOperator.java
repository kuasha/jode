/* NewArrayOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: NewArrayOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.type.ArrayType;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class NewArrayOperator extends Operator {
    String baseTypeString;

    public NewArrayOperator(Type arrayType, int dimensions) {
        super(arrayType, 0);
	initOperands(dimensions);
    }

    public int getDimensions() {
	return subExpressions.length;
    }

    public int getPriority() {
        return 900;
    }

    public void updateSubTypes() {
        for (int i=0; i< subExpressions.length; i++)
            subExpressions[i].setType(Type.tUInt);
    }

    public void updateType() {
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
        Type flat = type.getCanonic();
	int depth = 0;
        while (flat instanceof ArrayType) {
            flat = ((ArrayType)flat).getElementType();
	    depth++;
        }
	writer.print("new ");
	writer.printType(flat.getHint());
	for (int i=0; i< depth; i++) {
	    writer.breakOp();
	    writer.print("[");
            if (i < subExpressions.length)
		subExpressions[i].dumpExpression(writer, 0);
	    writer.print("]");
	}
    }
}
