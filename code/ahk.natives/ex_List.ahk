base ahk;

unit List;

struct List<[X]> {
	int size = 0;
	X[] array = [:0];
}

func <[X]> void ensureRemainingCapacity(List<[X]> list) {
	if(list.size == sizeof(list.array)) {
		X[] newArray = [:list.size*2];
		for(int i = 0 : i < list.size : i++)
			newArray[i] = list.array[i];
		list.array = newArray;
	}
}

global func <[X]> X at(List<[X]> list, int index) {
	return list.array[index];
}

global func <[X]> void push(List<[X]> list, X value) {
	ensureRemainingCapacity<[X]>(list);
	list.array[list.size] = value;
	list.size++;
}

global func <[X]> X pop(List<[X]> list) {
	if(list.size == 0)
		return null;
	list.size--;
	return list.array[list.size];
}

global func <[X]> void insert(List<[X]> list, X value, int index) {
	if(list.size <= index) {
		push<[X]>(list, value);
	} else {
		ensureRemainingCapacity<[X]>(list);
		for(int i = list.size : i > index : i--)
			list.array[i] = list.array[i-1];
		list.array[index] = value;
	}
}