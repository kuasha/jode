/* BasicBlockWriter Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: BasicBlockWriter.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Stack;
///#def COLLECTIONS java.util
import java.util.ArrayList;
///#enddef

/**
 * This is a helper class, that contains the method to write basic
 * blocks to a class file.
 */
class BasicBlockWriter implements Opcodes {

    private class LVTEntry {
	int startAddr, endAddr;
	LocalVariableInfo lvi;
    }

    BasicBlocks bb;
    int[] blockAddr;
    int[][] instrLength;

    int lntCount;
    short[] lnt;
    LVTEntry[] lvt;

    boolean retAtEnd;
    int lastRetAddr;

    BitSet isRet;
    BitSet isWide;
    BitSet isWideCond;

    public BasicBlockWriter(BasicBlocks bb, GrowableConstantPool gcp) {
	this.bb = bb;
	init(gcp);
	prepare(gcp);
    }

    public void buildNewLVT() {
	Block startBlock = bb.getStartBlock();
	Block[] blocks = bb.getBlocks();
	if (startBlock == null)
	    return;

	/* We begin with the first Instruction and follow program flow.
	 * We remember which locals are life at start of each block
	 * in atStart.
	 */
	LocalVariableInfo[][] atStart = 
	    new LocalVariableInfo[blocks.length][];
	int startBlockNr = startBlock.getBlockNr();
	atStart[startBlockNr] = new LocalVariableInfo[bb.getMaxLocals()];
	for (int i=0; i < bb.getParamCount(); i++) {
	    LocalVariableInfo lvi = bb.getParamInfo(i);
	    atStart[startBlockNr][i] = lvi.getName() != null ? lvi : null;
	}

	/* We currently ignore the jsr/ret issue.  Should be okay,
	 * though, since it can only generate a bit too much local
	 * information.  */
	Stack todo = new Stack();
	todo.push(startBlock);
	while (!todo.isEmpty()) {
	    Block block = (Block) todo.pop();
	    int blockNr = block.getBlockNr();
	    LocalVariableInfo[] life
		= (LocalVariableInfo[]) atStart[blockNr].clone();
	    Instruction[] instrs = block.getInstructions();
	    for (int i = 0; i < instrs.length; i++) {
		if (instrs[i].hasLocal()) {
		    LocalVariableInfo lvi = instrs[i].getLocalInfo();
		    int slot = lvi.getSlot();
		    life[slot] = lvi.getName() != null ? lvi : null;
		}
	    }
	    Block[] succs = block.getSuccs();
	    for (int j = 0; j < succs.length; j++) {
		if (succs[j] == null)
		    continue;
		int succNr = succs[j].getBlockNr();
		if (atStart[succNr] == null) {
		    atStart[succNr] = (LocalVariableInfo[]) life.clone();
		    todo.push(succs[j]);
		} else {
		    boolean changed = false;
		    for (int k = 0; k < life.length; k++) {
			if (atStart[succNr][k] != life[k]
			    && atStart[succNr][k] != null) {
			    atStart[succNr][k] = null;
			    changed = true;
			}
		    }
		    if (changed && !todo.contains(succs[j]))
			todo.push(succs[j]);
		}
	    }
	    Handler[] handlers = block.getHandlers();
	    for (int j = 0; j < handlers.length; j++) {
		int succNr = handlers[j].getCatcher().getBlockNr();
		if (atStart[succNr] == null) {
		    atStart[succNr] = (LocalVariableInfo[]) life.clone();
		    todo.push(handlers[j].getCatcher());
		} else {
		    boolean changed = false;
		    for (int k = 0; k < life.length; k++) {
			if (atStart[succNr][k] != life[k]
			    && atStart[succNr][k] != null) {
			    atStart[succNr][k] = null;
			    changed = true;
			}
		    }
		    if (changed && !todo.contains(handlers[j].getCatcher()))
			todo.push(handlers[j].getCatcher());
		}
	    }
	}

	ArrayList lvtEntries = new ArrayList();

	LVTEntry[] current = new LVTEntry[bb.getMaxLocals()];
	for (int slot=0; slot < bb.getParamCount(); slot++) {
	    LocalVariableInfo lvi = bb.getParamInfo(slot);
	    if (lvi.getName() != null) {
		current[slot] = new LVTEntry();
		current[slot].startAddr = 0;
		current[slot].lvi = lvi;
		System.err.println("lvi at init,"+slot+": "+lvi);
	    }
	}

	for (int i=0; i < blocks.length; i++) {
	    if (atStart[i] == null)
		// ignore unreachable blocks:
		continue;

	    Block block = blocks[i];
	    int addr = blockAddr[i];
	    for (int slot = 0; slot < current.length; slot++) {
		if (current[slot] != null
		    && current[slot].lvi != atStart[i][slot]) {
		    current[slot].endAddr = addr;
		    lvtEntries.add(current[slot]);
		    current[slot] = null;
		}
		if (current[slot] == null && atStart[i][slot] != null) {
		    current[slot] = new LVTEntry();
		    current[slot].startAddr = addr;
		    current[slot].lvi = atStart[i][slot];
		    System.err.println("lvi at "+i+","+slot+": "+current[slot].lvi);
		}
	    }

	    Instruction[] instrs = block.getInstructions();
	    for (int k = 0; k < instrs.length; k++) {
		Instruction instr = instrs[k];
		if (instr.hasLocal()) {
		    LocalVariableInfo lvi = instr.getLocalInfo();
		    int slot = lvi.getSlot();
		    if (current[slot] != null 
			&& current[slot].lvi != lvi) {
			current[slot].endAddr = addr;
			lvtEntries.add(current[slot]);
			current[slot] = null;
		    }
		    if (current[slot] == null
			&& lvi.getName() != null) {
			current[slot] = new LVTEntry();
			current[slot].startAddr = addr;
			current[slot].lvi = lvi;
			System.err.println("lvi at "+i+","+k+","+slot+": "+current[slot].lvi);
		    }
		}
		addr += instrLength[i][k];
	    }
	}
	
	for (int slot = 0; slot < current.length; slot++) {
	    if (current[slot] != null) {
		current[slot].endAddr = blockAddr[blockAddr.length - 1];
		lvtEntries.add(current[slot]);
		current[slot] = null;
	    }
	}
	if (lvtEntries.size() > 0)
	    lvt = (LVTEntry[]) lvtEntries.toArray
		(new LVTEntry[lvtEntries.size()]);
    }

