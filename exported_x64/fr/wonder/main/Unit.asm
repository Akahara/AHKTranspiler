%include"intrinsic.asm"
section .data

global fr_wonder_main_Unit_init
global fr_wonder_main_Unit_x
global fr_wonder_main_Unit_gi
global fr_wonder_main_Unit_array

global fr_wonder_main_Unit_gcd_iii
global fr_wonder_main_Unit_a_iii
global fr_wonder_main_Unit_a_ii
global fr_wonder_main_Unit_main_i

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

def_string str_cst_0,`str`
def_string str_cst_1,` / 5 = `
def_string str_cst_2,`   `
def_string str_cst_3,` % 5 = `

fr_wonder_main_Unit_x:
x dq str_cst_0
fr_wonder_main_Unit_gi:
gi dq 4
fr_wonder_main_Unit_array:
array dq 0

section .text

fr_wonder_main_Unit_init:
  push rbp
  mov rbp,rsp
  push qword 24
  call mem_alloc_block
  test rax,rax
  jns .special_0
  call e_throw
.special_0:
  push rax
  mov rax,56
  mov rbx,[rsp]
  mov [rbx],rax
  mov rax,42
  mov rbx,[rsp]
  mov [rbx+8],rax
  mov rax,37
  mov rbx,[rsp]
  mov [rbx+16],rax
  pop rax
  mov [array],rax
  mov rsp,rbp
  pop rbp
  ret

fr_wonder_main_Unit_gcd_iii:
gcd_iii:
  push rbp
  mov rbp,rsp
  ; if(y == 0)
  mov rax,[rbp+24]
  push rax
  pop rax
  test rax,rax
  jne .end_if@0
  ; return x;
  mov rax,[rbp+16]
  jmp .ret
  ; return x;
  jmp .end_else@1
.end_if@0:
  ; else
  ; return gcd(y, x%y);
  sub rsp,16
  mov rax,[rbp+24]
  mov [rsp],rax
  mov rax,[rbp+16]
  push rax
  pop rax
  mov rbx,[rbp+24]
  xor rdx,rdx
  idiv rbx
  mov rax,rdx
  mov [rsp+8],rax
  call gcd_iii
  jmp .ret
  ; return gcd(y, x%y);
.end_else@1:
.ret:
  mov rsp,rbp
  pop rbp
  ret 16

fr_wonder_main_Unit_a_iii:
a_iii:
  push rbp
  mov rbp,rsp
  sub rsp,8
  ; for(int i = -10..10) {
  mov qword [rsp],-10
  jmp .for@0_firstpass
.for@0:
  mov rax,[rsp]
  push rax
  pop rax
  inc rax
  mov [rsp],rax
.for@0_firstpass:
  mov rax,[rsp]
  push rax
  pop rax
  cmp rax,10
  jge .end_for@0
  ; Kernel.print(i);
  sub rsp,8
  mov rax,[rsp+8]
  mov [rsp],rax
  call ahk_Kernel_print_dec
  ; Kernel.print(" / 5 = ");
  sub rsp,8
  mov qword [rsp],str_cst_1
  call ahk_Kernel_print_str
  ; Kernel.print(i/5);
  sub rsp,8
  mov rax,[rsp+8]
  push rax
  pop rax
  mov rbx,5
  xor rdx,rdx
  idiv rbx
  mov [rsp],rax
  call ahk_Kernel_print_dec
  ; Kernel.print("   ");
  sub rsp,8
  mov qword [rsp],str_cst_2
  call ahk_Kernel_print_str
  ; Kernel.print(i);
  sub rsp,8
  mov rax,[rsp+8]
  mov [rsp],rax
  call ahk_Kernel_print_dec
  ; Kernel.print(" % 5 = ");
  sub rsp,8
  mov qword [rsp],str_cst_3
  call ahk_Kernel_print_str
  ; Kernel.print(i%5);
  sub rsp,8
  mov rax,[rsp+8]
  push rax
  pop rax
  mov rbx,5
  xor rdx,rdx
  idiv rbx
  mov rax,rdx
  mov [rsp],rax
  call ahk_Kernel_print_dec
  ; Kernel.println();
  call ahk_Kernel_print_ln
  ; }
  jmp .for@0
.end_for@0:
  ; return 0;
  xor rax,rax
  mov rsp,rbp
  pop rbp
  ret 16

fr_wonder_main_Unit_a_ii:
a_ii:
  push rbp
  mov rbp,rsp
  ; return a+1;
  mov rax,[rbp+16]
  push rax
  pop rax
  inc rax
  mov rsp,rbp
  pop rbp
  ret 8

fr_wonder_main_Unit_main_i:
main_i:
  push rbp
  mov rbp,rsp
  sub rsp,8
  ; for(int i =  0 : i < sizeof(array) : i++) {
  mov qword [rsp],0
  jmp .for@0_firstpass
.for@0:
  mov rax,[rsp]
  push rax
  pop rax
  inc rax
  mov [rsp],rax
.for@0_firstpass:
  mov rax,[rsp]
  push rax
  mov rbx,[array]
  xor rax,rax
  mov eax,[rbx-4]
  shr rax,3
  mov rbx,rax
  pop rax
  cmp rax,rbx
  jge .end_for@0
  ; Kernel.print(array[i]);
  sub rsp,8
  mov rax,[array]
  push rax
  mov rax,[rsp+16]
  pop rbx
  shl rax,3
  test rax,rax
  jns .special_1
  cmp dword[rbx-4],eax
  jl .special_1
  mov rax,-5
  call e_throw
.special_1:
  mov rax,[rax+rbx]
  mov [rsp],rax
  call ahk_Kernel_print_dec
  ; Kernel.println();
  call ahk_Kernel_print_ln
  ; }
  jmp .for@0
.end_for@0:
  ; return 0;
  xor rax,rax
  mov rsp,rbp
  pop rbp
  ret 0

