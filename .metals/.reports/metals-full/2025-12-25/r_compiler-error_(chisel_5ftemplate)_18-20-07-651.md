error id: 0480E0613B8DEE9CC3B8C4C0B876774A
file://<WORKSPACE>/src/main/scala/MiniRV.scala
### scala.reflect.internal.FatalError: no context found for source-file://<WORKSPACE>/src/main/scala/MiniRV.scala,line-13,offset=457

occurred in the presentation compiler.



action parameters:
offset: 457
uri: file://<WORKSPACE>/src/main/scala/MiniRV.scala
text:
```scala
// MiniRV 顶层模块.
// chisel使用伴生对象的方式生成verilog代码.
// 所以, 判断哪个module是顶层模块, 就看哪个class有伴生object, 其内部生成verilog代码.
// scala的命名空间没有子继承关系.  minirv和minirv_ifu完全是平级的, 只是易读其关系.



//冒险处理:
// stall: 在IDU(if_id_reg中)的use指令I检测到发生load-use冒险时启用. 
  //  pc[输出接输入打一拍], if_id_reg[输出接输入打一拍], id_ex_reg[输入端切换为气泡]. 后续寄存器不影响.

// flush: 在EXU(id_ex_reg中)的指令被发现是jump_en时启用. 这包括情况: 该指令是jal, jalr, 或满足branch_taken的B指令. 
  //  pc[接收jump_en和jump_addr, 强制跳转], if_id_reg[输入端切换为气泡(本周期pc取址将废弃@@)], id_ex_reg[插入气泡.]. 后续寄存器不影响.


// 提前分支: 实际上jal指令在ID的时候就可以提前发出flush信号. 等我做完再优化这点.


package minirv


import chisel3._
import chisel3.util._
import minirv.ifu._
import minirv.idu._
import minirv.exu._
import minirv.lsu._
import minirv.wbu._
import minirv.hdu._
import minirv.fwu._
import _root_.circt.stage.ChiselStage


/**
  * MiniRV 顶层模块
  * 
  * 5 级流水线实现：IF -> ID -> EX -> MEM(LSU) -> WB
  * 包含：
  *   - 流水线寄存器 (IF/ID, ID/EX, EX/MEM, MEM/WB)
  *   - 数据前递 (Forwarding)
  *   - Load-Use 冒险检测 + Stall
  *   - 控制冒险处理 (Flush)
  * 
  * 使用 DPI-C 机制访问存储器
  */
class MiniRV extends Module {
  val io = IO(new Bundle {
    // 调试接口
    val debug_pc = Output(UInt(Config.ADDR_WIDTH.W))
    val debug_inst = Output(UInt(Config.INST_WIDTH.W))
  })

  // =================================================================================
  // 1. 实例化各功能模块
  // =================================================================================
  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val lsu = Module(new LSU)
  val wbu = Module(new WBU)
  val regfile = Module(new RegFile)
  val pmem = Module(new PMEM)  // 统一的物理存储器模块
  val hdu = Module(new HDU)    // 冒险检测单元
  val fwu = Module(new FWU)    // 数据前递单元


  // =================================================================================
  // 2. 流水线寄存器定义
  // =================================================================================
  // IF/ID 寄存器
  val if_id_pc   = RegInit(0.U(Config.ADDR_WIDTH.W))
  val if_id_inst = RegInit(0.U(Config.INST_WIDTH.W))
  
  // ID/EX 寄存器
  // asTypeOf: 参数是一个[chisel实例], 它将`0.U`强制转换为指定[实例]类型的Chisel数据类型.
  val id_ex_reg = RegInit(0.U.asTypeOf(new ID2EX))
  
  // EX/MEM 寄存器
  val ex_mem_reg = RegInit(0.U.asTypeOf(new EX2LS))
  
  // MEM/WB 寄存器
  val mem_wb_reg = RegInit(0.U.asTypeOf(new LS2WB))




  // =================================================================================
  // 3. 冒险检测单元 (Hazard Detection Unit)
  // =================================================================================
  
  // 从 ID 阶段获取源寄存器地址
  val id_rs1_addr = idu.io.rs1_addr
  val id_rs2_addr = idu.io.rs2_addr
  
  // 连接 HDU 输入
  hdu.io.id_rs1_addr   := id_rs1_addr
  hdu.io.id_rs2_addr   := id_rs2_addr
  hdu.io.id_ex_mem_ren := id_ex_reg.mem_ren
  hdu.io.id_ex_rd_addr := id_ex_reg.rd_addr
  hdu.io.ex_jump_en    := exu.io.jump_en
  
  // HDU 输出的控制信号
  val stall = hdu.io.stall
  val flush = hdu.io.flush



  // =================================================================================
  // 4. 连接FWU 数据前递单元
  // =================================================================================
  
  // 连接 FWU 输入：来自 ID 阶段
  fwu.io.id_rs1_addr    := id_rs1_addr
  fwu.io.id_rs2_addr    := id_rs2_addr
  fwu.io.id_rs1_data_raw := regfile.io.rs1_data
  fwu.io.id_rs2_data_raw := regfile.io.rs2_data
  
  // 连接 FWU 输入：来自 EX/MEM 阶段 (前递源 A)
  fwu.io.ex_mem_rd_addr := ex_mem_reg.rd_addr
  fwu.io.ex_mem_rd_data := ex_mem_reg.alu_result
  fwu.io.ex_mem_reg_wen := ex_mem_reg.reg_wen
  
  // 连接 FWU 输入：来自 MEM/WB 阶段 (前递源 B)
  fwu.io.mem_wb_rd_addr := mem_wb_reg.rd_addr
  fwu.io.mem_wb_rd_data := mem_wb_reg.wb_data
  fwu.io.mem_wb_reg_wen := mem_wb_reg.reg_wen
  
  // FWU 输出的前递后数据
  val id_rs1_data_fwd = fwu.io.rs1_data_fwd
  val id_rs2_data_fwd = fwu.io.rs2_data_fwd



  // =================================================================================
  // 5. 流水线各阶段连接
  // =================================================================================

  // ---------------------------------------------------------------------------------
  // 5.1 IF 阶段 (Instruction Fetch)
  // ---------------------------------------------------------------------------------
  ifu.io.jump_en   := exu.io.jump_en
  ifu.io.jump_addr := exu.io.jump_addr
  ifu.io.stall     := stall
  

  // 连接 IFU 到 PMEM (指令存储器)
  pmem.io.imem_addr := ifu.io.imem_addr
  ifu.io.imem_rdata := pmem.io.imem_rdata


  // ---------------------------------------------------------------------------------
  // 5.2 IF/ID 寄存器更新
  // ---------------------------------------------------------------------------------
  when(flush) {
    // 分支/跳转发生，插入气泡 (NOP)
    if_id_pc   := 0.U //NOP携带的pc无意义, 防御性设为0,波形图容易看出来这是个气泡.
    if_id_inst := 0x00000013.U  // NOP: addi x0, x0, 0
  }.elsewhen(!stall) {
    if_id_pc   := ifu.io.out.pc
    if_id_inst := ifu.io.out.inst
  }
  // stall 时保持不变

  // ---------------------------------------------------------------------------------
  // 5.3 ID 阶段 (Instruction Decode)
  // ---------------------------------------------------------------------------------
  idu.io.in.pc   := if_id_pc
  idu.io.in.inst := if_id_inst
  
  // 寄存器堆读取
  regfile.io.rs1_addr := idu.io.rs1_addr
  regfile.io.rs2_addr := idu.io.rs2_addr
  idu.io.rs1_data := id_rs1_data_fwd  // 使用前递后的值
  idu.io.rs2_data := id_rs2_data_fwd

  // ---------------------------------------------------------------------------------
  // 5.4 ID/EX 寄存器更新
  // ---------------------------------------------------------------------------------
  when(flush || stall) {
    // 分支/跳转或 Load-Use 冒险，插入气泡
    id_ex_reg := 0.U.asTypeOf(new ID2EX)
  }.otherwise {
    id_ex_reg := idu.io.out
  }

  // ---------------------------------------------------------------------------------
  // 5.5 EX 阶段 (Execute)
  // ---------------------------------------------------------------------------------
  exu.io.in := id_ex_reg

  // ---------------------------------------------------------------------------------
  // 5.6 EX/MEM 寄存器更新
  // ---------------------------------------------------------------------------------
  ex_mem_reg := exu.io.out

  // ---------------------------------------------------------------------------------
  // 5.7 MEM 阶段 (Memory Access)
  // ---------------------------------------------------------------------------------
  lsu.io.in := ex_mem_reg
  
  // 连接 LSU 到 PMEM (数据存储器)
  pmem.io.dmem_raddr := lsu.io.dmem_raddr
  lsu.io.dmem_rdata  := pmem.io.dmem_rdata
  pmem.io.dmem_wen   := lsu.io.dmem_wen
  pmem.io.dmem_waddr := lsu.io.dmem_waddr
  pmem.io.dmem_wdata := lsu.io.dmem_wdata
  pmem.io.dmem_wmask := lsu.io.dmem_wmask

  // ---------------------------------------------------------------------------------
  // 5.8 MEM/WB 寄存器更新
  // ---------------------------------------------------------------------------------
  mem_wb_reg := lsu.io.out

  // ---------------------------------------------------------------------------------
  // 5.9 WB 阶段 (Write Back)
  // ---------------------------------------------------------------------------------
  wbu.io.in := mem_wb_reg
  regfile.io.rd_addr := wbu.io.rd_addr
  regfile.io.rd_data := wbu.io.rd_data
  regfile.io.rd_wen  := wbu.io.rd_wen

  // =================================================================================
  // 6. 其他 (EBREAK 检测与调试)
  // =================================================================================
  // EBREAK 检测 (通过 PMEM 模块)
  pmem.io.ebreak_inst  := if_id_inst
  pmem.io.ebreak_valid := true.B

  // 调试输出
  io.debug_pc   := if_id_pc
  io.debug_inst := if_id_inst
}




/**
  * 生成 Verilog
  * 运行命令：./mill minirv.runMain minirv.MiniRV
  */
object MiniRV extends App {
  // 指定输出目录
  val outputDir = "generated"
  
  println(s"正在生成 MiniRV.sv 文件到 $outputDir/ ...")
  ChiselStage.emitSystemVerilogFile(
    new MiniRV,
    args = Array("--target-dir", outputDir),  // 指定输出目录
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
  println(s"生成完成！文件位置: $outputDir/MiniRV.sv")
}

```


