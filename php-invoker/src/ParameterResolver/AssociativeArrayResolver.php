<?php

namespace Invoker\ParameterResolver;

use Override;
use ReflectionFunctionAbstract;

/**
 * 尝试将一个关联数组（字符串索引）映射到参数名称。
 *
 * 例如，`->call($callable, ['foo' => 'bar'])` 将把字符串 `'bar'`
 * 注入到名为 `$foo` 的参数中。
 *
 * 未使用字符串索引的参数将被忽略。
 */
class AssociativeArrayResolver implements ParameterResolver
{
    #[Override] public function getParameters(
        ReflectionFunctionAbstract $reflection,
        array                      $providedParameters,
        array                      $resolvedParameters
    ): array
    {
        // 获取反射对象中的所有参数
        $parameters = $reflection->getParameters();

        // 跳过已解析的参数
        if (!empty($resolvedParameters)) {
            // 从参数列表中排除已解析的参数
            $parameters = array_diff_key($parameters, $resolvedParameters);
        }

        // 遍历所有参数
        foreach ($parameters as $index => $parameter) {
            // 检查提供的参数中是否存在与当前参数名称相同的键
            if (array_key_exists($parameter->name, $providedParameters)) {
                // 将提供的参数值赋给已解析参数数组中的对应位置
                $resolvedParameters[$index] = $providedParameters[$parameter->name];
            }
        }

        // 返回最终解析的参数数组
        return $resolvedParameters;
    }
}
