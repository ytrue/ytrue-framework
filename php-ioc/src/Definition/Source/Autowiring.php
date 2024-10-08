<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\ObjectDefinition;

/**
 * Interface Autowiring
 *
 * 提供自动装配对象定义的方法。
 */
interface Autowiring
{
    /**
     * 自动装配给定的对象定义
     *
     * @param string $name 定义的名称。
     * @param ObjectDefinition|null $definition 可选的对象定义。
     * @return ObjectDefinition|null 返回自动装配后的对象定义或 null。
     */
    public function autowire(string $name, ?ObjectDefinition $definition = null): ObjectDefinition|null;
}
