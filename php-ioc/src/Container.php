<?php

namespace Ioc;

use Exception;
use Invoker\Exception\InvocationException;
use Invoker\Exception\NotCallableException;
use Invoker\Exception\NotEnoughParametersException;
use Invoker\Invoker;
use Invoker\InvokerInterface;
use Invoker\ParameterResolver\AssociativeArrayResolver;
use Invoker\ParameterResolver\Container\TypeHintContainerResolver;
use Invoker\ParameterResolver\DefaultValueResolver;
use Invoker\ParameterResolver\NumericArrayResolver;
use Invoker\ParameterResolver\ResolverChain;
use Ioc\Definition\Definition;
use Ioc\Definition\FactoryDefinition;
use Ioc\Definition\Helper\DefinitionHelper;
use Ioc\Definition\InstanceDefinition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\Resolver\ResolverDispatcher;
use Ioc\Definition\Source\DefinitionArray;
use Ioc\Definition\Source\MutableDefinitionSource;
use Ioc\Definition\Source\ReflectionBasedAutowiring;
use Ioc\Definition\Source\SourceChain;
use Ioc\Definition\ValueDefinition;
use Ioc\Invoker\DefinitionParameterResolver;
use Ioc\Proxy\ProxyFactory;
use Override;
use Psr\Container\ContainerInterface;

/**
 * 依赖注入容器实现，负责管理和解析依赖项。
 *
 * 该类实现了 FactoryInterface、ContainerInterface 和 InvokerInterface。
 * 它允许通过名称解析、注册、获取依赖项，并能够处理自动装配、代理对象和工厂方法。
 */
class Container implements FactoryInterface, ContainerInterface, InvokerInterface
{
    /**
     * 存储已经解析过的条目。
     * @var array
     */
    protected array $resolvedEntries = [];

    /**
     * 存储已获取的定义。
     * @var array
     */
    private array $fetchedDefinitions = [];

    /**
     * 正在解析的条目，避免循环依赖。
     * @var array
     */
    protected array $entriesBeingResolved = [];

    /**
     * 定义源，用于获取条目的定义。
     * @var MutableDefinitionSource
     */
    private MutableDefinitionSource $definitionSource;

    /**
     * 定义解析器，负责解析定义为实际的对象。
     * @var ResolverDispatcher
     */
    private ResolverDispatcher $definitionResolver;

    /**
     * 容器委托，用于处理依赖解析。
     * @var ContainerInterface
     */
    protected ContainerInterface $delegateContainer;

    /**
     * 代理工厂，用于创建代理对象。
     * @var ProxyFactory
     */
    protected ProxyFactory $proxyFactory;

    /**
     * 调用器，用于调用可调用对象并解析其依赖项。
     * @var InvokerInterface|null
     */
    private ?InvokerInterface $invoker = null;

    /**
     * 静态方法，用于创建容器实例并传入定义。
     *
     * @param array $definitions 定义数组
     * @return static 容器实例
     * @throws Exception
     */
    public static function create(array $definitions): static
    {
        // 创建一个定义源链
        $source = new SourceChain([new ReflectionBasedAutowiring]);
        // 设置可变定义源，使用传入的定义数组和反射自动装配
        $source->setMutableDefinitionSource(new DefinitionArray($definitions, new ReflectionBasedAutowiring));

        // 返回容器实例
        return new static($definitions);
    }

    /**
     * 创建默认的定义源。
     *
     * @param array $definitions 定义数组
     * @return SourceChain 定义源链
     * @throws Exception
     */
    private function createDefaultDefinitionSource(array $definitions): SourceChain
    {
        // 使用反射机制创建自动装配定义源
        $autowiring = new ReflectionBasedAutowiring();
        // 创建定义源链，并添加自动装配
        $source = new SourceChain([$autowiring]);
        // 设置可变定义源，使用传入的定义数组
        $source->setMutableDefinitionSource(new DefinitionArray($definitions, $autowiring));

        // 返回定义源链
        return $source;
    }

