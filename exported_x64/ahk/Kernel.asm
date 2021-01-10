%include"intrinsic.asm"
section .data

global ahk_Kernel_init
global ahk_Kernel_test_v

extern floatst
extern mem_alloc_block
extern e_throw
extern ahk_Test_test_v

section .text

ahk_Kernel_init:
  ret

ahk_Kernel_test_v:
test_v:
  push rbp
  mov rbp,rsp
  ; Test.test();
  call ahk_Test_test_v
  mov rsp,rbp
  pop rbp
  ret 0

