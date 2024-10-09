<?php

namespace Ioc\Definition;

use Ioc\Definition\Exception\InvalidDefinition;
use Override;

/**
 * ArrayDefinitionExtension 类继承自 ArrayDefinition，并实现了 ExtendsPreviousDefinition 接口
 * 该类用于扩展一个现有的 ArrayDefinition，将其值与当前定义的值合并
 */
class ArrayDefinitionExtension extends ArrayDefinition implements ExtendsPreviousDefinition
{

    /**
     * 被扩展的 ArrayDefinition 定义
     * @var ArrayDefinition|null
     */
    private ?ArrayDefinition $subDefinition = null;

    /**
     * 获取当前定义的值数组，并与扩展定义的值进行合并
     *
     * @return array 返回合并后的值数组
     */
    #[Override] public function getValues(): array
    {
        // 如果没有扩展的定义，直接返回父类的值数组
        if (!$this->subDefinition) {
            return parent::getValues();
        }

        // 如果有扩展的定义，将扩展定义的值与当前定义的值进行合并
        return array_merge($this->subDefinition->getValues(), parent::getValues());
    }

    /**
     * 设置要扩展的定义
     *
     * @param Definition $definition 要扩展的定义
     *
     * @throws InvalidDefinition 如果传入的定义不是 ArrayDefinition，则抛出异常
     */
    #[Override] public function setExtendedDefinition(Definition $definition): void
    {
        // 检查传入的定义是否是 ArrayDefinition 类型
        if (!$definition instanceof ArrayDefinition) {
            throw new InvalidDefinition(sprintf(
                '定义 %s 尝试添加数组条目，但之前的定义不是数组类型',
                $this->getName()
            ));
        }

        // 设置扩展定义
        $this->subDefinition = $definition;
    }
}
