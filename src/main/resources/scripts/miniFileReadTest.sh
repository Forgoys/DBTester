#!/bin/bash

# Path to the CIFAR-10 dataset directory
cifar_directory="$1"
# Output file for fio results
# output_file="$2"

# Check if the directory path is provided
if [ -z "$cifar_directory" ]; then
    echo "Usage: ./your_script.sh <cifar_directory>"
    exit 1
fi

# Select a random CIFAR-10 data file
random_file=$(ls -1 "$cifar_directory"/data_batch_*.bin | shuf -n 1)

# Fio configuration file
fio_config_file="fio_config.ini"

# Generate a random offset within the file
file_size=$(stat -c %s "$random_file")
random_offset=$((RANDOM % (file_size - 4096)))  # Choose a random offset, ensuring at least 4KB from the end
random_offset=$(((random_offset / 4096) * 4096))

# Create the fio configuration file
cat <<EOF > "$fio_config_file"
[global]
ioengine=libaio
direct=1
iodepth=1
thread=1
numjobs=1
bs=4k
randrepeat=0
time_based
runtime=30s
time_based

[random_read]
rw=randread
filename=$random_file
offset=$random_offset
EOF

# Run fio test
# fio "$fio_config_file" --output="$output_file"
fio "$fio_config_file"

# Parse and display results
echo "======== Fio Test Results ========\n"
