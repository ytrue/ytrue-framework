<?php

namespace Ioc\Definition\Resolver;

use Exception;
use Ioc\Definition\Definition;
use Ioc\Definition\InstanceDefinition;
use Ioc\DependencyException;
use Override;
use Psr\Container\NotFoundExceptionInterface;

readonly class InstanceInjector extends ObjectCreator implements DefinitionResolver
{
    /**
     * 在现有实例上注入依赖项。
     *
     * @param InstanceDefinition $definition 要解析的实例定义
     * @param array $parameters 传递的额外参数（默认空数组）
     * @return object|null 返回注入依赖项后的对象实例
     * @throws Exception 如果在注入过程中出现依赖错误，抛出该异常
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): ?object
    {
        /** @psalm-suppress InvalidCatch */
        try {
            // 注入方法和属性的依赖项
            $this->injectMethodsAndProperties($definition->getInstance(), $definition->getObjectDefinition());
        } catch (NotFoundExceptionInterface $e) {
            // 如果未找到依赖项，构建错误信息并抛出异常
            $message = sprintf(
                '在 %s 中注入依赖项时发生错误：%s',
                get_class($definition->getInstance()),
                $e->getMessage()
            );

            throw new DependencyException($message, 0, $e);
        }

        // 返回处理后的定义
        return $definition;
    }

    /**
     * 检查定义是否可以被解析
     *
     * @param Definition $definition 要检查的定义
     * @param array $parameters 传递的参数（默认空数组）
     * @return bool 返回 true 表示可以解析
     */
    #[Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        return true;
    }
}
