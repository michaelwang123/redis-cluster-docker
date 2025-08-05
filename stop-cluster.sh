#!/bin/bash

# Redis集群停止脚本

set -e

echo "🛑 停止Redis集群..."

# 停止并清理容器
docker-compose down -v

echo "🧹 清理数据（可选）..."
read -p "是否清理持久化数据？(y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  清理数据目录..."
    rm -rf data/node*/nodes-*.conf
    rm -rf data/node*/dump-*.rdb
    rm -rf data/node*/appendonly-*.aof
    echo "✅ 数据已清理"
else
    echo "📁 保留数据目录"
fi

echo "✅ Redis集群已停止"