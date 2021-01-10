section .data

global e_throw

extern ahk_Kernel_print_hex
extern ker_exit

section .text

; special case: the error code argument is stored in rax and
; not pushed onto the stack
e_throw:
  push rbp
  mov rbp,rsp
  
  push rax
  push rax
  call ahk_Kernel_print_hex
  call ker_exit
  
  mov rsp,rbp
  pop rbp
  ret