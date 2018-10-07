/* Handler Copyright (C) 2000-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: Handler.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;

/**
 * A simple class containing the info about one try-catch block.
 * 
 * @author Jochen Hoenicke
 */
public class Handler {
    Block start, end, catcher;
    String type;

    /**
     * The empty handler array.  Since handlers are often empty, we don't
     * want to create a new object each time.
     */
    final static Handler[] EMPTY = new Handler[0];
    
    /**
     * Creates a new handler.
     */
    Handler(Block s, Block e, Block c, String t) {
	start = s;
	end = e;
	catcher = c;
	type = t;
    }

    /**
     * Gets the first basic block of the try.
     */
    public Block getStart() {
	return start;
    }
    
    /**
     * Gets the last basic block of the try.
     */
    public Block getEnd() {
	return end;
    }
    
    /**
     * Gets the first basic block of the exception handler.
     */
    public Block getCatcher() {
	return catcher;
    }
    
    /**
     * Gets the class name of the exception type.
     */
    public String getType() {
	return type;
    }
    
    public void setStart(Block start) {
	this.start = start;
    }
    
    public void setEnd(Block end) {
	this.end = end;
    }
    
    public void setCatcher(Block catcher) {
	this.catcher = catcher;
    }
    
    /**
     * Sets the class name of the exception type.
     */
    public void setType(String type) {
	this.type = type;
    }
}

