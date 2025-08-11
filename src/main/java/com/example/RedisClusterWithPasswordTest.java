package com.example;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;

/**
 * Redis集群连接测试类 (带密码版本)
 * 用于验证Jedis客户端能否成功连接带密码的Redis集群
 */
public class RedisClusterWithPasswordTest {
    
    private static final String[] CLUSTER_NODES = {
        "127.0.0.1:6371",
        "127.0.0.1:6372", 
        "127.0.0.1:6373"
    };
    
    private static final String REDIS_PASSWORD = "redispassword";
    
    private JedisCluster jedisCluster;
    
    public RedisClusterWithPasswordTest() {
        initCluster();
    }
    
    /**
     * 初始化Redis集群连接
     */
    private void initCluster() {
        try {
            // 配置连接池
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(100);
            poolConfig.setMaxIdle(20);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            
            // 设置集群节点
            Set<HostAndPort> nodes = new HashSet<>();
            for (String node : CLUSTER_NODES) {
                String[] parts = node.split(":");
                nodes.add(new HostAndPort(parts[0], Integer.parseInt(parts[1])));
            }
            
            // 创建集群连接 (带密码)
            // 使用Jedis 5.x版本的构造方法
            DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password(REDIS_PASSWORD)
                .connectionTimeoutMillis(2000)
                .socketTimeoutMillis(2000)
                .build();
            
            jedisCluster = new JedisCluster(nodes, clientConfig);
            
            System.out.println("✅ Redis集群连接初始化成功 (带密码)");
            
        } catch (Exception e) {
            System.err.println("❌ Redis集群连接初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试基本操作
     */
    public void testBasicOperations() {
        System.out.println("\n=== 测试基本操作 ===");
        
        try {
            // 测试字符串操作
            String key = "test:string:" + System.currentTimeMillis();
            String value = "Hello Redis Cluster with Password!";
            
            jedisCluster.set(key, value);
            String result = jedisCluster.get(key);
            
            System.out.println("✅ 字符串操作测试: " + (value.equals(result) ? "成功" : "失败"));
            System.out.println("   设置值: " + value);
            System.out.println("   获取值: " + result);
            
            // 测试哈希操作
            String hashKey = "test:hash:" + System.currentTimeMillis();
            jedisCluster.hset(hashKey, "field1", "value1");
            jedisCluster.hset(hashKey, "field2", "value2");
            
            Map<String, String> hashResult = jedisCluster.hgetAll(hashKey);
            System.out.println("✅ 哈希操作测试: " + (hashResult.size() == 2 ? "成功" : "失败"));
            System.out.println("   哈希内容: " + hashResult);
            
            // 测试列表操作
            String listKey = "test:list:" + System.currentTimeMillis();
            jedisCluster.lpush(listKey, "item1", "item2", "item3");
            List<String> listResult = jedisCluster.lrange(listKey, 0, -1);
            
            System.out.println("✅ 列表操作测试: " + (listResult.size() == 3 ? "成功" : "失败"));
            System.out.println("   列表内容: " + listResult);
            
            // 测试集合操作
            String setKey = "test:set:" + System.currentTimeMillis();
            jedisCluster.sadd(setKey, "member1", "member2", "member3");
            Set<String> setResult = jedisCluster.smembers(setKey);
            
            System.out.println("✅ 集合操作测试: " + (setResult.size() == 3 ? "成功" : "失败"));
            System.out.println("   集合内容: " + setResult);
            
        } catch (Exception e) {
            System.err.println("❌ 基本操作测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试集群信息
     */
    public void testClusterInfo() {
        System.out.println("\n=== 测试集群信息 ===");
        
        try {
            // 获取集群的基本信息
            System.out.println("🔍 获取集群基本信息...");
            
            // 创建一个单独的连接到集群中的一个节点
            Jedis jedis = null;
            try {
                // 连接到第一个节点 (带密码)
                jedis = new Jedis("127.0.0.1", 6371);
                jedis.auth(REDIS_PASSWORD);
                
                // 执行 CLUSTER INFO 命令
                String clusterInfo = jedis.clusterInfo();
                System.out.println("✅ 集群信息获取成功:");
                
                // 解析并显示关键信息
                String[] infoLines = clusterInfo.split("\r\n");
                for (String line : infoLines) {
                    if (line.startsWith("cluster_state:") || 
                        line.startsWith("cluster_slots_assigned:") ||
                        line.startsWith("cluster_known_nodes:") ||
                        line.startsWith("cluster_size:")) {
                        System.out.println("   " + line);
                    }
                }
                
                System.out.println("\n🔍 获取集群节点信息...");
                
                // 执行 CLUSTER NODES 命令
                String clusterNodes = jedis.clusterNodes();
                System.out.println("✅ 集群节点信息获取成功:");
                
                // 解析并显示节点信息
                String[] nodeLines = clusterNodes.split("\n");
                int nodeCount = 0;
                for (String line : nodeLines) {
                    if (!line.trim().isEmpty()) {
                        nodeCount++;
                        String[] parts = line.split(" ");
                        if (parts.length >= 3) {
                            String nodeId = parts[0].substring(0, Math.min(8, parts[0].length()));
                            String address = parts[1];
                            String role = parts[2].contains("master") ? "主节点" : "从节点";
                            System.out.println("   节点" + nodeCount + ": " + address + " (" + role + ") ID:" + nodeId + "...");
                        }
                    }
                }
                
                System.out.println("\n🎯 集群拓扑验证:");
                System.out.println("   总节点数: " + nodeCount);
                System.out.println("   预期节点数: 3");
                System.out.println("   拓扑状态: " + (nodeCount == 3 ? "✅ 正常" : "⚠️ 异常"));
                
                // 测试节点连通性
                testClusterHealth(jedis);
                
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 集群信息测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试集群健康状态
     */
    private void testClusterHealth(Jedis jedis) {
        System.out.println("\n🔄 测试集群健康状态...");
        
        try {
            // 测试集群状态
            String clusterInfo = jedis.clusterInfo();
            boolean isHealthy = clusterInfo.contains("cluster_state:ok");
            System.out.println("   集群状态: " + (isHealthy ? "✅ 健康" : "❌ 异常"));
            
            // 测试所有槽位是否被分配
            if (clusterInfo.contains("cluster_slots_assigned:16384")) {
                System.out.println("   槽位分配: ✅ 完整 (16384/16384)");
            } else {
                System.out.println("   槽位分配: ⚠️ 不完整");
            }
            
            // 测试各节点连通性
            System.out.println("   节点连通性测试:");
            String[] testPorts = {"6371", "6372", "6373"};
            int reachableNodes = 0;
            
            for (String port : testPorts) {
                try (Jedis testJedis = new Jedis("127.0.0.1", Integer.parseInt(port))) {
                    testJedis.auth(REDIS_PASSWORD);
                    String pong = testJedis.ping();
                    if ("PONG".equals(pong)) {
                        reachableNodes++;
                        System.out.println("     端口" + port + ": ✅ 可达");
                    }
                } catch (Exception e) {
                    System.out.println("     端口" + port + ": ❌ 不可达 (" + e.getMessage() + ")");
                }
            }
            
            System.out.println("   可达节点: " + reachableNodes + "/3 " + 
                             (reachableNodes == 3 ? "✅" : "⚠️"));
            
        } catch (Exception e) {
            System.err.println("   ❌ 集群健康状态测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭连接
     */
    public void close() {
        if (jedisCluster != null) {
            try {
                jedisCluster.close();
                System.out.println("✅ Redis集群连接已关闭");
            } catch (Exception e) {
                System.err.println("❌ 关闭连接失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        System.out.println("🚀 开始Redis集群连接测试 (带密码)...");
        System.out.println("集群节点: " + String.join(", ", CLUSTER_NODES));
        System.out.println("密码: " + REDIS_PASSWORD);
        
        RedisClusterWithPasswordTest test = new RedisClusterWithPasswordTest();
        
        if (test.jedisCluster != null) {
            test.testBasicOperations();
            test.testClusterInfo();
            test.close();
            
            System.out.println("\n🎉 所有测试完成！");
        } else {
            System.err.println("❌ 无法连接到Redis集群，请检查集群状态和密码是否正确");
        }
    }
}
