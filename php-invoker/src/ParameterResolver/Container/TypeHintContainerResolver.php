<?php

namespace Invoker\ParameterResolver\Container;

use Invoker\ParameterResolver\ParameterResolver;
use Override;
use Psr\Container\ContainerInterface;
use ReflectionFunctionAbstract;
use ReflectionNamedType;

readonly class TypeHintContainerResolver implements ParameterResolver
{
    /**
     * 构造函数，接受一个实现了 ContainerInterface 的容器实例
     */
    public function __construct(private ContainerInterface $container)
    {
    }

    #[Override]
    public function getParameters(
        ReflectionFunctionAbstract $reflection,
        array $providedParameters,
        array $resolvedParameters
    ): array {
        // 获取反射对象中的参数
        $parameters = $reflection->getParameters();

        // 跳过已经解析的参数
        if (!empty($resolvedParameters)) {
            $parameters = array_diff_key($parameters, $resolvedParameters);
        }

        // 遍历参数，尝试通过容器解析
        foreach ($parameters as $index => $parameter) {
            $parameterType = $parameter->getType();

            if (!$parameterType) {
                // 如果没有类型，跳过
                continue;
            }
            if (!$parameterType instanceof ReflectionNamedType) {
                // 不支持联合类型
                continue;
            }
            if ($parameterType->isBuiltin()) {
                // 不支持原始类型
                continue;
            }

            // 获取参数的类名
            $parameterClass = $parameterType->getName();
            if ($parameterClass === 'self') {
                // 如果类型是 `self`，获取当前类的名称
                $parameterClass = $parameter->getDeclaringClass()->getName();
            }

            // 检查容器中是否存在该类的实例
            if ($this->container->has($parameterClass)) {
                // 从容器中获取实例并将其添加到已解析参数中
                $resolvedParameters[$index] = $this->container->get($parameterClass);
            }
        }

        // 返回已解析的参数
        return $resolvedParameters;
    }
}
