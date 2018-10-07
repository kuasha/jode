
public class InlinedAnon {

    private final Object getAnon(final int param) {
	return new Object() {
	    public String toString() {
		return ""+param;
	    }
	};
    }

    void test1() {
	Object o1 = getAnon(5);
	Object o2 = getAnon(3);
    }

    void test2() {
	Object o3 = getAnon(1);
	Object o4 = getAnon(2);
    }
}
