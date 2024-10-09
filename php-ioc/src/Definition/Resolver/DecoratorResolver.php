<?php

namespace Ioc\Definition\Resolver;

use Ioc\Definition\DecoratorDefinition;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use Override;
use Psr\Container\ContainerInterface;

/**
 * 解析装饰器定义为一个值。
 *
 * @template-implements DefinitionResolver<DecoratorDefinition>
 */
class DecoratorResolver implements DefinitionResolver
{
    /**
     * 解析器需要一个容器。该容器会作为参数传递给工厂方法，
     * 这样工厂可以访问容器中的其他条目。
     *
     * @param ContainerInterface $container 用于在解析过程中访问依赖项的容器
     * @param DefinitionResolver $definitionResolver 用于解析嵌套的定义
     */
    public function __construct(
        private ContainerInterface          $container,
        private readonly DefinitionResolver $definitionResolver
    )
    {
    }

    /**
     * 解析装饰器定义为一个值。
     *
     * 该方法会调用定义中的可调用项，并将被装饰的条目作为参数传递给它。
     *
     * @param DecoratorDefinition $definition 要解析的装饰器定义
     * @param array $parameters 传递的参数（默认空数组）
     * @return mixed 解析后的值
     * @throws InvalidDefinition 如果定义中的 callable 不是可调用的或装饰器定义无效，则抛出异常
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): mixed
    {
        // 获取定义中的可调用项
        $callable = $definition->getCallable();

        // 检查是否是有效的可调用项
        if (!is_callable($callable)) {
            throw new InvalidDefinition(sprintf(
                '装饰器 "%s" 不是可调用的',
                $definition->getName()
            ));
        }

        // 获取被装饰的定义
        $decoratedDefinition = $definition->getDecoratedDefinition();

        // 检查被装饰的定义是否是有效的定义实例
        if (!$decoratedDefinition instanceof Definition) {
            // 如果名称为空，抛出异常，因为装饰器不能嵌套在其他定义中
            if (!$definition->getName()) {
                throw new InvalidDefinition('装饰器不能嵌套在其他定义中');
            }

            // 如果没有找到被装饰的定义，抛出异常
            throw new InvalidDefinition(sprintf(
                '条目 "%s" 没有装饰任何内容：未找到具有相同名称的先前定义',
                $definition->getName()
            ));
        }

        // 解析被装饰的定义
        $decorated = $this->definitionResolver->resolve($decoratedDefinition, $parameters);

        //         $decoratorDefinition = new DecoratorDefinition(
        //            'test',
        //            function ($objDef,$ioc) {
        //                print_r($objDef);
        //                print_r($ioc);
        //            },
        //            []
        //        );
        //
        //        $decoratorDefinition->setExtendedDefinition($objectDefinition);
        //        $decoratorResolver = new DecoratorResolver($container,$resolverDispatcher);
        //        $decoratorResolver->resolve($decoratorDefinition);

        // 调用装饰器的可调用项并返回结果,其实这里就是装饰器增强
        return $callable($decorated, $this->container);
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
