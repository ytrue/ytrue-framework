<?php

namespace Invoker;

use Closure;
use Invoker\Exception\NotCallableException;
use Psr\Container\ContainerExceptionInterface;
use Psr\Container\ContainerInterface;
use Psr\Container\NotFoundExceptionInterface;
use ReflectionMethod;

readonly class CallableResolver
{
    /**
     * 构造函数，接受一个实现了 ContainerInterface 的容器实例
     *
     * @param ContainerInterface $container 容器实例
     */
    public function __construct(private ContainerInterface $container)
    {
    }

    /**
     * 解析给定的 callable，返回一个可调用的实例。
     *
     * @param callable|string|array $callable 要解析的可调用
     * @return callable 返回可调用实例
     * @throws ContainerExceptionInterface
     * @throws NotCallableException 如果给定的 callable 不是可调用的
     * @throws NotFoundExceptionInterface
     */
    public function resolve(callable|string|array $callable): callable
    {
        // 如果 $callable 是字符串且包含 '::'，则将其分割为数组
        if (is_string($callable) && (str_contains($callable, '::'))) {
            $callable = explode('::', $callable, 2);
        }

        // 从容器中解析 callable
        $callable = $this->resolveFromContainer($callable);

        // 检查解析后的 callable 是否可调用
        if (!is_callable($callable)) {
            throw NotCallableException::fromInvalidCallable($callable, true);
        }
        return $callable;
    }

    /**
     * 从容器中解析 callable。
     *
     * @param array|callable|string $callable 要解析的可调用
     * @return mixed 返回解析后的可调用
     * @throws NotCallableException 如果无法解析 callable
     * @throws NotFoundExceptionInterface
     * @throws ContainerExceptionInterface
     */
    private function resolveFromContainer(array|callable|string $callable): mixed
    {
        // 对于常见用例的快捷方式
        if ($callable instanceof Closure) {
            return $callable;
        }

        // 如果已经是可调用的，则无需处理
        if (is_callable($callable)) {
            if (!$this->isStaticCallToNonStaticMethod($callable)) {
                return $callable;
            }
        }

        // 如果 callable 是一个字符串，则视为容器条目的名称
        if (is_string($callable)) {
            try {
                return $this->container->get($callable);
            } catch (NotFoundExceptionInterface $e) {
                // 如果容器中存在该条目，则抛出异常
                if ($this->container->has($callable)) {
                    throw $e;
                }
                // 否则抛出 NotCallableException
                throw NotCallableException::fromInvalidCallable($callable, true);
            }
        }

        // 如果 callable 是一个数组，且第一个项是容器条目的名称
        // 例如: ['some-container-entry', 'methodToCall']
        if (is_array($callable) && is_string($callable[0])) {
            try {
                // 用实际对象替换容器条目的名称
                $callable[0] = $this->container->get($callable[0]);
                return $callable;
            } catch (NotFoundExceptionInterface $e) {
                // 如果容器中存在该条目，则抛出异常
                if ($this->container->has($callable[0])) {
                    throw $e;
                }
                // 否则抛出 NotCallableException
                throw new NotCallableException(sprintf(
                    '无法调用 %s()，因为 %s 既不是一个类也不是有效的容器条目',
                    $callable[1],
                    $callable[0]
                ));
            }
        }

        // 未识别的情况，留给后续处理
        return $callable;
    }

    /**
     * 检查 callable 是否表示对非静态方法的静态调用。
     *
     * @param mixed $callable 要检查的 callable
     * @return bool 如果是静态调用非静态方法，则返回 true
     */
    private function isStaticCallToNonStaticMethod(mixed $callable): bool
    {
        if (is_array($callable) && is_string($callable[0])) {
            [$class, $method] = $callable;

            // 检查方法是否存在
            if (!method_exists($class, $method)) {
                return false;
            }

            // 通过反射获取方法信息
            $reflection = new ReflectionMethod($class, $method);

            // 返回是否为非静态方法
            return !$reflection->isStatic();
        }

        return false;
    }
}
