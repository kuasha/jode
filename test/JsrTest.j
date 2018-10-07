.class public JsrTest
.super java/lang/Object

.method public static main([Ljava/lang/String;)V
	.limit locals 3
	.limit stack 5

	jsr big_sub
	jsr evil_jsrret
	astore_1
returninstr:
	return

evil_jsrret:
	astore_2
	jsr retinstr
retinstr:
	ret 2

big_sub:
	astore_2
	aload_0
	astore_1
	aload_0
	ifnull skip
	jsr subroutine
skip:
	aload_0
	ifnull end
	jsr sub2
end:
	ret 2



subroutine:
	astore_1
	aload_0
	ifnull gotoend1
	aload_0
	ifnonnull bothsubs
	ret 1
gotoend1:
	jsr innermostSub
	goto returninstr


sub2:
	astore_1
	aconst_null
	ifnonnull bothsubs
	ret 1
bothsubs:
	aload_0
	ifnull end
	jsr innermostSub
	goto end

innermostSub:
	astore_1
	ret 1

.end method
