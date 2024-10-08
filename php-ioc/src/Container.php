<?php

namespace Ioc;


use Invoker\InvokerInterface;
use Ioc\Definition\Definition;
use Ioc\Definition\Source\MutableDefinitionSource;
use Psr\Container\ContainerInterface;

class Container implements FactoryInterface, ContainerInterface, InvokerInterface
{

    /**
     * 已经解析的实体map
     */
    protected array $resolvedEntries = [];

    /**
     * bean定义map
     *
     * @var array<Definition|null>
     */
    private array $fetchedDefinitions = [];


    /**
     * bean定义容器
     * @var MutableDefinitionSource
     */
    private MutableDefinitionSource $definitionSource;

    /**
     * 创建对象
     * @param string $name
     * @param array $parameters
     * @return mixed
     * @throws NotFoundException
     */
    public function make(string $name, array $parameters = []): mixed
    {
        // 获取bean定义
        $definition = $this->getDefinition($name);
        // 如果没有就去resolvedEntries找
        if (!$definition) {
            // 如果条目已经解析，我们将其返回
            if (array_key_exists($name, $this->resolvedEntries)) {
                return $this->resolvedEntries[$name];
            }
            // 没有找到
            throw new NotFoundException("No entry or class found for '$name'");
        }

        // 根据bean定义解析出对象
        return $this->resolveDefinition($definition, $parameters);
    }


    /**
     * 获取bean定义
     * @param string $name
     * @return Definition|null
     */
    private function getDefinition(string $name): ?Definition
    {
        // 判断是否在fetchedDefinitions里有
        if (!array_key_exists($name, $this->fetchedDefinitions)) {
            // 尝试去bean容器获取
            $this->fetchedDefinitions[$name] = $this->definitionSource->getDefinition($name);
        }
        // 这是直接有的,直接根据index获取
        return $this->fetchedDefinitions[$name];
    }


    /**
     * 容器获取对象
     * @param string $id
     * @return mixed
     */
    public function get(string $id): mixed
    {

    }

    /**
     * 容器是否包含对象
     * @param string $id
     * @return bool
     */
    public function has(string $id): bool
    {
        // TODO: Implement has() method.
    }


    /**
     * 反射调用
     * @param $callable
     * @param array $parameters
     * @return void
     */
    public function call($callable, array $parameters = [])
    {
        // TODO: Implement call() method.
    }
}
