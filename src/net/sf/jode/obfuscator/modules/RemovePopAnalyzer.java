/* RemovePopAnalyzer Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: RemovePopAnalyzer.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.obfuscator.modules;
import net.sf.jode.bytecode.*;
import net.sf.jode.obfuscator.*;
import net.sf.jode.GlobalOptions;

import java.util.BitSet;
///#def COLLECTIONS java.util
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Stack;
///#enddef

public class RemovePopAnalyzer implements CodeTransformer, Opcodes {
    public RemovePopAnalyzer() {
    }

    static class BlockInfo {
	/* A bitset of stack entries at the beginning of the block,
	 * whose values should be never put put onto the stack.  
	 * This array is shared with all other blocks that have
	 * a common predecessor.
	 */
	int[] poppedBefore;

	/* A bitset of instructions, that should be removed, i.e. their
	 * parameters should just get popped.
	 */
	int[] removedInstrs;

	ArrayList predecessors;
	
	BlockInfo(int[] popped, int[] removed) {
	    this.poppedEntries = popped;
	    this.removedInstrs = removed;
	}

	boolean isPopped(int pos) {
	    return (poppedEntries[pos >> 5] & (1 << (pos & 31))) != 0;
	}
	
	boolean isRemoved(int pos) {
	    return (removedInstrs[pos >> 5] & (1 << (pos & 31))) != 0;
	}
    }

    public BlockInfo analyzeBlock(Block block, BlockInfo oldInfo) {
    }


    /**
     * This method propagates pops through a dup instruction, eventually
     * generating new code and a new set of popped instructions.
     *
     * @param opcode the opcode of the dup instruction.
     * @param newInstruction a list where the new instructions should
     * be added to the front.
     * @param stackDepth the stack depth after the dup is executed.
     * @param poppedEntries the stack slots that should be popped at
     * the end of this dup.  
     * @return The stack slots that should be popped at the start of
     * the dup.
     */
    byte movePopsThroughDup(int opcode,
			    List newInstructions,
			    int poppedEntries) {
	int count = (opcode - opc_dup)/3+1;
	int depth = (opcode - opc_dup)%3;

	/* Calculate which entries can be popped before this instruction,
	 * and update opcode.
	 */
	int newPopped =
	    (((poppedEntries + 1) << depth) - 1) & (poppedEntries >> count);
	int mask = ((1 << count) - 1);
	int poppedDeep = poppedEntries & mask;
	int poppedTop = (poppedEntries >> (depth + count)) & mask;
	boolean swapAfterDup = false;
	boolean swapBeforeDup = false;

	for (int i = count+depth; i > depth; i--) {
	    if ((newPopped & (1 << i)) != 0)
		depth--;
	}

	// adjust the depth
	for (int i = depth; i > 0; i--) {
	    if ((newPopped & (1 << i)) != 0)
		depth--;
	}
	
	// adjust the count and poppedDeep/3
	if ((poppedDeep & poppedTop & 1) != 0) {
	    count--;
	    poppedDeep >>= 1;
	    poppedTop >>= 1;
	    mask >>= 1;
	} else if ((poppedDeep & poppedTop & 2) != 0) {
	    count--;
	    poppedDeep &= 1;
	    poppedTop &= 1;
	    mask &= 1;
	}
	
	if (poppedDeep == mask
	    || (depth == 0 && poppedTop == mask)) {
	    // dup was not necessary
	    return newPopped;
	}
	
	/* Now (poppedDeep & poppedTop) == 0 */
	
	if (poppedTop > 0) {
	    /* Insert the pop for the top elements,  we add
	     * the dup later in front of these instructions.
	     */
	    if (poppedTop == 3) {
		newInstructions.addFirst
		    (Instruction.forOpcode(opc_pop2));
	    } else {
		newInstructions.addFirst
		    (Instruction.forOpcode(opc_pop));
		if (count == 2 && poppedTop == 1)
		    swapAfterDup = true;
	    }
	}

	if (poppedDeep != 0) {
	    if (poppedDeep == 2) {
		/* We swap before and after dupping to get to
		 * poppedDeep = 1 case.
		 */
		swapAfterDup = !swapAfterDup;
		swapBeforeDup = true;
	    }
	    /* The bottom most value is popped; decrease count
	     * and increase depth, so that it won't be created
	     * in the first place.
	     */
	    depth++;
	    count--;
	}

	/* Now all pops are resolved */
	/* Do a dup with count and depth now. */

	if (swapAfterDup)
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_swap));
	
	if (depth < 3) {
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_pop - 3 + 
				       depth + 3 * count));
	} else {
	    // I hope that this will almost never happen.
	    // depth = 3, count = 1;
	    // Note that instructions are backwards.
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_pop2));    //DABCD<
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_dup2_x2)); //DABCDAB<
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_pop));     //DCDAB<
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_dup_x2));  //DCDABD<
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_pop));     //DCABD<
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_dup2_x2)); //DCABDC<
	    swappedBeforeDup = !swappedBeforeDup;     //ABDC<
	}

	if (swapBeforeDup)
	    newInstructions.addFirst
		(Instruction.forOpcode(opc_swap));
	return newPopped;
    }

    /**
     * This method analyzes a block from end to start and removes the
     * pop instructions together with their pushes. It propagates pops
     * to the front removing various instructions on the fly, which may
     * generate new pops for their operands and so on.
     *
     * @param block the block of code.
     * @param poppedEntries the stack slots that should be popped at
     * the end of this block.
     * @return the stack slots that should be popped at the start of
     * the block.
     */
    BitSet movePopsToFront(Block block, BitSet poppedEntries) {
	/* Calculate stack height at end of block. */
	Instruction[] oldInstrs = block.getInstructions();
	LinkedList newInstructions = new LinkedList();

	int instrNr = oldInstrs.length;
	int stackDepth = block.getStackHeight() + block.getStackDelta();
	while (instrNr > 0) {
	    Instruction instr = oldInstrs[--instrNr];
	    if (instr.getOpcode() == opc_nop)
		continue;
	    if (instr.getOpcode() == opc_pop) {
		popsAtEnd.set(stackDepth++);
		continue;
	    }
	    if (instr.getOpcode() == opc_pop2) {
		popsAtEnd.set(stackDepth++);
		popsAtEnd.set(stackDepth++);
		continue;
	    }
	    
	    instr.getStackPopPush(poppush);

	    /* Check if this instr pushes a popped Entry. */
	    boolean push_a_popped = false;
	    boolean push_all_popped = true;
	    for (int j=0; j < poppush[1]; j++) {
		if (poppedEntries.get(j))
		    push_a_popped = true;
		else
		    push_all_popped = false;
	    }

	    if (!push_a_popped) {
		// Normal case:
		// add the instruction and adjust stack depth.
		newInstructions.addFirst(instr);
		stackDepth += poppush[0] - poppush[1];
		continue;
	    }

	    /* We push an entry, that gets popped later */
	    int opcode = instr.getOpcode();
	    switch (opcode) {
	    case opc_dup:
	    case opc_dup_x1:
	    case opc_dup_x2:
	    case opc_dup2: 
	    case opc_dup2_x1:
	    case opc_dup2_x2: {
		int popped = 0;
		for (int j = poppush[1] ; j > 0; j--) {
		    popped <<= 1;
		    if (poppedEntries.get(--stackDepth)) {
			popped |= 1;
			poppedEntries.clear(stackDepth);
		    }
		}
		popped = movePopsThroughDup(opcode, newInstructions, 
					    popped);
		for (int j=0; j < poppush[1]; j++) {
		    if ((popped & 1) != 0)
			poppedEntries.set(stackDepth);
		    stackDepth++;
		    popped >>=1;
		}
		break;
	    }
		
	    case opc_swap:
		if (!push_all_popped) {
		    // swap the popped status
		    if (poppedEntries.get(stackDepth - 1)) {
			poppedEntries.clear(stackDepth - 1);
			poppedEntries.set(stackDepth - 2);
		    } else {
			poppedEntries.set(stackDepth - 1);
			poppedEntries.clear(stackDepth - 2);
		    }
		}
		
	    case opc_ldc2_w:
	    case opc_lload: case opc_dload:
	    case opc_i2l: case opc_i2d:
	    case opc_f2l: case opc_f2d:
	    case opc_ldc:
	    case opc_iload: case opc_fload: case opc_aload:
	    case opc_new:
	    case opc_lneg: case opc_dneg:
	    case opc_l2d: case opc_d2l:
	    case opc_laload: case opc_daload:
	    case opc_ineg: case opc_fneg: 
	    case opc_i2f:  case opc_f2i:
	    case opc_i2b: case opc_i2c: case opc_i2s:
	    case opc_newarray: case opc_anewarray:
	    case opc_arraylength:
	    case opc_instanceof:
	    case opc_lshl: case opc_lshr: case opc_lushr:
	    case opc_iaload: case opc_faload: case opc_aaload:
	    case opc_baload: case opc_caload: case opc_saload:
	    case opc_iadd: case opc_fadd:
	    case opc_isub: case opc_fsub:
	    case opc_imul: case opc_fmul:
	    case opc_idiv: case opc_fdiv:
	    case opc_irem: case opc_frem:
	    case opc_iand: case opc_ior : case opc_ixor: 
	    case opc_ishl: case opc_ishr: case opc_iushr:
	    case opc_fcmpl: case opc_fcmpg:
	    case opc_l2i: case opc_l2f:
	    case opc_d2i: case opc_d2f:
	    case opc_ladd: case opc_dadd:
	    case opc_lsub: case opc_dsub:
	    case opc_lmul: case opc_dmul:
	    case opc_ldiv: case opc_ddiv:
	    case opc_lrem: case opc_drem:
	    case opc_land: case opc_lor : case opc_lxor:
	    case opc_lcmp:
	    case opc_dcmpl: case opc_dcmpg:
	    case opc_getstatic:
	    case opc_getfield:
	    case opc_multianewarray:

		/* The simple instructions, that can be removed. */
		if (!push_all_popped)
		    throw new InternalError("pop half of a long");
		if (poppush[0] < poppush[1]) {
		    for (int j=0; j < poppush[0] - poppush[1]; j++)
			poppedEntries.set(stackDepth++);
		} else if (poppush[0] < poppush[1]) {
		    for (int j=0; j < poppush[0] - poppush[1]; j++)
			poppedEntries.clear(--stackDepth);
		}
		
	    case opc_invokevirtual:
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_checkcast:
		
		/* These instructions can't be removed, since
		 * they have side effects.
		 */
		if (!push_all_popped)
		    throw new InternalError("pop half of a long");
		if (poppush[1] == 1) {
		    poppedEntries.clear(--stackDepth);
		    newInstructions
			.addFirst(Instruction.forOpcode(opc_pop));
		} else {
		    poppedEntries.clear(--stackDepth);
		    poppedEntries.clear(--stackDepth);
		    newInstructions
			.addFirst(Instruction.forOpcode(opc_pop2));
		}
		newInstructions.addFirst(instr);
	    default:
		throw new InternalError("Unexpected opcode!");
	    }
	}
	blocks[i].setCode((Instruction[]) newInstructions
			  .toArray(new Instruction[newInstructions.size()]),
			  blocks[i].getSuccs());
	return poppedEntries;
    }

    /**
     * This method analyzes a block from start to end and inserts the
     * pop instructions at the right places. It is used if a pop couldn't
     * be removed for some reason.
     *
     * @param block the block of code.
     * @param poppedEntries the stack slots that should be popped at
     * the end of this block.
     * @return the stack slots that should be popped at the start of
     * the block.
     */
    void movePopsToTail(Block block, BitSet poppedEntries) {
	/* Calculate stack height at end of block. */
	Instruction[] oldInstrs = block.getInstructions();
	ArrayList newInstructions = new ArrayList();

	int instrNr = oldInstrs.length;
	int stackDepth = block.getStackHeight();
	for (instrNr = 0; instrNr < oldInstrs.length; instrNr++) {
	    while (poppedEntries.get(stackDepth-1)) {
		poppedEntries.clear(--stackDepth);
		/* XXX opc_pop2?*/
		newInstructions.add(Instruction.forOpcode(opc_pop));
	    }

	    Instruction instr = oldInstrs[--instrNr];
	    instr.getStackPopPush(poppush);
	    /* XXX handle pops inside a opc_dup */
	}
	block.setCode((Instruction[]) newInstructions
		      .toArray(new Instruction[newInstructions.size()]),
		      block.getSuccs());
    }

    public void transformCode(BasicBlocks bb) {
	if (bb.getStartBlock() == null)
	    return;

	BlockInfo[] infos = calcBlockInfos(bb);

	int poppush[] = new int[2];
	boolean poppedEntries[] = new boolean[bb.getMaxStack()];
	Block[] blocks = bb.getBlocks();
	for (int i = 0; i < blocks.length; i++) {
	    blocks[i].setCode((Instruction[]) newInstructions
			      .toArray(oldInstrs), blocks[i].getSuccs());
	}
    }
}
