#!/bin/bash

# 检查参数数量是否正确
if [ $# -ne 3 ]; then
    echo "Usage: $0 <test_directory> <duration_limit_seconds> <output_directory>"
    exit 1
fi

# 从命令行参数获取测试目录、测试时长和输出目录
test_directory="$1"
duration_limit="$2"
output_dir="$3"

# 如果输出目录存在，则删除该目录及其内容
if [ -d "$output_dir" ]; then
    rm -rf "$output_dir"
fi

# 创建存储输出结果的目录
mkdir -p "$output_dir"

# 记录脚本开始时间
start_time=$(date +%s)

# 执行测试循环
while true; do
    # 执行测试命令并将输出保存到文件
    echo "Running fio test..."
    timestamp=$(date +%Y%m%d%H%M%S)
    output_file="$output_dir/fio_output_$timestamp.txt"
    fio_command="fio --directory=$test_directory --ioengine=libaio --direct=1 --iodepth=1 --thread=1 --numjobs=1 --group_reporting --allow_mounted_write=1 --rw=randrw --rwmixread=70 --bs=4k --size=1G --runtime=60 --name=fioTest"
    $fio_command > "$output_file" 2>&1

    # 记录当前时间
    current_time=$(date +%s)

    # 计算已经运行的时间
    elapsed_time=$((current_time - start_time))

    # 检查是否已经运行超过指定时长，如果是则退出循环
    if [ $elapsed_time -ge $duration_limit ]; then
        echo "Test duration reached. Exiting."
        break
    else
        # 休眠一段时间后继续下一轮测试
        echo "Sleeping for 10 seconds before the next test..."
        sleep 10  # 10秒，以秒为单位
    fi
done
