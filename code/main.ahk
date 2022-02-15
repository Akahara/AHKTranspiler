base fr.wonder.main;

import ahk.Kernel;
import ahk.List;

unit Main;

global func int main() {
	List<[int]> list = List<[int]>();
	int i = List.at<[int]>(list, 0);
	Kernel.out << i << "\n";
	return 5;
}
