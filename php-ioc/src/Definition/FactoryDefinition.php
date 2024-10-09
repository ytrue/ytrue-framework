<?php


namespace Ioc\Definition;

use Override;

/**
 * 工厂定义类
 *
 * 该类用于表示一个工厂定义，包括工厂名称、工厂函数或数组、参数等信息。
 * 它实现了 Definition 接口，并提供了相应的 getter 和 setter 方法。
 */
class FactoryDefinition implements Definition
{
    private string $name;

    /** @var callable|array|string $factory 工厂回调函数或方法 */
    private $factory;

    private array $parameters;

    /**
     * 构造函数
     *
     * callable 是一种泛型类型，可以代表任何可以被调用的内容。它可以是以下几种形式：
     *
     * 匿名函数（Closure）
     * 函数名字符串：例如 'functionName'
     * 对象方法数组：例如 [$object, 'methodName']
     * 静态类方法数组：例如 ['ClassName', 'staticMethodName']
     * 魔术方法 __invoke 的类实例：例如，任何实现了 __invoke() 方法的类实例。
     *
     * @param string $name 工厂名称
     * @param callable|array|string $factory 工厂回调函数、数组或字符串
     * @param array $parameters 传递给工厂的参数
     */
    public function __construct(string $name, callable|array|string $factory, array $parameters = [])
    {
        $this->name = $name;
        $this->factory = $factory;
        $this->parameters = $parameters;
    }

    /**
     * 获取工厂名称
     *
     * @return string 工厂名称
     */
    #[Override] public function getName(): string
    {
        return $this->name;
    }

    /**
     * 设置工厂名称
     *
     * @param string $name 要设置的工厂名称
     */
    #[Override] public function setName(string $name): void
    {
        $this->name = $name;
    }

    /**
     * 获取工厂
     *
     * @return callable|array|string 返回工厂回调函数、数组或字符串
     */
    public function getCallable(): callable|array|string
    {
        return $this->factory;
    }

    /**
     * 获取参数
     *
     * @return array 返回传递给工厂的参数
     */
    public function getParameters(): array
    {
        return $this->parameters;
    }

    /**
     * 替换嵌套的定义
     *
     * 该方法接受一个回调函数，用于替换参数中的嵌套定义。
     *
     * @param callable $replacer 替换回调函数
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 使用替换回调函数替换参数
        $this->parameters = array_map($replacer, $this->parameters);
    }

    /**
     * 将对象转换为字符串
     *
     * @return string 返回 'Factory' 字符串，表示该定义的类型
     */
    #[Override] public function __toString(): string
    {
        return 'Factory';
    }
}
