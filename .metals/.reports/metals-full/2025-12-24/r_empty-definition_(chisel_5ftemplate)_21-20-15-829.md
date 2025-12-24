error id: file://<WORKSPACE>/src/main/scala/MiniRV.scala:jump_en
file://<WORKSPACE>/src/main/scala/MiniRV.scala
empty definition using pc, found symbol in pc: jump_en
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/exu/io/jump_en.
	 -chisel3/exu/io/jump_en#
	 -chisel3/exu/io/jump_en().
	 -chisel3/util/exu/io/jump_en.
	 -chisel3/util/exu/io/jump_en#
	 -chisel3/util/exu/io/jump_en().
	 -minirv/ifu/exu/io/jump_en.
	 -minirv/ifu/exu/io/jump_en#
	 -minirv/ifu/exu/io/jump_en().
	 -minirv/idu/exu/io/jump_en.
	 -minirv/idu/exu/io/jump_en#
	 -minirv/idu/exu/io/jump_en().
	 -minirv/exu/exu/io/jump_en.
	 -minirv/exu/exu/io/jump_en#
	 -minirv/exu/exu/io/jump_en().
	 -minirv/lsu/exu/io/jump_en.
	 -minirv/lsu/exu/io/jump_en#
	 -minirv/lsu/exu/io/jump_en().
	 -minirv/wbu/exu/io/jump_en.
	 -minirv/wbu/exu/io/jump_en#
	 -minirv/wbu/exu/io/jump_en().
	 -exu/io/jump_en.
	 -exu/io/jump_en#
	 -exu/io/jump_en().
	 -scala/Predef.exu.io.jump_en.
	 -scala/Predef.exu.io.jump_en#
	 -scala/Predef.exu.io.jump_en().
