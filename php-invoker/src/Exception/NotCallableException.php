<?php

namespace Invoker\Exception;

/**
 * NotCallableException 异常类表示给定的 callable 实际上不是可调用的。
 *
 * 该异常在系统尝试调用一个对象、方法或变量时，如果它不符合 PHP 的 callable 规则，则会抛出。
 * 它继承自 `InvocationException`，是专门用于处理不可调用错误的异常类。
 */
class NotCallableException extends InvocationException
{
    /**
     * 根据无效的 callable 值生成一个异常实例。
     *
     * @param mixed $value 传递的值，期望为 callable，但不是。
     * @param bool $containerEntry 可选参数，标识该值是否为容器条目。
     * @return NotCallableException 返回异常实例。
     */
    public static function fromInvalidCallable(mixed $value, bool $containerEntry = false): self
    {
        // 如果传递的值是一个对象
        if (is_object($value)) {
            // 抛出异常消息，表明该对象实例不可调用
            $message = sprintf('实例 %s 不是可调用的', get_class($value));
        }
        // 如果传递的是一个数组，且该数组表示某个类的方法
        elseif (is_array($value) && isset($value[0], $value[1])) {
            // 获取类名
            $class = is_object($value[0]) ? get_class($value[0]) : $value[0];

            // 检查类中是否定义了 __call() 或 __callStatic() 方法
            $extra = method_exists($class, '__call') || method_exists($class, '__callStatic')
                ? ' 类定义了 __call() 或 __callStatic() 方法，但魔术方法不支持可调用判断。'
                : '';

            // 抛出异常消息，表明该方法不可调用
            $message = sprintf('%s::%s() 不是可调用的。%s', $class, $value[1], $extra);
        }
        // 如果值被标识为容器条目但不是可调用的
        elseif ($containerEntry) {
            $message = var_export($value, true) . ' 既不是可调用的，也不是有效的容器条目';
        }
        // 其他情况，直接抛出值不是可调用的消息
        else {
            $message = var_export($value, true) . ' 不是可调用的';
        }

        // 返回 NotCallableException 实例
        return new self($message);
    }
}
