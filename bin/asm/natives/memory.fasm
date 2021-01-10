%include"intrinsic.asm"

section .data

TABLE_SIZE equ 32    ; number of entry per table
ALLOC_SIZE equ 16384 ; number of bytes allocated every time 

E_BRK_ERROR equ -1      ; error code, used when the system call to brk
                        ; do not succeed
E_ALLOC_OVERFLOW equ -2 ; error code, used when a call to #mem_alloc_block
                        ; is made with a size exceeding the 32 bits limit


current_break dq 0
table_start dq 0
mem_start dq 0

global mem_init
global mem_alloc_block

section .text

; ==============================================================
; 
; A data block of size n is stored in the heap as
; a memory block of size n+4, with the value n being
; stored in the first 4 bytes. A pointer to the data
; points to the first byte of data, so [ptr-4] can be
; used at any time to retrieve the data size. This value
; must NEVER be modified by other than a mem_ function.
;
; ------/==============\/==========\------
;       | |            || |        |      
; ...   |n| <data>     ||n| <data> | ...  
;       | |            || |        |      
; ------\==============/\==========/------
;         ^ptr          ^ptr           
;
;
; An allocation table is stored as a memory block (see
; the description of memory blocks) of size 4+ 32*8+8.
; the first 8 bytes are the address of the next allocation
; table if there is one, 0 otherwise and the next 32 qwords
; are the entries of the table (pointers to memory blocks).
; So [ptr] is the next allocation table and [ptr+n*8] is
; the (32-n+1)th entry (n must be between 1 and 32 included).
; An exception is made for the first allocation table, which
; is not stored as a memory block and therefore hasn't its
; size before it.
; The 32nd entry is reserved for the next allocation table
; entry (as tables as stored as mem blocks, they need an entry).
; 
; ------/==========================\------
;       | | |   |   |   |       |  |
; ...   |n|T|E32|E31|E30|  ...  |E1| ...
;       | | |   |   |   |       |  |
; ------\==========================/------
;          ^ptr
; T is the next allocation table
; En is the n-th entry
;
; ==============================================================



; ==============================================================
; Initialize the dynamic memory allocator.
; This method grows the heap twice: once for the first
; allocation table and once more to give space to allocate
; data.
; The first table is not actually stored as a data block
; so its size is not stored before it.
; If one of the two allocation fails E_BRK_ERROR is returned
; and the program should stop execution.
;   return value: 0 for success, E_BRK_ERROR otherwise
; ==============================================================
mem_init:
  push rbp
  mov rbp,rsp
  
  ; get the current break
  mov rax,SYSCALL_BRK
  mov rbx,0
  int 0x80
  mov [current_break],rax
  mov [table_start],rax
  
  ; allocate the first memory table
  mov rbx,TABLE_SIZE
  inc rbx
  imul rbx,8
  ; rbx= 8*n+8  n the number of entry per table
  add rbx,rax
  mov rax,SYSCALL_BRK
  int 0x80
  cmp rax,[current_break]
  jne .successful_alloc
  ; if alloc failed, return with E_BRK_ERROR
  mov rax,E_BRK_ERROR
  jmp .ret
  
.successful_alloc:
  ; fill the alloc table with 0s
  ; store current break in edx 
  mov rdx,rax
.loop:
  sub rbx,8
  mov qword [rbx],0
  cmp rbx,[current_break]
  jne .loop
  
  inc rdx
  mov [current_break],rdx
  mov [mem_start],rdx
  
  ; allocate the first true memory block
  call grow_heap
.ret:
  mov rsp,rbp
  pop rbp
  ret
  
  
  
; ==============================================================
; deallocates all allocated memory with a call to brk
; also deallocates all allocation tables, do NOT try
; to call alloc after this method was called
; Also not necessary, the program memory space is deallocated
; by the kernel when the process exit
;   return value: none (/ the default break)
; ==============================================================
mem_dispose:
  push rbp
  mov rbp, rsp
  mov rax,SYSCALL_BRK
  mov rbx,[table_start]
  int 0x80
  mov rsp,rbp
  pop rbp
  ret
  
  
  
; ==============================================================
; grows the heap by ALLOC_SIZE bytes with a call to brk
;  return value: 0 in case of a success, E_BRK_ERROR otherwise
;  regs: rax & rbx
; ==============================================================
grow_heap:
  push rbp
  mov rbp,rsp
  
  mov rbx,[current_break]
  add rbx,ALLOC_SIZE
  mov rax,SYSCALL_BRK
  int 0x80
  cmp rax,[current_break]
  mov [current_break],rax
  mov rax,0
  jne .success
  
  mov rax,E_BRK_ERROR
.success:
  
  mov rsp,rbp
  pop rbp
  ret
  
  
  
