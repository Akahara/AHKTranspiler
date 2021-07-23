base fr.wonder.main;

unit Structs;

struct Structure {
  
  null (a=65);
  
  int a;
  
  /*constructor(int a);*/
  
}

struct Cyclic1 {
	
	Cyclic2 other;
	int a = 32;
	
}

struct Cyclic2 {

	int b = 29;
	Cyclic1 other;

}
