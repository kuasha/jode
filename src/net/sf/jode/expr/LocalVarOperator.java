/* LocalVarOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: LocalVarOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.GlobalOptions;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.LocalInfo;
import net.sf.jode.decompiler.TabbedPrintWriter;

///#def COLLECTIONS java.util
import java.util.Collection;
///#enddef

public abstract class LocalVarOperator extends Operator {
    LocalInfo local;

    public LocalVarOperator(Type lvalueType, LocalInfo local) {
        super(lvalueType);
        this.local = local;
        local.setOperator(this);
	initOperands(0);
    }

    public abstract boolean isRead();
    public abstract boolean isWrite();

    public void updateSubTypes() {
	if (parent != null
	    && (GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
	    GlobalOptions.err.println("local type changed in: "+parent);
        local.setType(type);
    }

    public void updateType() {
	updateParentType(local.getType());
    }

    public void fillInGenSet(Collection in, Collection gen) {
	if (isRead() && in != null)
	    in.add(getLocalInfo());
	if (gen != null)
	    gen.add(getLocalInfo());
	super.fillInGenSet(in, gen);
    }

    public void fillDeclarables(Collection used) {
	used.add(local);
	super.fillDeclarables(used);
    }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public void setLocalInfo(LocalInfo newLocal) {
	local = newLocal;
	updateType();
    }

    public int getPriority() {
        return 1000;
    }

    public void dumpExpression(TabbedPrintWriter writer) {
	writer.print(local.getName());
    }
}
