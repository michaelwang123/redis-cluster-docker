# Redis集群Docker解决方案

这个项目演示了如何在Mac本地环境使用Docker Compose启动Redis集群，并解决Docker网络隔离导致的连接问题。支持无密码和带密码两种配置方式。

## 🎯 解决的核心问题

**JedisCluster连接Docker Redis集群时的网络隔离问题**：
- Redis集群默认记录Docker容器内部IP（如172.18.0.2），宿主机无法访问
- 通过`cluster-announce-ip`和`cluster-announce-port`声明宿主机可访问的地址
- 确保Java客户端能正确获取并连接到Redis集群节点
- 支持带密码的Redis集群配置，增强安全性

## 📁 项目结构

```
redis-cluster-docker/
├── docker-compose.yml                    # 无密码Docker编排配置
├── docker-compose_with_password.yml      # 带密码Docker编排配置
├── config/                               # Redis配置文件
│   ├── redis-node-1.conf                # 无密码节点1配置
│   ├── redis-node-2.conf                # 无密码节点2配置
│   ├── redis-node-3.conf                # 无密码节点3配置
│   ├── redis-node-1-with-password.conf  # 带密码节点1配置
│   ├── redis-node-2-with-password.conf  # 带密码节点2配置
│   └── redis-node-3-with-password.conf  # 带密码节点3配置
├── data/                                 # 数据持久化目录
│   ├── node1/
│   ├── node2/
│   └── node3/
├── src/main/java/                        # Java测试代码
│   └── com/example/
│       ├── RedisClusterTest.java         # 无密码测试类
│       └── RedisClusterWithPasswordTest.java # 带密码测试类
├── start-cluster.sh                      # 无密码集群启动脚本
├── start-cluster-with-password.sh        # 带密码集群启动脚本
├── stop-cluster.sh                       # 停止集群脚本
├── test-java.sh                          # 无密码Java测试脚本
├── test-redis-with-password.sh           # 带密码Java测试脚本
└── README.md                             # 说明文档
```

## 🚀 快速开始

### 方案一：无密码Redis集群

#### 1. 启动无密码Redis集群

```bash
./start-cluster.sh
```

#### 2. 测试无密码Java连接

```bash
./test-java.sh
```

或者手动运行：

```bash
mvn compile exec:java -Dexec.mainClass="com.example.RedisClusterTest"
```

### 方案二：带密码Redis集群（推荐生产环境）

#### 1. 启动带密码Redis集群

```bash
./start-cluster-with-password.sh
```

#### 2. 测试带密码Java连接

```bash
./test-redis-with-password.sh
```

或者手动运行：

```bash
mvn compile exec:java -Dexec.mainClass="com.example.RedisClusterWithPasswordTest"
```

### 3. 停止集群

```bash
# 停止无密码集群
docker-compose down

# 停止带密码集群
docker-compose -f docker-compose_with_password.yml down
```

## 🔧 配置详解

### Docker Compose配置

#### 无密码配置 (docker-compose.yml)

```yaml
services:
  redis-node-1:
    image: redis:alpine3.19
    ports:
      - "6371:6379"      # Redis服务端口
      - "16371:16379"    # 集群总线端口
    volumes:
      - ./config/redis-node-1.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
```

#### 带密码配置 (docker-compose_with_password.yml)

```yaml
services:
  redis-node-1:
    image: redis:alpine3.19
    ports:
      - "6371:6379"      # Redis服务端口
      - "16371:16379"    # 集群总线端口
    volumes:
      - ./config/redis-node-1-with-password.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf --requirepass redispassword
```

### Redis配置关键点

#### 无密码配置

```conf
# 解决Docker网络隔离的关键配置
cluster-announce-ip 127.0.0.1        # 声明宿主机IP
cluster-announce-port 6371            # 声明映射端口  
cluster-announce-bus-port 16371       # 声明集群总线端口
```

#### 带密码配置

```conf
# 密码配置
requirepass redispassword
masterauth redispassword

# 解决Docker网络隔离的关键配置
cluster-announce-ip 127.0.0.1        # 声明宿主机IP
cluster-announce-port 6371            # 声明映射端口  
cluster-announce-bus-port 16371       # 声明集群总线端口
```

