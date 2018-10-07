; This class contains evil stack operations, that make it very hard to 
; produce correct code.

.class public StackOps
.super java/lang/Object

.method public static concatSwaped(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;
	.limit locals 3
	.limit stack 3
	aload_1
	aload_2
	iload 0
	ifeq dontswap
	swap
dontswap:
	invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	areturn
.end method

.method public static dupTest(Ljava/lang/String;)Ljava/lang/String;
	.limit locals 1
	.limit stack 2
	; first a simple test that we can resolve
	aload_0
	dup
	invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	; now concat again.
	dup
	invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	; Now a more evil test.
	aload_0
	swap
	ifnull pushagain
	dup
	goto   concat
pushagain:
	aload_0
concat:
	invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	areturn
.end method
