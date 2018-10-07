/* BasicBlockReader Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: BasicBlockReader.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.bytecode;

import net.sf.jode.GlobalOptions;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This is a helper class, that contains the method to read in basic
 * blocks from class files and write them back again.
 */
class BasicBlockReader implements Opcodes {

    static final int IS_BORDER    = 1;
    static final int IS_REACHABLE = 2;
    static final int IS_FORWARD   = 4;
    static final int IS_CATCHER   = 8;
    static final int IS_NULL      = 16;

    private class InstrInfo {
	Instruction instr;
	int         stack;
	int         flags;
	int         addr;
	int         nextAddr;
	int         blockNr;
	int[]       succs;

	InstrInfo () {
	    blockNr = stack = addr = nextAddr = -1;
	}
    }

    private class HandlerEntry {
	int start, end, catcher;
	String type;
    }

    private class LVTEntry {
	int local;
	int start, end;
	String name, type;
    }

    private class LNTEntry {
	int lineNr;
	int start;
    }

    InstrInfo[] infos;
    HandlerEntry[] handlers;
    BasicBlocks bb;
    Block[] blocks;

    int maxStack;
    int maxLocals;

    public BasicBlockReader(BasicBlocks bb) {
	this.bb = bb;
    }

    private void markReachableBlocks() throws ClassFormatException {
	Stack todo = new Stack();
	todo.push(infos[0]);
	infos[0].flags = IS_REACHABLE;
	infos[0].stack = 0;

	int[] poppush = new int[2];
	/* Iterate until no more reachable instructions are found */
	while (!todo.isEmpty()) {
	    InstrInfo info = (InstrInfo) todo.pop();
	    int stack = info.stack;
	    if ((info.flags & IS_CATCHER) == 0
		&& (info.instr.getOpcode() == opc_goto
		    || (info.instr.getOpcode() == opc_return
			&& info.stack == 0))) {
		/* This is a forward block.  We need to check for loops,
		 * though.
		 */
		InstrInfo succ = info;
		do {
		    if (succ.instr.getOpcode() == opc_return) {
			succ = null;
			break;
		    }
		    succ = infos[succ.succs[0]];
		} while ((succ.flags & IS_FORWARD) != 0);
		if (succ == info)
		    info.flags |= IS_NULL;
		else
		    info.flags |= IS_FORWARD;
	    } else {
		// Check for reachable exception handlers
		for (int i=0; i < handlers.length; i++) {
		    if (handlers[i].start <= info.addr
			&& handlers[i].end > info.addr) {
			InstrInfo catcher = infos[handlers[i].catcher];
			if ((catcher.flags & IS_REACHABLE) == 0) {
			    catcher.flags |= IS_REACHABLE;
			    catcher.stack = 1;
			    todo.push(catcher);
			}
		    }
		}

		InstrInfo prevInfo = null;
		// Search the end of the block and calculate next stack depth
		while (true) {
		    info.instr.getStackPopPush(poppush);
		    stack += poppush[1] - poppush[0];
		    if (stack < 0) {
			throw new ClassFormatException
			    ("Pop from empty stack: " + bb);
		    }

		    if (!info.instr.doesAlwaysJump()
			&& info.succs == null
			&& (infos[info.nextAddr].flags & IS_BORDER) == 0) {
			prevInfo = info;
			try {
			    info = infos[info.nextAddr];
			} catch (ArrayIndexOutOfBoundsException ex) {
			    throw new ClassFormatException
				("Flow falls out of method " + bb);
			}
		    } else
			break;
		}

		if (info.instr.getOpcode() == opc_goto
		    || (info.instr.getOpcode() == opc_return
			&& stack == 0)) {
		    /* If list is a goto or return, we step an instruction
		     * back.  We don't need to modify stack, since goto and
		     * return are neutral.
		     */
		    info = prevInfo;
		}
	    }

	    /* mark successors as reachable */
	    int[] succs = info.succs;
	    if (succs != null) {
		for (int i=0; i < succs.length; i++) {
		    InstrInfo succ = infos[succs[i]];
		    if ((succ.flags & IS_REACHABLE) == 0) {
			succ.flags |= IS_REACHABLE;
			int succstack = stack;
			if (info.instr.getOpcode() == opc_jsr)
			    succstack++;
			if (succ.stack < 0)
			    succ.stack = succstack;
			else if (succ.stack != succstack)
			    throw new ClassFormatException
				("Stack height varies: "+bb+":"+succs[i]);
			todo.push(succ);
		    }
		}
	    }
	    if (info.nextAddr < infos.length)
		infos[info.nextAddr].flags |= IS_BORDER;

	    if (!info.instr.doesAlwaysJump()) {
		InstrInfo succ = infos[info.nextAddr];
		if ((succ.flags & IS_REACHABLE) == 0) {
		    succ.flags |= IS_REACHABLE;
		    if (succ.stack < 0)
			succ.stack = stack;
		    else if (succ.stack != stack)
			throw new ClassFormatException
			    ("Stack height varies: "+bb+":"+info.nextAddr);
		    todo.push(succ);
		}
	    }
	}
    }