    public void init(GrowableConstantPool gcp) {
	Block[] blocks = bb.getBlocks();
	blockAddr = new int[blocks.length + 1];
	instrLength = new int[blocks.length + 1][];

	int[] gotos = new int[blocks.length + 1];
	int[] conds = new int[blocks.length + 1];
	
	boolean needRet = false;
	boolean hasRet = false;
	isRet = new BitSet();
	BitSet isJsr = new BitSet();

	int addr = 0;
	Block startBlock = bb.getStartBlock();
	if (startBlock == null) {
	    addr++;
	    isRet.set(0);
	    hasRet = true;
	    gotos[0] = -1;
	} else if (startBlock != blocks[0]) {
	    /* reserve 3 byte for a goto at the beginning */
	    addr += 3;
	    gotos[0] = startBlock.getBlockNr();
	}

	for (int i = 0; i < blocks.length; i++) {
	    boolean hasDefaultSucc = true;
	    blockAddr[i] = addr;
	    Instruction[] instrs = blocks[i].getInstructions();
	    instrLength[i] = new int[instrs.length];
	    Block[] succs = blocks[i].getSuccs();
	    for (int j = 0; j < instrs.length; j++) {
		Instruction instr = instrs[j];
		if (instr.hasLineNr())
		    lntCount++;

		conds[i+1] = -2;
		int opcode = instr.getOpcode();
		int length;
	    switch_opc:
		switch (opcode) {
		case opc_ldc:
		case opc_ldc2_w: {
		    Object constant = instr.getConstant();
		    if (constant == null) {
			length = 1;
			break switch_opc;
		    }
		    for (int k = 1; k < constants.length; k++) {
			if (constant.equals(constants[k])) {
			    length = 1;
			    break switch_opc;
			}
		    }
		    if (opcode == opc_ldc2_w) {
			gcp.putLongConstant(constant);
			length = 3;
			break switch_opc;
		    }
		    if (constant instanceof Integer) {
			int value = ((Integer) constant).intValue();
			if (value >= Byte.MIN_VALUE
			    && value <= Byte.MAX_VALUE) {
			    length = 2;
			    break switch_opc;
			} else if (value >= Short.MIN_VALUE
				   && value <= Short.MAX_VALUE) {
			    length = 3;
			    break switch_opc;
			}
		    }
		    if (gcp.putConstant(constant) < 256) {
			length = 2;
		    } else {
			length = 3;
		    }
		    break;
		} 
		case opc_iinc: {
		    int slot = instr.getLocalSlot();
		    int increment = instr.getIncrement();
		    if (slot < 256 
			&& increment >= Byte.MIN_VALUE 
			&& increment <= Byte.MAX_VALUE)
			length = 3;
		    else
			length = 6;
		    break;
		}
		case opc_iload: case opc_lload: 
		case opc_fload: case opc_dload: case opc_aload:
		case opc_istore: case opc_lstore: 
		case opc_fstore: case opc_dstore: case opc_astore:
		    if (instr.getLocalSlot() < 4) {
			length = 1;
			break;
		    }
		    if (instr.getLocalSlot() < 256)
			length = 2;
		    else 
			length = 4;
		    break;
		case opc_ret: {
		    if (instr.getLocalSlot() < 256)
			length = 2;
		    else 
			length = 4;
		    hasDefaultSucc = false;
		    break;
		} 
		case opc_lookupswitch: {
		    length = (~addr) & 3; /* padding */
		    int[] values = instr.getValues();
		    int   npairs = values.length;
		    for (int k=0; k< succs.length; k++) {
			if (succs[k] == null)
			    needRet = true;
		    }
		    if (npairs > 0
			&& 4 + 4 * (values[npairs-1] - values[0] + 1) 
			<= 8 * npairs) {
			// Use a table switch
			length += 13 + 4 * (values[npairs-1] - values[0] + 1);
		    } else {
			// Use a lookup switch
			length += 9 + 8 * npairs;
		    }
		    hasDefaultSucc = false;
		    break;
		}
		case opc_jsr:
		    conds[i+1] = succs[0].getBlockNr();
		    length = 3;
		    isJsr.set(i+1);
		    break;
		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple:
		case opc_if_acmpeq: case opc_if_acmpne:
		case opc_ifnull: case opc_ifnonnull:
		    if (succs[0] == null) {
			needRet = true;
			conds[i+1] = -1;
		    } else
			conds[i+1] = succs[0].getBlockNr();
		    length = 3;
		    break;
		case opc_multianewarray: {
		    if (instr.getDimensions() == 1) {
			String clazz = instr.getClazzType().substring(1);
			if (newArrayTypes.indexOf(clazz.charAt(0)) != -1) {
			    length = 2;
			} else {
			    gcp.putClassType(clazz);
			    length = 3;
			}
		    } else {
			gcp.putClassType(instr.getClazzType());
			length = 4;
		    }
		    break;
		}
		case opc_getstatic:
		case opc_getfield:
		case opc_putstatic:
		case opc_putfield:
		    gcp.putRef(ConstantPool.FIELDREF, instr.getReference());
		    length = 3;
		    break;
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokevirtual:
		    gcp.putRef(ConstantPool.METHODREF, instr.getReference());
		    length = 3;
		    break;
		case opc_invokeinterface:
		    gcp.putRef(ConstantPool.INTERFACEMETHODREF, instr.getReference());
		    length = 5;
		    break;
		case opc_new:
		case opc_checkcast:
		case opc_instanceof:
		    gcp.putClassType(instr.getClazzType());
		    length = 3;
		    break;
		case opc_ireturn: case opc_lreturn: 
		case opc_freturn: case opc_dreturn: case opc_areturn:
		case opc_return: 
		case opc_athrow:
		    length = 1;
		    hasDefaultSucc = false;
		    break;
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
		case opc_arraylength:
		case opc_monitorenter: case opc_monitorexit:
		    length = 1;
		    break;
		default:
		    throw new IllegalStateException("Invalid opcode "+opcode);
		}
		instrLength[i][j] = length;
		addr += length;
	    }
	    if (hasDefaultSucc) {
		Block defaultSucc = succs[succs.length-1];
		if (defaultSucc == null) {
		    // This is a return
		    gotos[i+1] = -1;
		    isRet.set(i+1);
		    lastRetAddr = addr;
		    hasRet = true;
		    addr++;
		} else if (defaultSucc.getBlockNr() == i + 1) {
		    // no need for any jump
		    gotos[i+1] = succs[succs.length-1].getBlockNr();
		} else {
		    // Reserve space for a normal goto.
		    gotos[i+1] = succs[succs.length-1].getBlockNr();
		    addr += 3;
		}
	    } else {
		// No goto needed for this block
		gotos[i+1] = -2;
	    }
	}
	if (needRet && !hasRet) {
	    retAtEnd = true;
	    lastRetAddr = addr;
	    addr++;
	}
	blockAddr[blocks.length] = addr;

	isWide = new BitSet();
	isWideCond = new BitSet();
	// Now check for wide goto/jsr/if, but only if method is big enough
	boolean changed = addr > Short.MAX_VALUE;
	while (changed) {
	    changed = false;
	    for (int i = 0; i < gotos.length; i++) {
		int gotoNr = gotos[i];
		int condNr = conds[i];
		if (!isWideCond.get(i) && condNr != -2) {
		    int from = blockAddr[i] - 3;
		    if (gotoNr != i + 1)
			from -= isRet.get(i) ? 1 : isWide.get(i) ? 5 : 3;
		    int dist = Integer.MAX_VALUE;
		    if (condNr == -1) {
			if (retAtEnd) {
			    dist = blockAddr[blockAddr.length-1] - 1 - from;
			} else {
			    for (int j = 0; j < gotos.length; j++) {
				if (isRet.get(j)) {
				    dist = blockAddr[j] - 1 - from;
				    if (dist >= Short.MIN_VALUE
					&& dist <= Short.MAX_VALUE) 
					break;
				}
			    }
			    if (dist == Integer.MAX_VALUE)
				throw new InternalError();
			}
		    } else {
			dist = blockAddr[condNr] - from;
		    }

		    if (dist < Short.MIN_VALUE || dist > Short.MAX_VALUE) {
			/* We must do the a wide cond: 
			 *   if_!xxx L
			 *   goto_w condNr
			 * L:goto   gotoNr
			 */
			isWideCond.set(i);
			int diff = isJsr.get(i) ? 2 : condNr == -1 ? 1 : 5;
			instrLength[i][instrLength[i].length-1] += diff;
			for (int j = i; j < blockAddr.length; j++)
			    blockAddr[j] += diff;
			changed = true;
		    }
		}
		if (!isWide.get(i) && gotoNr >= 0) {
		    int dist = blockAddr[gotoNr] - blockAddr[i] + 3;
		    if (dist < Short.MIN_VALUE || dist > Short.MAX_VALUE) {
			/* wide goto, correct addresses */
			isWide.set(i);
			for (int j = i; j < blockAddr.length; j++)
			    blockAddr[j] += 2;
			changed = true;
		    }
		}
	    }
	}
	buildNewLVT();
    }

