base fr.wonder.main;

import ahk.Kernel;
import fr.wonder.main.Structs;

unit Main;

struct Base {
	
}

struct Gen<X> {
	
}

global func int main() {
	Kernel.println("------ Expected: 4");
	Kernel.printlni(4);
	return 5;
}
