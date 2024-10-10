<?php

namespace Invoker\ParameterResolver\Container;

use Invoker\ParameterResolver\ParameterResolver;
use Override;
use Psr\Container\ContainerInterface;
use ReflectionFunctionAbstract;

readonly class ParameterNameContainerResolver implements ParameterResolver
{
    /**
     * 构造函数，接受一个实现了 ContainerInterface 的容器实例
     */
    public function __construct(private ContainerInterface $container)
    {
    }

    #[Override]
    public function getParameters(ReflectionFunctionAbstract $reflection, array $providedParameters, array $resolvedParameters): array
    {
        // 获取反射对象中的参数
        $parameters = $reflection->getParameters();

        // 跳过已经解析的参数
        if (!empty($resolvedParameters)) {
            $parameters = array_diff_key($parameters, $resolvedParameters);
        }

        // 遍历参数并解析
        foreach ($parameters as $index => $parameter) {
            // 获取参数名称
            $name = $parameter->name;

            // 检查容器中是否存在与参数名称对应的实例
            if ($name && $this->container->has($name)) {
                // 从容器中获取实例并将其添加到已解析参数中
                $resolvedParameters[$index] = $this->container->get($name);
            }
        }

        // 返回已解析的参数
        return $resolvedParameters;
    }
}
