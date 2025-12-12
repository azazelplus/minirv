#!/bin/bash
# MiniRV 仿真脚本
# 用法: ./sim.sh [program.bin] [max_cycles]

set -e

# sim项目根目录, 即`./sim`  
# $0表示自指, 当前脚本的文件名. dirname去掉的 最后一段路径. (不管最后是文件还是目录都一样)
PROJ_ROOT=$(dirname "$0")/..
cd "$PROJ_ROOT"

# 输出目录
BUILD_DIR="build"
GEN_DIR="generated"
SIM_DIR="sim"

# 创建构建目录
mkdir -p "$BUILD_DIR"

# 1. 生成 Verilog（如果需要）
echo "=== Step 1: Generating Verilog ==="
./mill minirv.runMain minirv.MiniRV

# 2. 使用 Verilator 编译
echo "=== Step 2: Compiling with Verilator ==="
cd "$BUILD_DIR"

verilator --cc --exe --build --trace \
    -Wall \
    -Wno-UNUSEDSIGNAL \
    -Wno-UNUSEDPARAM \
    --top-module MiniRV \
    -I../$GEN_DIR \
    ../$GEN_DIR/MiniRV.sv \
    ../$GEN_DIR/IFU.sv \
    ../$GEN_DIR/PC.sv \
    ../$GEN_DIR/IDU.sv \
    ../$GEN_DIR/EXU.sv \
    ../$GEN_DIR/LSU.sv \
    ../$GEN_DIR/WBU.sv \
    ../$GEN_DIR/RegFile.sv \
    ../$GEN_DIR/PMEMRead.sv \
    ../$GEN_DIR/PMEMWrite.sv \
    ../$GEN_DIR/EBREAKDetect.sv \
    ../$SIM_DIR/main.cpp \
    -o VMiniRV

cd ..

# 3. 运行仿真. $1表示第一个命令行参数, 即要加载的程序二进制文件. -n: 非空则为真. ${2:-10000}表示如果没有提供$2则默认为10000.
# 用法是 ./sim.sh <program.bin> [max_cycles]
echo "=== Step 3: Running Simulation ==="
if [ -n "$1" ]; then
    ./"$BUILD_DIR"/obj_dir/VMiniRV "$1" "${2:-10000}"
else
    echo "Usage: ./sim.sh <program.bin> [max_cycles]"
    echo "No program specified, skipping simulation."
fi
