<?php

namespace Ioc\Definition;

use Override;

/**
 * 装饰器定义类。
 *
 * 该类用于定义装饰器，装饰器是对现有对象的增强或修改。
 *
 * @api
 */
class DecoratorDefinition extends FactoryDefinition implements Definition, ExtendsPreviousDefinition
{
    /**
     * 被装饰的定义。
     *
     * @var Definition|null
     */
    private ?Definition $decorated = null;

    /**
     * 设置扩展定义。
     *
     * @param Definition $definition 要被装饰的定义。
     */
    #[Override] public function setExtendedDefinition(Definition $definition): void
    {
        $this->decorated = $definition;
    }

    /**
     * 获取被装饰的定义。
     *
     * @return Definition|null 返回被装饰的定义，如果没有装饰则返回 null。
     */
    public function getDecoratedDefinition(): ?Definition
    {
        return $this->decorated;
    }

    /**
     * 替换嵌套定义。
     *
     * 本类没有嵌套定义，因此此方法为空实现。
     *
     * @param callable $replacer 用于替换定义的回调函数。
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 无嵌套定义
    }

    /**
     * 将对象转换为字符串。
     *
     * @return string 返回装饰器的字符串表示形式。
     */
    public function __toString(): string
    {
        return 'Decorate(' . $this->getName() . ')';
    }
}
