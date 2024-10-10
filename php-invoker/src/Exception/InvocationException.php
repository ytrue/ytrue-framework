<?php

namespace Invoker\Exception;

use Exception;

/**
 * InvocationException 异常类表示在调用可执行函数时发生的错误。
 *
 * 该异常通常在尝试调用给定的 callable 时遇到问题时抛出。它是所有调用相关异常的基类。
 * 它继承自 PHP 的内置 Exception 类，因此可以使用所有标准的异常处理机制来捕获和处理此类错误。
 */
class InvocationException extends Exception
{
}
