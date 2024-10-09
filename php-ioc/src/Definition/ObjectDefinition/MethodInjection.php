<?php

namespace Ioc\Definition\ObjectDefinition;

use Ioc\Definition\Definition;
use Override;

/**
 * Class MethodInjection
 *
 * 该类表示一个方法注入定义，用于在对象构造后调用特定方法并传递参数。
 * 它实现了 Definition 接口，并提供方法名称和参数的管理功能。
 */
class MethodInjection implements Definition
{

    /**
     * @var string 要注入的方法名称
     */
    private string $methodName;

    /**
     * @var  array 方法的参数
     */
    private array $parameters = [];

    /**
     * MethodInjection constructor.
     *
     * @param string $methodName 要注入的方法名称。
     * @param array $parameters 要传递给方法的参数。
     */
    public function __construct(string $methodName, array $parameters)
    {
        $this->methodName = $methodName;
        $this->parameters = $parameters;
    }

    /**
     * 创建一个表示构造函数注入的实例。
     *
     * @param array $parameters 要传递给构造函数的参数。
     * @return self 返回构造函数注入的实例。
     */
    public static function constructor(array $parameters = []): self
    {
        return new self('__construct', $parameters);
    }

    /**
     * 获取要注入的方法名称。
     *
     * @return string 返回方法名称。
     */
    public function getMethodName(): string
    {
        return $this->methodName;
    }

    /**
     * 获取方法的参数。
     *
     * @return array 返回参数数组。
     */
    public function getParameters(): array
    {
        return $this->parameters;
    }

    /**
     * 替换方法的参数。
     *
     * @param array $parameters 新的参数数组。
     */
    public function replaceParameters(array $parameters): void
    {
        $this->parameters = $parameters;
    }

    /**
     * 合并另一个 MethodInjection 实例的参数。
     *
     * @param self $definition 另一个 MethodInjection 实例。
     */
    public function merge(self $definition): void
    {
        $this->parameters += $definition->parameters; // 合并参数
    }

    /**
     * 获取名称，返回空字符串。
     *
     * @return string 返回空字符串。
     */
    #[Override] public function getName(): string
    {
        return '';
    }

    /**
     * 设置名称，当前实现不做任何处理。
     *
     * @param string $name 要设置的名称。
     */
    #[Override] public function setName(string $name): void
    {
        // 当前实现不做任何处理
    }

    /**
     * 替换嵌套定义中的参数。
     *
     * @param callable $replacer 用于替换参数的回调函数。
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        $this->parameters = array_map($replacer, $this->parameters);
    }

    /**
     * 获取类的字符串表示。
     *
     * @return string 返回方法注入的字符串表示。
     */
    #[Override] public function __toString(): string
    {
        return sprintf('method(%s)', $this->methodName);
    }
}