    private int getSuccBlockNr(int succAddr) {
	InstrInfo succ = infos[succAddr];
	while ((succ.flags & IS_FORWARD) != 0) {
	    switch (succ.instr.getOpcode()) {
	    case opc_goto:
		succ = infos[succ.succs[0]];
		break;
	    case opc_return:
		return -1;
	    default:
		throw new IllegalStateException();
	    }
	}
	return succ.blockNr;
    }

    private Block getSuccBlock(int succAddr) {
	int nr = getSuccBlockNr(succAddr);
	return nr == -1 ? null : blocks[nr];
    }


    private Handler[] convertHandlers() {
	int newCount = 0;
	for (int i=0; i < handlers.length; i++) {
	    while (handlers[i].start < handlers[i].end
		   && infos[handlers[i].start].blockNr == -1)
		handlers[i].start = infos[handlers[i].start].nextAddr;
	    if (handlers[i].start == handlers[i].end)
		continue;

	    while (handlers[i].end < infos.length
		   && infos[handlers[i].end].blockNr == -1)
		handlers[i].end = infos[handlers[i].end].nextAddr;
	    if ((infos[handlers[i].catcher].flags & IS_REACHABLE) != 0)
		newCount++;
	}
	Handler[] newHandlers = new Handler[newCount];
	int ptr = 0;
	for (int i=0; i<handlers.length; i++) {	    
	    if (handlers[i].start < handlers[i].end
		&& (infos[handlers[i].catcher].flags & IS_REACHABLE) != 0) {
		int endBlock = handlers[i].end < infos.length
		    ? infos[handlers[i].end].blockNr : blocks.length;
		newHandlers[ptr++] = new Handler
		    (blocks[infos[handlers[i].start].blockNr], 
		     blocks[endBlock - 1],
		     getSuccBlock(handlers[i].catcher),
		     handlers[i].type);
	    }
	}
	return newHandlers;
    }

    private void convertBlock(int firstAddr, int count) {
	Instruction[] instrs = new Instruction[count];
	InstrInfo info = infos[firstAddr];
	int blockNr = info.blockNr;

	for (int i = 0; i < count; i++) {
	    if (i > 0)
		info = infos[info.nextAddr];
	    instrs[i] = info.instr;
	}
	
	int[] lastSuccs = info.succs;
	int succLength = lastSuccs != null ? lastSuccs.length : 0;
	boolean alwaysJump = info.instr.doesAlwaysJump();

	Block[] succs = new Block[succLength + (alwaysJump ? 0 : 1)];
	for (int i=0; i < succLength; i++)
	    succs[i] = getSuccBlock(lastSuccs[i]);
	if (!alwaysJump)
	    succs[succLength] = getSuccBlock(info.nextAddr);

	blocks[blockNr].setCode(instrs, succs);
    }

