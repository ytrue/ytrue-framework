<?php

namespace Ioc;

use Exception;
use Invoker\Exception\NotCallableException;
use Invoker\Exception\NotEnoughParametersException;
use Invoker\Invoker;
use Invoker\InvokerInterface;
use Invoker\ParameterResolver\AssociativeArrayResolver;
use Invoker\ParameterResolver\DefaultValueResolver;
use Invoker\ParameterResolver\NumericArrayResolver;
use Invoker\ParameterResolver\ResolverChain;
use Ioc\Compiler\RequestedEntryHolder;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Invoker\FactoryParameterResolver;
use LogicException;
use Override;

class CompiledContainer extends Container
{

    /**
     * 此常量在子类（编译后的容器类）中被覆盖，定义了依赖项与生成方法的映射关系。
     * @var array
     */
    protected const array METHOD_MAPPING = [];

    /**
     * 工厂调用器，用于调用工厂方法。
     *
     * @var InvokerInterface|null
     */
    private ?InvokerInterface $factoryInvoker = null;


    /**
     * 获取容器中的依赖项。
     *
     * 首先检查依赖项是否已经解析过，如果已解析则直接返回，否则检查是否为编译时定义的依赖项，
     * 如果是，则调用对应的方法进行解析并缓存结果。
     *
     * @param string $id 依赖项的唯一标识符。
     * @return mixed 返回解析后的依赖项。
     * @throws DependencyException 当检测到循环依赖时抛出异常。
     * @throws Exception
     */
    #[Override] public function get(string $id): mixed
    {
        // 尝试从已解析的单例依赖项中获取
        if (isset($this->resolvedEntries[$id]) || array_key_exists($id, $this->resolvedEntries)) {
            return $this->resolvedEntries[$id];
        }

        /** @psalm-suppress UndefinedConstant */
        // 检查是否有编译时定义的依赖项
        $method = static::METHOD_MAPPING[$id] ?? null;

        // 如果找到编译时定义的依赖项
        if ($method !== null) {
            // 检查是否正在解析此依赖项（防止循环依赖）
            if (isset($this->entriesBeingResolved[$id])) {
                $idList = implode(" -> ", [...array_keys($this->entriesBeingResolved), $id]);
                throw new DependencyException("检测到循环依赖，解析依赖项 '$id' 时出现问题: 依赖链: " . $idList);
            }
            // 标记此依赖项正在解析
            $this->entriesBeingResolved[$id] = true;

            try {
                // 调用编译时生成的方法解析依赖项
                $value = $this->$method();
            } finally {
                // 解析完成后移除标记
                unset($this->entriesBeingResolved[$id]);
            }

            // 缓存解析后的依赖项，避免重复解析
            $this->resolvedEntries[$id] = $value;

            return $value;
        }

        // 如果不是编译项，则调用父类的方法进行处理
        return parent::get($id);
    }


    /**
     * 检查容器中是否存在指定的依赖项。
     *
     * 优化了父类方法，通过检查编译时的映射数组，避免了解析定义。
     *
     * @param string $id 依赖项的唯一标识符。
     * @return bool 返回依赖项是否存在。
     */
    #[Override] public function has(string $id): bool
    {
        /** @psalm-suppress UndefinedConstant */
        // 检查是否有编译时定义的依赖项
        if (isset(static::METHOD_MAPPING[$id])) {
            return true;
        }

        // 如果没有找到编译项，则调用父类的方法
        return parent::has($id);
    }


    /**
     * 设置依赖项的定义。
     *
     * 由于编译后的容器的性能优化，此方法被禁用。因为动态设置定义会导致每次获取依赖时
     * 都需要重新解析，这与编译优化的初衷相悖。
     *
     * @param string $name 依赖项的名称。
     * @param Definition $definition 依赖项的定义。
     * @throws LogicException 当尝试在编译后的容器上设置定义时抛出异常。
     */
    #[Override] protected function setDefinition(string $name, Definition $definition): void
    {
        // 禁止在运行时设置定义
        throw new LogicException('无法在运行时为已编译的容器设置定义。您可以将定义放在文件中，禁用编译，或者直接通过 ->set() 方法设置原始值（PHP 对象、字符串、整数等），而不是 PHP-DI 定义。');
    }


    /**
     * 调用指定的工厂方法。
     *
     * 该方法用于解析工厂依赖项，支持传递额外的参数，并利用参数解析器解析参数。
     *
     * @param callable $callable 工厂方法。
     * @param string $entryName 依赖项的名称。
     * @param array $extraParameters 额外的参数。
     * @return mixed 返回工厂方法的调用结果。
     * @throws Exception 当工厂方法不可调用或参数不足时抛出异常。
     */
    protected function resolveFactory(callable $callable, string $entryName, array $extraParameters = []): mixed
    {
        // 如果工厂调用器尚未初始化，进行初始化
        if (!$this->factoryInvoker) {
            // 使用参数解析链来解析工厂方法的参数
            $parameterResolver = new ResolverChain([
                new AssociativeArrayResolver, // 通过关联数组解析参数
                new FactoryParameterResolver($this->delegateContainer), // 通过委派容器解析参数
                new NumericArrayResolver, // 通过数字数组解析参数
                new DefaultValueResolver, // 使用默认值解析器处理参数
            ]);

            // 初始化工厂调用器
            $this->factoryInvoker = new Invoker($parameterResolver, $this->delegateContainer);
        }

        // 将容器和请求的依赖项包装器作为参数传递
        $parameters = [$this->delegateContainer, new RequestedEntryHolder($entryName)];

        // 合并额外参数
        $parameters = array_merge($parameters, $extraParameters);

        try {
            // 调用工厂方法
            return $this->factoryInvoker->call($callable, $parameters);
        } catch (NotCallableException $e) {
            // 工厂方法不可调用时抛出异常
            throw new InvalidDefinition("依赖项 \"$entryName\" 无法解析: 工厂 " . $e->getMessage());
        } catch (NotEnoughParametersException $e) {
            // 参数不足时抛出异常
            throw new InvalidDefinition("依赖项 \"$entryName\" 无法解析: " . $e->getMessage());
        }
    }
}
