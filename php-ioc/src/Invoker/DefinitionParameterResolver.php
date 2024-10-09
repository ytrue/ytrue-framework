<?php

namespace Ioc\Invoker;

use Invoker\ParameterResolver\ParameterResolver;
use Ioc\Definition\Definition;
use Ioc\Definition\Helper\DefinitionHelper;
use Ioc\Definition\Resolver\DefinitionResolver;
use Override;
use ReflectionFunctionAbstract;

/**
 * 定义参数解析器，用于解析函数调用时的参数。
 *
 * 该类实现了 ParameterResolver 接口，使用定义解析器来解析传入的 Definition 实例。
 * Definition 实例表示容器中的依赖项定义，可以通过定义解析器将其解析为实际的值。
 *
 * @readonly 类是只读的，一旦实例化，其内部属性不能修改。
 */
readonly class DefinitionParameterResolver implements ParameterResolver
{
    /**
     * 构造函数，接收定义解析器。
     *
     * @param DefinitionResolver $definitionResolver 定义解析器，用于解析 Definition 实例。
     */
    public function __construct(private DefinitionResolver $definitionResolver)
    {
    }

    /**
     * 获取并解析传入的参数。
     *
     * 此方法会处理传入的参数列表，并解析所有的 Definition 实例。已经解析过的参数会被跳过，
     * 只有未解析的参数和 Definition 类型的参数会被处理。解析后的参数将被返回用于函数调用。
     *
     * @param ReflectionFunctionAbstract $reflection 反射函数对象，用于获取函数的参数信息。
     * @param array $providedParameters 提供的参数，可能包含 Definition 实例。
     * @param array $resolvedParameters 已解析的参数列表。
     *
     * @return array 解析后的参数列表。
     */
    #[Override] public function getParameters(ReflectionFunctionAbstract $reflection, array $providedParameters, array $resolvedParameters)
    {
        // 跳过已经解析的参数
        if (!empty($resolvedParameters)) {
            $providedParameters = array_diff_key($providedParameters, $resolvedParameters);
        }

        // 遍历所有提供的参数
        foreach ($providedParameters as $key => $value) {
            // 如果参数是 DefinitionHelper 实例，则获取其定义
            if ($value instanceof DefinitionHelper) {
                $value = $value->getDefinition('');
            }

            // 如果参数不是 Definition 实例，继续下一个循环
            if (!$value instanceof Definition) {
                continue;
            }

            // 使用定义解析器解析 Definition 实例
            $value = $this->definitionResolver->resolve($value);

            if (is_int($key)) {
                // 如果参数按位置索引，则将其放入对应位置
                $resolvedParameters[$key] = $value;
            } else {
                // 如果参数按名称索引，则按名称解析参数
                // 通过反射获取函数的所有参数信息
                // 优化此过程，避免性能问题
                $reflectionParameters = $reflection->getParameters();
                foreach ($reflectionParameters as $reflectionParameter) {
                    if ($key === $reflectionParameter->name) {
                        // 按名称找到对应的参数位置
                        $resolvedParameters[$reflectionParameter->getPosition()] = $value;
                    }
                }
            }
        }

        // 返回解析后的参数列表
        return $resolvedParameters;
    }
}