presentation compiler configuration:
Scala version: 2.13.16
Classpath:
<WORKSPACE>/.bloop/out/chisel_template/bloop-bsp-clients-classes/classes-Metals-TYdBTwHoRzyi_icOvsjAqw== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-RC1/chisel_2.13-7.0.0-RC1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.16/scala-library-2.13.16.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.13.1/commons-text-1.13.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.10.0/os-lib_2.13-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.7/json4s-native_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.7/data-class_2.13-0.2.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.16/scala-reflect-2.13.16.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.3.1/upickle_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.1/firtool-resolver_2.13-2.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.1.0/geny_2.13-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.7/json4s-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.7/json4s-native-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.3.1/ujson_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.3.1/upack_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.3.1/upickle-implicits_2.13-3.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.7/json4s-ast_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.7/json4s-scalap_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.3.1/upickle-core_2.13-3.3.1.jar [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-language:reflectiveCalls -deprecation -feature -Xcheckinit -Ymacro-annotations -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.tools.nsc.interactive.CompilerControl.$anonfun$doLocateContext$1(CompilerControl.scala:100)
	scala.tools.nsc.interactive.CompilerControl.doLocateContext(CompilerControl.scala:100)
	scala.tools.nsc.interactive.CompilerControl.doLocateContext$(CompilerControl.scala:99)
	scala.tools.nsc.interactive.Global.doLocateContext(Global.scala:115)
	scala.meta.internal.pc.PcDefinitionProvider.definitionTypedTreeAt(PcDefinitionProvider.scala:181)
	scala.meta.internal.pc.PcDefinitionProvider.definition(PcDefinitionProvider.scala:69)
	scala.meta.internal.pc.PcDefinitionProvider.definition(PcDefinitionProvider.scala:17)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$definition$1(ScalaPresentationCompiler.scala:490)
	scala.meta.internal.pc.CompilerAccess.retryWithCleanCompiler(CompilerAccess.scala:182)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withSharedCompiler$1(CompilerAccess.scala:155)
	scala.Option.map(Option.scala:242)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:154)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withNonInterruptableCompiler$1(CompilerAccess.scala:132)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	java.base/java.lang.Thread.run(Thread.java:840)
```
#### Short summary: 

scala.reflect.internal.FatalError: no context found for source-file://<WORKSPACE>/src/main/scala/MiniRV.scala,line-13,offset=457