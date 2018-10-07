/* TransformExceptionHandlers Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: TransformExceptionHandlers.java 1412 2012-03-01 22:52:08Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.GlobalOptions;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.LocalInfo;
import net.sf.jode.expr.*;

///#def COLLECTIONS java.util
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Set;
import java.util.Iterator;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.Comparable;
///#enddef

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformExceptionHandlers {
    SortedSet handlers;
    FlowBlock[] flowBlocks;
    
    static class Handler implements Comparable {
	FlowBlock start;
	FlowBlock end;
	FlowBlock handler;
	Type type;

	public Handler(FlowBlock tryBlock, FlowBlock endBlock, 
		       FlowBlock catchBlock, Type type) {
	    this.start = tryBlock;
	    this.end = endBlock;
	    this.handler = catchBlock;
	    this.type = type;
	}

	public int compareTo (Object o) {
	    Handler second = (Handler) o;

	    /* First sort by start offsets, highest block number first...*/
	    if (start.getBlockNr() != second.start.getBlockNr())
		/* this subtraction is save since block numbers are only 16 bit */
		return second.start.getBlockNr() - start.getBlockNr();

	    /* ...Second sort by end offsets, lowest block number first...
	     * this will move the innermost blocks to the beginning. */
	    if (end.getBlockNr() != second.end.getBlockNr())
		return end.getBlockNr() - second.end.getBlockNr();

	    /* ...Last sort by handler offsets, lowest first */
	    if (handler.getBlockNr() != second.handler.getBlockNr())
		return handler.getBlockNr() - second.handler.getBlockNr();
	    
	    /* ...Last sort by typecode signature.  Shouldn't happen to often.
	     */
	    if (type == second.type)
		return 0;
	    if (type == null)
		return -1;
	    if (second.type == null)
		return 1;
	    return type.getTypeSignature()
		.compareTo(second.type.getTypeSignature());
	}
    }

    public TransformExceptionHandlers(FlowBlock[] flowBlocks) {
	handlers = new TreeSet();
	this.flowBlocks = flowBlocks;
    }

    /**
     * Add an exception Handler.
     * @param start The start block number of the exception range.
     * @param end The end block number of the exception range + 1.
     * @param handler The block number of the handler.
     * @param type The type of the exception, null for ALL.
     */
    public void addHandler(FlowBlock tryBlock, FlowBlock endBlock, 
			   FlowBlock catchBlock, Type type) {
	handlers.add(new Handler(tryBlock, endBlock, catchBlock, type));
    }

    /**
     * Merge the try flow block with the catch flow block.  This is a kind
     * of special T2 transformation, as all jumps to the catch block are
     * implicit (exception can be thrown everywhere). <br>
     *
     * This method doesn't actually merge the contents of the blocks.  The
     * caller should do it right afterwards. <br>
     *
     * The flow block catchFlow mustn't have any predecessors.
     * @param tryFlow the flow block containing the try.
     * @param catchFlow the flow block containing the catch handler.
     */
    static void mergeTryCatch(FlowBlock tryFlow, FlowBlock catchFlow) {
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_ANALYZE) != 0)
	    GlobalOptions.err.println
		("mergeTryCatch(" + tryFlow.getBlockNr()
		 + ", " + catchFlow.getBlockNr() + ")");
	tryFlow.updateInOutCatch(catchFlow);
	tryFlow.mergeSuccessors(catchFlow);
	tryFlow.mergeBlockNr(catchFlow);
    }

									      
    /**
     * Analyzes a simple try/catch block.  The try and catch part are both
     * analyzed, the try block is already created, but the catch block
     * isn't.  <br>
     * The catchFlow block mustn't have any predecessors.
     *
     * @param type The type of the exception which is caught.
     * @param tryFlow The flow block containing the try.  The contained
     * block must be a try block.
     * @param catchFlow the flow block containing the catch handler.
     */
    static void analyzeCatchBlock(Type type, FlowBlock tryFlow, 
				  FlowBlock catchFlow) {
	/* Merge try and catch flow blocks */
	mergeTryCatch(tryFlow, catchFlow);

	/* Insert catch block into tryFlow */
        CatchBlock newBlock = new CatchBlock(type);
        ((TryBlock)tryFlow.block).addCatchBlock(newBlock);
        newBlock.setCatchBlock(catchFlow.block);
	tryFlow.lastModified = tryFlow.block;
    }

    /**
     * This transforms a sub routine, i.e. it checks if the beginning
     * local assignment matches the final ret and removes both.  It also
     * accepts sub routines that just pop their return address.
     */
    boolean transformSubRoutine(StructuredBlock subRoutineBlock) {
	StructuredBlock firstBlock = subRoutineBlock;
	if (firstBlock instanceof SequentialBlock)
	    firstBlock = subRoutineBlock.getSubBlocks()[0];

	LocalInfo local = null;
	if (firstBlock instanceof SpecialBlock) {
	    SpecialBlock popBlock
		= (SpecialBlock) firstBlock;
	    if (popBlock.type != SpecialBlock.POP
		|| popBlock.count != 1)
		return false;
	} else if (firstBlock instanceof InstructionBlock) {
	    Expression expr
		= ((InstructionBlock) firstBlock).getInstruction();
	    if (expr instanceof StoreInstruction
		&& ((StoreInstruction) 
		    expr).getLValue() instanceof LocalStoreOperator) {
		LocalStoreOperator store = (LocalStoreOperator) 
		    ((StoreInstruction)expr).getLValue();
		local = store.getLocalInfo();
		expr = ((StoreInstruction) expr).getSubExpressions()[1];
	    }
	    if (!(expr instanceof NopOperator))
		return false;
	} else
	    return false;

	/* We are now committed and can start changing code.  Remove
	 * the first Statement which stores/removes the return
	 * address.
	 */
	firstBlock.removeBlock();
        
	/* We don't check if there is a RET in the middle.
	 *
	 * This is a complicated task which isn't needed for javac nor
	 * jikes.  We just check if the last instruction is a ret and
	 * remove this.  This will never produce code with wrong semantic,
	 * as long as the bytecode was verified correctly.
	 */
        while (subRoutineBlock instanceof SequentialBlock)
            subRoutineBlock = subRoutineBlock.getSubBlocks()[1];

        if (subRoutineBlock instanceof RetBlock
            && (((RetBlock) subRoutineBlock).local.equals(local))) {
	    subRoutineBlock.removeBlock();
	}
        return true;
    }

    /**
     * Remove the locale that javac introduces to temporary store the return
     * value, when it executes a finally block resp. monitorexit
     * @param ret the ReturnBlock.
     */
    private void removeReturnLocal(ReturnBlock ret) {
	StructuredBlock pred = getPredecessor(ret);
	if (!(pred instanceof InstructionBlock))
	    return;
	Expression instr = ((InstructionBlock) pred).getInstruction();
	if (!(instr instanceof StoreInstruction))
	    return;

	Expression retInstr = ret.getInstruction();
	if (!(retInstr instanceof LocalLoadOperator
	      && ((StoreInstruction) instr).lvalueMatches
	      ((LocalLoadOperator) retInstr)))
	    return;

	Expression rvalue = ((StoreInstruction) instr).getSubExpressions()[1];
	ret.setInstruction(rvalue);
	ret.replace(ret.outer);
    }

    /**
     * Remove the JSRs jumping to the specified subRoutine. The right
     * JSRs are marked and we can just remove them.  For the other JSR
     * instructions we replace them with a warning.
     * @param tryFlow the FlowBlock of the try block.
     * @param subRoutine the FlowBlock of the sub routine.
     */
    private void removeJSR(FlowBlock tryFlow, FlowBlock subRoutine) {
        for (Jump jumps = tryFlow.removeJumps(subRoutine); 
	     jumps != null; jumps = jumps.next) {

            StructuredBlock prev = jumps.prev;
	    prev.removeJump();

            if (prev instanceof EmptyBlock
                && prev.outer instanceof JsrBlock
		&& ((JsrBlock) prev.outer).isGood()) {
		StructuredBlock next = prev.outer.getNextBlock();
		prev.outer.removeBlock();
		if (next instanceof ReturnBlock)
		    removeReturnLocal((ReturnBlock) next);
	    } else {
		/* We have a jump to the subroutine, that is badly placed.
		 * We complain here.
		 */
		DescriptionBlock msg = new DescriptionBlock
		    ("ERROR: invalid jump to finally block!");
		prev.appendBlock(msg);
	    }
        }
    }
    
    private static StructuredBlock getPredecessor(StructuredBlock stmt)
    {
	if (stmt.outer instanceof SequentialBlock) {
	    SequentialBlock seq = (SequentialBlock) stmt.outer;
	    if (seq.subBlocks[1] == stmt)
		return seq.subBlocks[0];
	    else if (seq.outer instanceof SequentialBlock)
		return seq.outer.getSubBlocks()[0];
	}
	return null;
    }

    /**
     * Gets the slot of the monitorexit instruction instr in the
     * stmt, or -1 if stmt isn't a InstructionBlock with a
     * monitorexit instruction.
     * @param stmt the stmt, may be null.
     */
    private static int getMonitorExitSlot(StructuredBlock stmt) {
	if (stmt instanceof InstructionBlock) {
	    Expression instr = ((InstructionBlock) stmt).getInstruction();
	    if (instr instanceof MonitorExitOperator) {
		MonitorExitOperator monExit = (MonitorExitOperator)instr;
		if (monExit.getFreeOperandCount() == 0
		    && (monExit.getSubExpressions()[0] 
			instanceof LocalLoadOperator))
		return ((LocalLoadOperator) monExit.getSubExpressions()[0])
                    .getLocalInfo().getSlot();
            }
        }
        return -1;
    }
    
    private boolean isMonitorExitSubRoutine(FlowBlock subRoutine, 
					    LocalInfo local) {
	if (transformSubRoutine(subRoutine.block)
	    && getMonitorExitSlot(subRoutine.block) == local.getSlot())
	    return true;
	return false;
    }

    private static StructuredBlock skipFinExitChain(StructuredBlock block)
    {
	StructuredBlock pred, result;
	if (block instanceof ReturnBlock)
	    pred = getPredecessor(block);
	else
	    pred = block;
	result = null;

	while (pred instanceof JsrBlock
	       || getMonitorExitSlot(pred) >= 0) {
	    result = pred;
	    pred = getPredecessor(pred);
	} 
	return result;
    }
					
				    
    private void checkAndRemoveJSR(FlowBlock tryFlow, 
				   FlowBlock subRoutine,
				   int startOutExit, int endOutExit) {
        Iterator iter = tryFlow.getSuccessors().iterator();
    dest_loop:
        while (iter.hasNext()) {
	    FlowBlock dest = (FlowBlock) iter.next();
            if (dest == subRoutine)
                continue dest_loop;

	    boolean isFirstJump = true;
            for (Jump jumps = tryFlow.getJumps(dest);
		 jumps != null; jumps = jumps.next, isFirstJump = false) {

                StructuredBlock prev = jumps.prev;
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
		    /* This jump is a jsr, since it doesn't leave the
		     * block forever, we can ignore it.
		     */
		    continue;
                }

		StructuredBlock pred = skipFinExitChain(prev);
		if (pred instanceof JsrBlock) {
		    JsrBlock jsr = (JsrBlock) pred;
		    StructuredBlock jsrInner = jsr.innerBlock;
		    if (jsrInner instanceof EmptyBlock
			&& jsrInner.jump != null
			&& jsrInner.jump.destination == subRoutine) {
			/* The jump is preceeded by the right jsr.  Mark the
			 * jsr as good.
			 */
			jsr.setGood(true);
			continue;
		    }
		}

		if (pred == null && isFirstJump) {
		    /* Now we have a jump that is not preceded by any
		     * jsr.  There's a last chance: the jump jumps
		     * directly to a correct jsr instruction, which
		     * lies outside the try/catch block.  
		     */
		    if (jumps.destination.predecessors.size() == 1
			&& jumps.destination.getBlockNr() >= startOutExit
			&& jumps.destination.getNextBlockNr() <= endOutExit) {
			jumps.destination.analyze(startOutExit, endOutExit);
		    
			StructuredBlock sb = jumps.destination.block;
			if (sb instanceof SequentialBlock)
			    sb = sb.getSubBlocks()[0];
			if (sb instanceof JsrBlock
			    && sb.getSubBlocks()[0] instanceof EmptyBlock
			    && (sb.getSubBlocks()[0].jump.destination
				== subRoutine)) {
			    StructuredBlock jsrInner = sb.getSubBlocks()[0];
			    jumps.destination.removeSuccessor(jsrInner.jump);
			    jsrInner.removeJump();
			    sb.removeBlock();
			    continue dest_loop;
			}
		    }
		}
		
                /* Now we have a jump with a wrong destination.
                 * Complain!
                 */
                DescriptionBlock msg 
                    = new DescriptionBlock("ERROR: no jsr to finally");
		if (pred != null)
		    pred.prependBlock(msg);
		else {
		    prev.appendBlock(msg);
		    msg.moveJump(prev.jump);
		}
            }
        }
	if (tryFlow.getSuccessors().contains(subRoutine))
	    removeJSR(tryFlow, subRoutine);
    }

    private void checkAndRemoveMonitorExit(FlowBlock tryFlow, 
					   LocalInfo local, 
					   int start, int end) {
        FlowBlock subRoutine = null;
        Iterator succs = tryFlow.getSuccessors().iterator();
    dest_loop:
        while (succs.hasNext()) {
	    boolean isFirstJump = true;
	    FlowBlock successor = (FlowBlock) succs.next();
            for (Jump jumps = tryFlow.getJumps(successor);
                 jumps != null; jumps = jumps.next, isFirstJump = false) {

                StructuredBlock prev = jumps.prev;
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
                    /* This jump is really a jsr, since it doesn't
		     * leave the block forever, we can ignore it.
		     */
		    continue;
                }
		StructuredBlock pred = skipFinExitChain(prev);
		if (pred instanceof JsrBlock) {
		    JsrBlock jsr = (JsrBlock) pred;
		    StructuredBlock jsrInner = jsr.innerBlock;
		    if (jsrInner instanceof EmptyBlock
			&& jsrInner.jump != null) {
			FlowBlock dest = jsrInner.jump.destination;

			if (subRoutine == null
			    && dest.getBlockNr() >= start
			    && dest.getNextBlockNr() <= end) {
			    dest.analyze(start, end);
			    if (isMonitorExitSubRoutine(dest, local))
				subRoutine = dest;
			}

			if (dest == subRoutine) {
			    /* The jump is preceeded by the right jsr.
			     * Mark it as good.
			     */
			    jsr.setGood(true);
			    continue;
			}
		    }
		} else if (getMonitorExitSlot(pred) == local.getSlot()) {
		    /* The jump is preceeded by the right monitor
		     * exit instruction.
		     */
		    pred.removeBlock();
		    if (prev instanceof ReturnBlock)
			removeReturnLocal((ReturnBlock) prev);
		    continue;
		}

		if (pred == null && isFirstJump) {
		    /* Now we have a jump that is not preceded by a
		     * monitorexit.  There's a last chance: the jump
		     * jumps directly to the correct monitorexit
		     * instruction, which lies outside the try/catch
		     * block.  
		     */
		    if (successor.predecessors.size() == 1
			&& successor.getBlockNr() >= start
			&& successor.getNextBlockNr() <=  end) {
			successor.analyze(start, end);
		    
			StructuredBlock sb = successor.block;
			if (sb instanceof SequentialBlock)
			    sb = sb.getSubBlocks()[0];
			if (sb instanceof JsrBlock
			    && sb.getSubBlocks()[0] instanceof EmptyBlock) {
			    StructuredBlock jsrInner = sb.getSubBlocks()[0];
			    FlowBlock dest = jsrInner.jump.destination;
			    if (subRoutine == null
				&& dest.getBlockNr() >= start
				&& dest.getNextBlockNr() <= end) {
				dest.analyze(start, end);
				if (isMonitorExitSubRoutine(dest, local))
				    subRoutine = dest;
			    }

			    if (subRoutine == dest) {
				successor.removeSuccessor(jsrInner.jump);
				jsrInner.removeJump();
				sb.removeBlock();
				continue dest_loop;
			    }
			}
			if (getMonitorExitSlot(sb) == local.getSlot()) {
			    sb.removeBlock();
			    continue dest_loop;
			}
		    }
		}
		
		/* Complain!
                 */
                DescriptionBlock msg 
                    = new DescriptionBlock("ERROR: no monitorexit");
                prev.appendBlock(msg);
                msg.moveJump(jumps);
            }  
        }

	if (subRoutine != null) {
	    if (tryFlow.getSuccessors().contains(subRoutine))
		removeJSR(tryFlow, subRoutine);
	    if (subRoutine.predecessors.size() == 0)
		tryFlow.mergeBlockNr(subRoutine);
	}
    }

    private StoreInstruction getExceptionStore(StructuredBlock catchBlock) {
        if (!(catchBlock instanceof SequentialBlock)
            || !(catchBlock.getSubBlocks()[0] instanceof InstructionBlock))
            return null;
        
        Expression instr = 
            ((InstructionBlock)catchBlock.getSubBlocks()[0]).getInstruction();
	if (!(instr instanceof StoreInstruction))
	    return null;

	StoreInstruction store = (StoreInstruction) instr;
	if (!(store.getLValue() instanceof LocalStoreOperator
	      && store.getSubExpressions()[1] instanceof NopOperator))
	    return null;
    
	return store;
    }

    private boolean analyzeSynchronized(FlowBlock tryFlow, 
                                        FlowBlock catchFlow,
                                        int endHandler) {
	/* Check if this is a synchronized block.  We mustn't change
	 * anything until we are sure.
	 */
	StructuredBlock catchBlock = catchFlow.block;

	/* Check for a optional exception store and skip it */
	StoreInstruction excStore = getExceptionStore(catchBlock);
	if (excStore != null)
	    catchBlock = catchBlock.getSubBlocks()[1];

        /* Check for the monitorexit instruction */
        if (!(catchBlock instanceof SequentialBlock
              && catchBlock.getSubBlocks()[0] 
              instanceof InstructionBlock))
            return false;
        Expression instr = 
            ((InstructionBlock)catchBlock.getSubBlocks()[0]).getInstruction();
        if (!(instr instanceof MonitorExitOperator
	      && instr.getFreeOperandCount() == 0
	      && (((MonitorExitOperator)instr).getSubExpressions()[0] 
		  instanceof LocalLoadOperator)
	      && catchBlock.getSubBlocks()[1] instanceof ThrowBlock))
	    return false;

        /* Check for the throw instruction */
        Expression throwInstr = 
	    ((ThrowBlock)catchBlock.getSubBlocks()[1]).getInstruction();
	if (excStore != null) {
	    if (!(throwInstr instanceof Operator
		  && excStore.lvalueMatches((Operator)throwInstr)))
		return false;
	} else {
	    if (!(throwInstr instanceof NopOperator))
		return false;
	}

	/* This is a synchronized block:
	 *
	 *  local_x = monitor object;  // later
	 *  monitorenter local_x       // later
	 *  tryFlow:
	 *   |- synchronized block
	 *   |  ...
	 *   |   every jump to outside is preceded by jsr subroutine-,
	 *   |  ...                                                  |
	 *   |- monitorexit local_x                                  |
	 *   `  jump after this block (without jsr monexit)          |
	 *                                                           |
	 *  catchBlock:                                               |
	 *      local_n = stack                                      |
	 *      monitorexit local_x                                  |
	 *      throw local_n                                        |
	 *   [OR ALTERNATIVELY:]                                     |
	 *      monitorexit local_x                                  |
	 *      throw stack                                          |
	 *  optional subroutine: <-----------------------------------'
	 *    astore_n
	 *    monitorexit local_x
	 *    return_n
	 */

	/* Merge try and catch flow blocks.  No need to insert the
	 * catchFlow.block into the try flow though, since all its
	 * instruction are synthetic.
	 */
	mergeTryCatch(tryFlow, catchFlow);
	
	MonitorExitOperator monexit = (MonitorExitOperator)
	    ((InstructionBlock) catchBlock.getSubBlocks()[0]).instr;
	LocalInfo local = 
	    ((LocalLoadOperator)monexit.getSubExpressions()[0])
	    .getLocalInfo();
	
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_ANALYZE) != 0)
	    GlobalOptions.err.println
		("analyzeSynchronized(" + tryFlow.getBlockNr()
		 + "," + tryFlow.getNextBlockNr() + "," + endHandler + ")");
	
	checkAndRemoveMonitorExit
	    (tryFlow, local, tryFlow.getNextBlockNr(), endHandler);
	
	SynchronizedBlock syncBlock = new SynchronizedBlock(local);
	TryBlock tryBlock = (TryBlock) tryFlow.block;
	syncBlock.replace(tryBlock);
	syncBlock.moveJump(tryBlock.jump);
	syncBlock.setBodyBlock(tryBlock.subBlocks.length == 1
			       ? tryBlock.subBlocks[0] : tryBlock);
	tryFlow.lastModified = syncBlock;
	return true;
    }

    /**
     * Merge try and finally flow blocks.
     * @param tryFlow The try flow block.  Its contained block must be
     * a try block.
     * @param catchFlow The catch flow block that contains the finally
     * block.
     * @param finallyBlock block that either contains the finally block.
     * It is part of the catchFlow.  The other parts of catchFlow are
     * synthetic and can be removed.
     */
    private void mergeFinallyBlock(FlowBlock tryFlow, FlowBlock catchFlow,
				   StructuredBlock finallyBlock) {
	TryBlock tryBlock = (TryBlock) tryFlow.block;
	if (tryBlock.getSubBlocks()[0] instanceof TryBlock) {
	    /* A try { try { } catch {} } finally{}  is equivalent
	     * to a try {} catch {} finally {}
	     * so remove the surrounding tryBlock.
	     */
	    TryBlock innerTry = (TryBlock)tryBlock.getSubBlocks()[0];
	    innerTry.gen = tryBlock.gen;
	    innerTry.replace(tryBlock);
	    tryBlock = innerTry;
	    tryFlow.lastModified = tryBlock;
	    tryFlow.block = tryBlock;
	}

	/* Now merge try and catch flow blocks */
	mergeTryCatch(tryFlow, catchFlow);
	FinallyBlock newBlock = new FinallyBlock();
	newBlock.setCatchBlock(finallyBlock);
	tryBlock.addCatchBlock(newBlock);
    }

    private boolean analyzeFinally(FlowBlock tryFlow, 
				   FlowBlock catchFlow, int end) {

        /* Layout of a try-finally block:  
         *     
         *   tryFlow:
         *    |- first instruction
         *    |  ...
         *    |  every jump to outside is preceded by jsr finally
         *    |  ...
         *    |  jsr finally -----------------,
         *    `- jump after finally           |
         *                                    |
         *   catchBlock:                      |
         *       local_n = stack              v
         *       jsr finally ---------------->|
         *       throw local_n;               |
         *   finally: <-----------------------'
         *      astore_n
         *      ...
         *      return_n
         */
        
	StructuredBlock catchBlock = catchFlow.block;
	StoreInstruction excStore = getExceptionStore(catchBlock);
	if (excStore == null)
	    return false;

	catchBlock = catchBlock.getSubBlocks()[1];
        if (!(catchBlock instanceof SequentialBlock))
            return false;
        
        StructuredBlock finallyBlock = null;

        if (catchBlock.getSubBlocks()[0] instanceof LoopBlock) {
            /* In case the try block has no exit (that means, it
             * throws an exception or loops forever), the finallyBlock
             * was already merged with the catchBlock.  We have to
             * check for this case separately:
             *
             * do {
             *    JSR
             *       break;
             *    throw local_x
             * } while(false);
             * finallyBlock; (starts with POP / local_y = POP)
             */
            LoopBlock doWhileFalse = (LoopBlock)catchBlock.getSubBlocks()[0];
            if (doWhileFalse.type == LoopBlock.DOWHILE
                && doWhileFalse.cond == LoopBlock.FALSE
                && doWhileFalse.bodyBlock instanceof SequentialBlock) {
		if (transformSubRoutine(catchBlock.getSubBlocks()[1])) {
		    finallyBlock = catchBlock.getSubBlocks()[1];
		    catchBlock = doWhileFalse.bodyBlock;
		}
            }
        }

        if (!(catchBlock instanceof SequentialBlock
	      && catchBlock.getSubBlocks()[0] instanceof JsrBlock
	      && catchBlock.getSubBlocks()[1] instanceof ThrowBlock))

	    return false;
	
	JsrBlock jsrBlock = (JsrBlock)catchBlock.getSubBlocks()[0];
	ThrowBlock throwBlock = (ThrowBlock) catchBlock.getSubBlocks()[1];


	if (!(throwBlock.getInstruction() instanceof Operator
	      && excStore.lvalueMatches((Operator)
					throwBlock.getInstruction())))
	    return false;

	FlowBlock subRoutine;
	if (finallyBlock != null) {
	    /* Check if the jsr breaks (see comment above). We don't 
	     * need to check if it breaks to the right block, because
	     * we know that there is only one Block around the jsr.
	     */
	    if (!(jsrBlock.innerBlock instanceof BreakBlock))
		return false;
	    
	    /* Check if the try block has no exit
	     */
 	    if (tryFlow.getSuccessors().size() > 0)
 		return false;

	    catchBlock = finallyBlock;
	    subRoutine = null;
	} else {
	    if (!(jsrBlock.innerBlock instanceof EmptyBlock))
		return false;
	    finallyBlock = jsrBlock.innerBlock;
	    subRoutine = finallyBlock.jump.destination;

	    /* We are committed now and can start changing the try
	     * block.
	     */
	    checkAndRemoveJSR(tryFlow, subRoutine,
			      tryFlow.getNextBlockNr(), end);


	    /* Now analyze and transform the subroutine.
	     */
	    while (subRoutine.analyze(tryFlow.getNextBlockNr(), end));
	    if (subRoutine.predecessors.size() == 1) {
		/* catchFlow is synthetic, so we can safely remove it
		 * here.
		 */
		subRoutine.mergeBlockNr(catchFlow);
		catchFlow = subRoutine;

		if (!transformSubRoutine(subRoutine.block)) {
		    finallyBlock = subRoutine.block;
		    DescriptionBlock msg = new DescriptionBlock
			("ERROR: Missing return address handling");
		    msg.replace(finallyBlock);
		    msg.appendBlock(finallyBlock);
		}
		finallyBlock = subRoutine.block;
	    }
	}

	/* Now finish it.
	 */
	mergeFinallyBlock(tryFlow, catchFlow, finallyBlock);
	return true;
    }

    private boolean analyzeSpecialFinally(FlowBlock tryFlow, 
					  FlowBlock catchFlow, int end) {
	StructuredBlock finallyBlock = catchFlow.block;
        StructuredBlock firstInstr = 
            finallyBlock instanceof SequentialBlock 
            ? finallyBlock.getSubBlocks()[0]: finallyBlock;

        if (!(firstInstr instanceof SpecialBlock 
	      && ((SpecialBlock)firstInstr).type == SpecialBlock.POP
	      && ((SpecialBlock)firstInstr).count == 1))
	    return false;

	/* This is a special try/finally-block, where
	 * the finally block ends with a break, return or
	 * similar.
	 */

	/* Make sure that resolveJump only works on the inside of the try
	 */
	tryFlow.lastModified = tryFlow.block.getSubBlocks()[0];
	if (finallyBlock instanceof SequentialBlock)
	    finallyBlock = finallyBlock.getSubBlocks()[1];
	else {
	    finallyBlock = new EmptyBlock();
	    finallyBlock.moveJump(firstInstr.jump);

	    /* Handle the jumps in the tryFlow to finallyFlow.
	     */
	    FlowBlock finallyFlow = finallyBlock.jump.destination;
	    if (tryFlow.getSuccessors().contains(finallyFlow)) {
		Jump jumps = tryFlow.removeJumps(finallyFlow);
		jumps = tryFlow.resolveSomeJumps(jumps, finallyFlow);
		tryFlow.resolveRemaining(jumps);
	    }
	}

	/* Complain about all other jumps in try block */
	Set trySuccs = tryFlow.getSuccessors();
	for (Iterator i = trySuccs.iterator(); i.hasNext(); ) {
	    for (Jump jumps = tryFlow.getJumps((FlowBlock) i.next());
		 jumps != null; jumps = jumps.next) {
		DescriptionBlock msg = 
		    new DescriptionBlock
		    ("ERROR: doesn't go through finally block!");
		if (jumps.prev instanceof ReturnBlock) {
		    msg.replace(jumps.prev);
		    msg.appendBlock(jumps.prev);
		} else {
		    jumps.prev.appendBlock(msg);
		    msg.moveJump(jumps);
		}
	    }
        }

	mergeFinallyBlock(tryFlow, catchFlow, finallyBlock);
	/* Following code will work be put inside the finallyBlock */
	tryFlow.lastModified = finallyBlock;
	return true;
    }

    void checkTryCatchOrder() {
        /* Check if try/catch ranges are okay.  The following succeeds
         * for all classes generated by the sun java compiler, but hand
         * optimized classes (or generated by other compilers) will fail.
         */
	Handler last = null;
	for (Iterator i = handlers.iterator(); i.hasNext(); ) {
	    Handler exc = (Handler) i.next();
	    int start = exc.start.getBlockNr();
	    int end = exc.end.getBlockNr();
	    int handler = exc.handler.getBlockNr();
	    if (start > end || handler <= end)
		throw new InternalError
			("ExceptionHandler order failed: not "
			 + start + " < " + end + " <= " + handler);
	    if (last != null
		&& (last.start.getBlockNr() != start
		    || last.end.getBlockNr() != end)) {
		/* The last handler does catch another range. 
		 * Due to the order:
		 *  start < last.start.getBlockNr()
		 *  || end > last.end.getBlockNr()
		 */
		if (end >= last.start.getBlockNr() 
		    && end < last.end.getBlockNr())
		    throw new InternalError
			("Exception handlers ranges are intersecting: ["
			 + last.start.getBlockNr()+", "
			 + last.end.getBlockNr()+"] and ["
			 + start+", "+end+"].");
	    }
	    last = exc;
	}
    }

    /**
     * Analyzes all exception handlers to try/catch/finally or
     * synchronized blocks.
     */
    public void analyze() { 
	checkTryCatchOrder();

	Iterator i = handlers.iterator();
	Handler exc = null;
	Handler next = i.hasNext() ? (Handler) i.next() : null;
	while(next != null) {
	    Handler last = exc;
	    exc = next;
	    next = i.hasNext() ? (Handler) i.next() : null;

	    int startNr = exc.start.getBlockNr();
	    int endNr   = exc.end.getBlockNr();
	    int endHandler = Integer.MAX_VALUE;
	    /* If the next exception handler catches a bigger range
	     * it must surround the handler completely.
	     */
	    if (next != null
		&& next.end.getBlockNr() > endNr)
		endHandler = next.end.getBlockNr() + 1;
	    
	    FlowBlock tryFlow = exc.start;
	    tryFlow.checkConsistent();
	    
	    if (last == null || exc.type == null
		|| last.start.getBlockNr() != startNr
		|| last.end.getBlockNr() != endNr) {
		/* The last handler does catch another range. 
		 * Create a new try block.
		 */
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_ANALYZE) != 0)
		    GlobalOptions.err.println
			("analyzeTry(" + startNr + ", " + endNr+")");
		while(true) {
		    while (tryFlow.analyze(startNr, endNr+1));
		    int nextNr = tryFlow.getNextBlockNr();
		    if (nextNr > endNr)
			break;
		    tryFlow = flowBlocks[nextNr];
		}
		if (tryFlow.getBlockNr() != startNr) {
		    GlobalOptions.err.println
			("Warning: Can't completely analyze try.");
		}
		new TryBlock(tryFlow);
	    } else if (!(tryFlow.block instanceof TryBlock))
		throw new InternalError("no TryBlock");
	    
	    FlowBlock catchFlow = exc.handler;
	    boolean isMultiUsed = catchFlow.predecessors.size() != 0;
	    if (!isMultiUsed && next != null) {
		for (Iterator j = handlers.tailSet(next).iterator(); 
		     j.hasNext();) {
		    Handler h = (Handler) j.next();
		    if (h.handler == catchFlow) {
			isMultiUsed = true;
			break;
		    }
		}
	    }
	    
	    if (isMultiUsed) {
		/* If this exception is used in other exception handlers,
		 * create a new flow block, that jumps to the handler.
		 * This will be our new exception handler.
		 */
		FlowBlock newFlow = new FlowBlock
		    (catchFlow.method, catchFlow.getBlockNr(),
		     catchFlow.prevByCodeOrder);
		newFlow.setSuccessors(new FlowBlock[] { catchFlow });
		newFlow.nextByCodeOrder = catchFlow;
		catchFlow.prevByCodeOrder = newFlow;
		catchFlow = newFlow;
	    } else {
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_ANALYZE) != 0)
		    GlobalOptions.err.println
			("analyzeCatch("
			 + catchFlow.getBlockNr() + ", " + endHandler + ")");
		while (catchFlow.analyze(catchFlow.getBlockNr(), 
					 endHandler));
	    }

	    if (exc.type != null)
		analyzeCatchBlock(exc.type, tryFlow, catchFlow);
		
	    else if (! analyzeSynchronized(tryFlow, catchFlow, endHandler)
		     && ! analyzeFinally(tryFlow, catchFlow, endHandler)
		     && ! analyzeSpecialFinally(tryFlow, catchFlow, 
						endHandler))
		/* As last resort make a catch(Object) block.  This doesn't
		 * compile, but at least it gives a hint what the code
		 * does.
		 */
		analyzeCatchBlock(Type.tObject, tryFlow, catchFlow);
	    
	    tryFlow.checkConsistent();
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_ANALYZE) != 0)
		GlobalOptions.err.println
		    ("analyzeTryCatch(" + tryFlow.getBlockNr() + ", "
		     + tryFlow.getNextBlockNr() + ") done.");
	}
    }
}
