%include"intrinsic.asm"

section .data

global ker_exit
global ahk_Kernel_print_hex
global ahk_Kernel_print_dec
global ahk_Kernel_print_str
global ahk_Kernel_print_strlen
global ahk_Kernel_print_ln
global floatst

ln_chars db `\n`,0xa
hex_chars db '0x................',0xa  ; hexadecimal characters
dec_chars db '-00000000000000000000',0xa  ; decimal characters, a 64 bits integer will always fit in 20 digites and the - sign

section .bss

floatst resq 1

section .text
  
  
ker_exit:
  ; the stack frame is not necessary
  ; but used for consistency
  push rbp
  mov rbp,rsp
  
  mov rbx,[rbp+16]
  mov rax,SYSCALL_EXIT
  int 0x80
  

ahk_Kernel_print_hex:
  push rbp
  mov rbp,rsp
  
  mov rax,[rsp+16] ; get the value to be printed
  
  mov rcx,16
.loop:
  mov rbx,rax
  and rbx,0xf
  cmp rbx,10
  jl .ignore
  add rbx,39 ; handle alphanumerical digits
.ignore:
  add rbx,48
  mov [hex_chars+rcx+1],bl
  shr rax,4
  loop .loop
  
  mov rax,SYSCALL_WRITE
  mov rbx,STD_OUT
  mov rcx,hex_chars
  mov rdx,18 ; 18 chars to be printed
  int 0x80 ; actual system call
  
  mov rsp,rbp
  pop rbp
  ret 8 ; __stdcall convention
  
  
; used to print a string which length is stored as a qword before
; the string characters. see the intrinsic.fasm#def_string macro
; if the string does not use this convention this function will print
; an undetermined number of characters!
ahk_Kernel_print_str:
  push rbp
  mov rbp,rsp
  mov rax,SYSCALL_WRITE
  mov rbx,STD_OUT
  mov rcx,[rsp+16]
  mov rdx,[rcx-8] ; n chars to be printed
  int 0x80
  mov rsp,rbp
  pop rbp
  ret 8
  
  
; used to print a string which length is not necessarily stored
; before its characters (mainly for compatibility with other languages)
; can be used to print an array of characters
ahk_Kernel_print_strlen:
  push rbp
  mov rbp,rsp
  mov rax,SYSCALL_WRITE
  mov rbx,STD_OUT
  mov rcx,[rsp+16]
  mov rdx,[rsp+24] ; n chars to be printed
  int 0x80
  mov rsp,rbp
  pop rbp
  ret 8

  
ahk_Kernel_print_ln:
  push rbp
  mov rbp,rsp
  
  mov rax,SYSCALL_WRITE
  mov rbx,STD_OUT
  mov rcx,ln_chars
  mov rdx,1 ; 1 char to be printed
  int 0x80 ; actual system call
  
  mov rsp,rbp
  pop rbp
  ret
  
  
ahk_Kernel_print_dec:
  mov rsi,20
  mov rax,[rsp+8] ; eax holds the number
  mov rbx,rax     ; get absolute
  sar rbx,31
  xor rax,rbx
  sub rax,rbx     ;/get absolute
  mov rbx,10      ; prepare for division
  xor rdx,rdx     ; clear dividende for division

  ; get indivudual characters
.loop:
  div rbx
  add rdx,'0'
  mov [dec_chars+rsi],dl
  dec rsi
  xor rdx,rdx
  test rax,rax
  jnz .loop
  
  ; add sign
  mov rax,[rsp+8]
  test rax,rax
  jns .positive
  mov [dec_chars+rsi],byte '-'
  dec rsi
.positive:
  
  ; print text
  mov rax,4
  mov rbx,1
  mov rcx,dec_chars
  add rcx,rsi
  inc rcx
  mov rdx,20
  sub rdx,rsi
  int 0x80
  
  ret 8
  
  
dbg_print_fpu_status:
  push rbp
  mov rbp,rsp
  sub rsp,8
  fstsw [rsp]
  call ahk_Kernel_print_hex
  call ahk_Kernel_print_ln
  mov rsp,rbp
  pop rbp
  ret
