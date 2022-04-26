import gdb
import re

IS_AHK_CONTEXT = False

TABLE_SIZE = None
C_GOLD = "\u001b[38;5;220m"
C_BLUE = "\u001b[34m"
C_CYAN = "\u001b[38;5;44m"
C_RED = "\u001b[38;5;124m"
C_RESET = "\u001b[0m"
C_ITALIC = "\u001b[3m"
C_BOLD = "\u001b[1m"

# qword format, can be used with the % operator
QWF = "0x%016x"

inferior = None

def log(*args):
	msg = ''.join(str(a) for a in args)
	if msg[-1] != '\n':
		msg += "\n"
	gdb.write(msg + C_RESET)
	
def eval(cmd):
	return int(gdb.parse_and_eval(cmd))
	
def exec(cmd):
	return gdb.execute(cmd, from_tty=False, to_string=True)

def is_valid_address(add):
	while (add & 0xcc) == 0xcc:
		add = add >> 8
	return add != 0

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


class AllocTable:
	
	tables_cache = []

	def __init__(self, table_index, mem_address):
		self.table_index = table_index
		self.mem_address = mem_address
		self.blocks = [None for i in range(TABLE_SIZE)]
		self.next_table = None
		if len(AllocTable.tables_cache) > table_index:
			raise Exception("Table " + str(table_index) + " already exists")
		AllocTable.tables_cache.append(self)
		
	@staticmethod
	def reload_tables():
		global TABLE_SIZE
		TABLE_SIZE = eval("&TABLE_SIZE")
		AllocTable.tables_cache = []
		table_start = eval("(void*) table_start")
		if is_valid_address(table_start):
			table = AllocTable(0, table_start)
			table.reload_blocks()
		
	@staticmethod
	def tables_count():
		return len(AllocTable.tables_cache)
			
	@staticmethod
	def get_table(tableId):
		return AllocTable.tables_cache[tableId] if tableId < AllocTable.tables_count() else None
		
	def reload_blocks(self):
		# log("reading table ", self.table_index, " from ", QWF%self.mem_address)
		table_data = self.read_bytes()
		for i in range(TABLE_SIZE):
			block_entry_index = TABLE_SIZE-i
			block_address = table_data[1+i]
			self.blocks[i] = MemoryBlock(self, block_entry_index, block_address)
		if self.next_table is None and is_valid_address(table_data[0]):
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
		# log("reading block ", alloc_table.table_index, ":", table_entry_index, " from ", QWF%address)
		if is_valid_address(address):
			self.size = qword(address-8) & 0x00ffffffffff
		else:
			self.size = -1
			
	def is_valid(self):
		return self.size != -1
		
	def __str__(self):
		return C_GOLD+("E%02d"%self.table_entry_index)+C_RESET+'= '+C_BLUE+(QWF%self.address)


def print_mem_table(table):
	log("Mem Table ", table.table_index, " ------------- ", QWF%table.mem_address)
	cols = 4
	for i in range(0, TABLE_SIZE, cols):
		s = ""
		for c in range(cols):
			s += str(table.blocks[TABLE_SIZE-1-(i+c)]) + " "
		log(s)
	log(C_GOLD, "next", C_RESET, "= ", C_BLUE, QWF % (table.next_table.mem_address if table.next_table else 0))


def memcmd_inspectmem(*args):
	mem_start = eval("(void*) mem_start")
	table_start = eval("(void*) table_start")
	current_break = eval("(void*) current_break")
	log("mem_start    = ", QWF % mem_start)
	log("table_start  = ", QWF % table_start)
	log("current_break= ", QWF % current_break)
	
	table = AllocTable.get_table(0)
	
	if table is None:
		log(C_RED + "No first table")
		return
	
	mem_blocks = []
	
	while True:
		print_mem_table(table)
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
		for i in range(-8, block.size):
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
	for i in range(0, max_used_layout_index):
		bid = layout[i]
		color = None
		
		if prev_bid != bid:
			h += "%02d " % bid
			if bid != -1:
				used_colors = (used_colors[1], used_colors[0])
				bold_count = 8
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
		s += color + "%02x " % full_mem[i] + C_RESET
		if i%32 == 31:
			log(h)
			log(s)
			h = ""
			s = ""
	if s:
		log(h)
		log(s)
		

