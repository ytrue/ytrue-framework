<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\Definition;

/**
 * Interface MutableDefinitionSource
 *
 * 该接口扩展了 DefinitionSource 接口，允许修改 bean 定义。
 */
interface MutableDefinitionSource extends DefinitionSource
{
    /**
     * 添加新的 bean 定义
     *
     * @param Definition $definition 要添加的 bean 定义。
     * @return void
     */
    public function addDefinition(Definition $definition): void;
}
