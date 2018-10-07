package net.sf.jode.obfuscator.modules;
import net.sf.jode.bytecode.*;
import net.sf.jode.GlobalOptions;
import junit.framework.*;
import java.util.BitSet;
import java.io.PrintWriter;

public class ConstAnaTest extends TestCase implements Opcodes {
    ConstantAnalyzer ca;
    Instruction[] callJsrInstr;
    BasicBlocks jsrMethod;

    public ConstAnaTest(String name) {
	super(name);
    }

    public void setUp() {
	ca = new ConstantAnalyzer();
	createJsrMethod();
    }

    public void createJsrMethod() {
	callJsrInstr = new Instruction[] {
	    Instruction.forOpcode(opc_jsr)
	};

	Block b0 = new Block();
	Block b1 = new Block();
	Block b2 = new Block();
	b0.setCode(callJsrInstr, new Block[] { b1, null });
	b1.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_astore, LocalVariableInfo.getInfo(2)),
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(1), -1),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_ifeq)
	}, new Block[] { b2, b0 });
	b2.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_ret, LocalVariableInfo.getInfo(2)),
	}, new Block[0]);
	jsrMethod = new BasicBlocks(new MethodInfo("foo", "(I)V", 0));
 	jsrMethod.setBlocks(new Block[] { b0, b1, b2 }, 
			    b0, new Handler[0]);
    }

    public void testSimple() throws Exception {
	Block b0 = new Block();
	Block b1 = new Block();
	Block b2 = new Block();
	b0.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(1), 1),
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(2), 1),
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(3), 1),
	}, new Block[] { b1 });
	b1.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_ldc, new Integer(0)),
	    Instruction.forOpcode(opc_istore, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_ldc, new Integer(4)),
	    Instruction.forOpcode(opc_istore, LocalVariableInfo.getInfo(2)),
	    Instruction.forOpcode(opc_ldc, new Integer(0)),
	    Instruction.forOpcode(opc_istore, LocalVariableInfo.getInfo(3)),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_ifeq)
	}, new Block[] { b2, b0 });
	b2.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(1), 1),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(2)),
	    Instruction.forOpcode(opc_if_icmplt)
	}, new Block[] { b2, null });
	BasicBlocks bb = new BasicBlocks(new MethodInfo("foo", "()V", 0));
 	bb.setBlocks(new Block[] { b0, b1, b2 }, b1, new Handler[0]);

	ca.analyzeCode(null, bb);

	BitSet reachable = (BitSet) ca.bbInfos.get(bb);
	assertEquals("Reachable	set",
		     "{1, 2}", reachable.toString());
	assertEquals("constant flow",
		     ca.CONSTANTFLOW,
		     Class.forName("net.sf.jode.obfuscator.modules.ConstantAnalyzer$ConstantInfo")
		     .getDeclaredField("flags")
		     .getInt(ca.constantInfos.get(b1.getInstructions()[7])));
	ca.transformCode(bb);

	Block[] blocks = bb.getBlocks();
	assertEquals(2, blocks.length);
	assertEquals(1, blocks[0].getSuccs().length);
	assertEquals(blocks[1], blocks[0].getSuccs()[0]);
	assertEquals(2, blocks[1].getSuccs().length);
	assertEquals(blocks[1], blocks[1].getSuccs()[0]);
	assertEquals(null, blocks[1].getSuccs()[1]);
    }

    public void testJsr() throws Exception {
	Block[] blocks = jsrMethod.getBlocks();

	ca.analyzeCode(null, jsrMethod);

	BitSet reachable = (BitSet) ca.bbInfos.get(jsrMethod);
	assertEquals("Reachable	set",
		     "{0, 1, 2}", reachable.toString());
	ca.transformCode(jsrMethod);

	blocks = jsrMethod.getBlocks();
	assertEquals(3, blocks.length);
	assertEquals(2, blocks[0].getSuccs().length);
	assertEquals(blocks[1], blocks[0].getSuccs()[0]);
	assertEquals(null, blocks[0].getSuccs()[1]);
	assertEquals(2, blocks[1].getSuccs().length);
	assertEquals(0, blocks[2].getSuccs().length);
    }

    public void testNestedJsr() throws Exception {
	Block b0 = new Block();
	Block b1 = new Block();
	Block b2 = new Block();
	Block b3 = new Block();
	Block b4 = new Block();
	Block b5 = new Block();
	Block b6 = new Block();
	Block b7 = new Block();
	Block b8 = new Block();
	Block b9 = new Block();
	
	b0.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_jsr)
	}, new Block[] { b2, b1 });
	b1.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_jsr)
	}, new Block[] { b5, null });
	b2.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_astore, LocalVariableInfo.getInfo(2)),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_ifeq)
	}, new Block[] { b3, b7 });
	b3.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_jsr)
	}, new Block[] { b5, b1 });
	b4.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_ret, LocalVariableInfo.getInfo(2)),
	}, new Block[0]);
	b5.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_astore, LocalVariableInfo.getInfo(3)),
	    Instruction.forOpcode(opc_ret, LocalVariableInfo.getInfo(3)),
	}, new Block[0]);
	b6.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_astore, LocalVariableInfo.getInfo(3))
	}, new Block[] { b4 });
	b7.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_ifeq)
	}, new Block[] { b8, b4 });
	b8.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_jsr)
	}, new Block[] { b6, b9 });
	b9.setCode(new Instruction[0], new Block[] { b7 });
		
	BasicBlocks bb = new BasicBlocks(new MethodInfo("foo", "(I)V", 0));
 	bb.setBlocks(new Block[] { b0, b1, b2, b3, b4, b5, b6, b7, b8, b9 }, 
		     b0, new Handler[0]);
	ca.analyzeCode(null, bb);

	BitSet reachable = (BitSet) ca.bbInfos.get(bb);
	assertEquals("Reachable	set",
		     "{0, 1, 2, 3, 4, 5, 6, 7, 8}", reachable.toString());
	assertNotNull("Constant Flow", 
		      ca.constantInfos.get(b8.getInstructions()[0]));

	ca.transformCode(bb);

	Block[] blocks = bb.getBlocks();
	assertEquals(9, blocks.length);
	assertEquals(2, blocks[0].getSuccs().length);
	assertEquals(2, blocks[1].getSuccs().length);
	assertEquals(2, blocks[3].getSuccs().length);
	assertEquals(1, blocks[8].getSuccs().length);
    }

    public static Test suite() {
	TestSuite suite = new TestSuite(); 
	suite.addTest(new ConstAnaTest("testSimple")); 
	suite.addTest(new ConstAnaTest("testJsr")); 
	suite.addTest(new ConstAnaTest("testNestedJsr")); 
	return suite;
    }
}
