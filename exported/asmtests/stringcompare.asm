%define SYSCALL_EXIT 60

section .data

global _start

str1 db 'abc',0
str2 db 'abc',0
str2l equ $ - str2

section .text
_start:
  push rbp
  mov rbp,rsp
  
break1:
  mov rsi,str1
  mov rdi,str1
  mov rcx,str2l
  cmp rax,rax ; set ZF
  repe cmpsb
  
  mov rdi,0
  mov rax,SYSCALL_EXIT
  syscall
