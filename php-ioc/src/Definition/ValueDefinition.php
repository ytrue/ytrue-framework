<?php

namespace Ioc\Definition;

use Override;
use Psr\Container\ContainerInterface;

/**
 * Class ValueDefinition
 *
 * 该类用于定义一个包含具体值的定义。它实现了 Definition 和 SelfResolvingDefinition 接口。
 * ValueDefinition 的实例可用于表示简单的值，例如数字、字符串、布尔值或其他任何类型的值。
 */
class ValueDefinition implements Definition, SelfResolvingDefinition
{
    private string $name = ''; // 定义的名称

    /**
     * ValueDefinition 构造函数
     *
     * @param mixed $value 需要存储的值
     */
    public function __construct(
        private readonly mixed $value, // 存储的值，支持任意类型
    )
    {
    }

    /**
     * 获取定义的名称
     *
     * @return string 返回定义的名称
     */
    #[Override] public function getName(): string
    {
        return $this->name; // 返回当前名称
    }

    /**
     * 设置定义的名称
     *
     * @param string $name 新的名称
     */
    #[Override] public function setName(string $name): void
    {
        $this->name = $name; // 设置名称
    }

    /**
     * 获取存储的值
     *
     * @return mixed 返回存储的值
     */
    public function getValue(): mixed
    {
        return $this->value; // 返回存储的值
    }

    /**
     * 解析该定义并返回值
     *
     * @param ContainerInterface $container 容器实例
     * @return mixed 返回存储的值
     */
    #[Override] public function resolve(ContainerInterface $container): mixed
    {
        return $this->getValue(); // 直接返回存储的值
    }

    /**
     * 检查该定义是否可解析
     *
     * @param ContainerInterface $container 容器实例
     * @return bool 始终返回 true，因为 ValueDefinition 总是可以解析
     */
    #[Override] public function isResolvable(ContainerInterface $container): bool
    {
        return true; // 该定义始终可解析
    }

    /**
     * 替换嵌套定义
     *
     * @param callable $replacer 替换器，未使用
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 此方法未实现具体逻辑，因为 ValueDefinition 只包含简单值
    }

    /**
     * 将对象转换为字符串
     *
     * @return string 返回格式化的字符串表示
     */
    #[Override] public function __toString(): string
    {
        return sprintf('Value (%s)', var_export($this->value, true)); // 返回值的字符串表示
    }
}