; ==============================================================
; Allocates a block of memory in the heap.
; Retrieves an empty mem block using #get_empty_block, creates
; an allocation table entry pointing to the allocated block and
; returns a pointer to it (see the memory block desc.).
; If the affected entry is the 31th of the last table a new
; table is allocated and its entry is created at the 32th place
; of the current latest table.
; If the address + the size of the block is greater than the
; current heap break (memory overflow) the heap is grown until
; the data fits.
;   return value: E_BRK_ERROR if an error occurs while growing
;        the heap. E_ALLOC_OVERFLOW if the given size exceed
;        the 32 bits limit. The allocated block address
;        otherwise
; ==============================================================
mem_alloc_block:
  push rbp
  mov rbp,rsp
  
  push qword[rsp+16] ; push S
  call get_empty_block
  ; rax = A an empty block address
  ; rbx = B an empty table entry address
  ; rcx = T the table in which the entry B resides
  test rax,rax
  js .ret_fail ; get_empty_block failed
  mov rsi,[rsp+16] ; rsi = S (fits into 4 bytes)
  
  mov dword[rax-4],esi
  mov dword[rbx],eax
  
  mov rdx,rcx
  add rdx,8
  cmp rbx,rdx ; check if the entry is the last of the table (located at T+8)
  jne .tables_not_full
  cmp qword[rcx],0 ; check if the table has a next one
  jnz .tables_not_full
.tables_full:
  push rax
  ; allocate new allocation table
  push qword TABLE_SIZE*8+8
  call get_empty_block
  test rax,rax
  js .ret_fail ; get_empty_block failed
  mov dword[rax-4],TABLE_SIZE*8+8
  mov [rcx],rax
  mov rdx,rcx
  mov rcx,TABLE_SIZE
.fill_loop: ; fill the table with 0s
  mov qword[rdx+rcx*8],0
  loop .fill_loop
  mov qword[rdx],0
  pop rax
  jmp .ret
  
.ret_fail:
  mov rax,-1 ; TODO add specific error codes
.tables_not_full:
.ret:
  mov rsp,rbp
  pop rbp
  ret 8
  
  
  
; ==============================================================
; Searches in the heap for a non-allocated memory block with
; the given size.
;   return values:
;        rax - the address of the allocated block. <!> May be out
;              of the current heap space <!>.
;        rbx - the address of an empty allocation table entry
;        rcx - the address of the allocation table of $rbx
; ==============================================================
get_empty_block:
  push rbp
  mov rbp,rsp
  
  mov rsi,[rsp+16]
  cmp rsi,0xffff
  jle .alloc
  mov rax,E_ALLOC_OVERFLOW ; too large size
  jmp .ret_fail
.alloc:
  
  ; rsi = S the size of the block to allocate
  ; rax = A the memory address at which the block can be allocated
  ; rbx = B the memory address of an empty allocation table entry
  ; rdx = T the memory address of the current allocation table
  ; rcx = t the position of the current entry in the current allocation table
  
  xor rbx,rbx
  mov rax,[mem_start]
  
.check_tables:
  mov rdx,[table_start]
.check_next_table:
  mov rcx,TABLE_SIZE
.check_table:
  lea r11,[rdx+rcx*8] ; r11 = |E| = T+t*8
  mov r9,[r11]        ; r9 = E = [T+t*8] = [r11]
  lea r12,[rax+rsi+4] ; r12 = A+S+4 the speculative end of the researched block
  test r9,r9          ; E=0 -> no overlap
  jz .nooverlapsave
  lea r10,[r9-4]      ; r10 = E-4 the size of the entry block
  cmp r10,r12
  jge .nooverlap      ; E-4>=A+S+4 -> no overlap
  add r9,[r10]        ; r9 = E+[E-4] = r9+[r10]
  cmp r9,rax
  jle .nooverlap      ; E+[E-4]<=A -> no overlap
.overlap:             ; -> overlap
  mov rax,r9          ; A = E+[E-4] the end of the overlapping block
  jmp .check_tables
  
.nooverlapsave:       ; keep track of an empty table entry in rbx
  test rbx,rbx
  jnz .nooverlap
  mov rbx,r11
.nooverlap:
  loop .check_table
  cmp dword[rdx],0
  jz .check_heap        ; a location was found
  mov rdx,[rdx]         ; T = [T] the next table
  jmp .check_next_table ; a table remains
  
.check_heap:
  push rax ; A
  push rbx ; B
  push rdx ; T (will be popped into rbx)
.grow_heap:
  mov rax,[rsp+16]
  cmp rax,[current_break]
  jle .ret
  call grow_heap
  jmp .grow_heap
  
.ret:
  pop rcx ; T
  pop rbx ; B
  pop rax ; A+4
  add rax,4
.ret_fail:
  mov rsp,rbp
  pop rbp
  ret 8
  
