<?php

namespace Ioc\Invoker;

use Invoker\ParameterResolver\ParameterResolver;
use Ioc\Factory\RequestedEntry;
use Override;
use Psr\Container\ContainerExceptionInterface;
use Psr\Container\ContainerInterface;
use Psr\Container\NotFoundExceptionInterface;
use ReflectionFunctionAbstract;
use ReflectionNamedType;


/**
 * 使用类型提示注入容器、定义或其他服务。
 *
 * {@internal 此类与 TypeHintingResolver 和 TypeHintingContainerResolver 相似，
 *            但出于性能原因，我们使用此类。}
 *
 */
readonly class FactoryParameterResolver implements ParameterResolver
{
    /**
     * 创建一个新的 FactoryParameterResolver 实例。
     *
     * @param ContainerInterface $container 用于解析参数的容器
     */
    public function __construct(
        private ContainerInterface $container
    )
    {
    }

    /**
     * 获取函数或方法的参数，并根据类型提示进行解析。
     *
     * @param ReflectionFunctionAbstract $reflection 反射的函数或方法
     * @param array $providedParameters 已提供的参数
     * @param array $resolvedParameters 已解析的参数（用于存储解析结果）
     * @return array 返回已解析的参数数组
     * @throws ContainerExceptionInterface
     * @throws NotFoundExceptionInterface
     */
    #[Override] public function getParameters(
        ReflectionFunctionAbstract $reflection,
        array                      $providedParameters,
        array                      $resolvedParameters
    ): array
    {
        // 获取函数或方法的参数列表
        $parameters = $reflection->getParameters();

        // 跳过已解析的参数
        if (!empty($resolvedParameters)) {
            // 从参数列表中去除已解析的参数
            $parameters = array_diff_key($parameters, $resolvedParameters);
        }

        // 遍历所有参数
        foreach ($parameters as $index => $parameter) {
            // 获取参数类型
            $parameterType = $parameter->getType();
            if (!$parameterType) {
                // 如果参数没有类型，继续处理下一个参数
                continue;
            }
            if (!$parameterType instanceof ReflectionNamedType) {
                // 如果参数是联合类型，当前不支持，继续处理下一个参数
                continue;
            }
            if ($parameterType->isBuiltin()) {
                // 如果参数是基本类型，当前不支持，继续处理下一个参数
                continue;
            }

            // 获取参数的类名
            $parameterClass = $parameterType->getName();

            // 检查参数类型并解析
            if ($parameterClass === ContainerInterface::class) {
                // 如果参数类型是容器接口，则注入当前容器实例
                $resolvedParameters[$index] = $this->container;
            } elseif ($parameterClass === RequestedEntry::class) {
                // 按约定，第二个参数是定义
                $resolvedParameters[$index] = $providedParameters[1];
            } elseif ($this->container->has($parameterClass)) {
                // 如果容器中有该类的实例，则获取并注入
                $resolvedParameters[$index] = $this->container->get($parameterClass);
            }
        }

        // 返回已解析的参数
        return $resolvedParameters;
    }
}
