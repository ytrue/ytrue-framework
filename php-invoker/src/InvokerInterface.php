<?php

namespace Invoker;

use Invoker\Exception\InvocationException;
use Invoker\Exception\NotCallableException;
use Invoker\Exception\NotEnoughParametersException;

/**
 * 接口 InvokerInterface 提供了一个调用函数的通用机制，支持使用动态参数调用可执行的函数或方法。
 */
interface InvokerInterface
{
    /**
     * 调用给定的函数并传入指定的参数。
     *
     * @param callable|array|string $callable 要调用的函数或方法。
     *                                        - callable: 可以是匿名函数、全局函数、对象方法等。
     *                                        - array: 包含对象和方法名的数组，格式为 [对象, '方法名']。
     *                                        - string: 方法名称或者函数名称。
     * @param array $parameters 调用函数时使用的参数数组。参数将按顺序传递给函数。
     *
     * @return mixed 返回函数的结果。调用的函数可能返回任何类型的值，视具体函数而定。
     *
     * @throws InvocationException 基本的调用异常类，所有下面列出的子异常类都继承自该类。
     * @throws NotCallableException 当提供的 $callable 参数无法被调用时抛出该异常。
     * @throws NotEnoughParametersException 当提供的参数数量不足以满足函数调用时抛出该异常。
     */
    public function call(callable|array|string $callable, array $parameters = []): mixed;
}