offset: 2248
uri: file://<WORKSPACE>/src/main/scala/MiniRV.scala
text:
```scala
// MiniRV 顶层模块.
// chisel使用伴生对象的方式生成verilog代码.
// 所以, 判断哪个module是顶层模块, 就看哪个class有伴生object, 其内部生成verilog代码.
// scala的命名空间没有子继承关系.  minirv和minirv_ifu完全是平级的, 只是易读其关系.

package minirv


import chisel3._
import chisel3.util._
import minirv.ifu._
import minirv.idu._
import minirv.exu._
import minirv.lsu._
import minirv.wbu._
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

  // ========== 实例化各功能模块 ==========
  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val lsu = Module(new LSU)
  val wbu = Module(new WBU)
  val regfile = Module(new RegFile)
  val pmem = Module(new PMEM)  // 统一的物理存储器模块


  // ========== 流水线寄存器定义 ==========
  // IF/ID 寄存器
  val if_id_pc   = RegInit(0.U(Config.ADDR_WIDTH.W))
  val if_id_inst = RegInit(0.U(Config.INST_WIDTH.W))
  
  // ID/EX 寄存器
  // asTypeOf: 参数是一个[chisel实例], 它将`0.U`强制转换为指定[实例]类型的Chisel数据类型.
  val id_ex_reg = RegInit(0.U.asTypeOf(new ID2EX))
  val id_ex_rs1_addr = RegInit(0.U(Config.REG_ADDR_W.W))  // 用于前递检测
  val id_ex_rs2_addr = RegInit(0.U(Config.REG_ADDR_W.W))
  
  // EX/MEM 寄存器
  val ex_mem_reg = RegInit(0.U.asTypeOf(new EX2LS))
  
  // MEM/WB 寄存器
  val mem_wb_reg = RegInit(0.U.asTypeOf(new LS2WB))




  // ========== 冒险检测单元 ==========
  
  // 从 ID 阶段获取源寄存器地址
  val id_rs1_addr = idu.io.rs1_addr
  val id_rs2_addr = idu.io.rs2_addr
  
  // Load-Use 冒险检测：ID/EX 阶段是 Load 指令，且其 rd 是当前 ID 阶段的 rs1 或 rs2
  // mem_ren信号就是is_load信号透传. 在当前周期, id_ex_reg中的信号是上一周期ID阶段的信号, 也就是上一条指令的信息.
  // 
  val load_use_hazard = id_ex_reg.mem_ren && 
                        (id_ex_reg.rd_addr =/= 0.U) &&
                        ((id_ex_reg.rd_addr === id_rs1_addr) || 
                         (id_ex_reg.rd_addr === id_rs2_addr))
  
  // Stall 信号：Load-Use 冒险时暂停 IF 和 ID 阶段
  val stall = load_use_hazard
  
  // Flush 信号：分支/跳转发生时清空 IF/ID 和 ID/EX 阶段
  val flush = exu.io.ju@@mp_en

  // ========== 数据前递单元 ==========
  
  // 前递源：EX/MEM 阶段的结果（ALU 结果）
  val ex_mem_rd_addr = ex_mem_reg.rd_addr
  val ex_mem_rd_data = ex_mem_reg.alu_result
  val ex_mem_reg_wen = ex_mem_reg.reg_wen
  
  // 前递源：MEM/WB 阶段的结果（写回数据）
  val mem_wb_rd_addr = mem_wb_reg.rd_addr
  val mem_wb_rd_data = mem_wb_reg.wb_data
  val mem_wb_reg_wen = mem_wb_reg.reg_wen
  
  // 计算前递后的 rs1_data
  // 优先级：EX/MEM > MEM/WB > 寄存器堆原值
  val id_rs1_data_raw = regfile.io.rs1_data
  val id_rs1_data_fwd = MuxCase(id_rs1_data_raw, Seq(
    (ex_mem_reg_wen && (ex_mem_rd_addr =/= 0.U) && (ex_mem_rd_addr === id_rs1_addr)) -> ex_mem_rd_data,
    (mem_wb_reg_wen && (mem_wb_rd_addr =/= 0.U) && (mem_wb_rd_addr === id_rs1_addr)) -> mem_wb_rd_data
  ))
  
  // 计算前递后的 rs2_data
  val id_rs2_data_raw = regfile.io.rs2_data
  val id_rs2_data_fwd = MuxCase(id_rs2_data_raw, Seq(
    (ex_mem_reg_wen && (ex_mem_rd_addr =/= 0.U) && (ex_mem_rd_addr === id_rs2_addr)) -> ex_mem_rd_data,
    (mem_wb_reg_wen && (mem_wb_rd_addr =/= 0.U) && (mem_wb_rd_addr === id_rs2_addr)) -> mem_wb_rd_data
  ))

  // ========== IF 阶段 ==========
  ifu.io.jump_en   := exu.io.jump_en
  ifu.io.jump_addr := exu.io.jump_addr
  ifu.io.stall     := stall
  
  // 连接 IFU 到 PMEM (指令存储器)
  pmem.io.imem_addr := ifu.io.imem_addr
  ifu.io.imem_rdata := pmem.io.imem_rdata

  // ========== IF/ID 寄存器更新 ==========
  when(flush) {
    // 分支/跳转发生，插入气泡 (NOP)
    if_id_pc   := 0.U
    if_id_inst := 0x00000013.U  // NOP: addi x0, x0, 0
  }.elsewhen(!stall) {
    if_id_pc   := ifu.io.out.pc
    if_id_inst := ifu.io.out.inst
  }
  // stall 时保持不变

  // ========== ID 阶段 ==========
  idu.io.in.pc   := if_id_pc
  idu.io.in.inst := if_id_inst
  
  // 寄存器堆读取
  regfile.io.rs1_addr := idu.io.rs1_addr
  regfile.io.rs2_addr := idu.io.rs2_addr
  idu.io.rs1_data := id_rs1_data_fwd  // 使用前递后的值
  idu.io.rs2_data := id_rs2_data_fwd

  // ========== ID/EX 寄存器更新 ==========
  when(flush || stall) {
    // 分支/跳转或 Load-Use 冒险，插入气泡
    id_ex_reg := 0.U.asTypeOf(new ID2EX)
    id_ex_rs1_addr := 0.U
    id_ex_rs2_addr := 0.U
  }.otherwise {
    id_ex_reg := idu.io.out
    id_ex_rs1_addr := id_rs1_addr
    id_ex_rs2_addr := id_rs2_addr
  }

  // ========== EX 阶段 ==========
  exu.io.in := id_ex_reg

  // ========== EX/MEM 寄存器更新 ==========
  ex_mem_reg := exu.io.out

  // ========== MEM 阶段 ==========
  lsu.io.in := ex_mem_reg
  
  // 连接 LSU 到 PMEM (数据存储器)
  pmem.io.dmem_raddr := lsu.io.dmem_raddr
  lsu.io.dmem_rdata  := pmem.io.dmem_rdata
  pmem.io.dmem_wen   := lsu.io.dmem_wen
  pmem.io.dmem_waddr := lsu.io.dmem_waddr
  pmem.io.dmem_wdata := lsu.io.dmem_wdata
  pmem.io.dmem_wmask := lsu.io.dmem_wmask

  // ========== MEM/WB 寄存器更新 ==========
  mem_wb_reg := lsu.io.out

  // ========== WB 阶段 ==========
  wbu.io.in := mem_wb_reg
  regfile.io.rd_addr := wbu.io.rd_addr
  regfile.io.rd_data := wbu.io.rd_data
  regfile.io.rd_wen  := wbu.io.rd_wen

  // ========== EBREAK 检测 (通过 PMEM 模块) ==========
  pmem.io.ebreak_inst  := if_id_inst
  pmem.io.ebreak_valid := true.B

  // ========== 调试输出 ==========
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


#### Short summary: 

empty definition using pc, found symbol in pc: jump_en