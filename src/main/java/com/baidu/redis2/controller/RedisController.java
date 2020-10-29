package com.baidu.redis2.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
public class RedisController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    String ta_prd = "4B|70B856";
    String bank_no = "1001|RETAIN";
    String branch_no = "1001|3550|TOTAL";


    @GetMapping("/testLua")
    public String testLua() {
        String lua = "local delQuota = tonumber(redis.call('hget', '"+ta_prd+"','"+branch_no+"')or 0); \n" +
                     "redis.call('hset','"+ta_prd+"','"+branch_no+"', 0); \n" +
                     "redis.call('hincrbyfloat','"+ta_prd+"','"+bank_no+"',delQuota)";

        Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(String.class);
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(String.class);


        String execute = stringRedisTemplate.execute(script, serializer, serializer, null);


        return execute;
    }

    @GetMapping("/testRedis")
    public Map<Object, Object> testRedis() {
        stringRedisTemplate.opsForHash().put(ta_prd,bank_no,Integer.toString(600));
        stringRedisTemplate.opsForHash().put(ta_prd,branch_no,Integer.toString(400));
        stringRedisTemplate.opsForHash().put(ta_prd,"1001|TOTAL",Integer.toString(1000));
        stringRedisTemplate.opsForHash().put(ta_prd,"1001|3550|3504|TOTAL",Integer.toString(400));
        stringRedisTemplate.opsForHash().put(ta_prd,"1001|3550|USED",Integer.toString(400));
        stringRedisTemplate.opsForHash().put(ta_prd,"1001|3550|3501|TOTAL",Integer.toString(300));
        stringRedisTemplate.opsForHash().put(ta_prd,"1001|3550|3506|USED",Integer.toString(200));
        stringRedisTemplate.opsForHash().put(ta_prd,"1001|3550|3500|TOTAL",Integer.toString(100));
        Map<Object, Object> test2 = stringRedisTemplate.opsForHash().entries(ta_prd);
        return test2;
    }

    @GetMapping("/testHscan")
    public Cursor<Map.Entry<Object, Object>> testHscan() throws Exception{
        ScanOptions scanOptions = ScanOptions.scanOptions().match("1001|3550|*").count(2).build();

        Cursor<Map.Entry<Object, Object>> scan = stringRedisTemplate.opsForHash().scan(ta_prd, scanOptions);
        while (scan.hasNext()) {
            Map.Entry<Object, Object> next = scan.next();
            Object key = next.getKey();
            Object value = next.getValue();

            System.out.println(key);
            System.out.println(value);
        }


        scan.close();
        return scan;
    }



    @GetMapping("/testHkeys")
    public Object testHkeys() throws Exception{
        String no = "1001|3550|3500|";
        Set<Object> keys = stringRedisTemplate.opsForHash().keys(ta_prd);
        for (Object o : keys) {
            if (null == o) continue;

            String key = o.toString();
            if (!key.startsWith(no)) continue;

            String replace = key.replace(no, "");

            if (replace.contains("|")) {
                return "有下级机构";
            }
        }
        return "没有下级机构";
    }

    /**
     * 使用lua脚本的方式操作redis命令
     * @return
     */
    @GetMapping("/redisLua")
    public Object redisLua() {
        String no = "1001|3550|"; //要删除的机构号的key
        String superNo = "1001|RETAIN";

        // 全局变量
        String local =
                "local delOrganizeNo = '"+no+"'; \n" +
                "local delOrganizeNoLength = '"+(no.length()+1)+"'; \n" +   // 注意：lua的下标从1开始
                "local ta_prd = '"+ta_prd+"'; \n" +
                "local delNo = '"+(no+"TOTAL")+"'; \n" +
                "local superNo = '"+superNo+"'; \n" +
                "local retCode = 'success'; \n" +
                "local retMessage = '处理成功'; \n" +
                "local errCode = 'error'; \n" +
                "local successCode='success'; \n" +
                "local returnTable = {}; \n" +
                "local delNoArr = {'"+(no+"USED")+"','"+(no+"TOTAL")+"'}; \n";

        // 校验要删除的机构是否有下级机构
        String script =
                "local hashKeys = redis.pcall('HKEYS',ta_prd); \n" +
                "for key,value in pairs(hashKeys) do \n" +  // 根据key获取hash所有的key
                "   if (string.find(value,delOrganizeNo)==1) then \n" + // 循环判断该key是否以要删除额度的机构代码开头
                "       local subStr = string.sub(value,delOrganizeNoLength); \n" + // 将开头截取，lua的下标从1开始
                "       local strTem = string.match(subStr,'|'); \n" +      // 剩下的字符串中是否有 "|",有则说明有下级机构
                "           if (strTem ~= nil) then \n" +  // 如果不等于nil，说明匹配到了值
                "               retCode = errCode; \n" +
                "               retMessage='额度删除失败!有下级机构!'; \n" +
                "           end \n" +
                "   end \n" +
                "end \n";
        StringBuffer sf = new StringBuffer();
        sf.append(local).append(script);

        // 删除额度的脚本
        String delScript =
                "if (retCode == successCode) then \n" +
                "   local delQuota = tonumber(redis.pcall('HGET',ta_prd,delNo)or 0); \n" +
                "   redis.pcall('hincrbyfloat',ta_prd,superNo,delQuota); \n" +
                "   for key,value in pairs(delNoArr) do \n" +
                "       redis.pcall('HDEL',ta_prd,value); \n" +
                "   end \n" +
                "end \n";
        sf.append(delScript);

        // 日志和返回结果
        String log =
                "redis.log(redis.LOG_NOTICE,'删除额度的TA_产品['..ta_prd..']'); \n" + //.. 在java中是 + 的意思，连接
                "returnTable['retCode'] = retCode;\n" +
                "returnTable['retMessage'] = retMessage; \n" +
                "return cjson.encode(returnTable); \n";
        sf.append(log);

        FastJsonRedisSerializer<String> serializer = new FastJsonRedisSerializer(String.class);
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setResultType(String.class);
        redisScript.setScriptText(sf.toString());
        System.out.println(sf.toString());

        String execute = stringRedisTemplate.execute(redisScript, serializer, serializer, null);
        JSONObject jsonObject = JSONObject.parseObject(execute);

        Object retCode = jsonObject.get("retCode");
        if ("error".equals(retCode)) {
            throw new RuntimeException(jsonObject.get("retMessage").toString());
        }

        return execute;
    }

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        int c = 3;
        if (a > 2 || b > 2 || c > 2) {
            System.out.println("好");
        }
    }

    /**
     * 使用lua脚本的方式操作redis命令
     * @return
     */
    @GetMapping("/redisLuaDelete")
    public Object redisLuaDelete() {
        String no = "1001|3550|"; //要删除的机构号的key
        String superNo = "1001|RETAIN";

        // 全局变量
        String local =
                "local delOrganizeNo = '"+no+"'; \n" +
                        "local delOrganizeNoLength = '"+(no.length()+1)+"'; \n" +   // 注意：lua的下标从1开始
                        "local ta_prd = '"+ta_prd+"'; \n" +
                        "local delNo = '"+(no+"TOTAL")+"'; \n" +
                        "local delValueTem = ''; \n" +
                        "local superNo = '"+superNo+"'; \n" +
                        "local retCode = 'success'; \n" +
                        "local retMessage = '处理成功'; \n" +
                        "local errCode = 'error'; \n" +
                        "local successCode='success'; \n" +
                        "local returnTable = {}; \n";

        StringBuffer sf = new StringBuffer();
        sf.append(local);

        String delOrNo =
                "local value = redis.pcall('hget',ta_prd,delNo); \n" +
                "delValueTem = value; \n" +
                "if (value) then \n" +
                "   retCode = successCode;\n" +
                "else \n" +
                "   retCode = errCode;\n"+
                "   retMessage='额度已删除';\n"+
                "end \n";
        sf.append(delOrNo);
        // 日志和返回结果
        String log =
                "redis.log(redis.LOG_NOTICE,'删除额度的TA_产品['..ta_prd..']'); \n" + //.. 在java中是 + 的意思，连接
                        "returnTable['retCode'] = retCode;\n" +
                        "returnTable['retMessage'] = retMessage; \n" +
                        "returnTable['delValueTem'] = delValueTem; \n" +
                        "return cjson.encode(returnTable); \n";
        sf.append(log);

        FastJsonRedisSerializer<String> serializer = new FastJsonRedisSerializer(String.class);
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setResultType(String.class);
        redisScript.setScriptText(sf.toString());
        System.out.println(sf.toString());

        String execute = stringRedisTemplate.execute(redisScript, serializer, serializer, null);
        JSONObject jsonObject = JSONObject.parseObject(execute);

        Object retCode = jsonObject.get("retCode");
        if ("error".equals(retCode)) {
            throw new RuntimeException(jsonObject.get("retMessage").toString());
        }

        return execute;
    }

    @GetMapping("/testDelete")
    public Object testDelete() {
        String taPrd = "4B|70B856";
        Long delete = stringRedisTemplate.opsForHash().delete(taPrd, "1001|TOTAL");
        Object o = stringRedisTemplate.opsForHash().get(taPrd, "1001|TOTAL");
        System.out.println(o);
        return o;
    }
}
