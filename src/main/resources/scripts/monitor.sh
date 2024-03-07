#!/bin/bash

# 输出CSV文件的路径（从命令行参数中获取）
output_file="$1"

# 初始迭代次数
iteration=0

# 检查输出文件是否存在，如果存在则删除
if [ -f "$output_file" ]; then
    rm "$output_file"
fi

# 创建并写入标题行
echo "Iteration,User CPU Usage (%),System CPU Usage (%),I/O waite CPU Usage (%),Memory Usage (%)" > "$output_file"

while true; do
    # 增加迭代计数器
    ((iteration++))

    # 获取CPU占用率（使用1秒采样间隔）
    cpu_info=$(top -b -n 1 | grep "%Cpu(s)" | awk '{print $2, $4, $10}')
    user_cpu=$(echo "$cpu_info" | awk '{print $1}')
    system_cpu=$(echo "$cpu_info" | awk '{print $2}')
    io_wait_cpu=$(echo "$cpu_info" | awk '{print $3}')
    if [[ "$io_wait_cpu" == "wa," ]]; then
        io_wait_cpu=0.0
    fi

    # 获取内存占用率
    memory_usage=$(free -m | awk '/Mem/{printf "%.2f", $3/$2*100}')

    # 将信息写入CSV文件
    echo "$iteration,$user_cpu,$system_cpu,$io_wait_cpu,$memory_usage" >> "$output_file"
    # 休眠1秒后再次采集信息
    sleep 1
done
