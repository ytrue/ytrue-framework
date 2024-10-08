<?php

namespace Ioc\Definition\Exception;

use Exception;
use Ioc\Definition\Definition;
use Psr\Container\ContainerExceptionInterface;
use const PHP_EOL;

/**
 * Class InvalidDefinition
 *
 * 该类用于表示在处理依赖注入定义时发生的无效定义异常。
 * 它扩展了 Exception 类，并实现了 ContainerExceptionInterface。
 */
class InvalidDefinition extends Exception implements ContainerExceptionInterface
{
    /**
     * 创建一个新的 InvalidDefinition 实例。
     *
     * @param Definition $definition 发生异常的定义
     * @param string $message 异常消息
     * @param Exception|null $previous 上一个异常（可选）
     * @return self 返回新的 InvalidDefinition 实例
     */
    public static function create(Definition $definition, string $message, ?Exception $previous = null): self
    {
        return new self(sprintf(
            '%s' . PHP_EOL . 'Full definition:' . PHP_EOL . '%s',
            $message,
            (string)$definition // 将定义转为字符串以便输出
        ), 0, $previous); // 调用父类构造函数，传递消息和上一个异常
    }
}
