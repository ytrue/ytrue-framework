<?php

namespace Ioc\Definition\Resolver;

use Ioc\Definition\ArrayDefinition;
use Ioc\Definition\DecoratorDefinition;
use Ioc\Definition\Definition;
use Ioc\Definition\EnvironmentVariableDefinition;
use Ioc\Definition\FactoryDefinition;
use Ioc\Definition\InstanceDefinition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\SelfResolvingDefinition;
use Ioc\Proxy\ProxyFactory;
use Override;
use Psr\Container\ContainerInterface;
use RuntimeException;

class ResolverDispatcher implements DefinitionResolver
{
    // 各种解析器的实例，使用延迟初始化
    private ?ArrayResolver $arrayResolver = null;
    private ?FactoryResolver $factoryResolver = null;
    private ?DecoratorResolver $decoratorResolver = null;
    private ?ObjectCreator $objectResolver = null;
    private ?InstanceInjector $instanceResolver = null;
    private ?EnvironmentVariableResolver $envVariableResolver = null;

    /**
     * ResolverDispatcher 构造函数
     *
     * @param ContainerInterface $container 依赖注入容器实例
     * @param ProxyFactory $proxyFactory 用于创建代理的工厂
     */
    public function __construct(
        private readonly ContainerInterface $container,
        private readonly ProxyFactory       $proxyFactory,
    )
    {
    }

    /**
     * 解析给定的定义并返回相应的值
     *
     * @param Definition $definition 要解析的定义对象
     * @param array $parameters 传递给解析器的参数
     *
     * @return mixed 解析得到的值
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): mixed
    {
        // 如果定义是自解析定义，直接调用其解析方法
        if ($definition instanceof SelfResolvingDefinition) {
            return $definition->resolve($this->container);
        }


        // 获取适当的定义解析器
        $definitionResolver = $this->getDefinitionResolver($definition);

        // 使用找到的解析器解析定义
        return $definitionResolver->resolve($definition, $parameters);
    }

    /**
     * 判断给定的定义是否可解析
     *
     * @param Definition $definition 要检查的定义对象
     * @param array $parameters 传递给解析器的参数
     *
     * @return bool 返回是否可解析
     */
    #[Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        // 如果定义是自解析定义，直接调用其可解析性检查方法
        if ($definition instanceof SelfResolvingDefinition) {
            return $definition->isResolvable($this->container);
        }

        // 获取适当的定义解析器
        $definitionResolver = $this->getDefinitionResolver($definition);

        // 使用找到的解析器检查定义的可解析性
        return $definitionResolver->isResolvable($definition, $parameters);
    }

    /**
     * 根据定义的类型返回相应的定义解析器
     *
     * @param Definition $definition 要解析的定义对象
     *
     * @return DefinitionResolver 对应的定义解析器
     *
     * @throws RuntimeException 如果没有找到合适的定义解析器
     */
    private function getDefinitionResolver(Definition $definition): DefinitionResolver
    {
        switch (true) {
            case $definition instanceof ObjectDefinition:
                // 初始化对象解析器
                if (!$this->objectResolver) {
                    $this->objectResolver = new ObjectCreator($this, $this->proxyFactory);
                }
                return $this->objectResolver;

            case $definition instanceof DecoratorDefinition:
                // 初始化装饰器解析器
                if (!$this->decoratorResolver) {
                    $this->decoratorResolver = new DecoratorResolver($this->container, $this);
                }
                return $this->decoratorResolver;

            case $definition instanceof FactoryDefinition:
                // 初始化工厂解析器
                if (!$this->factoryResolver) {
                    $this->factoryResolver = new FactoryResolver($this->container, $this);
                }
                return $this->factoryResolver;

            case $definition instanceof ArrayDefinition:
                // 初始化数组解析器
                if (!$this->arrayResolver) {
                    $this->arrayResolver = new ArrayResolver($this);
                }
                return $this->arrayResolver;

            case $definition instanceof EnvironmentVariableDefinition:
                // 初始化环境变量解析器
                if (!$this->envVariableResolver) {
                    $this->envVariableResolver = new EnvironmentVariableResolver($this);
                }
                return $this->envVariableResolver;

            case $definition instanceof InstanceDefinition:
                // 初始化实例注入器
                if (!$this->instanceResolver) {
                    $this->instanceResolver = new InstanceInjector($this, $this->proxyFactory);
                }
                return $this->instanceResolver;

            default:
                // 如果没有找到合适的解析器，抛出异常
                throw new RuntimeException('No definition resolver was configured for definition of type ' . $definition::class);
        }
    }
}
