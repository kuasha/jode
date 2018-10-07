/**
 * This class should be optimized through the obfuscator:
 *
 * <pre>
 *  java jode.Obfuscator --dest tmp.zip --preserve 'jode.test.OptimizerTest.test.(*)*' jode
 * </pre>
 */

public class OptimizerTest {
    String blah, blub;
    Object o;

    private static String manipulateString(String input) {
	char[] chars = input.toCharArray();
	char[] dests = new char[chars.length];
	dests[0] = chars[0];
	for (int i=1; i< chars.length; i++) {
	    if (chars[i] > chars[i-1]) {
		dests[i] = (char) (chars[i] - chars[i-1]);
	    } else if (chars[i] == chars[i-1]) {
		dests[i] = 0;
	    } else if (chars[i] < chars[i-1]) {
		dests[i] = (char) (Character.MAX_VALUE
				   - (chars[i-1] - chars[i] - 1));
	    } else
		dests[i] = 'X';
	}
	return new String(dests);
    }

    public static String doubleCompare(double d1, double d2) {
	return d1 + ((d1 > d2) ? " > " 
		     : (d1 < d2) ? " < "
		     : (d1 == d2) ? " == " : " ?? ") + d2;
    }
	
    public void test() {
	System.err.println(manipulateString("ABCDEFGGFEDCBA"));
	blah = manipulateString("hello world");
	o = new Object();
	blub = "Hallo"+manipulateString("ABCDEFGGFDECBA");
	System.err.println(blub);

	System.err.println(doubleCompare(0.0, 0.0));
	System.err.println(doubleCompare(0.0, 1.0));
	System.err.println(doubleCompare(0.0, -1.0));
	System.err.println(doubleCompare(0.0, Double.NaN));
	System.err.println(doubleCompare(Double.NaN, 0.0));
	System.err.println(doubleCompare(Double.NaN, Double.NaN));
	System.err.println(doubleCompare(Double.NEGATIVE_INFINITY, 
					 Double.POSITIVE_INFINITY));

	System.err.println(doubleCompare(Math.exp(1), 2.718281828459045));

	System.err.println((int)Math.round(Math.exp(Math.log(5)*5))/ 625);
    }
}

