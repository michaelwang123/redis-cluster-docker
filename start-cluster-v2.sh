#!/bin/bash

# Redis集群两阶段启动脚本
# 第一阶段：使用初始化配置创建集群
# 第二阶段：配置cluster-announce-ip供Java客户端使用

set -e

echo "🚀 开始启动Redis集群（两阶段初始化）..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 停止并清理现有容器（如果存在）
echo "🧹 清理现有容器..."
docker-compose down -v 2>/dev/null || true

echo "📦 第一阶段：使用初始化配置启动容器..."

# 临时修改docker-compose.yml使用初始化配置
cp docker-compose.yml docker-compose.yml.backup

# 修改配置文件路径为初始化配置
sed -i.tmp 's|config/redis-node-\([123]\)\.conf|config/init/redis-node-\1.conf|g' docker-compose.yml

# 启动容器
docker-compose up -d

# 等待容器启动
echo "⏳ 等待容器启动完成..."
sleep 10

# 检查容器状态
echo "🔍 检查容器状态..."
for i in {1..3}; do
    if ! docker-compose ps | grep -q "redis-node-$i.*Up"; then
        echo "❌ 容器 redis-node-$i 启动失败"
        docker-compose logs redis-node-$i
        exit 1
    fi
done

echo "✅ 所有容器启动成功"

# 初始化集群
echo "🔧 初始化Redis集群..."
sleep 5

# 使用redis-cli创建集群
docker exec redis-node-1 redis-cli --cluster create \
    redis-node-1:6379 \
    redis-node-2:6379 \
    redis-node-3:6379 \
    --cluster-replicas 0 \
    --cluster-yes

echo "⏳ 等待集群初始化完成..."
sleep 5

echo "📦 第二阶段：配置cluster-announce-ip..."

# 恢复原始docker-compose.yml
mv docker-compose.yml.backup docker-compose.yml
rm -f docker-compose.yml.tmp

# 为每个节点配置cluster-announce-ip
docker exec redis-node-1 redis-cli config set cluster-announce-ip 127.0.0.1
docker exec redis-node-1 redis-cli config set cluster-announce-port 6371
docker exec redis-node-1 redis-cli config set cluster-announce-bus-port 16371

docker exec redis-node-2 redis-cli config set cluster-announce-ip 127.0.0.1
docker exec redis-node-2 redis-cli config set cluster-announce-port 6372
docker exec redis-node-2 redis-cli config set cluster-announce-bus-port 16372

docker exec redis-node-3 redis-cli config set cluster-announce-ip 127.0.0.1
docker exec redis-node-3 redis-cli config set cluster-announce-port 6373
docker exec redis-node-3 redis-cli config set cluster-announce-bus-port 16373

echo "🔄 重新加载集群配置..."
docker exec redis-node-1 redis-cli cluster saveconfig
docker exec redis-node-2 redis-cli cluster saveconfig
docker exec redis-node-3 redis-cli cluster saveconfig

# 验证集群状态
echo "🔍 验证集群状态..."

echo -e "\n=== 集群节点信息 ==="
docker exec redis-node-1 redis-cli cluster nodes

echo -e "\n=== 集群信息 ==="
docker exec redis-node-1 redis-cli cluster info

echo -e "\n=== 测试集群连接 ==="
# 测试每个节点的连接
for port in 6371 6372 6373; do
    echo "测试端口 $port:"
    if timeout 3 bash -c "</dev/tcp/127.0.0.1/$port"; then
        echo "  ✅ 端口 $port 可达"
    else
        echo "  ❌ 端口 $port 不可达"
    fi
done

echo -e "\n🎉 Redis集群启动完成！"
echo -e "\n📋 集群信息:"
echo "   - 节点1: 127.0.0.1:6371"
echo "   - 节点2: 127.0.0.1:6372" 
echo "   - 节点3: 127.0.0.1:6373"
echo ""
echo "💡 使用以下命令测试Java应用:"
echo "   mvn compile exec:java -Dexec.mainClass=\"com.example.RedisClusterTest\""
echo ""
echo "🛠️  常用命令:"
echo "   查看集群状态: docker exec redis-node-1 redis-cli cluster info"
echo "   查看节点信息: docker exec redis-node-1 redis-cli cluster nodes"
echo "   连接到节点1: docker exec -it redis-node-1 redis-cli"
echo "   停止集群: docker-compose down"