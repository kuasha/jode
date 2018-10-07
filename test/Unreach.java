/* Unreach Copyright (C) 1999 Dave Mugridge.
 *
 * $Id: Unreach.java 1149 1999-08-19 15:16:59Z jochen $
 */


/* A test class submitted by dave@onekiwi.demon.co.uk */
class Unreach
{
	static int j = 0;
	final double[] d = {0.5, 0.4, 0.3};	// won't decompile

    public static final void m(int i) throws Exception {
	switch (i) {
	case 1:
	    j += 2;
	        for (;;) {
		        j += 3;
		        switch (j) {
		        case 2:
		            break;
		        default:
		            j += 4;
		            return;
		        }
	        }
	        // An unreachable break is inserted here
	default:
	    j += 5;	// decompiles as j = j + 1;  -- not quite optimal
	}
    }
}
