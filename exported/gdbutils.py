import gdb

TABLE_SIZE = 32
C_GOLD = "\u001b[38;5;220m"
C_BLUE = "\u001b[34m"
C_CYAN = "\u001b[38;5;44m"
C_RED = "\u001b[38;5;124m"
C_RESET = "\u001b[0m"
C_ITALIC = "\u001b[3m"
C_BOLD = "\u001b[1m"
C_UNDERLINE = "\u001b[4m"

inferior = None

def log(*args):
    gdb.write("P] " + ''.join(str(a) for a in args) + C_RESET + "\n")
    
def eval(cmd):
    return int(gdb.parse_and_eval(cmd))

""" reads qwords from memory at the given address """
def qwords(mem_start, count):
    memory_view = inferior.read_memory(mem_start, count*8)
    return [int.from_bytes(memory_view[i*8:i*8+8], 'little') for i in range(count)]
def mem_bytes(mem_start, count):
    memory_view = inferior.read_memory(mem_start, count)
    return [int.from_bytes(b, 'little') for b in memory_view]

""" reads a single qword from memory at the given address """
def qword(mem_start):
    return qwords(mem_start, 1)[0]


def set_inferior():
    global inferior
    inferior = gdb.inferiors()[0]


if __name__ == '__main__':
    # gdb.Breakpoint("mem_alloc_block", gdb.BP_BREAKPOINT)
    pass


class MemCommand(gdb.Command):
    def __init__(self):
        super().__init__("inspectmem", gdb.COMMAND_DATA)
        self.dont_repeat()
        
    def invoke(self, argument, from_tty):
        if not len(argument):
            memcmd_print_info()
        else:
            log(">>")
MemCommand()

class AllocTable:
    def __init__(self, table_index, mem_address):
        self.table_index = table_index
        self.mem_address = mem_address
        self.blocks = [None for i in range(TABLE_SIZE)]
        self.next_table = None
            
    def reload_blocks(self):
        table_data = self.read_bytes()
        for i in range(TABLE_SIZE):
            block_entry_index = TABLE_SIZE-i
            block_address = table_data[1+i]
            self.blocks[i] = MemoryBlock(self, block_entry_index, block_address)
        if self.next_table is None and table_data[0] != 0:
            self.next_table = AllocTable(self.table_index+1, table_data[0])
        if self.next_table is not None:
            self.next_table.reload_blocks()
    
    def read_bytes(self):
        return qwords(self.mem_address, TABLE_SIZE+1)
        

class MemoryBlock:
    def __init__(self, alloc_table, table_entry_index, address):
        self.alloc_table = alloc_table
        self.table_entry_index = table_entry_index
        self.address = address
        if address:
            self.size = qword(address-8)
            self.struct_address = qword(address-16)
        else:
            self.size = -1
            self.struct_address = -1
            
    def is_valid(self):
        return self.size != -1
        
    def __str__(self):
        return C_GOLD+("E%02d"%self.table_entry_index)+C_RESET+'= '+C_BLUE+("0x%08x"%self.address)

def memcmd_print_info():
    set_inferior()
    mem_start = eval("(void*) mem_start")
    table_start = eval("(void*) table_start")
    current_break = eval("(void*) current_break")
    log("%s= 0x%08x" % ("mem_start", mem_start))
    log("%s= 0x%08x" % ("table_start", table_start))
    log("%s= 0x%08x" % ("current_break", current_break))
    
    table = AllocTable(1, table_start)
    table.reload_blocks()
    mem_blocks = []
    
    cols = 4
    while True:
        log("Mem Table ", table.table_index, " -------------")
        for i in range(0, TABLE_SIZE, cols):
            s = ""
            for c in range(cols):
                s += str(table.blocks[TABLE_SIZE-1-(i+c)]) + " "
            log(s)
        mem_blocks += [b for b in table.blocks if b.is_valid()]
        if table.next_table is None:
            break
        table = table.next_table
    log("Found ", table.table_index, " tables, ", len(mem_blocks), " active memory blocks")
    
    if not mem_blocks:
        return
    
    full_mem = mem_bytes(mem_start, int(current_break-mem_start))
    
    layout = [ -1 for _ in full_mem ]
    max_used_layout_index = 0
    for block in mem_blocks:
        for i in range(-16, block.size):
            lindex = block.address-mem_start+i
            if layout[lindex] != -1:
                layout[lindex] = -2
            else:
                layout[lindex] = block.table_entry_index
        max_used_layout_index = max(max_used_layout_index, block.address+block.size-mem_start)
    
    h = ""
    s = ""
    prev_bid = -3
    used_colors = (C_BLUE, C_CYAN)
    bold_count = 0
    underline_count = 0
    for i in range(0, max_used_layout_index):
        bid = layout[i]
        color = None
        
        if prev_bid != bid:
            h += "%02d " % bid
            if bid != -1:
                used_colors = (used_colors[1], used_colors[0])
                bold_count = 8
                underline_count = 16
            prev_bid = bid
        else:
            h += "   "
            
        if bid == -1:
            color = C_RESET
        elif bid == -2:
            color = C_RED
        else:
            color = used_colors[0]
            
        if bold_count > 0:
            s += C_BOLD
            bold_count -= 1
        if underline_count > 0:
            s += C_UNDERLINE
            underline_count -= 1
        s += color + "%02x " % full_mem[i] + C_RESET
        if i%32 == 31:
            log(h)
            log(s)
            h = ""
            s = ""
    if s:
        log(h)
        log(s)
    