// MiniRV 统一物理存储器模块
// 封装所有 DPI-C 存储器访问接口

package minirv

import chisel3._
import chisel3.util._

/**
  * PMEM - 统一物理存储器模块
  * 
  * 功能：
  * 1. 提供取指接口 (IMEM) - 给 IFU 使用
  * 2. 提供数据读写接口 (DMEM) - 给 LSU 使用
  * 3. EBREAK 指令检测 - 用于仿真终止
  * 
  * 内部使用 DPI-C 机制与 C++ 仿真环境交互
  */
class PMEM extends Module {
  val io = IO(new Bundle {
    // ========== 取指接口 (IMEM) IFU<->PMEM ==========
    val imem_addr  = Input(UInt(32.W))   // 指令地址. IFU->PMEM, IFU提供[当前PC]给PMEM, 请求返回指令.
    val imem_rdata = Output(UInt(32.W))  // 指令数据. PMEM->IFU, 即IFU从PMEM读到的指令. 
    
    // ========== 数据读接口 (DMEM Read) LSU<->PMEM ==========
    val dmem_raddr = Input(UInt(32.W))   // 数据读地址. LSU->PMEM, LSU提供[读地址]给PMEM, 请求返回数据.
    val dmem_rdata = Output(UInt(32.W))  // 数据读出.   PMEM->LSU, 即LSU从PMEM读到的数据.
    
    // ========== 数据写接口 (DMEM Write) LSU->PMEM ==========
    val dmem_wen   = Input(Bool())       // 写使能. LSU->PMEM, LSU产生写使能信号给PMEM.
    val dmem_waddr = Input(UInt(32.W))   // 写地址. LSU->PMEM, LSU产生写地址给PMEM. 
    val dmem_wdata = Input(UInt(32.W))   // 写数据. LSU->PMEM, LSU产生写数据给PMEM.
    val dmem_wmask = Input(UInt(4.W))    // 写掩码（按字节）. LSU->PMEM, LSU产生写掩码给PMEM.
    
    // ========== EBREAK 检测接口 MiniRV->PMEM ==========
    val ebreak_inst  = Input(UInt(32.W)) // 用于检测 EBREAK 的指令
    val ebreak_valid = Input(Bool())     // 指令有效
  })

  // ========== 内部实例化 DPI-C 模块 ==========
  
  // 指令存储器读取
  val imem_read = Module(new PMEMRead)
  imem_read.io.clock := clock
  imem_read.io.raddr := io.imem_addr
  io.imem_rdata := imem_read.io.rdata
  
  // 数据存储器读取
  val dmem_read = Module(new PMEMRead)
  dmem_read.io.clock := clock
  dmem_read.io.raddr := io.dmem_raddr
  io.dmem_rdata := dmem_read.io.rdata
  
  // 数据存储器写入
  val dmem_write = Module(new PMEMWrite)
  dmem_write.io.clock := clock
  dmem_write.io.wen   := io.dmem_wen
  dmem_write.io.waddr := io.dmem_waddr
  dmem_write.io.wdata := io.dmem_wdata
  dmem_write.io.wmask := io.dmem_wmask
  
  // EBREAK 检测
  val ebreak_detect = Module(new EBREAKDetect)
  ebreak_detect.io.clock := clock
  ebreak_detect.io.inst  := io.ebreak_inst
  ebreak_detect.io.valid := io.ebreak_valid
}
