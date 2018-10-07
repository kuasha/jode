/* Opcodes Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: Opcodes.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.decompiler;
import net.sf.jode.type.Type;
import net.sf.jode.type.IntegerType;
import net.sf.jode.expr.*;
import net.sf.jode.flow.*;
import net.sf.jode.bytecode.*;

/**
 * This is an abstract class which creates flow blocks for the
 * opcodes in a byte stream.
 */
public abstract class Opcodes implements net.sf.jode.bytecode.Opcodes {

    private final static Type tIntHint
	= new IntegerType(IntegerType.IT_I,
			  IntegerType.IT_I
			  | IntegerType.IT_B
			  | IntegerType.IT_C
			  | IntegerType.IT_S);
    private final static Type tBoolIntHint
	= new IntegerType(IntegerType.IT_I
			  | IntegerType.IT_Z,
			  IntegerType.IT_I
			  | IntegerType.IT_B
			  | IntegerType.IT_C
			  | IntegerType.IT_S
			  | IntegerType.IT_Z);

    private final static int LOCAL_TYPES = 0;
    private final static int ARRAY_TYPES = 1;
    private final static int UNARY_TYPES = 2;
    private final static int I2BCS_TYPES = 3;
    private final static int BIN_TYPES   = 4;
    private final static int ZBIN_TYPES  = 5;

    private final static Type types[][] = {
	// Local types
        { Type.tBoolUInt, Type.tLong, Type.tFloat, Type.tDouble, 
	  Type.tUObject },
	// Array types
        { Type.tInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject, 
          Type.tBoolByte, Type.tChar, Type.tShort },
	// ifld2ifld and shl types
        { Type.tInt, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject },
	// i2bcs types
        { Type.tByte, Type.tChar, Type.tShort },
	// cmp/add/sub/mul/div types
        { tIntHint, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject },
	// and/or/xor types
        { tBoolIntHint, Type.tLong, Type.tFloat, Type.tDouble, Type.tUObject }
    };
    
    private static StructuredBlock createNormal(MethodAnalyzer ma, 
						Instruction instr,
						Expression expr)
    {
        return new InstructionBlock(expr);
    }

    private static StructuredBlock createSpecial(MethodAnalyzer ma, 
						 Instruction instr,
						 int type, 
						 int stackcount, int param)
    {
        return new SpecialBlock(type, stackcount, param);
    }

    private static StructuredBlock createJsr(MethodAnalyzer ma, 
					     Instruction instr)
    {
        return new JsrBlock();
    }

    private static StructuredBlock createIfGoto(MethodAnalyzer ma, 
						Instruction instr,
						Expression expr)
    {
        return new ConditionalBlock(expr);
    }

    private static StructuredBlock createSwitch(MethodAnalyzer ma,
						Instruction instr,
                                                int[] cases)
    {
        return new SwitchBlock(new NopOperator(Type.tUInt), cases);
    }

    private static StructuredBlock createBlock(MethodAnalyzer ma,
                                               Instruction instr,
                                               StructuredBlock block)
    {
        return block;
    }

    private static StructuredBlock createRet(MethodAnalyzer ma,
					     Instruction instr,
					     LocalInfo local)
    {
	return new RetBlock(local);
    }