    /**
     * 容器构造函数。
     *
     * @param array|MutableDefinitionSource $definitions 定义数组或定义源
     * @param ProxyFactory|null $proxyFactory 代理工厂
     * @param ContainerInterface|null $wrapperContainer 包装容器
     * @throws Exception
     */
    public function __construct(
        array|MutableDefinitionSource $definitions = [],
        ?ProxyFactory                 $proxyFactory = null,
        ?ContainerInterface           $wrapperContainer = null
    )
    {
        // 如果传入的是数组，则创建默认的定义源
        if (is_array($definitions)) {
            $this->definitionSource = $this->createDefaultDefinitionSource($definitions);
        } else {
            // 如果传入的是定义源，则直接使用
            $this->definitionSource = $definitions;
        }

        // 设置委托容器，默认为自身
        $this->delegateContainer = $wrapperContainer ?: $this;
        // 设置代理工厂，如果未传入则使用默认的 ProxyFactory
        $this->proxyFactory = $proxyFactory ?: new ProxyFactory;
        // 初始化定义解析器，传入委托容器和代理工厂
        $this->definitionResolver = new ResolverDispatcher($this->delegateContainer, $this->proxyFactory);

        // 自动注册容器自身，存储一些默认的条目
        $this->resolvedEntries = [
            self::class => $this, // 容器类
            ContainerInterface::class => $this->delegateContainer, // 容器接口
            FactoryInterface::class => $this, // 工厂接口
            InvokerInterface::class => $this, // 调用器接口
        ];
    }

    /**
     * 根据名称获取实例。
     *
     * @param string $name 实例名称
     * @param array $parameters 可选的参数
     * @return mixed 实例对象
     * @throws Exception 如果没有找到对应的定义则抛出异常
     */
    #[Override] public function make(string $name, array $parameters = []): mixed
    {
        // 获取条目的定义
        $definition = $this->getDefinition($name);
        if (!$definition) {
            // 如果已经解析过该条目，则直接返回解析的条目
            if (array_key_exists($name, $this->resolvedEntries)) {
                return $this->resolvedEntries[$name];
            }
            // 如果没有找到定义，则抛出异常
            throw new NotFoundException("No entry or class found for '$name'");
        }

        // 解析定义并返回实例
        return $this->resolveDefinition($definition, $parameters);
    }

    /**
     * 解析定义并返回实例。
     *
     * @param Definition $definition 定义对象
     * @param array $parameters 传递的参数
     * @return mixed 解析后的实例
     * @throws DependencyException
     */
    private function resolveDefinition(Definition $definition, array $parameters = []): mixed
    {
        // 获取条目的名称
        $entryName = $definition->getName();

        // 检查该条目是否已经在解析，防止循环依赖
        if (isset($this->entriesBeingResolved[$entryName])) {
            // 如果检测到循环依赖，则抛出异常，显示依赖链
            $entryList = implode(" -> ", [...array_keys($this->entriesBeingResolved), $entryName]);
            throw new DependencyException("Circular dependency detected while trying to resolve entry '$entryName': Dependencies: " . $entryList);
        }
        // 将该条目标记为正在解析
        $this->entriesBeingResolved[$entryName] = true;

        try {
            // 使用定义解析器解析定义并获取结果
            $value = $this->definitionResolver->resolve($definition, $parameters);
        } finally {
            // 解析完成后，移除该条目的解析标记
            unset($this->entriesBeingResolved[$entryName]);
        }

        // 返回解析后的实例
        return $value;
    }

    /**
     * 获取指定名称的定义。
     *
     * @param string $name 名称
     * @return Definition|null 返回定义，如果未找到则返回 null
     */
    private function getDefinition(string $name): ?Definition
    {
        // 如果该条目定义未被获取过，则从定义源中获取
        if (!array_key_exists($name, $this->fetchedDefinitions)) {
            $this->fetchedDefinitions[$name] = $this->definitionSource->getDefinition($name);
        }
        // 返回已获取的定义
        return $this->fetchedDefinitions[$name];
    }

    /**
     * 从容器中获取对象。
     *
     * @param string $id 对象标识符
     * @return mixed 对象实例
     * @throws Exception 如果未找到对象则抛出异常
     */
    #[Override] public function get(string $id): mixed
    {
        // 如果该条目已经解析过，直接返回
        if (isset($this->resolvedEntries[$id]) || array_key_exists($id, $this->resolvedEntries)) {
            return $this->resolvedEntries[$id];
        }

        // 获取该条目的定义
        $definition = $this->getDefinition($id);
        if (!$definition) {
            // 如果没有定义，抛出未找到异常
            throw new NotFoundException("No entry or class found for '$id'");
        }

        // 解析定义并返回实例
        $value = $this->resolveDefinition($definition);

        // 缓存结果
        $this->resolvedEntries[$id] = $value;

        return $value;
    }

    /**
     * 检查容器中是否有指定的条目。
     *
     * @param string $id 条目标识符
     * @return bool 如果条目存在返回 true，否则返回 false
     */
    #[Override] public function has(string $id): bool
    {
        // 检查该条目是否已经解析过
        if (array_key_exists($id, $this->resolvedEntries)) {
            return true;
        }
        // 检查是否有该条目的定义
        $definition = $this->getDefinition($id);
        if ($definition === null) {
            return false;
        }
        // 是否能解析
        return $this->definitionResolver->isResolvable($definition);
    }