    public int getSize() {
	/* maxStack:    2
	 * maxLocals:   2
	 * code:        4 + codeLength
	 * exc count:   2
	 * exceptions:  n * 8
	 * attributes:
	 *  lvt_name:    2
	 *  lvt_length:  4
	 *  lvt_count:   2
	 *  lvt_entries: n * 10
	 * attributes:
	 *  lnt_name:    2
	 *  lnt_length:  4
	 *  lnt_count:   2
	 *  lnt_entries: n * 4
	 */
	int attrsize = 0;
	if (lvt != null)
	    attrsize += 8 + lvt.length * 10;
	if (lntCount > 0)
	    attrsize += 8 + lntCount * 4;
	return 10
	    + blockAddr[blockAddr.length - 1]
	    + bb.getExceptionHandlers().length * 8
	    + attrsize;
    }

    protected int getAttributeCount() {
	int count = 0;
	if (lvt != null)
	    count++;
	if (lntCount > 0)
	    count++;
	return count;
    }

    public void prepare(GrowableConstantPool gcp) {
	Handler[] handlers = bb.getExceptionHandlers();
	for (int i = 0; i< handlers.length; i++) {
	    if (handlers[i].type != null)
		gcp.putClassName(handlers[i].type);
	}
	if (lvt != null) {
	    gcp.putUTF8("LocalVariableTable");
            int count = lvt.length;
            for (int i=0; i < count; i++) {
	      System.err.println("lvt: "+lvt[i].lvi);
		gcp.putUTF8(lvt[i].lvi.getName());
		gcp.putUTF8(lvt[i].lvi.getType());
	    }
	}
	if (lntCount > 0)
	    gcp.putUTF8("LineNumberTable");
    }

