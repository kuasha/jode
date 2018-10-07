

public class CheckPrivate {
    private class InnerClass {
	private int field;
	
	private InnerClass(int j) {
	    field = j;
	}
	
	private int method(int a) {
	    int old = field;
	    field = a;
	    return old;
	}

	private void test() {
	    outerField = 4;
	    outerMethod();
	    System.err.println(outerField);
	    new CheckPrivate();
	}
    }

    private int outerField;

    private int outerMethod() {
	return outerField;
    }

    private CheckPrivate() {
	InnerClass inner = new InnerClass(1);
	inner.field = 3;
	inner.method(inner.field);
    }

    public static void main(String[] test) {
	new CheckPrivate();
    }
}
