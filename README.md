# Redis 集群搭建完整指南：从源码编译到集群验证

> 本文详细介绍了如何从源码编译 Redis 7.2.4 并搭建一个三节点的 Redis 集群，包含完整的步骤和验证方法。

## 📋 目录
- [环境准备](#环境准备)
- [源码编译](#源码编译)
- [集群配置](#集群配置)
- [启动节点](#启动节点)
- [创建集群](#创建集群)
- [集群验证](#集群验证)
- [使用示例](#使用示例)

## 🛠️ 环境准备

在开始之前，请确保您的系统已安装以下工具：
- `curl` - 用于下载源码
- `tar` - 用于解压文件
- `make` - 用于编译源码
- `gcc` - C语言编译器

## 🔨 源码编译

### 步骤 1：下载源码
```bash
curl -O http://download.redis.io/releases/redis-7.2.4.tar.gz
```

### 步骤 2：解压源码
```bash
tar zxvf redis-7.2.4.tar.gz
cd redis-7.2.4
```

### 步骤 3：编译安装
```bash
make -j
```

> 💡 **提示**：`-j` 参数表示使用多线程编译，可以加快编译速度。如果不指定，系统会自动选择合适的线程数。

## ⚙️ 集群配置

在编译完成后，您需要为每个节点创建配置文件。项目已经为您准备了三个节点的配置文件：

- `redis-cluster/node1/node1.conf` - 节点1配置
- `redis-cluster/node2/node2.conf` - 节点2配置  
- `redis-cluster/node3/node3.conf` - 节点3配置

## 🚀 启动节点

在 `redis-7.2.4` 目录下，依次启动三个 Redis 节点：

```bash
# 启动节点1
./src/redis-server ../redis-cluster/node1/node1.conf

# 启动节点2  
./src/redis-server ../redis-cluster/node2/node2.conf

# 启动节点3
./src/redis-server ../redis-cluster/node3/node3.conf
```

> ⚠️ **注意**：每个节点需要在不同的终端窗口中启动，或者使用后台运行的方式。

## 🔗 创建集群

使用 Redis CLI 创建集群，将三个节点组成一个集群：

```bash
./src/redis-cli -a redispassword --cluster create \
  127.0.0.1:6371 127.0.0.1:6372 127.0.0.1:6373 \
  --cluster-replicas 0 --cluster-yes
```

**参数说明**：
- `-a redispassword` - 指定集群密码
- `--cluster-replicas 0` - 不设置副本节点
- `--cluster-yes` - 自动确认集群创建

## ✅ 集群验证

### 查看集群信息
```bash
./redis-7.2.4/src/redis-cli -a redispassword -p 6371 cluster info
```

### 查看节点信息
```bash
./redis-7.2.4/src/redis-cli -a redispassword -p 6371 cluster nodes
```

## 💡 使用示例

### 集群模式操作
使用 `-c` 参数启用集群模式，Redis CLI 会自动跟随重定向：

```bash
# 设置键值对
./redis-7.2.4/src/redis-cli -c -a redispassword -p 6371 set k v

# 获取键值对
./redis-7.2.4/src/redis-cli -c -a redispassword -p 6371 get k
```

> 🔄 **说明**：集群模式下，Redis 会根据键的哈希值自动将请求路由到正确的节点。如果键不在当前连接的节点上，Redis 会自动重定向到正确的节点。

## 📝 总结

通过以上步骤，您已经成功搭建了一个包含三个节点的 Redis 集群。这个集群具有以下特点：

- ✅ 高可用性：节点间相互备份
- ✅ 负载均衡：数据自动分片
- ✅ 自动重定向：客户端请求自动路由
- ✅ 密码保护：增强安全性

如果您在生产环境中使用，建议：
1. 增加副本节点以提高可用性
2. 配置监控和告警
3. 定期备份数据
4. 考虑使用 Redis Sentinel 进行故障检测



 