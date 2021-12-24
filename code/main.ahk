
base fr.wonder.main;

import ahk.Kernel;
import ahk.Streams;

unit Main;

global func int main() {
	if(0 | 1)
		Kernel.out << "true\n";
	else
		Kernel.out << "false\n";
	
	Kernel.out << (3 | 4);
	
	return 5;
}
