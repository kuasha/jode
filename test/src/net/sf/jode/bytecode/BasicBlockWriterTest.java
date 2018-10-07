package net.sf.jode.bytecode;
import junit.framework.*;
import java.io.*;

public class BasicBlockWriterTest extends TestCase implements Opcodes {
    public BasicBlockWriterTest(String name) {
	super(name);
    }

    GrowableConstantPool gcp = new GrowableConstantPool();
    Instruction[] someNops;
    Instruction[] manyNops;
    Instruction[] whileHead;
    Instruction[] whileCond;
    Instruction[] whileFoot;

    /**
     * The whileHead block in bytecode
     */
    private final static String whileHeadStr="\3=";
    /**
     * The whileCond block in bytecode, without the if_icmpeq instruction.
     */
    private final static String whileCondStr="\34\33";
    /**
     * The whileFoot block in bytecode.
     */
    private final static String whileFootStr="\204\2\1";
    /**
     * The someNops block in bytecode, without the if_icmpeq instruction.
     */
    private final static String someNopsStr="\0\0";

    public void setUp() {
	Instruction nop = Instruction.forOpcode(opc_nop);
	someNops = new Instruction[] { nop, nop };
	manyNops = new Instruction[35000];
	for (int i = 0; i < manyNops.length; i++)
	    manyNops[i] = nop;

	whileHead = new Instruction[] {
	    Instruction.forOpcode(opc_ldc, new Integer(0)),
	    Instruction.forOpcode(opc_istore, LocalVariableInfo.getInfo(2)),
	};
	whileCond = new Instruction[] {
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(2)),
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_if_icmpeq)
	};
	whileFoot = new Instruction[] {
	    Instruction.forOpcode(opc_iinc, LocalVariableInfo.getInfo(2), 1),
	};
    }

    public byte[] write(BasicBlockWriter bbw) throws IOException {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	DataOutputStream dos = new DataOutputStream(baos);
	bbw.write(gcp, dos);
	dos.close();
	return baos.toByteArray();
    }

    public void testEmpty() throws IOException {
	BasicBlocks bb = new BasicBlocks(new MethodInfo("foo", "()V", 0));
	bb.setBlocks(new Block[0], null, new Handler[0]);
	BasicBlockWriter bbw = new BasicBlockWriter(bb, gcp);
	gcp.write(new DataOutputStream(new ByteArrayOutputStream()));

	assertEquals("Code differs",
		     "\0\0\0\1\0\0\0\1" /* no stack, one local, length 1 */
		     +"\261" /* opc_return */
		     +"\0\0" /* no exception handlers */,
		     new String(write(bbw), "iso-8859-1"));
    }

    public void testSimple() throws IOException {
	Block bb1 = new Block();
	Block bb2 = new Block();
	bb1.setCode(someNops, new Block[] {bb2});
	bb2.setCode(someNops, new Block[] {null});
	BasicBlocks bb = new BasicBlocks(new MethodInfo("foo", "()V", 0));
 	bb.setBlocks(new Block[] { bb1, bb2}, bb1, new Handler[0]);
	BasicBlockWriter bbw = new BasicBlockWriter(bb, gcp);
	gcp.write(new DataOutputStream(new ByteArrayOutputStream()));

	assertEquals("Code differs",
		     "\0\0\0\1\0\0\0\5" /* no stack, one local, length 5 */
		     +someNopsStr+someNopsStr+"\261"
		     +"\0\0" /* no exception handlers */,
		     new String(write(bbw), "iso-8859-1"));
    }

    public void testWhile() throws IOException {
	Block b1 = new Block();
	Block b2 = new Block();
	Block b3 = new Block();
	Block b4 = new Block();
	b1.setCode(whileHead, new Block[] { b2 });
	b2.setCode(whileCond, new Block[] { null, b3 });
	b3.setCode(someNops,  new Block[] { b4 });
	b4.setCode(whileFoot,  new Block[] { b2 });
	BasicBlocks bb = new BasicBlocks(new MethodInfo("a", "(I)V", 0));
 	bb.setBlocks(new Block[] { b1, b2, b3, b4}, b1, new Handler[0]);
	BasicBlockWriter bbw = new BasicBlockWriter(bb, gcp);
	gcp.write(new DataOutputStream(new ByteArrayOutputStream()));
	assertEquals(5, bbw.blockAddr.length);
	assertEquals(0, bbw.blockAddr[0]);
	assertEquals(2, bbw.blockAddr[1]);
	assertEquals(7, bbw.blockAddr[2]);
	assertEquals(9, bbw.blockAddr[3]);
	assertEquals(16, bbw.blockAddr[4]);

	assertEquals("Code differs",
		     "\0\2\0\3\0\0\0\20"
		     +whileHeadStr+whileCondStr+"\237\0\13"
		     +someNopsStr+whileFootStr+"\247\377\366"+"\261"+"\0\0",
		     new String(write(bbw), "iso-8859-1"));
    }

    public void testTableSwitch() throws IOException {
	Block b1 = new Block();
	Block b2 = new Block();
	Instruction[] switchBlock = new Instruction[] { 
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_lookupswitch, new int[] { 1, 3, 5})
	};
	b1.setCode(switchBlock, new Block[] { b2, null, b1, null });
	b2.setCode(someNops, new Block[] { null });
	BasicBlocks bb = new BasicBlocks(new MethodInfo("s", "(I)V", 0));
 	bb.setBlocks(new Block[] { b1, b2 }, b1, new Handler[0]);
	BasicBlockWriter bbw = new BasicBlockWriter(bb, gcp);
	gcp.write(new DataOutputStream(new ByteArrayOutputStream()));
	assertEquals(3, bbw.blockAddr.length);
	assertEquals(0, bbw.blockAddr[0]);
	assertEquals(36, bbw.blockAddr[1]);
	assertEquals(39, bbw.blockAddr[2]);
	assertEquals("Code differs",
		     "\0\1\0\2\0\0\0\47"
		     +"\33\252\0\0" /*iload_0 + tableswitch + align */
		     +"\0\0\0\45\0\0\0\1\0\0\0\5" /* def, low, high */
		     +"\0\0\0\43\0\0\0\45\0\0\0\45\0\0\0\45\377\377\377\377"
		     +someNopsStr+"\261"+"\0\0",
		     new String(write(bbw), "iso-8859-1"));
    }

    public void testLookupSwitch() throws IOException {
	Block b1 = new Block();
	Block b2 = new Block();
	Instruction[] switchBlock = new Instruction[] { 
	    Instruction.forOpcode(opc_iload, LocalVariableInfo.getInfo(1)),
	    Instruction.forOpcode(opc_lookupswitch, new int[] { 1, 5, 7})
	};
	b1.setCode(switchBlock, new Block[] { b2, null, b1, null });
	b2.setCode(someNops, new Block[] { null });
	BasicBlocks bb = new BasicBlocks(new MethodInfo("s", "(I)V", 0));
 	bb.setBlocks(new Block[] { b1, b2 }, b1, new Handler[0]);
	BasicBlockWriter bbw = new BasicBlockWriter(bb, gcp);
	gcp.write(new DataOutputStream(new ByteArrayOutputStream()));
	assertEquals(3, bbw.blockAddr.length);
	assertEquals(0, bbw.blockAddr[0]);
	assertEquals(36, bbw.blockAddr[1]);
	assertEquals(39, bbw.blockAddr[2]);
	assertEquals("Code differs",
		     "\0\1\0\2\0\0\0\47"
		     +"\33\253\0\0" /*iload_0 + lookupswitch + align */
		     +"\0\0\0\45\0\0\0\3"   /* def , nitem */
		     +"\0\0\0\1\0\0\0\43"
		     +"\0\0\0\5\0\0\0\45"
		     +"\0\0\0\7\377\377\377\377"
		     +someNopsStr+"\261"+"\0\0",
		     new String(write(bbw), "iso-8859-1"));
    }

    public void testException() throws IOException {
	Block b1 = new Block();
	Block b2 = new Block();
	Instruction[] catchInstrs = new Instruction[] { 
	    Instruction.forOpcode(opc_athrow)
	};
	b1.setCode(someNops, new Block[] { null });
	b2.setCode(catchInstrs, new Block[0]);
	BasicBlocks bb = new BasicBlocks(new MethodInfo("e", "()V", 0));
	Handler h = new Handler(b1, b1, b2, "java.lang.RuntimeException");
 	bb.setBlocks(new Block[] { b1, b2 }, b1, new Handler[] {h});
	assertEquals(0, b1.blockNr);
	assertEquals(1, b2.blockNr);
	assertEquals(1, b1.catchers.length);
	assertEquals(0, b2.catchers.length);
	assertSame(h, b1.catchers[0]);
	BasicBlockWriter bbw = new BasicBlockWriter(bb, gcp);
	gcp.write(new DataOutputStream(new ByteArrayOutputStream()));
	int cpoolEntry = gcp.putClassName("java.lang.RuntimeException");
	assertEquals(3, bbw.blockAddr.length);
	assertEquals(0, bbw.blockAddr[0]);
	assertEquals(3, bbw.blockAddr[1]);
	assertEquals(4, bbw.blockAddr[2]);
	assertEquals("Code differs",
		     "\0\1\0\1\0\0\0\4"
		     + someNopsStr + "\261" + "\277"
		     + "\0\1\0\0\0\3\0\3\0"+(char)cpoolEntry,
		     new String(write(bbw), "iso-8859-1"));
    }

    public static Test suite() {
	TestSuite suite = new TestSuite(); 
	suite.addTest(new BasicBlockWriterTest("testEmpty")); 
	suite.addTest(new BasicBlockWriterTest("testSimple")); 
	suite.addTest(new BasicBlockWriterTest("testWhile")); 
	suite.addTest(new BasicBlockWriterTest("testTableSwitch")); 
	suite.addTest(new BasicBlockWriterTest("testLookupSwitch")); 
	suite.addTest(new BasicBlockWriterTest("testException")); 
	return suite;
    }
}