    void convert() throws ClassFormatException {
	markReachableBlocks();

	int blockCount = 0;
	/* Count the blocks */
	for (int i=0; i< infos.length; i = infos[i].nextAddr) {
	    if ((infos[i].flags & (IS_REACHABLE | IS_FORWARD)) == IS_REACHABLE)
		infos[i].blockNr = blockCount++;
	}
	
	blocks = new Block[blockCount];
	for (int i=0; i< blocks.length; i++)
	    blocks[i] = new Block();

	int start = -1;
	int count = 0;
	for (int i = 0; i < infos.length; i = infos[i].nextAddr) {
	    InstrInfo info = infos[i];
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_BYTECODE) != 0) {
		if ((info.flags & IS_BORDER) != 0)
		    GlobalOptions.err.println
			(""+info.addr+": "+info.flags+","+info.blockNr+";"+info.stack);
	    }
	    if ((info.flags & IS_BORDER) != 0) {
		if (start != -1)
		    convertBlock(start, count);
		start = -1;
	    }
	    if ((info.flags & (IS_REACHABLE | IS_FORWARD)) == IS_REACHABLE) {
		if ((info.flags & IS_NULL) != 0) {
		    convertBlock(i, 0);
		} else {
		    start = i;
		    count = 0;
		}
	    }
	    if (start != -1)
		count++;
	}
	if (start != -1)
	    convertBlock(start, count);
	bb.setBlocks(blocks, getSuccBlock(0), convertHandlers());
	if (bb.maxStack > maxStack)
	    throw new ClassFormatException("Only allocated "+maxStack
					   +" stack slots for method, needs "
					   +bb.maxStack);
	if (bb.maxLocals > maxLocals)
	    throw new ClassFormatException("Only allocated "+maxLocals
					   +" local slots for method, needs "
					   +bb.maxLocals);
    }

    public void readCode(ConstantPool cp, 
			 DataInputStream input) throws IOException {
        maxStack = input.readUnsignedShort();
	maxLocals = input.readUnsignedShort(); 

        int codeLength = input.readInt();
	infos = new InstrInfo[codeLength];
	{
	    int addr = 0;
	    while (addr < codeLength) {
		Instruction instr;
		int length;

		infos[addr] = new InstrInfo();
		int opcode = input.readUnsignedByte();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		    GlobalOptions.err.print(addr+": "+opcodeString[opcode]);

		switch (opcode) {
		case opc_wide: {
		    int wideopcode = input.readUnsignedByte();
		    switch (wideopcode) {
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_istore: case opc_fstore: case opc_astore: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals)
			    throw new ClassFormatException
				("Invalid local slot "+slot);
			LocalVariableInfo lvi
			    = LocalVariableInfo.getInfo(slot);
			instr = new SlotInstruction(wideopcode, lvi);
			length = 4;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print
				(" " + opcodeString[wideopcode] + " " + slot);
			break;
		    }
		    case opc_lload: case opc_dload:
		    case opc_lstore: case opc_dstore: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals-1)
			    throw new ClassFormatException
				("Invalid local slot "+slot);
			LocalVariableInfo lvi
			    = LocalVariableInfo.getInfo(slot);
			instr = new SlotInstruction(wideopcode, lvi);
			length = 4;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print
				(" " + opcodeString[wideopcode] + " " + slot);
			break;
		    }
		    case opc_ret: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals)
			    throw new ClassFormatException
				("Invalid local slot "+slot);
			LocalVariableInfo lvi
			    = LocalVariableInfo.getInfo(slot);
			instr = new SlotInstruction(wideopcode, lvi);
			length = 4;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" ret "+slot);
			break;
		    }
		    case opc_iinc: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals)
			    throw new ClassFormatException
				("Invalid local slot "+slot);
			LocalVariableInfo lvi
			    = LocalVariableInfo.getInfo(slot);
			int incr = input.readShort();
			instr = new IncInstruction(wideopcode, lvi, incr);
			length = 6;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print
				(" iinc " + slot + " " + instr.getIncrement());
			break;
		    }
		    default:
			throw new ClassFormatException("Invalid wide opcode "
						       +wideopcode);
		    }
		    break;
		}
		case opc_iload_0: case opc_iload_1:
		case opc_iload_2: case opc_iload_3:
		case opc_lload_0: case opc_lload_1:
		case opc_lload_2: case opc_lload_3:
		case opc_fload_0: case opc_fload_1:
		case opc_fload_2: case opc_fload_3:
		case opc_dload_0: case opc_dload_1:
		case opc_dload_2: case opc_dload_3:
		case opc_aload_0: case opc_aload_1:
		case opc_aload_2: case opc_aload_3: {
		    int slot = (opcode-opc_lload_0) & 3;
		    if (slot >= maxLocals)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    instr = new SlotInstruction
			(opc_iload + (opcode-opc_iload_0)/4, lvi);
		    length = 1;
		    break;
		}
		case opc_istore_0: case opc_istore_1: 
		case opc_istore_2: case opc_istore_3:
		case opc_fstore_0: case opc_fstore_1:
		case opc_fstore_2: case opc_fstore_3:
		case opc_astore_0: case opc_astore_1:
		case opc_astore_2: case opc_astore_3: {
		    int slot = (opcode-opc_istore_0) & 3;
		    if (slot >= maxLocals)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    instr = new SlotInstruction
			(opc_istore + (opcode-opc_istore_0)/4, lvi);
		    length = 1;
		    break;
		}
		case opc_lstore_0: case opc_lstore_1: 
		case opc_lstore_2: case opc_lstore_3:
		case opc_dstore_0: case opc_dstore_1:
		case opc_dstore_2: case opc_dstore_3: {
		    int slot = (opcode-opc_lstore_0) & 3;
		    if (slot >= maxLocals-1)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    instr = new SlotInstruction
			(opc_lstore + (opcode-opc_lstore_0)/4, lvi);
		    length = 1;
		    break;
		}
		case opc_iload: case opc_fload: case opc_aload:
		case opc_istore: case opc_fstore: case opc_astore: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    instr = new SlotInstruction(opcode, lvi);
		    length = 2;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot);
		    break;
		}
		case opc_lstore: case opc_dstore:
		case opc_lload: case opc_dload: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals - 1)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    instr = new SlotInstruction(opcode, lvi);
		    length = 2;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot);
		    break;
		}
		case opc_ret: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    instr = new SlotInstruction(opcode, lvi);
		    length = 2;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot);
		    break;
		}
		case opc_aconst_null:
		case opc_iconst_m1: 
		case opc_iconst_0: case opc_iconst_1: case opc_iconst_2:
		case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
		case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
		    instr = new ConstantInstruction
			(opc_ldc, constants[opcode - opc_aconst_null]);
		    length = 1;
		    break;
		case opc_lconst_0: case opc_lconst_1:
		case opc_dconst_0: case opc_dconst_1:
		    instr = new ConstantInstruction
			(opc_ldc2_w, constants[opcode - opc_aconst_null]);
		    length = 1;
		    break;
		case opc_bipush:
		    instr = new ConstantInstruction
			(opc_ldc, new Integer(input.readByte()));
		    length = 2;
		    break;
		case opc_sipush:
		    instr = new ConstantInstruction
			(opc_ldc, new Integer(input.readShort()));
		    length = 3;
		    break;
		case opc_ldc: {
		    int index = input.readUnsignedByte();
		    int tag = cp.getTag(index);
		    if (tag != ConstantPool.STRING && tag != ConstantPool.CLASS
			 && tag != ConstantPool.INTEGER && tag != ConstantPool.FLOAT)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr = new ConstantInstruction
			(opc_ldc, cp.getConstant(index));
		    length = 2;
		    break;
		}
		case opc_ldc_w: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != ConstantPool.STRING && tag != ConstantPool.CLASS
			 && tag != ConstantPool.INTEGER && tag != ConstantPool.FLOAT)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr = new ConstantInstruction
			(opc_ldc, cp.getConstant(index));
		    length = 3;
		    break;
		}
		case opc_ldc2_w: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != ConstantPool.LONG && tag != ConstantPool.DOUBLE)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr = new ConstantInstruction
			(opc_ldc2_w, cp.getConstant(index));
		    length = 3;
		    break;
		}
		case opc_iinc: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals)
			throw new ClassFormatException
			    ("Invalid local slot "+slot);
		    LocalVariableInfo lvi
			= LocalVariableInfo.getInfo(slot);
		    int incr = input.readByte();
		    instr = new IncInstruction(opcode, lvi, incr);
		    length = 3;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print
			    (" " + slot + " " + instr.getIncrement());
		    break;
		}
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
		    instr = new Instruction(opcode);
		    length = 3;
		    infos[addr].succs = new int[] { addr+input.readShort() };
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+infos[addr].succs[0]);
		    break;

		case opc_goto_w:
		case opc_jsr_w:
		    instr = new Instruction(opcode - (opc_goto_w - opc_goto));
		    length = 5;
		    infos[addr].succs = new int[] { addr+input.readInt() };
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+infos[addr].succs[0]);
		    break;

		case opc_tableswitch: {
		    length = 3 - (addr % 4);
		    input.readFully(new byte[length]);
		    int def  = input.readInt();
		    int low  = input.readInt();
		    int high = input.readInt();
		    int[] dests = new int[high-low+1];
		    int npairs = 0;
		    for (int i=0; i < dests.length; i++) {
			dests[i] = input.readInt();
			if (dests[i] != def)
			    npairs++;
		    }
		    infos[addr].succs = new int[npairs + 1];
		    int[] values = new int[npairs];
		    int pos = 0;
		    for (int i=0; i < dests.length; i++) {
			if (dests[i] != def) {
			    values[pos] = i+low;
			    infos[addr].succs[pos] = addr + dests[i];
			    pos++;
			}
		    }
		    infos[addr].succs[npairs] = addr + def;
		    instr = new SwitchInstruction(opc_lookupswitch, values);
		    length += 13 + 4 * (high-low+1);
		    break;
		}
		case opc_lookupswitch: {
		    length = 3 - (addr % 4);
		    input.readFully(new byte[length]);
		    int def = input.readInt();
		    int npairs = input.readInt();
		    infos[addr].succs = new int[npairs + 1];
		    int[] values = new int[npairs];
		    for (int i=0; i < npairs; i++) {
			values[i] = input.readInt();
			if (i > 0 && values[i-1] >= values[i])
			    throw new ClassFormatException
				("lookupswitch not sorted");
			infos[addr].succs[i] = addr + input.readInt();
		    }
		    infos[addr].succs[npairs] = addr + def;
		    instr = new SwitchInstruction(opc_lookupswitch, values);
		    length += 9 + 8 * npairs;
		    break;
		}
			    
		case opc_getstatic:
		case opc_getfield:
		case opc_putstatic:
		case opc_putfield:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokevirtual: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (opcode < opc_invokevirtual) {
			if (tag != ConstantPool.FIELDREF)
			    throw new ClassFormatException
				("field tag mismatch: "+tag);
		    } else {
			if (tag != ConstantPool.METHODREF)
			    throw new ClassFormatException
				("method tag mismatch: "+tag);
		    }
		    Reference ref = cp.getRef(index);
		    if (ref.getName().charAt(0) == '<'
			&& (!ref.getName().equals("<init>")
			    || opcode != opc_invokespecial))
			throw new ClassFormatException
			    ("Illegal call of special method/field "+ref);
		    instr = new ReferenceInstruction(opcode, ref);
		    length = 3;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+ref);
		    break;
		}
		case opc_invokeinterface: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != ConstantPool.INTERFACEMETHODREF)
			throw new ClassFormatException
			    ("interface tag mismatch: "+tag);
		    Reference ref = cp.getRef(index);
		    if (ref.getName().charAt(0) == '<')
			throw new ClassFormatException
			    ("Illegal call of special method "+ref);
		    int nargs = input.readUnsignedByte();
		    if (TypeSignature.getParameterSize(ref.getType())
			!= nargs - 1)
			throw new ClassFormatException
			    ("Interface nargs mismatch: "+ref+" vs. "+nargs);
		    if (input.readUnsignedByte() != 0)
			throw new ClassFormatException
			    ("Interface reserved param not zero");

		    instr = new ReferenceInstruction(opcode, ref);
		    length = 5;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+ref);
		    break;
		}

		case opc_new:
		case opc_checkcast:
		case opc_instanceof: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    if (opcode == opc_new && type.charAt(0) == '[')
			throw new ClassFormatException
			    ("Can't create array with opc_new");
		    instr = new TypeInstruction(opcode, type);
		    length = 3;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+type);
		    break;
		}
		case opc_multianewarray: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    int dims = input.readUnsignedByte();
		    if (dims == 0)
			throw new ClassFormatException
			    ("multianewarray dimension is 0.");
		    for (int i=0; i < dims; i++) {
			/* Note that since type is a valid type
			 * signature, there must be a non bracket
			 * character, before the string is over.  
			 * So there is no StringIndexOutOfBoundsException.
			 */
			if (type.charAt(i) != '[')
			    throw new ClassFormatException
				("multianewarray called for non array:"+ type);
		    }
		    instr = new TypeDimensionInstruction(opcode, type, dims);
		    length = 4;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" " + type + " " + dims);
		    break;
		}
		case opc_anewarray: {
		    String type 
			= "["+cp.getClassType(input.readUnsignedShort());
		    instr = new TypeDimensionInstruction
			(opc_multianewarray, type.intern(), 1);
		    length = 3;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+type);
		    break;
		}
		case opc_newarray: {
		    char sig = newArrayTypes.charAt
			(input.readUnsignedByte()-4);
		    String type = new String (new char[] { '[', sig });
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+type);
		    instr = new TypeDimensionInstruction
			(opc_multianewarray, type.intern(), 1);
		    length = 2;
		    break;
		}
		
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
		    instr = new Instruction(opcode);
		    length = 1;
		    break;
		default:
		    throw new ClassFormatException("Invalid opcode "+opcode);
		}
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		    GlobalOptions.err.println();

		infos[addr].instr = instr;
		infos[addr].addr  = addr;
		infos[addr].nextAddr = addr + length;
		if (addr + length == codeLength && !instr.doesAlwaysJump())
		    throw new ClassFormatException
			("Flow falls out of method " + bb);
		addr += length;
	    }
	    if (addr != codeLength)
		throw new ClassFormatException("last instruction too long");
	}

	int handlersLength = input.readUnsignedShort();
	handlers = new HandlerEntry[handlersLength];
	for (int i=0; i< handlersLength; i ++) {
	    handlers[i] = new HandlerEntry();
	    handlers[i].start = input.readUnsignedShort();
	    handlers[i].end = input.readUnsignedShort();
	    handlers[i].catcher = input.readUnsignedShort();
	    int index = input.readUnsignedShort();
	    handlers[i].type = (index == 0) ? null
		: cp.getClassName(index);

	    if (i > 0 && handlers[i].start == handlers[i-1].end
		&& handlers[i].catcher == handlers[i-1].catcher
		&& handlers[i].type == handlers[i-1].type) {
		/* Javac 1.4 splits handlers at return instruction
		 * (see below).  We merge them together here.
		 */
		handlers[i-1].end = handlers[i].end;
		handlersLength--;
		i--;
	    }

	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_BYTECODE) != 0) 
		GlobalOptions.err.println("Handler "+handlers[i].start
					  +"-"+handlers[i].end
					  +" @"+handlers[i].catcher
					  + ": "+handlers[i].type);

	    if (infos[handlers[i].catcher].instr.getOpcode() == opc_athrow) {
		/* There is an obfuscator, which inserts bogus
		 * exception entries jumping directly to a throw
		 * instruction.  Remove those handlers.
		 */
		handlersLength--;
		i--;
		continue;
	    }

	    if (handlers[i].start <= handlers[i].catcher
		&& handlers[i].end > handlers[i].catcher)
	    {
		/* Javac 1.4 is a bit paranoid with finally and
		 * synchronize blocks and even breaks the JLS.
		 * We fix it here.  Hopefully this won't produce
		 * any other problems.
		 */
		if (handlers[i].start == handlers[i].catcher) {
		    handlersLength--;
		    i--;
		    continue;
		} else {
		    handlers[i].end = handlers[i].catcher;
		}
	    }

	    if (infos[handlers[i].end].instr.getOpcode() >= opc_ireturn
		&& infos[handlers[i].end].instr.getOpcode() <= opc_return) {
		/* JDK 1.4 sometimes doesn't put return instruction into try
		 * block, which breaks the decompiler later.  The return
		 * instruction can't throw exceptions so it doesn't really
		 * matter.
		 *
		 * FIXME: This may break other things if the return
		 * instruction is reachable from outside the try block.
		 */
		handlers[i].end++;
	    }
	}
	if (handlersLength < handlers.length) {
	    HandlerEntry[] newHandlers = new HandlerEntry[handlersLength];
	    System.arraycopy(handlers, 0, newHandlers, 0,
			     handlersLength);
	    handlers = newHandlers;
	}

	for (int i=0; i< infos.length; i++) {
	    if (infos[i] != null && infos[i].succs != null) {
		int[] succs = infos[i].succs;
		for (int j=0; j < succs.length; j++) {
		    try {
			infos[succs[j]].flags |= IS_BORDER;
		    } catch (RuntimeException ex) {
			throw new ClassFormatException
			    ("Illegal successor: " + bb+":"+i);
		    }
		}
	    }
	}

	for (int i=0; i< handlersLength; i ++) {
	    /* Mark the instructions as border instructions.
	     * reachable.
	     */
	    infos[handlers[i].start].flags |= IS_BORDER;
	    if (handlers[i].end < infos.length)
		infos[handlers[i].end].flags |= IS_BORDER;
	    infos[handlers[i].catcher].flags |= IS_BORDER | IS_CATCHER;
	}
    }

    public void readLVT(int length, ConstantPool cp, 
			DataInputStream input) throws IOException {
	if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0) 
	    GlobalOptions.err.println("LocalVariableTable of "+bb);
	int count = input.readUnsignedShort();
	if (length != 2 + count * 10) {
	    if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0) 
		GlobalOptions.err.println("Illegal LVT length, ignoring it");
	    return;
	}
	Vector[] lvt = new Vector[maxLocals];
	for (int i=0; i < count; i++) {
	    LVTEntry lve = new LVTEntry();
	    lve.start  = input.readUnsignedShort();
	    lve.end    = lve.start + input.readUnsignedShort();
	    int nameIndex = input.readUnsignedShort();
	    int typeIndex = input.readUnsignedShort();
	    int slot = input.readUnsignedShort();
	    if (nameIndex == 0 || cp.getTag(nameIndex) != ConstantPool.UTF8
		|| typeIndex == 0 || cp.getTag(typeIndex) != ConstantPool.UTF8
		|| slot >= maxLocals) {
		
		// This is probably an evil lvt as created by HashJava
		// simply ignore it.
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_LVT) != 0) 
		    GlobalOptions.err.println
			("Illegal entry, ignoring LVT");
		lvt = null;
		return;
	    }
	    lve.name = cp.getUTF8(nameIndex);
	    lve.type = cp.getUTF8(typeIndex);
	    if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0)
		GlobalOptions.err.println("\t" + lve.name + ": "
					  + lve.type
					  +" range "+lve.start
					  +" - "+lve.end
					  +" slot "+slot);
	    if (lvt[slot] == null)
		lvt[slot] = new Vector();
	    lvt[slot].addElement(lve);
	}
	for (int i = 0; i< infos.length; i = infos[i].nextAddr) {
	    Instruction instr = infos[i].instr;
	    if (instr.hasLocal()) {
		LocalVariableInfo lvi = instr.getLocalInfo();
		int slot = lvi.getSlot();
		if (lvt[slot] == null)
		    continue;
		int addr = i;
		if (instr.getOpcode() >= opc_istore
		    && instr.getOpcode() <= opc_astore)
		    addr = infos[i].nextAddr;

		Enumeration enumeration = lvt[slot].elements();
		LVTEntry match = null;
		while (enumeration.hasMoreElements()) {
		    LVTEntry lve = (LVTEntry) enumeration.nextElement();
		    if (lve.start <= addr && lve.end > addr) {
			if (match != null
			    && (!match.name.equals(lve.name)
				|| !match.type.equals(lve.type))) {
			    /* Multiple matches..., give no info */
			    match = null;
			    break;
			}
			match = lve;
		    }
		}
		if (match != null)
		    instr.setLocalInfo(LocalVariableInfo
				       .getInfo(slot, match.name, match.type));
	    }
	}

	int paramCount = bb.getParamCount();
	for (int slot=0; slot< paramCount; slot++) {
	    if (lvt[slot] == null)
		continue;
	    Enumeration enumeration = lvt[slot].elements();
	    LVTEntry match = null;
	    while (enumeration.hasMoreElements()) {
		LVTEntry lve = (LVTEntry) enumeration.nextElement();
		if (lve.start == 0) {
		    if (match != null
			&& (!match.name.equals(lve.name)
			    || !match.type.equals(lve.type))) {
			/* Multiple matches..., give no info */
			match = null;
			break;
		    }
		    match = lve;
		}
	    }
	    if (match != null) {
		bb.setParamInfo(LocalVariableInfo
				.getInfo(slot, match.name, match.type));
	    }
        }
    } 

    public void readLNT(int length, ConstantPool cp, 
			DataInputStream input) throws IOException {
	int count = input.readUnsignedShort();
	if (length != 2 + count * 4) {
	    GlobalOptions.err.println
		("Illegal LineNumberTable, ignoring it");
	    return;
	}
	for (int i = 0; i < count; i++) {
	    int start = input.readUnsignedShort();
	    infos[start].instr.setLineNr(input.readUnsignedShort());
	}

	int lastLine = -1;
	for (int i = 0; i< infos.length; i = infos[i].nextAddr) {
	    Instruction instr = infos[i].instr;
	    if (instr.hasLineNr())
		lastLine = instr.getLineNr();
	    else
		instr.setLineNr(lastLine);
	}
    }
}

