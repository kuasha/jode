/* ConstantInstruction Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ConstantInstruction.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;
import net.sf.jode.util.StringQuoter;

/**
 * This class represents an instruction in the byte code.
 *
 */
class ConstantInstruction extends Instruction {
    /**
     * The typesignature of the class/array.
     */
    private Object constant;

    ConstantInstruction(int opcode, Object constant) {
	super(opcode);
	this.constant = constant;
    }

    public final Object getConstant() 
    {
	return constant;
    }

    public final void setConstant(Object constant) 
    {
	this.constant = constant;
    }

    public String toString() {
	return super.toString() + ' ' +
	    (constant instanceof String
	     ? StringQuoter.quote((String) constant) : constant);
    }
}