def memcmd_memtable(*args):
	table_index = None
	try:
		if len(args) == 0:
			raise ValueError()
		table_index = int(args[0])
	except ValueError:
		log("Usage: memtable <table index>")
		return
	table = AllocTable.get_table(table_index)
	if table is None:
		log(C_RED, "There are only ", AllocTable.tables_count(), " allocation tables")
	else:
		print_mem_table(table)
		

def memcmd_printstack(*args):
	stack_start = eval("(void*) stack_start")
	stack_stop = eval("$rsp")
	stack_size = (stack_start-stack_stop)/8
	log("stack_start=", QWF % stack_start)
	log("stack_stop =", QWF % stack_stop)
	
	if stack_size != int(stack_size):
		log(C_RED, "Invalid stack size: ", stack_size)
		return
	stack_size = int(stack_size)
	log("stack_size =", stack_size)
	stack_content = qwords(stack_stop, stack_size)
	cols = 4
	while len(stack_content) % cols != 0:
		stack_content.append(0)
	for i in range(0, stack_size, cols):
		address = stack_start - i*8
		content = ""
		for j in range(0, cols):
			content += " " + QWF % stack_content[i+j]
		log("#", i//cols, " ", C_BLUE, QWF % address, C_RESET, content)
		
		
def memcmd_printfpu(*args):
	log(exec("info all-registers st0"))
	log(exec("info all-registers st1"))
	log(exec("info all-registers st2"))
	log(exec("info all-registers st3"))
	
	
def memcmd_printeflags(*args):
	def print_flag(eflags, bit, flag_name):
		if (eflags & (1<<bit)) != 0:
			log("- ", flag_name)
			return eflags^(1<<bit)
		return eflags
		
	flags = exec("info register eflags")
	log(flags)
	eflags = int(re.split(" +", flags)[1], 16)
	eflags ^= 1<<1 # (ignore the reserved, always on bit)
	eflags = print_flag(eflags, 0, C_BLUE+"Carry"+C_RESET+" flag")
	eflags = print_flag(eflags, 2,  C_BLUE+"Parity"+C_RESET+" flag")
	eflags = print_flag(eflags, 4,  "Adjust flag")
	print_flag(eflags, 6,  C_BLUE+"Zero"+C_RESET+" flag")
	eflags = print_flag(eflags, 6,  C_BLUE+"Equal"+C_RESET+" flag")
	eflags = print_flag(eflags, 7,  C_BLUE+"Sign"+C_RESET+" flag")
	eflags = print_flag(eflags, 9,  C_RED+"Interrupt enable"+C_RESET+" flag")
	eflags = print_flag(eflags, 11, C_BLUE+"Overflow"+C_RESET+" flag")
	eflags = print_flag(eflags, 16, C_RED+"Resume"+C_RESET+" flag")
	if eflags != 0:
		log("Remaining: ", hex(eflags))
	


class MemCommand(gdb.Command):
	def __init__(self, name, function):
		super().__init__(name, gdb.COMMAND_DATA)
		self.function = function
		self.name = name
		self.dont_repeat()
		
	def prepare_command(self):
		global inferior, IS_AHK_CONTEXT
		inferior = gdb.inferiors()[0]
		
		try:
			gdb.parse_and_eval("(void)AHK_LANGUAGE")
			IS_AHK_CONTEXT = True
		except gdb.error:
			IS_AHK_CONTEXT = False
			
		if IS_AHK_CONTEXT:
			AllocTable.reload_tables()
	
	def invoke(self, argument, from_tty):
		self.prepare_command()
		log("AHK> " if IS_AHK_CONTEXT else ">> ", self.name, " ", argument)
		self.function(argument)
		

if __name__ == "__main__":
	for i in range(10):
		exec("break break" + str(i))
	MemCommand("inspectmem", memcmd_inspectmem)
	MemCommand("memtable", memcmd_memtable)
	MemCommand("printstack", memcmd_printstack)
	MemCommand("ifpu", memcmd_printfpu)
	MemCommand("iflags", memcmd_printeflags)
