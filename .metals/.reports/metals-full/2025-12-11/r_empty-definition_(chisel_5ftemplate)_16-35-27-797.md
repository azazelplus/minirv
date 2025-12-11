error id: file://<WORKSPACE>/src/main/scala/IFU/PC.scala:
file://<WORKSPACE>/src/main/scala/IFU/PC.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -foo.
	 -foo#
	 -foo().
	 -scala/Predef.foo.
	 -scala/Predef.foo#
	 -scala/Predef.foo().
offset: 1489
uri: file://<WORKSPACE>/src/main/scala/IFU/PC.scala
text:
```scala
// MiniRV 程序计数器 (Program Counter)
package minirv.ifu

import chisel3._
import chisel3.util._
import minirv._

/**
  * PC - 程序计数器
  * 
  * 功能：
  * 1. 维护当前 PC 值
  * 2. 根据控制信号更新 PC (顺序 +4 或跳转)
  */
class PC extends Module {
  val io = IO(new Bundle {
    // PC 更新控制
    val jump_en   = Input(Bool())                       // 跳转使能
    val jump_addr = Input(UInt(Config.ADDR_WIDTH.W))    // 跳转目标地址
    
    // 当前 PC 输出
    val pc        = Output(UInt(Config.ADDR_WIDTH.W))   // 当前 PC 值
    val next_pc   = Output(UInt(Config.ADDR_WIDTH.W))   // 下一条 PC (用于调试)
  })

  // PC 寄存器，初始值为 0x80000000 (RISC-V 典型复位地址)
  val pc_reg = RegInit("h80000000".U(Config.ADDR_WIDTH.W))

  // 计算下一条 PC

  val next_pc_val = Mux(io.jump_en, io.jump_addr, pc_reg + 4.U)

  // 更新 PC 寄存器
  pc_reg := next_pc_val

  // 输出.
  //
  io.pc      := pc_reg
  io.next_pc := next_pc_val
}





//教学......

// Mux(cond, a, b)是一个选择器，如果 cond 为 true，则输出 a，否则输出 b.
    // 实现细节: scala允许一个apply语法糖: 如果myclass有一个apply()方法, 那麽myclass()等价于myclass.apply()
    // 最终, Mux(cond, a, b)是个表达式. 调用Mux的apply方法, 返回值是一个Chisel3中的 Data 对象. 这个Data代表电路中的一个节点, 在elaboration阶段, 会被转换成硬件描述语言中的一个节点.

// := 是Chisel3中的赋值操作符. 方向是「右边驱动左边」.



// Mix in(混入) 以及scala的消除菱形问题
// 这个机制用with实现. 它几乎和继承效果一样.  scala不允许class的多重继承, 但是允许trait的多重mix in.
// 例如: class a extends BASE with TRAIT1 with TRAIT2 with ...
// a继承自BASE, 同时mix in了TRAIT1, TRAIT2, ...
// a享有BASE, TRAIT1, TRAIT2, ... 的所有成员和方法.
// 但是继承父类方法时, scala的行为和C不同:
    // 对C来说, 如果
class A {
public:
    void fo@@o() { std::cout << "A::foo\n"; }
};

class B : public A {
public:
    void run() { std::cout << "B::run\n"; }
};

class C : public A {
public:
    void run() { std::cout << "C::run\n"; }
};

class D : public B, public C {
    // D 同时继承了 B::run 和 C::run
};


// val in  = Input(UInt(4.W))
// 编译器看到UInt(), 于是认为调用了UInt这个class的伴生对象的apply方法. 
// 即: UInt(4.W) 等价于 UInt.apply(4.W)














```


#### Short summary: 

empty definition using pc, found symbol in pc: 