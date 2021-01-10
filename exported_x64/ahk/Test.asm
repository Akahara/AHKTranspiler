%include"intrinsic.asm"
section .data

global ahk_Test_init
global ahk_Test_test_v

extern floatst
extern mem_alloc_block
extern e_throw
extern ahk_Kernel_print_dec
extern ahk_Kernel_print_hex
extern ahk_Kernel_print_str
extern ahk_Kernel_print_strlen
extern ahk_Kernel_print_hex
extern ahk_Kernel_print_ln
extern ker_exit
extern ahk_Kernel_test_v

section .text

ahk_Test_init:
  ret

ahk_Test_test_v:
test_v:
  push rbp
  mov rbp,rsp
  ; Kernel.print(4);
  sub rsp,8
  mov qword [rsp],4
  call ahk_Kernel_print_dec
  mov rsp,rbp
  pop rbp
  ret 0

