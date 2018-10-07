package net.sf.jode.bytecode;
import junit.framework.*;
import java.io.*;

public class BasicBlocksTest extends TestCase implements Opcodes {
    public BasicBlocksTest(String name) {
	super(name);
    }
    
    public void testJsr() {
	Block b0 = new Block();
	Block b1 = new Block();
	Block b2 = new Block();
	b0.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_jsr)
	}, new Block[] { b1, null });
	b1.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_astore, LocalVariableInfo.getInfo(2)),
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(1), -1),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_ifeq)
	}, new Block[] { b2, b0 });
	b2.setCode(new Instruction[] {
	    Instruction.forOpcode(opc_ret, LocalVariableInfo.getInfo(0)),
	}, new Block[0]);
	assertEquals("pop0", 0, b0.maxpop);
	assertEquals("push0", 0, b0.maxpush);
	assertEquals("delta0", 0, b0.delta);
	assertEquals("pop1", 1, b1.maxpop);
	assertEquals("push1", 0, b1.maxpush);
	assertEquals("delta1", -1, b1.delta);
	assertEquals("pop2", 0, b2.maxpop);
	assertEquals("push2", 0, b2.maxpush);
	assertEquals("delta2", 0, b2.delta);
	BasicBlocks bb = new BasicBlocks(new MethodInfo("foo", "(I)V", 0));
 	bb.setBlocks(new Block[] { b0, b1, b2 }, b0, new Handler[0]);
	assertEquals("stack0", 0, b0.stackHeight);
	assertEquals("stack1", 1, b1.stackHeight);
	assertEquals("stack2", 0, b2.stackHeight);
    }

    public static Test suite() {
	TestSuite suite = new TestSuite(); 
	suite.addTest(new BasicBlocksTest("testJsr")); 
	return suite;
    }
}
