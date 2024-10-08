<?php

namespace Ioc\Definition;

use Psr\Container\ContainerInterface;

/**
 * 自解析定义接口
 *
 * 该接口用于定义自解析的条目，这些条目可以在容器中直接解析其依赖关系。
 * 它提供了用于解析和检查可解析性的方法。
 */
interface SelfResolvingDefinition
{
    /**
     * 解析当前定义
     *
     * @param ContainerInterface $container 容器实例
     * @return mixed 返回解析得到的条目
     */
    public function resolve(ContainerInterface $container): mixed;

    /**
     * 检查当前定义是否可解析
     *
     * @param ContainerInterface $container 容器实例
     * @return bool 如果定义可以在容器中解析，则返回 true；否则返回 false
     */
    public function isResolvable(ContainerInterface $container): bool;
}
