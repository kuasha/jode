.class public RemovePopExample
.super java/lang/Object

.field private sng I
.field private dlb J
.field private obj Ljava/lang/Object;

.method <init>()V
	.limit locals 1
	.limit stack 1
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method

.method singlePop()V
	.limit locals 3
	.limit stack 20
	
	iconst_0
	istore 0
	
	iconst_0
	pop
	dconst_0
	pop2

	iload_0
	pop

	aconst_null
	iconst_0
	iaload
	pop
	
	iconst_0
	dup
	pop

	dup
	istore_0	
	pop
	
	lconst_1
	iconst_0
	dup_x2
	lshl
	lstore_1
	pop

	iload_0
	iconst_4
	dup_x1
	pop
	pop2

	iconst_4
	lload_1
	dup2
	pop2

	dup2_x1
	lstore_1
	istore_0
	pop2

	return

.end method

