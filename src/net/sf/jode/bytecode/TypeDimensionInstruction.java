/* TypeDimensionInstruction Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: TypeDimensionInstruction.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;

/**
 * This class represents an opc_multianewarray instruction.
 *
 */
class TypeDimensionInstruction extends TypeInstruction {
    /**
     * The dimension of this multianewarray operation.
     */
    private int dimension;

    public TypeDimensionInstruction(int opcode, String type, int dimension) {
	super(opcode, type);
	this.dimension = dimension;
    }

    /**
     * Get the dimensions for an opc_anewarray opcode.
     */
    public final int getDimensions()
    {
	return dimension;
    }

    /**
     * Get the dimensions for an opc_anewarray opcode.
     */
    public final void setDimensions(int dim)
    {
	dimension = dim;
    }

    /**
     * This returns the number of stack entries this instruction
     * pushes and pops from the stack.  The result fills the given
     * array.
     *
     * @param poppush an array of two ints.  The first element will
     * get the number of pops, the second the number of pushes.  
     */
    public void getStackPopPush(int[] poppush)
    /*{ require { poppush != null && poppush.length == 2
        :: "poppush must be an array of two ints" } } */
    {
	poppush[0] = dimension;
	poppush[1] = 1;
    }

    public String toString() {
	return super.toString()+' '+dimension;
    }
}