    /**
     * Converts an instruction to a StructuredBlock and appencs it to the
     * flow block.
     * @param flow    The flowblock to which we should add.
     * @param instr   The instruction to add.
     * @param ma      The Method Analyzer 
     *                (where further information can be get from).
     * @return The FlowBlock representing this opcode
     *         or null if the stream is empty.
     */
    public static void addOpcode(FlowBlock flow, Instruction instr, 
				 MethodAnalyzer ma)
    {
	ClassPath cp = ma.getClassAnalyzer().getClassPath();
        int opcode = instr.getOpcode();
        switch (opcode) {
        case opc_nop:
	    break;
        case opc_ldc:
        case opc_ldc2_w:
	    {
		Expression expr;
		if (instr.getConstant() instanceof Reference) {
		    Reference ref = (Reference) instr.getConstant();
		    expr = new ClassFieldOperator
			(Type.tType(cp, ref.getClazz()));
		} else {
		    expr = new ConstOperator(instr.getConstant());
		}
		flow.appendBlock(createNormal(ma, instr, expr));
	    }
	    break;
        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload: {
	    LocalInfo local = ma.getLocalInfo(instr.getLocalInfo());
            flow.appendReadBlock
		(createNormal
		 (ma, instr, new LocalLoadOperator
		  (types[LOCAL_TYPES][opcode-opc_iload], ma, local)), local);
	    break;
	}
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload:
            flow.appendBlock
		(createNormal
		 (ma, instr, new ArrayLoadOperator
		  (types[ARRAY_TYPES][opcode - opc_iaload])));
	    break;
        case opc_istore: case opc_lstore: 
        case opc_fstore: case opc_dstore: case opc_astore: {
	    LocalInfo local = ma.getLocalInfo(instr.getLocalInfo());
            flow.appendWriteBlock
		(createNormal
		 (ma, instr, new StoreInstruction
		  (new LocalStoreOperator
		   (types[LOCAL_TYPES][opcode-opc_istore], local))), local);
	    break;
	}
        case opc_iastore: case opc_lastore:
        case opc_fastore: case opc_dastore: case opc_aastore:
        case opc_bastore: case opc_castore: case opc_sastore:
            flow.appendBlock
		(createNormal
		 (ma, instr, new StoreInstruction
		  (new ArrayStoreOperator
		   (types[ARRAY_TYPES][opcode - opc_iastore]))));
	    break;
        case opc_pop: case opc_pop2:
            flow.appendBlock
		(createSpecial
		 (ma, instr, SpecialBlock.POP, opcode - opc_pop + 1, 0));
	    break;
	case opc_dup: case opc_dup_x1: case opc_dup_x2:
        case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
            flow.appendBlock
		(createSpecial
		 (ma, instr, SpecialBlock.DUP, 
		  (opcode - opc_dup)/3+1, (opcode - opc_dup)%3));
	    break;
        case opc_swap:
            flow.appendBlock
		(createSpecial(ma, instr, SpecialBlock.SWAP, 1, 0));
	    break;
        case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
        case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
        case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
        case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
        case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
            flow.appendBlock
		(createNormal
		 (ma, instr, new BinaryOperator
		  (types[BIN_TYPES][(opcode - opc_iadd)%4],
		   (opcode - opc_iadd)/4+Operator.ADD_OP)));
	    break;
        case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
            flow.appendBlock
		(createNormal
		 (ma, instr, new UnaryOperator
		  (types[UNARY_TYPES][opcode - opc_ineg], Operator.NEG_OP)));
	    break;
        case opc_ishl: case opc_lshl:
        case opc_ishr: case opc_lshr:
        case opc_iushr: case opc_lushr:
            flow.appendBlock
		(createNormal
		 (ma, instr, new ShiftOperator
		  (types[UNARY_TYPES][(opcode - opc_ishl)%2],
		   (opcode - opc_ishl)/2 + Operator.SHIFT_OP)));
	    break;
        case opc_iand: case opc_land:
        case opc_ior : case opc_lor :
        case opc_ixor: case opc_lxor:
            flow.appendBlock
		(createNormal
		 (ma, instr, new BinaryOperator
		  (types[ZBIN_TYPES][(opcode - opc_iand)%2],
		   (opcode - opc_iand)/2 + Operator.AND_OP)));
	    break;
        case opc_iinc: {
	    LocalInfo local = ma.getLocalInfo(instr.getLocalInfo());
            int value = instr.getIncrement();
            int operation = Operator.ADD_OP;
            if (value < 0) {
                value = -value;
                operation = Operator.SUB_OP;
            }
            flow.appendReadBlock
		(createNormal
		 (ma, instr, new IIncOperator
		  (new LocalStoreOperator(Type.tInt, local), 
		   value, operation + Operator.OPASSIGN_OP)), local);
	    break;
        }
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
            int from = (opcode-opc_i2l)/3;
            int to   = (opcode-opc_i2l)%3;
            if (to >= from)
                to++;
            flow.appendBlock
		(createNormal
		 (ma, instr, new ConvertOperator(types[UNARY_TYPES][from], 
						 types[UNARY_TYPES][to])));
	    break;
        }
        case opc_i2b: case opc_i2c: case opc_i2s:
            flow.appendBlock(createNormal
                (ma, instr, new ConvertOperator
                 (types[UNARY_TYPES][0], types[I2BCS_TYPES][opcode-opc_i2b])));
	    break;
        case opc_lcmp:
        case opc_fcmpl: case opc_fcmpg:
        case opc_dcmpl: case opc_dcmpg:
            flow.appendBlock(createNormal
                (ma, instr, new CompareToIntOperator
                 (types[BIN_TYPES][(opcode-(opc_lcmp-3))/2], 
                  (opcode == opc_fcmpg || opcode == opc_dcmpg))));
	    break;
        case opc_ifeq: case opc_ifne: 
            flow.appendBlock(createIfGoto
		(ma, instr,
                 new CompareUnaryOperator
                 (Type.tBoolInt, opcode - (opc_ifeq-Operator.COMPARE_OP))));
	    break;
        case opc_iflt: case opc_ifge: case opc_ifgt: case opc_ifle:
            flow.appendBlock(createIfGoto
		(ma, instr,
                 new CompareUnaryOperator
                 (Type.tInt, opcode - (opc_ifeq-Operator.COMPARE_OP))));
	    break;
        case opc_if_icmpeq: case opc_if_icmpne:
            flow.appendBlock
		(createIfGoto
		 (ma, instr,
		  new CompareBinaryOperator
		  (tBoolIntHint, 
		   opcode - (opc_if_icmpeq-Operator.COMPARE_OP))));
	    break;
        case opc_if_icmplt: case opc_if_icmpge: 
        case opc_if_icmpgt: case opc_if_icmple:
            flow.appendBlock
		(createIfGoto
		 (ma, instr,
		  new CompareBinaryOperator
		  (tIntHint, opcode - (opc_if_icmpeq-Operator.COMPARE_OP))));
	    break;
        case opc_if_acmpeq: case opc_if_acmpne:
            flow.appendBlock
		(createIfGoto
		 (ma, instr,
		  new CompareBinaryOperator
		  (Type.tUObject, 
		   opcode - (opc_if_acmpeq-Operator.COMPARE_OP))));
	    break;
        case opc_jsr:
            flow.appendBlock(createJsr(ma, instr));
	    break;
	case opc_ret: {
	    LocalInfo local = ma.getLocalInfo(instr.getLocalInfo());
            flow.appendReadBlock(createRet(ma, instr, local), local);
	    break;
	}
        case opc_lookupswitch: {
	    int[] cases = instr.getValues();
            flow.appendBlock(createSwitch(ma, instr, cases));
	    break;
        }
        case opc_ireturn: case opc_lreturn: 
        case opc_freturn: case opc_dreturn: case opc_areturn: {
            Type retType = Type.tSubType(ma.getReturnType());
            flow.appendBlock
		(createBlock
		 (ma, instr, new ReturnBlock(new NopOperator(retType))));
	    break;
        }
        case opc_return:
	    throw new InternalError("opc_return no longer allowed");

        case opc_getstatic:
        case opc_getfield: {
            Reference ref = instr.getReference();
            flow.appendBlock(createNormal
			     (ma, instr, new GetFieldOperator
			      (ma, opcode == opc_getstatic, ref)));
	    break;
        }
        case opc_putstatic:
        case opc_putfield: {
            Reference ref = instr.getReference();
            flow.appendBlock
		(createNormal
		 (ma, instr, new StoreInstruction
		  (new PutFieldOperator(ma, opcode == opc_putstatic, ref))));
	    break;
        }
        case opc_invokevirtual:
        case opc_invokespecial:
        case opc_invokestatic :
        case opc_invokeinterface: {
            Reference ref = instr.getReference();
	    int flag = (ref.getName().equals("<init>")
			? InvokeOperator.CONSTRUCTOR
			: opcode == opc_invokestatic ? InvokeOperator.STATIC 
			: opcode == opc_invokespecial ? InvokeOperator.SPECIAL
			: InvokeOperator.VIRTUAL);
            StructuredBlock block = createNormal
                (ma, instr, new InvokeOperator(ma, flag, ref));
            flow.appendBlock(block);
	    break;
        }
        case opc_new: {
            Type type = Type.tType(cp, instr.getClazzType());
            ma.useType(type);
            flow.appendBlock(createNormal(ma, instr, new NewOperator(type)));
	    break;
        }
        case opc_arraylength:
            flow.appendBlock(createNormal
			     (ma, instr, new ArrayLengthOperator()));
	    break;
        case opc_athrow:
            flow.appendBlock(createBlock
			     (ma, instr, 
			      new ThrowBlock(new NopOperator(Type.tUObject))));
	    break;
        case opc_checkcast: {
            Type type = Type.tType(cp, instr.getClazzType());
            ma.useType(type);
            flow.appendBlock(createNormal
			     (ma, instr, new CheckCastOperator(type)));
	    break;
        }
        case opc_instanceof: {
            Type type = Type.tType(cp, instr.getClazzType());
            ma.useType(type);
            flow.appendBlock(createNormal
			     (ma, instr, new InstanceOfOperator(type)));
	    break;
        }
        case opc_monitorenter:
            flow.appendBlock(createNormal(ma, instr,
					  new MonitorEnterOperator()));
	    break;
        case opc_monitorexit:
            flow.appendBlock(createNormal(ma, instr,
					  new MonitorExitOperator()));
	    break;
        case opc_multianewarray: {
            Type type = Type.tType(cp, instr.getClazzType());
	    ma.useType(type);
            int dimension = instr.getDimensions();
            flow.appendBlock(createNormal
			     (ma, instr, 
			      new NewArrayOperator(type, dimension)));
	    break;
	}
        case opc_ifnull: case opc_ifnonnull:
            flow.appendBlock(createIfGoto
			     (ma, instr, new CompareUnaryOperator
			      (Type.tUObject, 
			       opcode - (opc_ifnull-Operator.COMPARE_OP))));
	    break;
        default:
            throw new InternalError("Invalid opcode "+opcode);
        }
    }
}

