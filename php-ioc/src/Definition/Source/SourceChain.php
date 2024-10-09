<?php

namespace Ioc\Definition\Source;


use Ioc\Definition\Definition;
use Ioc\Definition\ExtendsPreviousDefinition;
use Override;

/**
 * SourceChain 类用于管理多个定义源的链。它允许通过链条依次搜索依赖定义，并支持扩展之前的定义。
 */
class SourceChain implements DefinitionSource, MutableDefinitionSource
{
    /**
     * @var MutableDefinitionSource|null  可变定义源，用于添加新的定义
     */
    private ?MutableDefinitionSource $mutableSource;

    /**
     * 构造函数
     *
     * @param array<DefinitionSource> $sources 定义源的列表。
     */
    public function __construct(
        private array $sources,
    )
    {
    }

    /**
     * 获取指定名称的定义。
     *
     * @param string $name 定义名称。
     * @param int $startIndex 从定义源链的哪个索引开始查找，默认为 0。
     * @return Definition|null 如果找到定义则返回，否则返回 null。
     */
    #[Override] public function getDefinition(string $name, int $startIndex = 0): null|Definition
    {
        // 默认这里会有两个  [DefinitionArray,ReflectionBasedAutowiring]
        $count = count($this->sources);

        for ($i = $startIndex; $i < $count; ++$i) {
            /** @var DefinitionSource $source */
            $source = $this->sources[$i];

            // 从当前源中获取定义
            $definition = $source->getDefinition($name);

            // 一般不会走这个
            if ($definition) {
                // 如果定义是扩展的定义，解析它
                if ($definition instanceof ExtendsPreviousDefinition) {
                    $this->resolveExtendedDefinition($definition, $i);
                }

                return $definition;
            }
        }


        return null;
    }

    /**
     * 获取所有定义。
     *
     * @return array 所有定义的数组，键为定义名称，值为定义。
     */
    #[Override]   public function getDefinitions(): array
    {
        // 合并所有源的定义
        $allDefinitions = array_merge(...array_map(fn($source) => $source->getDefinitions(), $this->sources));

        // 获取所有定义的名称
        /** @var string[] $allNames */
        $allNames = array_keys($allDefinitions);

        // 过滤并获取所有定义值
        $allValues = array_filter(array_map(fn($name) => $this->getDefinition($name), $allNames));

        // 返回定义名称和值的组合
        return array_combine($allNames, $allValues);
    }

    /**
     * 添加定义到可变定义源。
     *
     * @param Definition $definition 要添加的定义。
     * @throws \LogicException 如果没有初始化可变定义源。
     */
    #[Override] public function addDefinition(Definition $definition): void
    {
        if (!$this->mutableSource) {
            throw new \LogicException("容器的定义源未正确初始化");
        }

        $this->mutableSource->addDefinition($definition);
    }

    /**
     * 解析扩展的定义，将它与后续的定义进行合并。
     *
     * @param ExtendsPreviousDefinition $definition 当前定义。
     * @param int $currentIndex 当前定义源的索引。
     */
    private function resolveExtendedDefinition(ExtendsPreviousDefinition $definition, int $currentIndex): void
    {
        // 仅查找后续的源，避免无限递归
        $subDefinition = $this->getDefinition($definition->getName(), $currentIndex + 1);

        if ($subDefinition) {
            $definition->setExtendedDefinition($subDefinition);
        }
    }

    /**
     * 设置可变定义源，并将其添加到源链的起始位置。
     *
     * @param MutableDefinitionSource $mutableSource 可变定义源。
     */
    public function setMutableDefinitionSource(MutableDefinitionSource $mutableSource): void
    {
        $this->mutableSource = $mutableSource;

        // 将可变源插入源链的开头
        array_unshift($this->sources, $mutableSource);
    }
}
