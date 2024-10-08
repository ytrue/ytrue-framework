<?php

namespace Ioc\Definition\Helper;

use Ioc\Definition\Definition;

/**
 * Interface DefinitionHelper
 *
 * 该接口提供了获取依赖注入定义的方法。
 * 任何实现此接口的类都应提供具体的方式来获取指定名称的定义。
 */
interface DefinitionHelper
{
    /**
     * 根据条目名称获取相应的依赖注入定义。
     *
     * @param string $entryName 要获取定义的条目名称。
     * @return Definition 返回与指定条目名称关联的 Definition 实例。
     */
    public function getDefinition(string $entryName): Definition;
}
