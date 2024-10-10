<?php

namespace Invoker\ParameterResolver;

use Override;
use ReflectionFunctionAbstract;

class ResolverChain implements ParameterResolver
{
    /**
     * @param ParameterResolver[] $resolvers
     */
    public function __construct(private array $resolvers)
    {
        // 构造函数，接受一个 ParameterResolver 数组，并初始化私有属性 $resolvers
    }

    #[Override] public function getParameters(
        ReflectionFunctionAbstract $reflection,
        array                      $providedParameters,
        array                      $resolvedParameters
    ): array
    {
        // 获取反射对象中的参数
        // Array
        //(
        //    [0] => ReflectionParameter Object
        //        (
        //            [name] => name
        //        )
        //
        //    [1] => ReflectionParameter Object
        //        (
        //            [name] => age
        //        )
        //
        //)
        $reflectionParameters = $reflection->getParameters();

//        $providedParameters[1]=2;
//        $resolvedParameters[0] =1;

        // 遍历每个解析器
        foreach ($this->resolvers as $resolver) {
            // 使用当前解析器解析参数
            $resolvedParameters = $resolver->getParameters($reflection, $providedParameters, $resolvedParameters);
            // 计算尚未解析的参数
            $diff = array_diff_key($reflectionParameters, $resolvedParameters);

            // 如果没有未解析的参数，停止遍历
            if (empty($diff)) {
                // 所有参数均已解析，返回已解析参数
                return $resolvedParameters;
            }
        }

        // 返回已解析的参数
        return $resolvedParameters;
    }

    /**
     * 向解析器链末尾追加解析器
     */
    public function appendResolver(ParameterResolver $resolver): void
    {
        $this->resolvers[] = $resolver;
    }

    /**
     * 向解析器链开头添加解析器
     */
    public function prependResolver(ParameterResolver $resolver): void
    {
        array_unshift($this->resolvers, $resolver);
    }
}
