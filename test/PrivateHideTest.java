
/**
 * This class tests accesses to private methods and fields, that can be 
 * accessed via a cast.
 * 
 * The test class has some problems, that no compiler will produce
 * correct code.  When all constructors are declared as private all
 * compilers won't even compile it.  
 *
 * I wish there would be a clear document saying what constructs are 
 * allowed and a compiler that would compile them all...
 */
public class PrivateHideTest {

    public static class Test {
	private String  id;

	Test(int val) {
	    id = "Test"+val;
	}

	private void a(String descr) {
	    System.err.println(descr+" = "+id);
	}

	class Mid extends Test {
	    private String id;

	    Mid(int val) {
		super(val);
		id = "Mid"+val;
	    }
	    
	    private void a(String descr) {
		System.err.println(descr+" = "+id);
	    }

	    private Test getTestThis() {
		return Test.this;
	    }

	    class Inner extends Mid {
		private String id;

		Inner(int val, Test midOuter) {
		    midOuter.super(val);
		    id = "Inner"+val;
		}

		private void a(String descr) {
		    System.err.println(descr+" = "+id);
		}

		private void methodTest() {
		    this.a("this");
		    super.a("super");
		    ((Mid) this).a("(Mid) this");
		    ((Test) this).a("(Test) this");
		    Mid.this.a("Mid.this");
		    ((Test) Mid.this).a("(Test) Mid.this");
		    Test.this.a("Test.this");
		    ((Mid) this).getTestThis().a("((Mid) this).getTestThis()");
		    Mid.this.getTestThis().a("Mid.this.getTestThis()");
		}


		private void fieldTest() {
		    System.err.println("this = "+this.id);
		    System.err.println("(Mid) this = " + ((Mid) this).id);
		    System.err.println("(Test) this = " + ((Test) this).id);
		    System.err.println("Mid.this = " + Mid.this.id);
		    System.err.println("(Test) Mid.this = " + ((Test) Mid.this).id);
		    System.err.println("Test.this = " + Test.this.id);
		    System.err.println("((Mid) this).getTestThis() = " + ((Mid)this).getTestThis().id);
		    System.err.println("Mid.this.getTestThis() = " + Mid.this.getTestThis().id);
		}
	    }
	}
    }
	
    public static void main(String[] argv) {
	Test.Mid.Inner inner = 
	    new Test(1).new Mid(2).new Inner(3, new Test(4).new Mid(5));
	inner.methodTest();
	System.err.println("--");
	inner.fieldTest();
	((Test.Mid)inner).a("(Test.Mid) inner");
    }
}
