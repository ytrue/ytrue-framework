<?php

namespace Invoker\ParameterResolver;

use Override;
use ReflectionFunctionAbstract;
use ReflectionNamedType;

/**
 * 使用类型提示注入条目。
 *
 * 尝试将类型提示与提供的参数匹配。
 */
class TypeHintResolver implements ParameterResolver
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
            // 获取参数的类型
            $parameterType = $parameter->getType();

            if (!$parameterType) {
                // 如果没有类型提示，继续下一个参数
                continue;
            }
            if (!$parameterType instanceof ReflectionNamedType) {
                // 如果是联合类型（Union Types），不支持，继续下一个参数
                continue;
            }
            if ($parameterType->isBuiltin()) {
                // 如果是原始类型（如 int, string 等），不支持，继续下一个参数
                continue;
            }

            // 获取参数的类名
            $parameterClass = $parameterType->getName();

            // 如果类型是 'self'，则获取当前类名
            if ($parameterClass === 'self') {
                $parameterClass = $parameter->getDeclaringClass()->getName();
            }

            // 检查提供的参数中是否存在与当前参数类型相同的键
            if (array_key_exists($parameterClass, $providedParameters)) {
                // 将提供的参数值赋给已解析参数数组中的对应位置
                $resolvedParameters[$index] = $providedParameters[$parameterClass];
            }
        }

        // 返回最终解析的参数数组
        return $resolvedParameters;
    }
}
