; -------------------------------------------------- ;
; intrinsic.fasm
; 
; This unit contains all kernel constants such as
; system calls values and standard identifiers.
; 
; This file contains constants only, it adds no data
; to the object file and is only used by the
; preprocessor. These values may be used by any .asm
; file that includes this one
; 
; -------------------------------------------------- ;

%ifndef NATIVE_CONSTANTS
  %define NATIVE_CONSTANTS
  
  ; define system calls
  %define SYSCALL_WRITE 0x4
  %define SYSCALL_EXIT 0x1
  %define SYSCALL_BRK 0x2d
  
  
  %define STD_OUT 1
  
%endif


%ifndef FASM_MACRO
  %define FASM_MACRO
  
  ; define a NULL value, used when a variable is explicitely
  ; set to NULL, (see NONE for uninitialized variables).
  ; this value expands to '0', the null pointer. 
  %define NULL 0
  
  ; define a NONE value, used for values declared without
  ; an initial value (similar to NULL but exclusive to
  ; non-initialized variables)
  ; As for NULL this expands to '0', the null pointer.
  %define NONE 0
  
  ; define String declarations:
  ;  def_string name,'abc'
  ; will expand to :
  ;   dd 3
  ;   name: db 'abc'
  ; this is used to store the string length just before
  ; the string without having to move the string label
  %macro def_string 2
    %strlen len %2
    dq len
    %1: db %2
    %undef len
  %endmacro
  
%endif

