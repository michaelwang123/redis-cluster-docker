#!/bin/bash

# 测试带密码的Redis集群连接
# 这个脚本会编译并运行RedisClusterWithPasswordTest类

set -e

echo "🚀 开始测试带密码的Redis集群连接..."

# 检查Redis集群是否已启动
if ! nc -z 127.0.0.1 6371 2>/dev/null; then
    echo "❌ Redis集群未启动，请先运行 ./start-cluster-with-password.sh"
    exit 1
fi

# 编译并运行测试类
echo "📦 编译并运行测试类..."
mvn compile exec:java -Dexec.mainClass="com.example.RedisClusterWithPasswordTest"

echo "✅ 测试完成"
