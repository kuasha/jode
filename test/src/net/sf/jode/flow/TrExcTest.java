package net.sf.jode.flow;
import net.sf.jode.decompiler.LocalInfo;
import junit.framework.*;
import net.sf.jode.expr.*;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.TabbedPrintWriter;
import net.sf.jode.GlobalOptions;

public class TrExcTest extends TestCase {
    private static final boolean VERBOSE = false;

    public TrExcTest(String name) {
	super (name);
    }

    public void setUp() {
	GlobalOptions.debuggingFlags |= GlobalOptions.DEBUG_CHECK;
	if (VERBOSE)
	    GlobalOptions.debuggingFlags
		|= GlobalOptions.DEBUG_ANALYZE | GlobalOptions.DEBUG_FLOW;
    }

    FlowBlock[] createFlowBlocks(int n) {
	FlowBlock[] flows = new FlowBlock[n];
	for (int i = 0; i < n; i++)
	    flows[i] = new FlowBlock(null, i, i > 0 ? flows[i-1] : null);
	return flows;
    }

    public void testSynchronized11() throws java.io.IOException {
	FlowBlock[] flows = createFlowBlocks(5);
	LocalInfo thisLocal = new LocalInfo(null, 0);
	LocalInfo tmpLocal = new LocalInfo(null, 1);
	LocalInfo tmp2Local = new LocalInfo(null, 2);

	/* Monitorenter */
	flows[0].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[0].appendBlock
	    (new InstructionBlock(new MonitorEnterOperator()));
	flows[0].setSuccessors(new FlowBlock[] { flows[1] });
	
	/* Synchronized Blocks */
	flows[1].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[1].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmpLocal))), tmpLocal);
	flows[1].appendBlock(new JsrBlock());
	flows[1].setSuccessors(new FlowBlock[] { flows[4], flows[2] });

	flows[2].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, tmpLocal)), tmpLocal);
	flows[2].appendBlock
	    (new ReturnBlock(new NopOperator(Type.tUObject)));
	flows[2].setSuccessors(new FlowBlock[] { FlowBlock.END_OF_METHOD });
	
	/* Catch Exception Blocks */
	flows[3].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[3].appendBlock
	    (new InstructionBlock(new MonitorExitOperator()));
	flows[3].appendBlock
	    (new ThrowBlock(new NopOperator(Type.tUObject)));
	flows[3].setSuccessors(new FlowBlock[0]);

	/* monitorexit subroutine */
	flows[4].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmp2Local))), tmp2Local);
	flows[4].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[4].appendBlock
	    (new InstructionBlock(new MonitorExitOperator()));
	flows[4].appendReadBlock
	    (new RetBlock(tmp2Local), tmp2Local);
	flows[4].setSuccessors(new FlowBlock[0]);

	flows[0].addStartPred();
	TransformExceptionHandlers exc = new TransformExceptionHandlers(flows);
	exc.addHandler(flows[1],flows[2],flows[3], null);
	exc.analyze();
	flows[0].analyze();
	flows[0].removeStartPred();
	if (VERBOSE)
	    flows[0].dumpSource(new TabbedPrintWriter(GlobalOptions.err));
	assertTrue(flows[0].getBlock() instanceof SynchronizedBlock);
	assertTrue(flows[0].getBlock().getSubBlocks()[0] 
		   instanceof ReturnBlock);
    }

    public void testSynchronized13() throws java.io.IOException {
	FlowBlock[] flows = createFlowBlocks(5);
	LocalInfo thisLocal = new LocalInfo(null, 0);
	LocalInfo tmpLocal = new LocalInfo(null, 1);
	LocalInfo tmp2Local = new LocalInfo(null, 2);

	/* Monitorenter */
	flows[0].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[0].appendBlock
	    (new InstructionBlock(new MonitorEnterOperator()));
	flows[0].setSuccessors(new FlowBlock[] { flows[1] });
	
	/* Synchronized Blocks */
	flows[1].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[1].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmpLocal))), tmpLocal);
	flows[1].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[1].appendBlock
	    (new InstructionBlock(new MonitorExitOperator()));
	flows[1].setSuccessors(new FlowBlock[] { flows[2] });

	flows[2].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, tmpLocal)), tmpLocal);
	flows[2].appendBlock
	    (new ReturnBlock(new NopOperator(Type.tUObject)));
	flows[2].setSuccessors(new FlowBlock[] { FlowBlock.END_OF_METHOD });
	
	/* Catch Exception Blocks */
	flows[3].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[3].appendBlock
	    (new InstructionBlock(new MonitorExitOperator()));
	flows[3].appendBlock
	    (new ThrowBlock(new NopOperator(Type.tUObject)));
	flows[3].setSuccessors(new FlowBlock[0]);

	/* monitorexit subroutine */
	flows[4].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmp2Local))), tmp2Local);
	flows[4].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[4].appendBlock
	    (new InstructionBlock(new MonitorExitOperator()));
	flows[4].appendReadBlock
	    (new RetBlock(tmp2Local), tmp2Local);
	flows[4].setSuccessors(new FlowBlock[0]);

	flows[0].addStartPred();
	TransformExceptionHandlers exc = new TransformExceptionHandlers(flows);
	exc.addHandler(flows[1],flows[2],flows[3], null);
	exc.analyze();
	flows[0].analyze();
	flows[0].removeStartPred();
	if (VERBOSE)
	    flows[0].dumpSource(new TabbedPrintWriter(GlobalOptions.err));
	assertTrue(flows[0].getBlock() instanceof SynchronizedBlock);
	assertTrue(flows[0].getBlock().getSubBlocks()[0] 
		   instanceof ReturnBlock);
    }

    public void testSpecialFin() throws java.io.IOException {
	FlowBlock[] flows = createFlowBlocks(3);
	LocalInfo thisLocal = new LocalInfo(null, 0);
	LocalInfo tmpLocal = new LocalInfo(null, 1);
	LocalInfo tmp2Local = new LocalInfo(null, 2);

	/* Try Blocks */
	flows[0].setSuccessors(new FlowBlock[] { flows[2] });
	
	/* Catch Exception Blocks */
	flows[1].appendBlock(new SpecialBlock(SpecialBlock.POP, 1, 0));
	flows[1].setSuccessors(new FlowBlock[] { flows[2] });

	/* subroutine */
	flows[2].appendBlock(new DescriptionBlock("/*FINALLY*/"));
	flows[2].setSuccessors(new FlowBlock[] { FlowBlock.END_OF_METHOD });

	flows[0].addStartPred();
	TransformExceptionHandlers exc = new TransformExceptionHandlers(flows);
	exc.addHandler(flows[0],flows[0],flows[1], null);
	exc.analyze();
	flows[0].analyze();
	flows[0].removeStartPred();
	if (VERBOSE)
	    flows[0].dumpSource(new TabbedPrintWriter(GlobalOptions.err));
	assertTrue("Try", flows[0].getBlock() instanceof TryBlock);
	assertTrue("Empty", flows[0].getBlock().getSubBlocks()[0] instanceof EmptyBlock);
	assertTrue("Finally", flows[0].getBlock().getSubBlocks()[1] instanceof FinallyBlock);
	assertTrue("Descr", flows[0].getBlock().getSubBlocks()[1].getSubBlocks()[0] instanceof DescriptionBlock);
    }

    public void testSpecialFinTryLoops() throws java.io.IOException {
	FlowBlock[] flows = createFlowBlocks(3);
	LocalInfo thisLocal = new LocalInfo(null, 0);
	LocalInfo tmpLocal = new LocalInfo(null, 1);
	LocalInfo tmp2Local = new LocalInfo(null, 2);

	/* Try Blocks */
	flows[0].setSuccessors(new FlowBlock[] { flows[0] });
	
	/* Catch Exception Blocks */
	flows[1].appendBlock(new SpecialBlock(SpecialBlock.POP, 1, 0));
	flows[1].setSuccessors(new FlowBlock[] { flows[2] });

	/* subroutine */
	flows[2].appendBlock(new DescriptionBlock("/*FINALLY*/"));
	flows[2].setSuccessors(new FlowBlock[] { FlowBlock.END_OF_METHOD });

	flows[0].addStartPred();
	TransformExceptionHandlers exc = new TransformExceptionHandlers(flows);
	exc.addHandler(flows[0],flows[0],flows[1], null);
	exc.analyze();
	flows[0].analyze();
	flows[0].removeStartPred();
	if (VERBOSE)
	    flows[0].dumpSource(new TabbedPrintWriter(GlobalOptions.err));
	assertTrue("Try", flows[0].getBlock() instanceof TryBlock);
	assertTrue("Loop", flows[0].getBlock().getSubBlocks()[0] instanceof LoopBlock);
	assertTrue("Empty", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0] instanceof EmptyBlock);
	assertTrue("Finally", flows[0].getBlock().getSubBlocks()[1] instanceof FinallyBlock);
	assertTrue("Descr", flows[0].getBlock().getSubBlocks()[1].getSubBlocks()[0] instanceof DescriptionBlock);
    }

    public void testFinBreaksJikes() throws java.io.IOException {
	FlowBlock[] flows = createFlowBlocks(5);
	LocalInfo thisLocal = new LocalInfo(null, 0);
	LocalInfo tmpLocal = new LocalInfo(null, 1);
	LocalInfo tmp2Local = new LocalInfo(null, 2);

	/* Try Blocks */
	flows[0].appendBlock(new JsrBlock());
	flows[0].setSuccessors(new FlowBlock[] { flows[3], flows[4] });
	
	/* Catch Exception Blocks */
	flows[1].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmpLocal))), tmpLocal);
	flows[1].appendBlock(new JsrBlock());
	flows[1].setSuccessors(new FlowBlock[] { flows[3], flows[2] });

	flows[2].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, tmpLocal)), tmpLocal);
	flows[2].appendBlock
	    (new ThrowBlock(new NopOperator(Type.tUObject)));
	flows[2].setSuccessors(new FlowBlock[0]);

	/* subroutine */
	flows[3].appendBlock(new SpecialBlock(SpecialBlock.POP, 1, 0));
	flows[3].setSuccessors(new FlowBlock[] { flows[4] });

	flows[4].appendBlock(new DescriptionBlock("/*HERE*/"));
	flows[4].setSuccessors(new FlowBlock[] { FlowBlock.END_OF_METHOD });

	flows[0].addStartPred();
	TransformExceptionHandlers exc = new TransformExceptionHandlers(flows);
	exc.addHandler(flows[0],flows[0],flows[1], null);
	exc.analyze();
	flows[0].analyze();
	flows[0].removeStartPred();
	if (VERBOSE)
	    flows[0].dumpSource(new TabbedPrintWriter(GlobalOptions.err));
	assertTrue("Sequ", flows[0].getBlock() instanceof SequentialBlock);
	assertTrue("Loop", flows[0].getBlock().getSubBlocks()[0] instanceof LoopBlock);
	assertTrue("Try", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0] instanceof TryBlock);
	assertTrue("Empty", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[0] instanceof EmptyBlock);
	assertTrue("Finally", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[1] instanceof FinallyBlock);
	assertTrue("Break", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[1].getSubBlocks()[0] instanceof BreakBlock);
	assertTrue("Descr", flows[0].getBlock().getSubBlocks()[1] 
		   instanceof DescriptionBlock); 
    }

    public void testFinCondBreaks() throws java.io.IOException {
	FlowBlock[] flows = createFlowBlocks(6);
	LocalInfo thisLocal = new LocalInfo(null, 0);
	LocalInfo tmpLocal = new LocalInfo(null, 1);
	LocalInfo tmp2Local = new LocalInfo(null, 2);

	/* Try Blocks */
	flows[0].appendBlock(new JsrBlock());
	flows[0].setSuccessors(new FlowBlock[] { flows[3], flows[5] });
	
	/* Catch Exception Blocks */
	flows[1].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmpLocal))), tmpLocal);
	flows[1].appendBlock(new JsrBlock());
	flows[1].setSuccessors(new FlowBlock[] { flows[3], flows[2] });

	flows[2].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, tmpLocal)), tmpLocal);
	flows[2].appendBlock
	    (new ThrowBlock(new NopOperator(Type.tUObject)));
	flows[2].setSuccessors(new FlowBlock[0]);

	/* subroutine */
	flows[3].appendWriteBlock
	    (new InstructionBlock
	     (new StoreInstruction
	      (new LocalStoreOperator(Type.tUObject, tmp2Local))), tmp2Local);
	flows[3].appendReadBlock
	    (new InstructionBlock
	     (new LocalLoadOperator(Type.tUObject, null, thisLocal)), thisLocal);
	flows[3].appendBlock(new ConditionalBlock
			     (new CompareUnaryOperator
			      (Type.tUObject, Operator.EQUALS_OP)));
	flows[3].setSuccessors(new FlowBlock[] { flows[4], flows[5] });
	flows[4].appendReadBlock
	    (new RetBlock(tmp2Local), tmp2Local);
	flows[4].setSuccessors(new FlowBlock[0]);

	flows[5].appendBlock(new DescriptionBlock("/*HERE*/"));
	flows[5].setSuccessors(new FlowBlock[] { FlowBlock.END_OF_METHOD });

	flows[0].addStartPred();
	TransformExceptionHandlers exc = new TransformExceptionHandlers(flows);
	exc.addHandler(flows[0],flows[0],flows[1], null);
	exc.analyze();
	flows[0].analyze();
	flows[0].removeStartPred();
	if (VERBOSE)
	    flows[0].dumpSource(new TabbedPrintWriter(GlobalOptions.err));
	assertTrue("Sequ", flows[0].getBlock() instanceof SequentialBlock);
	assertTrue("Loop", flows[0].getBlock().getSubBlocks()[0] instanceof LoopBlock);
	assertTrue("Try", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0] instanceof TryBlock);
	assertTrue("Empty", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[0] instanceof EmptyBlock);
	assertTrue("Finally", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[1] instanceof FinallyBlock);
	assertTrue("If", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[1].getSubBlocks()[0] instanceof IfThenElseBlock);
	assertTrue("Break", flows[0].getBlock().getSubBlocks()[0].getSubBlocks()[0].getSubBlocks()[1].getSubBlocks()[0].getSubBlocks()[0] instanceof BreakBlock);
	assertTrue("Descr", flows[0].getBlock().getSubBlocks()[1] 
		   instanceof DescriptionBlock); 
    }

    public static Test suite() {
	TestSuite suite = new TestSuite(); 
	suite.addTest(new TrExcTest("testSynchronized11")); 
	suite.addTest(new TrExcTest("testSynchronized13")); 
	suite.addTest(new TrExcTest("testSpecialFin")); 
	suite.addTest(new TrExcTest("testSpecialFinTryLoops")); 
	suite.addTest(new TrExcTest("testFinBreaksJikes")); 
	suite.addTest(new TrExcTest("testFinCondBreaks")); 
	return suite;
    }
}
