base ahk;

unit Strings;

@native("ahk_Strings_int2str");
global func str int2str(int i) {};
@native("ahk_Strings_bool2str");
global func str bool2str(bool b) {};
@native("ahk_Strings_int2hexstr");
global func str int2hexstr(int i) {};
@native("ahk_Strings_float2str");
global func str float2str(float f) {};