    public void writeAttributes(GrowableConstantPool gcp,
				DataOutputStream output) 
	throws IOException {
	if (lvt != null) {
	    output.writeShort(gcp.putUTF8("LocalVariableTable"));
            int count = lvt.length;
	    int length = 2 + 10 * count;
	    output.writeInt(length);
	    output.writeShort(count);
            for (int i=0; i < count; i++) {
		output.writeShort(lvt[i].startAddr);
		output.writeShort(lvt[i].endAddr);
		output.writeShort(gcp.putUTF8(lvt[i].lvi.getName()));
		output.writeShort(gcp.putUTF8(lvt[i].lvi.getType()));
		output.writeShort(lvt[i].lvi.getSlot());
            }
	}
	if (lntCount > 0) {
	    output.writeShort(gcp.putUTF8("LineNumberTable"));
	    int length = 2 + 4 * lntCount;
	    output.writeInt(length);
	    output.writeShort(lntCount);
            for (int i = 0; i < lntCount; i++) {
		output.writeShort(lnt[2*i]);
		output.writeShort(lnt[2*i+1]);
            }
	}
    }

    public void write(GrowableConstantPool gcp, 
		      DataOutputStream output) throws IOException {
	output.writeShort(bb.getMaxStack());
	output.writeShort(bb.getMaxLocals());
	Block[] blocks = bb.getBlocks();
	if (blockAddr[blockAddr.length - 1] > 65535)
	    throw new ClassFormatError("Method too long");
	output.writeInt(blockAddr[blockAddr.length-1]);
	lnt = new short[lntCount*2];

	int addr = 0;
	Block startBlock = bb.getStartBlock();
	if (isRet.get(0)) {
	    output.writeByte(opc_return);
	    addr ++;
	} else if (isWide.get(0)) {	    
	    output.writeByte(opc_goto_w);
	    output.writeInt(blockAddr[startBlock.getBlockNr()]);
	    addr += 5;
	} else if (startBlock != blocks[0]) {
	    output.writeByte(opc_goto);
	    output.writeShort(blockAddr[startBlock.getBlockNr()]);
	    addr += 3;
	}
	int lntPtr = 0;
	
	for (int i = 0; i< blocks.length; i++) {
	    boolean hasDefaultSucc = true;
	    Block[] succs = blocks[i].getSuccs();
	    if (addr != blockAddr[i])
		throw new InternalError("Address calculation broken for "+i+": "+blockAddr[i]+"!="+addr+"!");
	    Instruction[] instructions = blocks[i].getInstructions();
	    int size = instructions.length;
	    for (int j = 0; j < size; j++) {
		Instruction instr = instructions[j];
		if (instr.hasLineNr()) {
		    lnt[lntPtr++] = (short) addr;
		    lnt[lntPtr++] = (short) instr.getLineNr();
		}
		int opcode = instr.getOpcode();
	    switch_opc:
		switch (opcode) {
		case opc_iload: case opc_lload: 
		case opc_fload: case opc_dload: case opc_aload:
		case opc_istore: case opc_lstore: 
		case opc_fstore: case opc_dstore: case opc_astore: {
		    int slot = instr.getLocalSlot();
		    if (slot < 4) {
			if (opcode < opc_istore)
			    output.writeByte(opc_iload_0
					     + 4*(opcode-opc_iload)
					     + slot);
			else
			    output.writeByte(opc_istore_0
					     + 4*(opcode-opc_istore)
					     + slot);
		    } else if (slot < 256) {
			output.writeByte(opcode);
			output.writeByte(slot);
		    } else {
			output.writeByte(opc_wide);
			output.writeByte(opcode);
			output.writeShort(slot);
		    }
		    break;
		}		
		case opc_ret: {
		    int slot = instr.getLocalSlot();
		    if (slot < 256) {
			output.writeByte(opcode);
			output.writeByte(slot);
		    } else {
			output.writeByte(opc_wide);
			output.writeByte(opcode);
			output.writeShort(slot);
		    }
		    hasDefaultSucc = false;
		    break;
		}
		case opc_ldc:
		case opc_ldc2_w: {
		    Object constant = instr.getConstant();
		    if (constant == null) {
			output.writeByte(opc_aconst_null);
			break switch_opc;
		    }
		    for (int k = 1; k < constants.length; k++) {
			if (constant.equals(constants[k])) {
			    output.writeByte(opc_aconst_null + k);
			    break switch_opc;
			}
		    }
		    if (opcode == opc_ldc2_w) {
			output.writeByte(opcode);
			output.writeShort(gcp.putLongConstant(constant));
		    } else {
			if (constant instanceof Integer) {
			    int value = ((Integer) constant).intValue();
			    if (value >= Byte.MIN_VALUE
				&& value <= Byte.MAX_VALUE) {
				
				output.writeByte(opc_bipush);
				output.writeByte(((Integer)constant)
						 .intValue());
				break switch_opc;
			    } else if (value >= Short.MIN_VALUE
				       && value <= Short.MAX_VALUE) {
				output.writeByte(opc_sipush);
				output.writeShort(((Integer)constant)
						  .intValue());
				break switch_opc;
			    }
			}
			if (instrLength[i][j] == 2) {
			    output.writeByte(opc_ldc);
			    output.writeByte(gcp.putConstant(constant));
			} else {
			    output.writeByte(opc_ldc_w);
			    output.writeShort(gcp.putConstant(constant));
			}
		    }
		    break;
		}
		case opc_iinc: {
		    int slot = instr.getLocalSlot();
		    int incr = instr.getIncrement();
		    if (instrLength[i][j] == 3) {
			output.writeByte(opcode);
			output.writeByte(slot);
			output.writeByte(incr);
		    } else {
			output.writeByte(opc_wide);
			output.writeByte(opcode);
			output.writeShort(slot);
			output.writeShort(incr);
		    }
		    break;
		}
		case opc_jsr: {
		    int dist = blockAddr[succs[0].getBlockNr()] - addr;
		    if (isWideCond.get(i+1)) {
			/* wide jsr */
			output.writeByte(opc_jsr_w);
			output.writeInt(dist);
		    } else {
			/* wide jsr */
			output.writeByte(opc_jsr);
			output.writeShort(dist);
		    }
		    break;
		}

		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple:
		case opc_if_acmpeq: case opc_if_acmpne:
		case opc_ifnull: case opc_ifnonnull: {
		    Block dest = succs[0];
		    if (isWideCond.get(i+1)) {
			/* swap condition */
			if (opcode >= opc_ifnull)
			    opcode = opcode ^ 1;
			else
			    opcode = 1 + ((opcode - 1) ^ 1);
			output.writeByte(opcode);
			if (dest == null) {
			    output.writeShort(4);
			    output.writeByte(opc_ret);
			} else {
			    output.writeShort(8);
			    output.writeByte(opc_goto_w);
			    int dist = blockAddr[dest.getBlockNr()] - addr;
			    output.writeInt(dist);
			}
		    } else {
			int dist;
			if (dest == null) {
			    if (retAtEnd) {
				dist = blockAddr[blocks.length] - 1 - addr;
			    } else {
				for (int k = 0; ; k++) {
				    if (isRet.get(k)) {
					dist = blockAddr[k] - 1 - addr;
					if (dist >= Short.MIN_VALUE
					    && dist <= Short.MAX_VALUE) 
					    break;
				    }
				    if (k == blocks.length)
					throw new InternalError();
				}
			    }
			} else {
			    dist = blockAddr[dest.getBlockNr()] - addr;
			}
			output.writeByte(opcode);
			output.writeShort(dist);
		    }
		    break;
		}

		case opc_lookupswitch: {
		    int align = 3-(addr % 4);
		    int[] values = instr.getValues();
		    int npairs = values.length;
		    Block defBlock = succs[npairs];
		    int defAddr = defBlock == null ? lastRetAddr
			: blockAddr[defBlock.getBlockNr()];
			
		    if (npairs > 0) {
			int tablesize = values[npairs-1] - values[0] + 1;
			if (4 + tablesize * 4 <= 8 * npairs) {
			    // Use a table switch
			    output.writeByte(opc_tableswitch);
			    output.write(new byte[align]);
			    /* def */
			    output.writeInt(defAddr - addr);
			    /* low */
			    output.writeInt(values[0]); 
			    /* high */
			    output.writeInt(values[npairs-1]);
			    int pos = values[0];
			    for (int k = 0; k < npairs; k++) {
				while (pos++ < values[k])
				    output.writeInt(defAddr - addr);
				int dest = succs[k] == null ? lastRetAddr
				    : blockAddr[succs[k].getBlockNr()];
				output.writeInt(dest - addr);
			    }
			    hasDefaultSucc = false;
			    break;
			}
		    }
		    // Use a lookup switch
		    output.writeByte(opc_lookupswitch);
		    output.write(new byte[align]);
		    /* def */
		    output.writeInt(defAddr - addr);
		    output.writeInt(npairs);
		    for (int k = 0; k < npairs; k++) {
			output.writeInt(values[k]);
			int dest = succs[k] == null ? lastRetAddr
			    : blockAddr[succs[k].getBlockNr()];
			output.writeInt(dest - addr);
		    }
		    hasDefaultSucc = false;
		    break;
		}
		
		case opc_getstatic:
		case opc_getfield:
		case opc_putstatic:
		case opc_putfield:
		    output.writeByte(opcode);
		    output.writeShort(gcp.putRef(ConstantPool.FIELDREF, 
						 instr.getReference()));
		    break;
		    
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual: {
		    Reference ref = instr.getReference();
		    output.writeByte(opcode);
		    if (opcode == opc_invokeinterface) {
			output.writeShort
			    (gcp.putRef(ConstantPool.INTERFACEMETHODREF, ref));
			output.writeByte
			    (TypeSignature
			     .getParameterSize(ref.getType()) + 1);
			output.writeByte(0);
		    } else 
			output.writeShort(gcp.putRef(ConstantPool.METHODREF, ref));
		    break;
		}
		case opc_new:
		case opc_checkcast:
		case opc_instanceof:
		    output.writeByte(opcode);
		    output.writeShort(gcp.putClassType(instr.getClazzType()));
		    break;
		case opc_multianewarray:
		    if (instr.getDimensions() == 1) {
			String clazz = instr.getClazzType().substring(1);
			int index = newArrayTypes.indexOf(clazz.charAt(0));
		    if (index != -1) {
			output.writeByte(opc_newarray);
			output.writeByte(index + 4);
		    } else {
			output.writeByte(opc_anewarray);
			output.writeShort(gcp.putClassType(clazz));
		    }
		    } else {
			output.writeByte(opcode);
			output.writeShort
			    (gcp.putClassType(instr.getClazzType()));
			output.writeByte(instr.getDimensions());
		    }
		    break;
		    
		case opc_ireturn: case opc_lreturn: 
		case opc_freturn: case opc_dreturn: case opc_areturn:
		case opc_athrow: case opc_return:
		    output.writeByte(opcode);
		    hasDefaultSucc = false;
		    break;

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
		case opc_arraylength:
		case opc_monitorenter: case opc_monitorexit:
		    output.writeByte(opcode);
		    break;
		default:
		    throw new ClassFormatException("Invalid opcode "+opcode);
		}
		addr += instrLength[i][j];
	    }
	    if (hasDefaultSucc) {
		// Check which type of goto we should use at end of this block.
		Block defaultSucc = succs[succs.length - 1];
		if (isRet.get(i+1)) {
		    output.writeByte(opc_return);
		    addr++;
		} else if (isWide.get(i+1)) {
		    output.writeByte(opc_goto_w);
		    output.writeInt(blockAddr[defaultSucc.getBlockNr()]
				    - addr);
		    addr+=5;
		} else if (defaultSucc.getBlockNr() != i+1) {
		    output.writeByte(opc_goto);
		    output.writeShort(blockAddr[defaultSucc.getBlockNr()]
				      - addr);
		    addr+=3;
		}
	    }
	}
	if (retAtEnd) {
	    output.writeByte(opc_return);
	    addr++;
	}	    
	if (addr != blockAddr[blocks.length])
	    throw new InternalError("Address calculation broken!");

	Handler[] handlers = bb.getExceptionHandlers();
	output.writeShort(handlers.length);
	for (int i = 0; i< handlers.length; i++) {
	    output.writeShort(blockAddr[handlers[i].start.getBlockNr()]);
	    output.writeShort(blockAddr[handlers[i].end.getBlockNr()+1]);
	    output.writeShort(blockAddr[handlers[i].catcher.getBlockNr()]);
	    output.writeShort((handlers[i].type == null) ? 0
			      : gcp.putClassName(handlers[i].type));
	}
    }
}
