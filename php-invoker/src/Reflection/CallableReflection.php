<?php

namespace Invoker\Reflection;

use Closure;
use Invoker\Exception\NotCallableException;
use ReflectionFunction;
use ReflectionFunctionAbstract;
use ReflectionMethod;

/**
 * CallableReflection 类用于创建反射类，反射可调用的函数或方法。
 *
 * 该类通过分析传入的可调用（callable）参数，返回相应的反射类实例（ReflectionFunction 或 ReflectionMethod）。
 * 可处理的可调用类型包括：
 * - 闭包（Closure）
 * - 数组（表示类方法调用，数组格式为 [class, method]）
 * - 实现了 __invoke 方法的对象
 * - 标准函数名称
 *
 * 如果传入的可调用无法被识别，将抛出 NotCallableException 异常。
 */
class CallableReflection
{
    /**
     * 根据传入的可调用类型创建相应的反射类（ReflectionFunction 或 ReflectionMethod）。
     *
     * @param callable|array|string|object $callable 可调用的函数、方法或闭包。
     * @return ReflectionFunctionAbstract 反射类的实例，用于进一步操作可调用对象。
     * @throws \ReflectionException 如果反射过程中发生错误。
     * @throws NotCallableException 如果传入的参数无法被调用。
     */
    public static function create(callable|array|string|object $callable): ReflectionFunctionAbstract
    {
        // 处理闭包的反射
        if ($callable instanceof Closure) {
            return new ReflectionFunction($callable);
        }

        // 处理类方法的反射（数组格式 [class, method]）
        if (is_array($callable)) {
            [$class, $method] = $callable;

            // 检查类方法是否存在
            if (!method_exists($class, $method)) {
                throw NotCallableException::fromInvalidCallable($callable);
            }

            return new ReflectionMethod($class, $method);
        }

        // 处理实现了 __invoke 方法的对象
        if (is_object($callable) && method_exists($callable, '__invoke')) {
            return new ReflectionMethod($callable, '__invoke');
        }

        // 处理标准函数名称的反射
        if (is_string($callable) && function_exists($callable)) {
            return new ReflectionFunction($callable);
        }

        // 如果都不符合，抛出异常
        throw new NotCallableException(sprintf(
            '%s is not a callable',
            is_string($callable) ? $callable : 'Instance of ' . get_class($callable)
        ));
    }
}
