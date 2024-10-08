<?php

namespace Ioc\Definition;

/**
 * ExtendsPreviousDefinition 接口继承了 Definition 接口
 * 用于定义可以扩展其他定义的功能
 */
interface ExtendsPreviousDefinition extends Definition
{
    /**
     * 设置扩展的定义
     *
     * @param Definition $definition 要扩展的定义对象
     */
    public function setExtendedDefinition(Definition $definition): void;
}
