error id: 5D9DA2EF3160103CAEB39C899602722F
file://<WORKSPACE>/src/main/scala/IFU/PC.scala
### java.lang.UnsupportedOperationException: Position.start on NoPosition

occurred in the presentation compiler.



action parameters:
offset: 1459
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
    // 对C来说, 如果@@
class A {
public:
    void foo() { std::cout << "A::foo\n"; }
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


presentation compiler configuration:
Scala version: 2.13.16
Classpath:
<WORKSPACE>/.bloop/out/chisel_template/bloop-bsp-clients-classes/classes-Metals-7eIU-94GSneMMbFxn4Rr9Q== [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/sourcegraph/semanticdb-javac/0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-RC1/chisel_2.13-7.0.0-RC1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.16/scala-library-2.13.16.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.13.1/commons-text-1.13.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.10.0/os-lib_2.13-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.7/json4s-native_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.7/data-class_2.13-0.2.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.16/scala-reflect-2.13.16.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.3.1/upickle_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.1/firtool-resolver_2.13-2.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.1.0/geny_2.13-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.7/json4s-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.7/json4s-native-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.3.1/ujson_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.3.1/upack_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.3.1/upickle-implicits_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.7/json4s-ast_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.7/json4s-scalap_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.3.1/upickle-core_2.13-3.3.1.jar [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-language:reflectiveCalls -deprecation -feature -Xcheckinit -Ymacro-annotations -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.reflect.internal.util.Position.fail(Position.scala:24)
	scala.reflect.internal.util.UndefinedPosition.start(Position.scala:104)
	scala.reflect.internal.util.UndefinedPosition.start(Position.scala:100)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.compoundTypeRest(Parsers.scala:1258)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.compoundTypeRest$(Parsers.scala:1235)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anon$2.compoundTypeRest(Parsers.scala:2349)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.compoundType(Parsers.scala:1232)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.compoundType$(Parsers.scala:1230)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anon$2.compoundType(Parsers.scala:2349)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.asInfix$1(Parsers.scala:1281)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.$anonfun$infixTypeRest$3(Parsers.scala:1285)
	scala.reflect.internal.Trees$TreeContextApiImpl.orElse(Trees.scala:111)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.infixTypeRest(Parsers.scala:1285)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.infixTypeRest$(Parsers.scala:1262)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anon$2.infixTypeRest(Parsers.scala:2349)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.$anonfun$infixType$1(Parsers.scala:1324)
	scala.tools.nsc.ast.parser.Parsers$Parser.placeholderTypeBoundary(Parsers.scala:523)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.infixType(Parsers.scala:1324)
	scala.tools.nsc.ast.parser.Parsers$Parser$PatternContextSensitive.infixType$(Parsers.scala:1323)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anon$2.infixType(Parsers.scala:2349)
	scala.tools.nsc.ast.parser.Parsers$Parser.startInfixType(Parsers.scala:2366)
	scala.tools.nsc.ast.parser.Parsers$Parser.typeOrInfixType(Parsers.scala:1598)
	scala.tools.nsc.ast.parser.Parsers$Parser.parseOther$1(Parsers.scala:1790)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr0(Parsers.scala:1820)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr(Parsers.scala:1665)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$templateStatSeq$1(Parsers.scala:3488)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.tools.nsc.ast.parser.Parsers$Parser.checkNoEscapingPlaceholders(Parsers.scala:500)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateStatSeq(Parsers.scala:3486)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateBody(Parsers.scala:3412)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateBodyOpt(Parsers.scala:3419)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateOpt(Parsers.scala:3386)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$classDef$1(Parsers.scala:3243)
	scala.tools.nsc.ast.parser.Parsers$Parser.savingClassContextBounds(Parsers.scala:345)
	scala.tools.nsc.ast.parser.Parsers$Parser.classDef(Parsers.scala:3229)
	scala.tools.nsc.ast.parser.Parsers$Parser.tmplDef(Parsers.scala:3197)
	scala.tools.nsc.ast.parser.Parsers$Parser.topLevelTmplDef(Parsers.scala:3182)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$topStat$1.$anonfun$applyOrElse$1(Parsers.scala:3476)
	scala.tools.nsc.ast.parser.Parsers$Parser.joinComment(Parsers.scala:797)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$topStat$1.applyOrElse(Parsers.scala:3476)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$topStat$1.applyOrElse(Parsers.scala:3469)
	scala.tools.nsc.ast.parser.Parsers$Parser.statSeq(Parsers.scala:3453)
	scala.tools.nsc.ast.parser.Parsers$Parser.topStatSeq(Parsers.scala:3468)
	scala.tools.nsc.ast.parser.Parsers$Parser.topstats$1(Parsers.scala:3664)
	scala.tools.nsc.ast.parser.Parsers$Parser.topstats$1(Parsers.scala:3656)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$compilationUnit$1(Parsers.scala:3670)
	scala.tools.nsc.ast.parser.Parsers$Parser.checkNoEscapingPlaceholders(Parsers.scala:500)
	scala.tools.nsc.ast.parser.Parsers$Parser.compilationUnit(Parsers.scala:3634)
	scala.tools.nsc.ast.parser.Parsers$SourceFileParser.$anonfun$parseStartRule$1(Parsers.scala:169)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$parse$1(Parsers.scala:370)
	scala.tools.nsc.ast.parser.Parsers$Parser.parseRule(Parsers.scala:363)
	scala.tools.nsc.ast.parser.Parsers$Parser.parse(Parsers.scala:370)
	scala.tools.nsc.ast.parser.Parsers$UnitParser.$anonfun$smartParse$1(Parsers.scala:266)
	scala.tools.nsc.ast.parser.Parsers$UnitParser.smartParse(Parsers.scala:241)
	scala.tools.nsc.ast.parser.SyntaxAnalyzer.scala$tools$nsc$ast$parser$SyntaxAnalyzer$$initialUnitBody(SyntaxAnalyzer.scala:94)
	scala.tools.nsc.ast.parser.SyntaxAnalyzer$ParserPhase.apply(SyntaxAnalyzer.scala:106)
	scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:483)
	scala.tools.nsc.Global$Run.$anonfun$compileLate$2(Global.scala:1703)
	scala.tools.nsc.Global$Run.$anonfun$compileLate$2$adapted(Global.scala:1702)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1306)
	scala.tools.nsc.Global$Run.compileLate(Global.scala:1702)
	scala.tools.nsc.interactive.Global.parseAndEnter(Global.scala:669)
	scala.tools.nsc.interactive.Global.typeCheck(Global.scala:679)
	scala.meta.internal.pc.Compat.$anonfun$runOutline$1(Compat.scala:74)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterable.foreach(Iterable.scala:935)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:66)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:35)
	scala.meta.internal.pc.Compat.runOutline$(Compat.scala:33)
	scala.meta.internal.pc.MetalsGlobal.runOutline(MetalsGlobal.scala:39)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:18)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:13)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$documentHighlight$1(ScalaPresentationCompiler.scala:527)
	scala.meta.internal.pc.CompilerAccess.retryWithCleanCompiler(CompilerAccess.scala:182)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withSharedCompiler$1(CompilerAccess.scala:155)
	scala.Option.map(Option.scala:242)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:154)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withInterruptableCompiler$1(CompilerAccess.scala:92)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	java.base/java.lang.Thread.run(Thread.java:840)
```
#### Short summary: 

java.lang.UnsupportedOperationException: Position.start on NoPosition