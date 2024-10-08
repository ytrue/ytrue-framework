<?php

namespace Ioc\Definition\Helper;

use Ioc\Definition\DecoratorDefinition;
use Ioc\Definition\Definition;
use Ioc\Definition\FactoryDefinition;

/**
 * 工厂定义助手类。
 *
 * 该类帮助创建工厂定义或装饰器定义，并提供设置工厂参数的功能。
 */
class FactoryDefinitionHelper implements DefinitionHelper
{
    /**
     * 工厂，可以是可调用的、数组或字符串。
     *
     * @var callable|array|string $factory
     */
    private $factory;

    /**
     * 是否使用装饰器。
     *
     * @var bool
     */
    private bool $decorate;

    /**
     * 传递给工厂的方法参数。
     *
     * @var array
     */
    private array $parameters = [];

    /**
     * 构造函数。
     *
     * @param callable|array|string $factory 工厂，接受可调用的、数组或字符串。
     * @param bool $decorate 是否使用装饰器，默认为 false。
     */
    public function __construct(callable|array|string $factory, bool $decorate = false)
    {
        $this->factory = $factory;
        $this->decorate = $decorate;
    }

    /**
     * 根据给定的入口名称获取定义。
     *
     * @param string $entryName 入口名称。
     * @return Definition 返回对应的定义，可以是工厂定义或装饰器定义。
     */
    public function getDefinition(string $entryName): Definition
    {
        if ($this->decorate) {
            return new DecoratorDefinition($entryName, $this->factory, $this->parameters);
        }

        return new FactoryDefinition($entryName, $this->factory, $this->parameters);
    }

    /**
     * 定义要传递给工厂的方法参数。
     *
     * 因为工厂方法尚不支持属性或自动装配，因此该方法应用于定义除 ContainerInterface 和 RequestedEntry 之外的所有参数。
     *
     * 可以多次调用此方法以覆盖单个值。
     *
     * @param string $parameter 参数的名称或索引。
     * @param mixed  $value     要为该参数提供的值。
     *
     * @return $this 返回当前实例，以便链式调用。
     */
    public function parameter(string $parameter, mixed $value) : self
    {
        $this->parameters[$parameter] = $value;

        return $this;
    }
}
