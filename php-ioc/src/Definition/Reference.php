<?php

namespace Ioc\Definition;

use Override;
use Psr\Container\ContainerExceptionInterface;
use Psr\Container\ContainerInterface;
use Psr\Container\NotFoundExceptionInterface;

/**
 * 引用定义类
 *
 * 该类用于表示对其他定义的引用，包括目标条目的名称和解析该条目的方法。
 * 它实现了 Definition 接口，并提供了相应的 getter 和 setter 方法。
 */
class Reference implements Definition, SelfResolvingDefinition
{
    private string $name = '';

    /**
     * 构造函数
     *
     * @param string $targetEntryName 目标条目的名称
     */
    public function __construct(
        private readonly string $targetEntryName,
    )
    {
    }

    /**
     * 获取名称
     *
     * @return string 返回引用的名称
     */
    #[Override] public function getName(): string
    {
        return $this->name;
    }

    /**
     * 设置名称
     *
     * @param string $name 要设置的名称
     */
    #[Override] public function setName(string $name): void
    {
        $this->name = $name;
    }

    /**
     * 获取目标条目的名称
     *
     * @return string 返回目标条目的名称
     */
    public function getTargetEntryName(): string
    {
        return $this->targetEntryName;
    }

    /**
     * 解析目标条目
     *
     * @param ContainerInterface $container 容器实例
     * @return mixed 返回解析得到的目标条目
     * @throws NotFoundExceptionInterface 如果目标条目未找到
     * @throws ContainerExceptionInterface 如果容器发生错误
     */
    #[Override] public function resolve(ContainerInterface $container): mixed
    {
        return $container->get($this->getTargetEntryName());
    }

    /**
     * 检查目标条目是否可解析
     *
     * @param ContainerInterface $container 容器实例
     * @return bool 如果目标条目存在，则返回 true；否则返回 false
     */
    #[Override] public function isResolvable(ContainerInterface $container): bool
    {
        return $container->has($this->getTargetEntryName());
    }

    /**
     * 替换嵌套的定义
     *
     * 该方法当前未实现，留待将来使用。
     *
     * @param callable $replacer 替换回调函数
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 当前未实现任何替换逻辑
    }

    /**
     * 将对象转换为字符串
     *
     * @return string 返回格式化的字符串，表示该引用的目标条目名称
     */
    #[Override] public function __toString(): string
    {
        return sprintf(
            'get(%s)',
            $this->targetEntryName
        );
    }
}
