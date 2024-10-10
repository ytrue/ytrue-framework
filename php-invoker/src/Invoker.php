<?php

namespace Invoker;

use Invoker\Exception\NotCallableException;
use Invoker\Exception\NotEnoughParametersException;
use Invoker\ParameterResolver\AssociativeArrayResolver;
use Invoker\ParameterResolver\DefaultValueResolver;
use Invoker\ParameterResolver\NumericArrayResolver;
use Invoker\ParameterResolver\ParameterResolver;
use Invoker\ParameterResolver\ResolverChain;
use Invoker\Reflection\CallableReflection;
use Override;
use Psr\Container\ContainerExceptionInterface;
use Psr\Container\ContainerInterface;
use Psr\Container\NotFoundExceptionInterface;
use ReflectionException;
use ReflectionParameter;
use function assert;

class Invoker implements InvokerInterface
{
    private ?CallableResolver $callableResolver;

    private ?ParameterResolver $parameterResolver;

    private ?ContainerInterface $container;

    /**
     * 调用指定的 callable，传递参数并返回结果。
     *
     * @param callable|array|string|object $callable |array|string $callable 要调用的 callable
     * @param array $parameters 调用时传递的参数
     * @return mixed 返回 callable 的执行结果
     * @throws ContainerExceptionInterface
     * @throws NotCallableException 如果 $callable 不是可调用的
     * @throws NotEnoughParametersException 如果提供的参数不足以满足 callable 的需求
     * @throws NotFoundExceptionInterface
     * @throws ReflectionException
     */
    #[Override]
    public function call(callable|array|string|object $callable, array $parameters = []): mixed
    {
        // 解析 callable
        if ($this->callableResolver) {
            $callable = $this->callableResolver->resolve($callable);
        }

        // 检查 callable 是否有效
        if (!is_callable($callable)) {
            throw new NotCallableException(sprintf(
                '%s is not a callable',
                is_object($callable) ? 'Instance of ' . get_class($callable) : var_export($callable, true)
            ));
        }

        // 创建 callable 的反射对象
        $callableReflection = CallableReflection::create($callable);

        // 获取解析后的参数
        $args = $this->parameterResolver->getParameters($callableReflection, $parameters, []);

        // 按数组键排序，因为 call_user_func_array 会忽略数字键
        ksort($args);

        // 检查所有参数是否都已解析
        $diff = array_diff_key($callableReflection->getParameters(), $args);
        $parameter = reset($diff);
        if ($parameter && assert($parameter instanceof ReflectionParameter) && !$parameter->isVariadic()) {
            throw new NotEnoughParametersException(sprintf(
                'Unable to invoke the callable because no value was given for parameter %d ($%s)',
                $parameter->getPosition() + 1,
                $parameter->name
            ));
        }

        // 调用 callable 并返回结果
        return call_user_func_array($callable, $args);
    }

    /**
     * 创建参数解析器的链。
     *
     * @return ParameterResolver 返回参数解析器
     */
    private function createParameterResolver(): ParameterResolver
    {
        return new ResolverChain([
            new NumericArrayResolver,
            new AssociativeArrayResolver,
            new DefaultValueResolver,
        ]);
    }

    /**
     * 获取当前的 CallableResolver 实例。
     *
     * @return CallableResolver|null 返回当前的 CallableResolver 实例或 null
     */
    public function getCallableResolver(): ?CallableResolver
    {
        return $this->callableResolver;
    }

    /**
     * 获取当前的 ParameterResolver 实例。
     *
     * @return ParameterResolver|null 返回当前的 ParameterResolver 实例或 null
     */
    public function getParameterResolver(): ?ParameterResolver
    {
        return $this->parameterResolver;
    }

    /**
     * 获取当前的 ContainerInterface 实例。
     *
     * @return ContainerInterface|null 返回当前的容器实例或 null
     */
    public function getContainer(): ?ContainerInterface
    {
        return $this->container;
    }
}
