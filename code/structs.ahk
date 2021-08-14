base fr.wonder.main;

unit Structs;

global struct Structure {
  
  null (a=65);
  
  global int a;
  
  global constructor(int a);
  
  operator opStructPlus : Structure + Structure = Structure;
  
}

func Structure opStructPlus() {
	return null;
}

global struct Cyclic1 {
	
	global Cyclic2 other;
	global int a = 32;
	
}

global struct Cyclic2 {

	global int b = 29;
	global Cyclic1 other;

}
