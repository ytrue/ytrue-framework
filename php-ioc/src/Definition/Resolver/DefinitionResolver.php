<?php

namespace Ioc\Definition\Resolver;

use Ioc\Definition\Definition;

/**
 * Interface DefinitionResolver
 *
 * 该接口定义了用于解析 bean 定义的行为。
 */
interface DefinitionResolver
{
    /**
     * 解析 bean 定义，返回解析后的对象
     *
     * @param Definition $definition 要解析的定义。
     * @param array $parameters 额外的参数，可以用于解析时的自定义设置。
     * @return mixed 返回解析后的对象。
     */
    public function resolve(Definition $definition, array $parameters = []): mixed;

    /**
     * 检查是否能够解析给定的定义。
     *
     * @param Definition $definition 要检查的定义。
     * @param array $parameters 额外的参数，可以用于解析时的自定义设置。
     * @return bool 如果可以解析，返回 true；否则返回 false。
     */
    public function isResolvable(Definition $definition, array $parameters = []): bool;
}