    /**
     * 调用可调用对象，并自动注入依赖。
     *
     * @param callable|string|array $callable 要调用的函数或方法
     * @param array $parameters 可选的参数
     * @return mixed 调用结果
     * @throws InvocationException
     * @throws NotCallableException
     * @throws NotEnoughParametersException
     */
    #[Override] public function call($callable, array $parameters = []): mixed
    {
        return $this->getInvoker()->call($callable, $parameters);
    }


    /**
     * 获取或创建调用器。
     *
     * @return InvokerInterface 调用器实例
     */
    private function getInvoker(): InvokerInterface
    {
        if (!$this->invoker) {
            $parameterResolver = new ResolverChain([
                new DefinitionParameterResolver($this->definitionResolver),
                new NumericArrayResolver,
                new AssociativeArrayResolver,
                new DefaultValueResolver,
                new TypeHintContainerResolver($this->delegateContainer),
            ]);

            $this->invoker = new Invoker($parameterResolver, $this);
        }

        return $this->invoker;
    }


    /**
     * 设置容器中的条目。
     *
     * @param string $name 条目名称
     * @param mixed $value 条目值
     */
    public function set(string $name, mixed $value): void
    {
        if ($value instanceof DefinitionHelper) {
            // 如果是定义帮助类，转换为定义
            $value = $value->getDefinition($name);
        } else if ($value instanceof \Closure) {
            // 如果是闭包，转换为工厂定义
            $value = new FactoryDefinition($name, $value);
        }

        if ($value instanceof ValueDefinition) {
            // 如果是值定义，直接获取值
            $this->resolvedEntries[$name] = $value->getValue();
        } else if ($value instanceof Definition) {
            // 设置定义名称并存储定义
            $value->setName($name);
            $this->setDefinition($name, $value);
        } else {
            $this->resolvedEntries[$name] = $value;
        }
    }


    /**
     * 获取容器中已知的条目名称。
     *
     * @return array 已知条目的名称列表
     */
    public function getKnownEntryNames(): array
    {
        $entries = array_unique(array_merge(
            array_keys($this->definitionSource->getDefinitions()),
            array_keys($this->resolvedEntries)
        ));
        sort($entries);

        return $entries;
    }

    /**
     * 调试指定条目，返回条目的字符串表示。
     *
     * @param string $name 条目名称
     * @return string 条目的调试信息
     * @throws Exception 如果未找到条目则抛出异常
     */
    public function debugEntry(string $name): string
    {
        $definition = $this->definitionSource->getDefinition($name);
        if ($definition instanceof Definition) {
            return (string)$definition;
        }

        if (array_key_exists($name, $this->resolvedEntries)) {
            return $this->getEntryType($this->resolvedEntries[$name]);
        }

        throw new NotFoundException("No entry or class found for '$name'");
    }

    /**
     * 返回条目的类型信息。
     *
     * @param mixed $entry 条目实例
     * @return string 条目类型的字符串表示
     */
    private function getEntryType(mixed $entry): string
    {
        if (is_object($entry)) {
            return sprintf("Object (\n    class = %s\n)", $entry::class);
        }

        if (is_array($entry)) {
            return preg_replace(['/^array \(/', '/\)$/'], ['[', ']'], var_export($entry, true));
        }

        if (is_string($entry)) {
            return sprintf('Value (\'%s\')', $entry);
        }

        if (is_bool($entry)) {
            return sprintf('Value (%s)', $entry === true ? 'true' : 'false');
        }

        return sprintf('Value (%s)', is_scalar($entry) ? (string)$entry : ucfirst(gettype($entry)));
    }

    /**
     * 设置定义并清除缓存。
     *
     * @param string $name 定义名称
     * @param Definition $definition 定义对象
     */
    private function setDefinition(string $name, Definition $definition): void
    {
        if (array_key_exists($name, $this->resolvedEntries)) {
            unset($this->resolvedEntries[$name]);
        }

        // 清除已获取定义的缓存
        $this->fetchedDefinitions = [];

        $this->definitionSource->addDefinition($definition);
    }

    /**
     * 注入依赖项到指定实例。
     *
     * @param object $instance 实例对象
     * @return object 返回注入后的实例
     */
    public function injectOn(object $instance): object
    {
        $className = $instance::class;

        // 匿名类不会缓存其定义
        $objectDefinition = str_contains($className, '@anonymous')
            ? $this->definitionSource->getDefinition($className)
            : $this->getDefinition($className);

        if (!$objectDefinition instanceof ObjectDefinition) {
            return $instance;
        }

        $definition = new InstanceDefinition($instance, $objectDefinition);

        // 解析定义并注入到实例中
        $this->definitionResolver->resolve($definition);

        return $instance;
    }

}

