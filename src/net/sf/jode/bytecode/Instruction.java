/* Instruction Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: Instruction.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;

/**
 * <p> This class represents an instruction in the byte code.
 * Instructions can be created with the static {@link #forOpcode}
 * methods. </p>
 *
 * <p> We only allow a subset of opcodes.  Other opcodes are mapped to
 * their simpler version.  Don't worry about this, when writing the
 * bytecode the shortest possible bytecode is produced. </p>
 *
 * The opcodes we map are:
 * <pre>
 * [iflda]load_x           -&gt; [iflda]load
 * [iflda]store_x          -&gt; [iflda]store
 * [ifa]const_xx, ldc_w    -&gt; ldc
 * [dl]const_xx            -&gt; ldc2_w
 * wide opcode             -&gt; opcode
 * tableswitch             -&gt; lookupswitch
 * [a]newarray             -&gt; multianewarray
 * </pre> 
 */
public class Instruction implements Opcodes{
    /**
     * The opcode and lineNr of the instruction.  
     * opcode is <code>(lineAndOpcode &amp; 0xff)</code>, while 
     * lineNr is <code>(lineAndOpcode &gt;&gt; 8)</code>.
     * If line number is not known or unset, it is -1.
     */
    private int lineAndOpcode;

    /**
     * Creates a new simple Instruction with no parameters.  We map
     * some opcodes, so you must always use the mapped opcode.
     * @param opcode the opcode of this instruction.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode needs a parameter.  */
    public static Instruction forOpcode(int opcode) {
	switch (opcode) {
	case opc_nop:
	case opc_iaload: case opc_laload: case opc_faload:
	case opc_daload: case opc_aaload:
	case opc_baload: case opc_caload: case opc_saload:
	case opc_iastore: case opc_lastore: case opc_fastore:
	case opc_dastore: case opc_aastore:
	case opc_bastore: case opc_castore: case opc_sastore:
	case opc_pop: case opc_pop2:
	case opc_dup: case opc_dup_x1: case opc_dup_x2:
	case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
	case opc_swap:
	case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
	case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
	case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
	case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
	case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
	case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
	case opc_ishl: case opc_lshl:
	case opc_ishr: case opc_lshr:
	case opc_iushr: case opc_lushr: 
	case opc_iand: case opc_land:
	case opc_ior: case opc_lor: 
	case opc_ixor: case opc_lxor:
	case opc_i2l: case opc_i2f: case opc_i2d:
	case opc_l2i: case opc_l2f: case opc_l2d:
	case opc_f2i: case opc_f2l: case opc_f2d:
	case opc_d2i: case opc_d2l: case opc_d2f:
	case opc_i2b: case opc_i2c: case opc_i2s:
	case opc_lcmp: case opc_fcmpl: case opc_fcmpg:
	case opc_dcmpl: case opc_dcmpg:
	case opc_ireturn: case opc_lreturn: 
	case opc_freturn: case opc_dreturn: case opc_areturn:
	case opc_return: 
	case opc_athrow:
	case opc_arraylength:
	case opc_monitorenter: case opc_monitorexit:
	case opc_goto:
	case opc_jsr:
	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple:
	case opc_if_acmpeq: case opc_if_acmpne:
	case opc_ifnull: case opc_ifnonnull:
	    return new Instruction(opcode);
	default:
	    throw new IllegalArgumentException("Instruction has a parameter");
	}
    }


    /**
     * Creates a new ldc Instruction.
     * @param opcode the opcode of this instruction.
     * @param constant the constant parameter.
     * @exception IllegalArgumentException if opcode is not opc_ldc or
     * opc_ldc2_w.
     */
    public static Instruction forOpcode(int opcode, Object constant) {
	if (opcode == opc_ldc || opcode == opc_ldc2_w)
	    return new ConstantInstruction(opcode, constant);
	throw new IllegalArgumentException("Instruction has no constant");
    }

    /**
     * Creates a new Instruction with a local variable as parameter.
     * @param opcode the opcode of this instruction.
     * @param lvi the local variable parameter.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode doesn't need a single local variable as parameter.
     */
    public static Instruction forOpcode(int opcode, LocalVariableInfo lvi) {
	if (opcode == opc_ret
	    || opcode >= opc_iload && opcode <= opc_aload
	    || opcode >= opc_istore && opcode <= opc_astore)
	    return new SlotInstruction(opcode, lvi);
	throw new IllegalArgumentException("Instruction has no slot");
    }

