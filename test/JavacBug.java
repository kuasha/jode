
/**
 * This class shows a bug in javac 1.2-pre2 compiler.
 * Decompile the generated class to see whats happening.
 */
public class JavacBug {

    class Inner {
	public String toString() {
	    return "Inner";
	}
    }

    public Inner test() {
	final int a = 1;
	final int b = 2;
	return new Inner() {
	    /* jdk1.2 javac misses these initializers */
	    int c = a;
	    int d = 3;
	    
	    public String toString() {
		return "b="+b+"; c="+c+"; d="+d;
	    }
	};
    }

    public static void main(String[] argv) {
	Inner inner = new JavacBug().test();
	System.err.println(inner.toString());
    }
}
	
