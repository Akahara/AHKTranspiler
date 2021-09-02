base fr.wonder.main;

import ahk.Kernel;
import fr.wonder.main.Structs;

unit Main;

global func int main() {
	Kernel.println("------ Expected: 4");
	Kernel.printlni(4);
	return 5;
}
