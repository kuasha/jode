/* GetFieldOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: GetFieldOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.bytecode.Reference;
import net.sf.jode.decompiler.MethodAnalyzer;
import net.sf.jode.decompiler.FieldAnalyzer;

public class GetFieldOperator extends FieldOperator {
    public GetFieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			    Reference ref) {
        super(methodAnalyzer, staticFlag, ref);
    }

    public Expression simplify() {
	if (!staticFlag) {
	    subExpressions[0] = subExpressions[0].simplify();
	    subExpressions[0].parent = this;
	    if (subExpressions[0] instanceof ThisOperator) {
		FieldAnalyzer field = getField();
		/* This should check for isFinal(), but sadly,
		 * sometimes jikes doesn't make a val$ field final.  I
		 * don't know when, or why, so I currently ignore
		 * isFinal.  
		 */
		if (field != null && field.isSynthetic()) {
		    Expression constant = field.getConstant();
		    if (constant instanceof ThisOperator
			|| constant instanceof OuterLocalOperator)
			return constant;
		}
	    }
	}
	return this;
    }

    public boolean opEquals(Operator o) {
	return o instanceof GetFieldOperator
	    && ((GetFieldOperator)o).ref.equals(ref);
    }
}
