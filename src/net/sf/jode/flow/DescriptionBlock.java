/* DescriptionBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: DescriptionBlock.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.decompiler.TabbedPrintWriter;

/**
 * This is a block which contains a comment/description of what went 
 * wrong.  Use this, if you want to tell the user, that a construct
 * doesn't have the exspected form.
 *
 * @author Jochen Hoenicke
 */
public class DescriptionBlock extends StructuredBlock {
    String description;

    public DescriptionBlock(String description) {
        this.description = description;
    }

    /**
     * Tells if this block is empty and only changes control flow.
     */
    public boolean isEmpty() {
        return true;
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println(description);
    }
}