    /**
     * Creates a new Instruction with reference as parameter.
     * @param opcode the opcode of this instruction.
     * @param reference the reference parameter.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode doesn't need a reference as parameter.
     */
    public static Instruction forOpcode(int opcode, Reference reference) {
	if (opcode >= opc_getstatic && opcode <= opc_invokeinterface)
	    return new ReferenceInstruction(opcode, reference);
	throw new IllegalArgumentException("Instruction has no reference");
    }

    /**
     * Creates a new Instruction with type signature as parameter.
     * @param opcode the opcode of this instruction.
     * @param typeSig the type signature parameter.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode doesn't need a type signature as parameter.
     */
    public static Instruction forOpcode(int opcode, String typeSig) {
	switch (opcode) {
	case opc_new: 
	case opc_checkcast:
	case opc_instanceof:
	    return new TypeInstruction(opcode, typeSig);
	default:
	    throw new IllegalArgumentException("Instruction has no type");
	}
    }

    /**
     * Creates a new switch Instruction.
     * @param opcode the opcode of this instruction must be opc_lookupswitch.
     * @param values an array containing the different cases.
     * @exception IllegalArgumentException if opcode is not opc_lookupswitch.
     */
    public static Instruction forOpcode(int opcode, int[] values) {
	if (opcode == opc_lookupswitch)
	    return new SwitchInstruction(opcode, values);
	throw new IllegalArgumentException("Instruction has no values");
    }

    /**
     * Creates a new increment Instruction.
     * @param opcode the opcode of this instruction.
     * @param lvi the local variable parameter.
     * @param increment the increment parameter.
     * @exception IllegalArgumentException if opcode is not opc_iinc.
     */
    public static Instruction forOpcode(int opcode, 
					LocalVariableInfo lvi, int increment) {
	if (opcode == opc_iinc)
	    return new IncInstruction(opcode, lvi, increment);
	throw new IllegalArgumentException("Instruction has no increment");
    }

