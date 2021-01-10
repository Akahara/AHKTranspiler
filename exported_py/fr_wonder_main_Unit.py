from ahk_Kernel import *

class Unit:

  @staticmethod
  def b(x):
    print(x)


  @staticmethod
  def b_1(x):
    print(x)


  @staticmethod
  def a(a):
    for i in range(0, a):
      print(i)
    return 0


  @staticmethod
  def main():
    return Unit.a(5)



if __name__ == '__main__':
  exit(Unit.main())
