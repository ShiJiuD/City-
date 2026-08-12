-- ============================================================
-- 秒杀库存扣减 Lua 脚本
-- 保证 GET + 判断 + DECRBY 三条命令的原子性（Redis 单线程执行）
-- ============================================================
-- KEYS[1] : 展览库存 key（exhibition:stock:{id}）
-- ARGV[1] : 购买数量
-- 返回值  :
--    1  扣减成功
--    0  库存不足
--   -1  key 不存在（未预热，需从 DB 加载）
-- ============================================================

-- 1.参数列表
local stockKey = KEYS[1]
local quantity = ARGV[1]

-- 2.脚本业务
-- 2.1.获取当前库存
local stock = redis.call('get', stockKey)

-- 2.2.key 不存在 → 未预热，通知 Java 侧从 DB 加载
if (not stock) then
    return -1
end

-- 2.3.库存不足
if (tonumber(stock) < tonumber(quantity)) then
    return 0
end

-- 2.4.库存充足 → 原子扣减
redis.call('decrby', stockKey, quantity)
return 1
