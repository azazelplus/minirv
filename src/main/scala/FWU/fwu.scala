// MiniRV 数据前递单元 (Forwarding Unit)
package minirv.fwu

import chisel3._
import chisel3.util._
import minirv._

/**
  * FWU - 数据前递单元
  * 
  * 功能：
  * 1. 检测数据依赖
  * 2. 从 EX/MEM 或 MEM/WB 阶段前递数据到 ID 阶段
  * 3. 输出前递后的 rs1_data 和 rs2_data
  */
class FWU extends Module {
  val io = IO(new Bundle {
    // ========== 来自 ID 阶段的信号 ==========
    val id_rs1_addr = Input(UInt(Config.REG_ADDR_W.W))  // 当前 ID 阶段指令的 rs1 地址
    val id_rs2_addr = Input(UInt(Config.REG_ADDR_W.W))  // 当前 ID 阶段指令的 rs2 地址
    val id_rs1_data_raw = Input(UInt(Config.XLEN.W))    // 寄存器堆读出的 rs1 原始值
    val id_rs2_data_raw = Input(UInt(Config.XLEN.W))    // 寄存器堆读出的 rs2 原始值
    
    // ========== 来自 EX/MEM 寄存器的信号 (前递源 A) ==========
    val ex_mem_rd_addr = Input(UInt(Config.REG_ADDR_W.W)) // EX/MEM 阶段指令的目标寄存器地址
    val ex_mem_rd_data = Input(UInt(Config.XLEN.W))       // EX/MEM 阶段的 ALU 结果
    val ex_mem_reg_wen = Input(Bool())                    // EX/MEM 阶段指令是否写寄存器
    
    // ========== 来自 MEM/WB 寄存器的信号 (前递源 B) ==========
    val mem_wb_rd_addr = Input(UInt(Config.REG_ADDR_W.W)) // MEM/WB 阶段指令的目标寄存器地址
    val mem_wb_rd_data = Input(UInt(Config.XLEN.W))       // MEM/WB 阶段的写回数据
    val mem_wb_reg_wen = Input(Bool())                    // MEM/WB 阶段指令是否写寄存器
    
    // ========== 输出前递后的数据 ==========
    val rs1_data_fwd = Output(UInt(Config.XLEN.W))        // 前递后的 rs1 数据
    val rs2_data_fwd = Output(UInt(Config.XLEN.W))        // 前递后的 rs2 数据
  })

  // ============================================================
  // 计算前递后的 rs1_data
  // ============================================================
  // 优先级：A (EX/MEM) > B (MEM/WB) > C (寄存器堆原值)
  // A: 当 [当前MEM阶段指令]要写回寄存器堆, 且rd不为0, 且rd等于[当前ID阶段指令]的rs1地址时
  io.rs1_data_fwd := MuxCase(io.id_rs1_data_raw, Seq(
    (io.ex_mem_reg_wen && (io.ex_mem_rd_addr =/= 0.U) && (io.ex_mem_rd_addr === io.id_rs1_addr)) -> io.ex_mem_rd_data,
    (io.mem_wb_reg_wen && (io.mem_wb_rd_addr =/= 0.U) && (io.mem_wb_rd_addr === io.id_rs1_addr)) -> io.mem_wb_rd_data
  ))

  // ============================================================
  // 计算前递后的 rs2_data
  // ============================================================
  io.rs2_data_fwd := MuxCase(io.id_rs2_data_raw, Seq(
    (io.ex_mem_reg_wen && (io.ex_mem_rd_addr =/= 0.U) && (io.ex_mem_rd_addr === io.id_rs2_addr)) -> io.ex_mem_rd_data,
    (io.mem_wb_reg_wen && (io.mem_wb_rd_addr =/= 0.U) && (io.mem_wb_rd_addr === io.id_rs2_addr)) -> io.mem_wb_rd_data
  ))
}
