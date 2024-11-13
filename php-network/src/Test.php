<?php

namespace Network;

require '../vendor/autoload.php';

class Test
{
    public static function main(): void
    {
        // 创建一个Worker监听2345端口，使用http协议通讯
        $worker = new Worker("http://0.0.0.0:2345");

        // 启动4个进程对外提供服务
        $worker->count = 4;

        Worker::runAll();
    }
}


Test::main();