    /**
     * Creates a new Instruction with type signature and a dimension
     * as parameter.
     * @param opcode the opcode of this instruction.
     * @param typeSig the type signature parameter.
     * @param dimension the array dimension parameter.
     * @exception IllegalArgumentException if opcode is not
     * opc_multianewarray.  
     */
    public static Instruction forOpcode(int opcode, 
					String typeSig, int dimension) {
	if (opcode == opc_multianewarray)
	    return new TypeDimensionInstruction(opcode, typeSig, dimension);
	throw new IllegalArgumentException("Instruction has no dimension");
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    Instruction(int opcode) {
	this.lineAndOpcode = (-1 << 8) | opcode;
    }

    /**
     * Gets the opcode of the instruction.  
     * @return the opcode of the instruction.  
     */
    public final int getOpcode() {
	return lineAndOpcode & 0xff;
    }

    /**
     * Tells whether there is a line number information for this
     * instruction.
     * @return true if there is a line number information for this
     * instruction.
     */
    public final boolean hasLineNr() {
	return lineAndOpcode >= 0;
    }

    /**
     * Gets the line number of this instruction.
     * @return the line number, or -1 if there isn't one.
     */
    public final int getLineNr() {
	return lineAndOpcode >> 8;
    }

    /**
     * Sets the line number of this instruction.
     * @param nr the line number; use -1 to clear it.
     */
    public final void setLineNr(int nr) {
	lineAndOpcode = (nr << 8) | (lineAndOpcode & 0xff);
    }

    /**
     * Checks whether this instruction is a local store instruction, i.e.
     * one of <code>astore</code>, <code>istore</code>, <code>lstore</code>, 
     * <code>fstore</code> or <code>dstore</code>.
     */
    public boolean isStore() {
	return false;
    }

    /**
     * Checks whether this instruction accesses a local slot.
     */
    public boolean hasLocal() {
	return false;
    }
	    
    /**
     * Gets the slot number of the local this instruction accesses.
     * @return the slot number.
     * @throws IllegalArgumentException if this instruction doesn't
     * access a local slot.
     */
    public int getLocalSlot()
    {
	throw new IllegalArgumentException();
	// UnsupportedOperationException would be more appropriate
    }

    /**
     * Gets the information of the local this instruction accesses.
     * @return the local variable info.
     * @throws IllegalArgumentException if this instruction doesn't
     * access a local.
     */
    public LocalVariableInfo getLocalInfo()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the information of the local this instruction accesses.
     * @param info the local variable info.
     * @throws IllegalArgumentException if this instruction doesn't
     * access a local.
     */
    public void setLocalInfo(LocalVariableInfo info) 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the slot of the local this instruction accesses.
     * @param slot the local slot
     * @throws IllegalArgumentException if this instruction doesn't
     * access a local.
     */
    public void setLocalSlot(int slot) 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Gets the increment for an opc_iinc instruction.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public int getIncrement()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the increment for an opc_iinc instruction.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public void setIncrement(int incr)
    {
	throw new IllegalArgumentException();
    }

    /**
     * Gets the dimensions for an opc_multianewarray opcode.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public int getDimensions()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the dimensions for an opc_multianewarray opcode.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public void setDimensions(int dims)
    {
	throw new IllegalArgumentException();
    }

    /**
     * Gets the constant for a opc_ldc or opc_ldc2_w opcode.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public Object getConstant() 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the constant for a opc_ldc or opc_ldc2_w opcode.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public void setConstant(Object constant) 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Gets the reference of the field or method this instruction accesses.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public Reference getReference()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the reference of the field or method this instruction accesses.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public void setReference(Reference ref)
    {
	throw new IllegalArgumentException();
    }

    /**
     * Gets the class type this instruction uses, e.g if its a class cast.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public String getClazzType() 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the class type this instruction uses, e.g if its a class cast.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public void setClazzType(String type)
    {
	throw new IllegalArgumentException();
    }

    /**
     * Gets the values of a opc_lookupswitch opcode.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public int[] getValues()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Sets the values of a opc_lookupswitch opcode.
     * @throws IllegalArgumentException if this instruction doesn't
     * support this.
     */
    public void setValues(int[] values) 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Checks whether this instruction always changes program flow.
     * Returns false for opc_jsr it.
     * @return true if this instruction always changes flow, i.e. if
     * its an unconditional jump, a return, a throw, a ret or a switch.
     */
    public final boolean doesAlwaysJump() {
	switch (getOpcode()) {
	case opc_ret:
	case opc_goto:
	case opc_lookupswitch:
	case opc_ireturn: 
	case opc_lreturn: 
	case opc_freturn: 
	case opc_dreturn: 
	case opc_areturn:
	case opc_return: 
	case opc_athrow:
	    return true;
	default:
	    return false;
	}
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
	byte delta = (byte) stackDelta.charAt(getOpcode());
	poppush[0] = delta & 7;
	poppush[1] = delta >> 3;
    }

    /**
     * Gets a printable representation of the opcode with its
     * parameters.  This will not include the destination for jump
     * instructions, since this information is not stored inside the
     * instruction.  
     */
    public final String getDescription() {
	return toString();
    }

    /**
     * Gets a printable representation of the opcode with its
     * parameters.  This will not include the destination for jump
     * instructions, since this information is not stored inside the
     * instruction.  
     */
    public String toString() {
	return opcodeString[getOpcode()];
    }

    /**
     * stackDelta contains \100 if stack count of opcode is variable
     * \177 if opcode is illegal, or 8*stack_push + stack_pop otherwise
     * The string is created by scripts/createStackDelta.pl
     */
    final static String stackDelta = 
	"\000\010\010\010\010\010\010\010\010\020\020\010\010\010\020\020\010\010\010\010\020\010\020\010\020\010\010\010\010\010\020\020\020\020\010\010\010\010\020\020\020\020\010\010\010\010\012\022\012\022\012\012\012\012\001\002\001\002\001\001\001\001\001\002\002\002\002\001\001\001\001\002\002\002\002\001\001\001\001\003\004\003\004\003\003\003\003\001\002\021\032\043\042\053\064\022\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\011\022\011\022\012\023\012\023\012\023\012\024\012\024\012\024\000\021\011\021\012\012\022\011\021\021\012\022\012\011\011\011\014\012\012\014\014\001\001\001\001\001\001\002\002\002\002\002\002\002\002\000\000\000\001\001\001\002\001\002\001\000\100\100\100\100\100\100\100\100\177\010\011\011\011\001\011\011\001\001\177\100\001\001\000\000";
}
