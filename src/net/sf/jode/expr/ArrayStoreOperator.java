/* ArrayStoreOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ArrayStoreOperator.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.type.ArrayType;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class ArrayStoreOperator extends ArrayLoadOperator
    implements LValueExpression {

    public ArrayStoreOperator(Type type) {
	super(type);
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof ArrayLoadOperator;
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	Type arrType = subExpressions[0].getType().getHint();
	if (arrType instanceof ArrayType) {
	    Type elemType = ((ArrayType) arrType).getElementType();
	    if (!elemType.isOfType(getType())) {
		/* We need an explicit widening cast */
		writer.print("(");
		writer.startOp(TabbedPrintWriter.EXPL_PAREN, 1);
		writer.print("(");
		writer.printType(Type.tArray(getType().getHint()));
		writer.print(") ");
		writer.breakOp();
		subExpressions[0].dumpExpression(writer, 700);
		writer.print(")");
		writer.breakOp();
		writer.print("[");
		subExpressions[1].dumpExpression(writer, 0);
		writer.print("]");
		return;
	    }
	}
	super.dumpExpression(writer);
    }
}
