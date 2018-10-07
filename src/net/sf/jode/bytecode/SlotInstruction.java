/* SlotInstruction Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: SlotInstruction.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;

/**
 * This class represents an instruction in the byte code.
 *
 */
class SlotInstruction extends Instruction {
    private LocalVariableInfo lvi;

    /**
     */
    public SlotInstruction(int opcode, LocalVariableInfo lvi) {
	super(opcode);
	this.lvi = lvi;
    }

    public boolean isStore() {
	int opcode = getOpcode();
	return opcode >= opc_istore && opcode <= opc_astore;
    }

    public boolean hasLocal() {
	return true;
    }
	    
    public final int getLocalSlot()
    {
	return lvi.getSlot();
    }

    public final LocalVariableInfo getLocalInfo()
    {
	return lvi;
    }

    public final void setLocalInfo(LocalVariableInfo info) 
    {
	this.lvi = info;
    }

    public final void setLocalSlot(int slot) 
    {
	if (lvi.getName() == null)
	    this.lvi = LocalVariableInfo.getInfo(slot);
	else
	    this.lvi = LocalVariableInfo.getInfo(slot, 
						 lvi.getName(), lvi.getType());
    }

    public String toString() {
	return super.toString()+' '+lvi;
    }
}
