#! /bin/bash
  
# 检查参数数量
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <directory> <file_num>"
    exit 1
fi

# 目录
directory="$1"

# file_num
file_num="$2"

# 读写方式
methods=write

# 读写块大小
blocksize=4

# numjobs
numjobs=1

for ((i=0; i<${file_num}; i++)); do
    file_size=$((RANDOM % 2048 + 128))
    echo "Running test: results with block size ${blocksize[i]}" 
    fio -directory="$directory" -name="$methods" -ioengine=libaio \
    -rw="$methods" -bs=4k -size="${file_size}k" -numjobs="$numjobs" -direct=1 
    echo "------------------------------------------"  
done
