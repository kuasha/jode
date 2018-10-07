/* ReferenceInstruction Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ReferenceInstruction.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;

/**
 * This class represents an instruction that needs a reference, i.e.
 * a method invocation or field access instruction.
 */
class ReferenceInstruction extends Instruction {
    private Reference reference;

    public ReferenceInstruction(int opcode, Reference ref) {
	super(opcode);
	this.reference = ref;
    }

    public final Reference getReference()
    {
	return reference;
    }

    public final void setReference(Reference ref)
    {
	reference = ref;
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
	String typeSig = reference.getType();
	int opcode = getOpcode();
	switch (opcode) {
	case opc_invokevirtual:
	case opc_invokespecial:
	case opc_invokestatic:
	case opc_invokeinterface:
	    poppush[0] = opcode != opc_invokestatic ? 1 : 0;
	    poppush[0] += TypeSignature.getParameterSize(typeSig);
	    poppush[1] = TypeSignature.getReturnSize(typeSig);
	    break;
	
	case opc_putfield:
	case opc_putstatic:
	    poppush[1] = 0;
	    poppush[0] = TypeSignature.getTypeSize(typeSig);
	    if (opcode == opc_putfield)
		poppush[0]++;
	    break;

	case opc_getstatic:
	case opc_getfield:
	    poppush[1] = TypeSignature.getTypeSize(typeSig);
	    poppush[0] = opcode == opc_getfield ? 1 : 0;
	    break;
	}
    }
    public String toString() {
	return super.toString()+' '+reference;
    }
}