### Java客户端配置

#### 无密码连接

```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("127.0.0.1", 6371));  // 使用宿主机地址
nodes.add(new HostAndPort("127.0.0.1", 6372));
nodes.add(new HostAndPort("127.0.0.1", 6373));
JedisCluster jedisCluster = new JedisCluster(nodes);
```

#### 带密码连接

```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("127.0.0.1", 6371));
nodes.add(new HostAndPort("127.0.0.1", 6372));
nodes.add(new HostAndPort("127.0.0.1", 6373));

// 使用Jedis 5.x版本的构造方法
DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
    .password("redispassword")
    .connectionTimeoutMillis(2000)
    .socketTimeoutMillis(2000)
    .build();

JedisCluster jedisCluster = new JedisCluster(nodes, clientConfig);
```

## 🔍 验证和调试

### 查看集群状态

#### 无密码集群

```bash
# 集群信息
docker exec redis-node-1 redis-cli cluster info

# 节点信息  
docker exec redis-node-1 redis-cli cluster nodes

# 连接到节点
docker exec -it redis-node-1 redis-cli
```

#### 带密码集群

```bash
# 集群信息
docker exec redis-node-1 redis-cli -a redispassword cluster info

# 节点信息  
docker exec redis-node-1 redis-cli -a redispassword cluster nodes

# 连接到节点
docker exec -it redis-node-1 redis-cli -a redispassword
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
# 查看无密码集群日志
docker-compose logs redis-node-1

# 查看带密码集群日志
docker-compose -f docker-compose_with_password.yml logs redis-node-1

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

### 4. 密码认证失败

**原因**：Java客户端密码配置错误或Redis节点密码不一致

**解决**：
- 确保Java代码中的密码与Redis配置一致
- 检查`requirepass`和`masterauth`配置
- 验证Jedis客户端使用正确的构造函数

### 5. JedisCluster构造函数错误

**原因**：Jedis版本不兼容或构造函数参数错误

**解决**：
- 使用Jedis 5.x版本的`DefaultJedisClientConfig.builder()`方法
- 确保构造函数参数正确：`new JedisCluster(nodes, clientConfig)`

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

- **Redis版本**：7.x (alpine3.19)
- **Jedis版本**：5.1.5  
- **Java版本**：21
- **Docker Compose版本**：3.8
- **集群密码**：redispassword

## 🎉 总结

这个解决方案通过以下关键配置解决了Docker网络隔离问题：

1. **cluster-announce-ip**：声明宿主机可访问的IP
2. **cluster-announce-port**：声明映射后的端口
3. **正确的端口映射**：容器端口到宿主机端口的映射
4. **集群总线端口**：Redis集群节点间通信端口
5. **密码认证**：支持带密码的Redis集群配置，增强安全性

通过这种配置，Java应用可以成功连接到Docker启动的Redis集群，避免了网络隔离导致的连接问题。同时提供了无密码和带密码两种配置方案，满足不同环境的安全需求。

### 推荐使用场景

- **开发环境**：使用无密码配置，简化开发和测试
- **生产环境**：使用带密码配置，确保数据安全
- **学习研究**：两种配置都提供了完整的示例代码和文档

## 📋 快速参考

### 集群信息

| 配置类型 | 节点1 | 节点2 | 节点3 | 密码 |
|---------|-------|-------|-------|------|
| 无密码 | 127.0.0.1:6371 | 127.0.0.1:6372 | 127.0.0.1:6373 | 无 |
| 带密码 | 127.0.0.1:6371 | 127.0.0.1:6372 | 127.0.0.1:6373 | redispassword |

### 常用命令

```bash
# 启动集群
./start-cluster.sh                    # 无密码
./start-cluster-with-password.sh      # 带密码

# 测试连接
./test-java.sh                       # 无密码
./test-redis-with-password.sh        # 带密码

# 停止集群
docker-compose down                   # 无密码
docker-compose -f docker-compose_with_password.yml down  # 带密码

# 查看集群状态
docker exec redis-node-1 redis-cli cluster info                    # 无密码
docker exec redis-node-1 redis-cli -a redispassword cluster info   # 带密码
```
