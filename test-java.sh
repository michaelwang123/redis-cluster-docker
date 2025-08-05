#!/bin/bash

# Java应用测试脚本

set -e

echo "☕ 开始测试Java Redis集群连接..."

# 检查集群是否运行
echo "🔍 检查Redis集群状态..."
if ! docker-compose ps | grep -q "Up"; then
    echo "❌ Redis集群未运行，请先运行 ./start-cluster.sh"
    exit 1
fi

# 验证端口连通性
echo "🔗 验证端口连通性..."
for port in 6371 6372 6373; do
    if ! nc -z 127.0.0.1 $port 2>/dev/null; then
        echo "❌ 端口 $port 不可达"
        exit 1
    fi
done
echo "✅ 所有端口可达"

# 编译Java项目
echo "🔨 编译Java项目..."
mvn compile -q

# 运行测试
echo "🚀 运行Redis集群测试..."
mvn exec:java -Dexec.mainClass="com.example.RedisClusterTest" -q

echo "✅ 测试完成！"