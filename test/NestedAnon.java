
public class NestedAnon {
    public NestedAnon(int maxdepth) {
	class NestMyself {
	    int depth;
///#ifndef JAVAC11
	    NestMyself son;
///#endif

	    public NestMyself(int d) {
		depth = d;
///#ifndef JAVAC11
		if (d > 0)
		    son = new NestMyself(d-1);
///#endif
	    }
	}
	new NestMyself(maxdepth);
    }

}
