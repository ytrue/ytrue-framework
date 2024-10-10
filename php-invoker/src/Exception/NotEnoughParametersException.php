<?php

namespace Invoker\Exception;

/**
 * NotEnoughParametersException 异常类表示在调用 callable 时，无法解析足够的参数。
 *
 * 该异常用于当传递给可调用函数的参数不足，或者无法满足函数的所有参数需求时抛出。
 * 它继承自 `InvocationException`，用于处理与调用相关的错误。
 */
class NotEnoughParametersException extends InvocationException
{
}
