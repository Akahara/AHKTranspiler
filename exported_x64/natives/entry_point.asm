; -------------------------------------------------- ;
; entry_point.fasm
; 
; The entry point of the program, containing the
; _start method. This unit initializes the FPU,
; the dynamic memory management unit and calls the
; "true" program entry point specified by the
; program manifest. If no calls to Kernel.exit is
; made before the entry point method returns, its
; return value is used as the program exit code.
; 
; TODO comment &units_initialization
;
; All references in this file of "fr_wonder_main_Unit_main_i"
; are replaced at compile time by the manifest value
; for "entry-point".
; 
; ;The FPU is initialized with rounding countrol set
; ;toward 0. The variable "floatst" is stored in
; ;FPUControl.fasm
; 
; There must be no other global _start method in any
; other .asm file.
; 
; -------------------------------------------------- ;


%include"intrinsic.asm"

segment .text

global _main

extern fr_wonder_main_Unit_main_i
extern floatst
extern mem_init

extern fr_wonder_main_Unit_init


_main:
  ; initialize the floating point unit (FPU)
  ; set rounding control to round to 0 with 0xc00
  finit
  fnstcw [floatst]
  mov ax,[floatst]
  or ax,0xc00
  mov [floatst],ax
  fldcw [floatst]
  
  ; call the dynamic memory allocation initialization
  call mem_init
  
  ; call every unit initialization function
  call fr_wonder_main_Unit_init
  
  
  ; call the main function of the program
  ; fr_wonder_main_Unit_main_i will be replaced by the actual
  ; function name
  call fr_wonder_main_Unit_main_i
  
  ; exit with the return value of fr_wonder_main_Unit_main_i
  mov rbx,rax
  mov rax,SYSCALL_EXIT
  int 0x80
