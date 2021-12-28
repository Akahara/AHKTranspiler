
base fr.wonder.main;

import ahk.Kernel;
import ahk.Streams;

unit Main;

func int foo() {
	Kernel.out << "foo\n";
	return 2;
}

func void printArray(int[] array) {
	Kernel.out << "array[:" << sizeof(array) << "] ";
	for(int i : 0..sizeof(array)) {
		Kernel.out << array[i];
		Kernel.out << " ";
	}
	Kernel.out << "\n";
}

global func int main() {
	int[] array = [1, 3, 5];
	
	Kernel.out << "-- Expected [1 3 5]\n";
	printArray(array);
	
	Kernel.out << "-- Expected 3 foo 5\n";
	Kernel.printlni(sizeof(array));
	Kernel.printlni(array[foo()]);
	
	Kernel.out << "-- Expected [2 3 4]\n";
	array[0] = 2;
	array[-1] = 4;
	printArray(array);
	
	Kernel.out << "-- Expected 2xfoo [2 3 6]\n";
	array[3] = foo();
	array[foo()] = 6;
	printArray(array);
	
	Kernel.out << "-- Expected []\n";
	int[][] double = [:0];
	printArray(double[0]);
	
	Kernel.out << "-- Expected [2 6 3]\n";
	array[1], array[2] = array[2], array[1];
	printArray(array);
	
	Kernel.out << "-- Expected [2 0 3]\n";
	array[3], array[1] = array[1], array[3];
	printArray(array);
	
	return 5;
}
