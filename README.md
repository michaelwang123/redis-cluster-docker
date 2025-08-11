

# 在 redis-7.2.4 目录下
./src/redis-server ../redis-cluster/node1/node1.conf
./src/redis-server ../redis-cluster/node2/node2.conf
./src/redis-server ../redis-cluster/node3/node3.conf


创建集群
./src/redis-cli -a redispassword --cluster create \
  127.0.0.1:6371 127.0.0.1:6372 127.0.0.1:6373 \
  --cluster-replicas 0 --cluster-yes


./redis-7.2.4/src/redis-cli -a redispassword -p 6371 cluster info
./redis-7.2.4/src/redis-cli -a redispassword -p 6371 cluster nodes


用 redis-cli 的集群模式自动跟随重定向，或者直接连到提示的节点
./redis-7.2.4/src/redis-cli -c -a redispassword -p 6371 set k v
./redis-7.2.4/src/redis-cli -c -a redispassword -p 6371 get k



 