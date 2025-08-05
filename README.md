# Redis集群Docker解决方案

这个项目演示了如何在Mac本地环境使用Docker Compose启动Redis集群，并解决Docker网络隔离导致的连接问题。

## 🎯 解决的核心问题

**JedisCluster连接Docker Redis集群时的网络隔离问题**：
- Redis集群默认记录Docker容器内部IP（如172.18.0.2），宿主机无法访问
- 通过`cluster-announce-ip`和`cluster-announce-port`声明宿主机可访问的地址
- 确保Java客户端能正确获取并连接到Redis集群节点

## 📁 项目结构

```
redis-cluster-docker/
├── docker-compose.yml          # Docker编排配置  
├── config/                     # Redis配置文件
│   ├── redis-node-1.conf      # 节点1配置
│   ├── redis-node-2.conf      # 节点2配置  
│   └── redis-node-3.conf      # 节点3配置
├── data/                       # 数据持久化目录
│   ├── node1/
│   ├── node2/
│   └── node3/
├── src/main/java/              # Java测试代码
├── start-cluster.sh           # 启动集群脚本
├── stop-cluster.sh            # 停止集群脚本
├── test-java.sh              # Java测试脚本
└── README.md                 # 说明文档
```

## 🚀 快速开始

### 1. 启动Redis集群

```bash
./start-cluster.sh
```

这个脚本会：
- 启动3个Redis节点容器
- 自动初始化集群
- 验证集群状态
- 测试端口连通性

### 2. 测试Java连接

```bash
./test-java.sh
```

或者手动运行：

```bash
mvn compile exec:java -Dexec.mainClass="com.example.RedisClusterTest"
```

### 3. 停止集群

```bash
./stop-cluster.sh
```

## 🔧 配置详解

### Docker Compose配置

```yaml
services:
  redis-node-1:
    image: redis:7-alpine
    ports:
      - "6371:6379"      # Redis服务端口
      - "16371:16379"    # 集群总线端口
    volumes:
      - ./config/redis-node-1.conf:/usr/local/etc/redis/redis.conf
```

### Redis配置关键点

```conf
# 解决Docker网络隔离的关键配置
cluster-announce-ip 127.0.0.1        # 声明宿主机IP
cluster-announce-port 6371            # 声明映射端口  
cluster-announce-bus-port 16371       # 声明集群总线端口
```

### Java客户端配置

```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("127.0.0.1", 6371));  // 使用宿主机地址
nodes.add(new HostAndPort("127.0.0.1", 6372));
nodes.add(new HostAndPort("127.0.0.1", 6373));
JedisCluster jedisCluster = new JedisCluster(nodes);
```

## 🔍 验证和调试

### 查看集群状态

```bash
# 集群信息
docker exec redis-node-1 redis-cli cluster info

# 节点信息  
docker exec redis-node-1 redis-cli cluster nodes

# 连接到节点
docker exec -it redis-node-1 redis-cli
```

### 测试端口连通性

```bash
# 测试端口是否可达
telnet 127.0.0.1 6371
telnet 127.0.0.1 6372  
telnet 127.0.0.1 6373
```

### 查看容器日志

```bash
# 查看特定节点日志
docker-compose logs redis-node-1

# 查看所有节点日志
docker-compose logs
```

## ⚠️ 常见问题

### 1. JedisNoReachableClusterNodeException

**原因**：集群返回的节点IP是Docker内部IP，宿主机无法访问

**解决**：确保配置文件中的`cluster-announce-ip`设置正确

### 2. 端口连接被拒绝

**原因**：端口映射配置错误或容器未完全启动

**解决**：
- 检查`docker-compose.yml`中的端口映射
- 等待容器完全启动（约10-15秒）
- 验证端口映射：`docker-compose ps`

### 3. 集群初始化失败

**原因**：节点间无法通信或配置冲突

**解决**：
- 清理数据目录：`rm -rf data/node*/nodes-*.conf`
- 重新启动：`./stop-cluster.sh && ./start-cluster.sh`

## 🔄 其他解决方案

### 方案1：使用host.docker.internal（推荐Mac环境）

```conf
cluster-announce-ip host.docker.internal
```

### 方案2：使用Docker自定义网络

```yaml
networks:
  redis-cluster:
    driver: bridge
```

### 方案3：使用主机网络模式

```yaml
network_mode: "host"
```

## 📚 技术细节

- **Redis版本**：7.x
- **Jedis版本**：5.1.5  
- **Java版本**：21
- **Docker Compose版本**：3.8

## 🎉 总结

这个解决方案通过以下关键配置解决了Docker网络隔离问题：

1. **cluster-announce-ip**：声明宿主机可访问的IP
2. **cluster-announce-port**：声明映射后的端口
3. **正确的端口映射**：容器端口到宿主机端口的映射
4. **集群总线端口**：Redis集群节点间通信端口

通过这种配置，Java应用可以成功连接到Docker启动的Redis集群，避免了网络隔离导致的连接问题。
