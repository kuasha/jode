public class CheckSuperRemove {
    public class Inner {
	public void test() {
	    class MyInner extends Inner {
	    }
	    MyInner myInner = new MyInner();
	    Inner anonInner = new Inner() {
		    public void test() {}
		};
	    MyInner anonInner2 = new MyInner() {
		    public void test() {}
		};
	}
    }

    public class SubInner extends Inner {
	public SubInner(int a) {
	}

	public SubInner() {
	}
    }

    public static void main(String[] param) {
	new CheckSuperRemove().new SubInner();
	new CheckSuperRemove().new SubInner(1);
    }
}
