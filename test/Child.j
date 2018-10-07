.class public Child
.super Base

.field private test I

.method <init>()V
	.limit locals 1
	.limit stack 2
	aload_0
	invokespecial Base/<init>()V
	getstatic Base/test I	
	pop
	aload_0
	getfield Base/test J
	pop2
	aload_0
	getfield Child/test I
	pop
	aload_0
	getfield Child/test J
	pop2
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit locals 1
	.limit stack 0
	return
.end method
