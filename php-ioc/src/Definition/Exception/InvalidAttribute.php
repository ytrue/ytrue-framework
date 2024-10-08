<?php

namespace Ioc\Definition\Exception;

/**
 * Class InvalidAttribute
 *
 * 该类表示在处理依赖注入定义时，发生了与属性相关的无效定义异常。
 * 它继承自 InvalidDefinition 类，包含了无效定义的所有功能。
 */
class InvalidAttribute extends InvalidDefinition
{
    // 此类当前没有额外的属性或方法，
    // 它仅用作特定类型的无效定义异常，便于在异常处理时进行类型判断。
}
