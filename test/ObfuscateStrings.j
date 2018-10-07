; This class contains a hand optimized (and hard to decompile) 
; string obfuscating method.  Maybe I will use it in the Obfuscator
; some day, but probably the decompiler will handle those string, too.

.class public ObfuscateStrings
.super java/lang/Object

.method private static obf(Ljava/lang/String;)Ljava/lang/String;
	.limit locals 1
	.limit stack 7
	aload_0
	invokevirtual java/lang/String/toCharArray()[C
	dup
	iconst_0
	ldc 0x12345678
	goto firstloop

loopstart:
;  next pseudo random
;  char array
	dup_x1
	swap
	iload_0
	swap
	ldc 0x7fffffff
	iand

firstloop:
;stack content:
;  char array
;  char array copy
;  current index
;  current pseudo random

	ldc 1103515245
	imul
	sipush 12345
	iadd
	dup_x2
	sipush 0xff
	iand
	dup_x2
	pop
	
;stack content:
;  char array
;  next pseudo random
;  xor mask
;  char array copy
;  current index

	dup2_x1

;stack content:
;  char array
;  next pseudo random
;  char array copy
;  current index
;  xor mask
;  char array copy
;  current index

	caload
	ixor

;stack content:
;  char array
;  next pseudo random
;  char array copy
;  current index
;  new char

	swap
	dup_x1
	istore_0
	iinc 0 1
	castore

;stack content:
;  char array
;  next pseudo random
;locals:  1 = current index
	
	swap
	dup
	arraylength
	iload_0
	if_icmpne loopstart

	new java/lang/String
	dup_x2
	swap
	invokespecial java/lang/String/<init>([C)V
	pop
	areturn
.end method

.method private static obf2(Ljava/lang/String;)Ljava/lang/String;
	.limit locals 1
	.limit stack 8
	aload_0
	invokevirtual java/lang/String/toCharArray()[C
	ldc 0x12345678
	istore_0
	iconst_0

loop:
;  char array
;  next index


;stack content:
;  char array
;  current index
	dup2
	dup2
	caload

;  char array
;  current index
;  char array
;  current index
;  original char

	iload_0
	ldc 0x7fffffff
	iand

	dup
	ldc 1103515245
	imul
	sipush 12345
	iadd
	istore_0
	sipush 0xff
	iand
	ixor

;  char array
;  current index
;  char array
;  current index
;  new char

	castore
	iconst_1
	iadd

;  char array
;  next index
	dup2
	swap
	arraylength
	if_icmplt loop
	
;  char array
;  next index
	pop

	new java/lang/String
	dup_x1
	swap
	invokespecial java/lang/String/<init>([C)V
	areturn
.end method

