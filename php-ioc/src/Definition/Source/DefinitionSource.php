<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\Definition;

/**
 * Interface DefinitionSource
 *
 * 该接口定义了用于获取 bean 定义的行为。
 */
interface DefinitionSource
{
    /**
     * 根据名称获取 bean 定义
     *
     * @param string $name 要获取的 bean 定义的名称。
     * @return Definition|null 如果找到定义，返回该定义；否则返回 null。
     */
    public function getDefinition(string $name): Definition|null;

    /**
     * 获取所有的 bean 定义
     *
     * @return array 返回包含所有 bean 定义的数组。
     */
    public function getDefinitions(): array;
}
