/* Conflicts Copyright (C) 1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: ResolveConflicts.java 1224 2000-01-30 16:49:58Z jochen $
 */

/**
 * This class tests name conflicts and their resolvation.  Note that every
 * name in this file should be the shortest possible name.
 */
public class ResolveConflicts 
{
    public class Conflicts
    {
	int Conflicts;
	
	class Blah
	{
	    Conflicts Inner;
	    
	    void Conflicts() {
///#ifndef JAVAC11
///#ifndef JAVAC12
		Inner = ResolveConflicts.Conflicts.this;
///#endif
///#endif
	    }
	}
	
	class Inner
	{
	    int Conflicts;
	    Conflicts Inner;
	    
	    class Blah 
		extends Conflicts.Blah 
	    {
		int Blah;
		
		void Inner() {
		    this.Inner.Inner();
		    this.Conflicts();
///#ifndef JAVAC11
///#ifndef JAVAC12
		    ResolveConflicts.Conflicts.Inner.this.Inner.Inner();
		    ResolveConflicts.Conflicts.Inner.this.Conflicts();
///#endif
///#endif
		}
		
		Blah() {
		    /* empty */
		}
		
		Blah(Conflicts Conflicts) {
		    Conflicts.super();
		}
	    }
	    
	    void Conflicts() {
		int Conflicts = 4;
		Conflicts();
		new Object() {
		    void Inner() {
///#ifndef JAVAC11
///#ifndef JAVAC12
			ResolveConflicts.Conflicts.this.Inner();
///#endif
///#endif
		    }
		};
		this.Conflicts = Conflicts;
		Inner();
///#ifndef JAVAC11
///#ifndef JAVAC12
		ResolveConflicts.Conflicts.this.Conflicts = this.Conflicts;
///#endif
///#endif
	    }
	    
	    Conflicts Conflicts(Inner Conflicts) {
///#ifndef JAVAC11
///#ifndef JAVAC12
		ResolveConflicts.Conflicts Inner
		    = ResolveConflicts.Conflicts.this;
///#endif
///#endif
		return ResolveConflicts.this.new Conflicts();
	    }
	}
	
	class Second 
	    extends Conflicts.Inner.Blah 
	{
	    Inner Blah = new Inner();
	    
	    class Inner extends Conflicts.Inner
	    {
	    }
	    
	    Conflicts.Inner create() {
///#ifndef JAVAC11
///#ifndef JAVAC12
		ResolveConflicts.Conflicts.Inner inner
		    = ResolveConflicts.Conflicts.this.new Inner();
///#endif
///#endif
///#ifdef JAVAC11
		return null;
///#else
		return ResolveConflicts.this.new Conflicts().new Inner();
///#endif
	    }
	    
	    Second(Conflicts.Inner Blah) {
		Blah.super();
	    }
	}
	
	public void Inner() {
	    /* empty */
	}
	
	public Conflicts() {
	    int Conflicts = this.Conflicts;
	    Inner Inner = new Inner();
	    Inner.Conflicts = 5;
	    new Inner().Conflicts(Inner).Inner();
	}
    }
}
