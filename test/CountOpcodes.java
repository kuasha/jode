import jode.bytecode.*;
import java.util.*;
import com.sun.java.util.collections.Iterator;

public class CountOpcodes {
    static int[] opcodeCount = new int[256];
    static int[] predsCount = new int[1024];
    static int[] succsCount = new int[1024];
    static Vector instructions = new Vector(73400);

    public static void handleBytecode(BytecodeInfo bc) {
	for (Iterator i = bc.getInstructions().iterator(); i.hasNext();) {
	    Instruction instr = (Instruction) i.next();
	    instructions.addElement(instr);
	    opcodeCount[instr.getOpcode()]++;
	    Instruction[] p = instr.getPreds();
	    if (p == null)
		predsCount[0]++;
	    else
		predsCount[p.length]++;

	    Instruction[] s = instr.getSuccs();
	    if (s == null)
		succsCount[0]++;
	    else
		succsCount[s.length]++;
	}
    }

    public static void handlePackage(String pack) {
	Enumeration subs = ClassInfo.getClassesAndPackages(pack);
	while (subs.hasMoreElements()) {
	    String comp = (String) subs.nextElement();
	    String full = pack + "." + comp;
	    if (ClassInfo.isPackage(full))
		handlePackage(full);
	    else {
		ClassInfo clazz = ClassInfo.forName(full);
		clazz.loadInfo(ClassInfo.FULLINFO);
		MethodInfo[] ms = clazz.getMethods();
		for (int i=0; i < ms.length; i++) {
		    BytecodeInfo bc = ms[i].getBytecode();
		    if (bc != null)
			handleBytecode(bc);
		}
	    }
	}
    }

    public static void main(String[] params) {
	ClassInfo.setClassPath(params[0]);
	Runtime runtime = Runtime.getRuntime();
	long free = runtime.freeMemory();
	long last;
	do {
	    last = free;
	    runtime.gc();
	    runtime.runFinalization();
	    free = runtime.freeMemory();
	} while (free < last);
	System.err.println("used before: "+(runtime.totalMemory()- free));
	long time = System.currentTimeMillis();
	handlePackage("com");
	System.err.println("Time used: "+(System.currentTimeMillis() - time));
	free = runtime.freeMemory();
	do {
	    last = free;
	    runtime.gc();
	    runtime.runFinalization();
	    free = runtime.freeMemory();
	} while (free < last);
	System.err.println("used after: "+(runtime.totalMemory()- free));
	System.err.println("instruction count: "+instructions.size());
	for (int i=0; i< 256; i++) {
	    if (opcodeCount[i] > 0)
		System.err.println("Opcode "+i+": \t   ("+Opcodes.opcodeString[i]+")\t"+opcodeCount[i]);
	}
	int moreThanTwo = 0;
	for (int i=0; i< predsCount.length; i++) {
	    if (predsCount[i] > 0) {
		System.err.println("preds "+i+": \t"+predsCount[i]);
		if (i>1)
		    moreThanTwo +=predsCount[i];
	    }
	}
	System.err.println("preds >2: \t"+moreThanTwo);
	
	moreThanTwo = 0;
	for (int i=0; i< succsCount.length; i++) {
	    if (succsCount[i] > 0) {
		System.err.println("succs "+i+": \t"+succsCount[i]);
		if (i>1)
		    moreThanTwo +=succsCount[i];
	    }
	}
	System.err.println("succs >2: \t"+moreThanTwo);
    }
}

