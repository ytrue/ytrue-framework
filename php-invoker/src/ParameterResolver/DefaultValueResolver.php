<?php

namespace Invoker\ParameterResolver;

use Override;
use ReflectionException;
use ReflectionFunctionAbstract;
use ReflectionParameter;
use function assert;

class DefaultValueResolver implements ParameterResolver
{
    #[Override] public function getParameters(ReflectionFunctionAbstract $reflection, array $providedParameters, array $resolvedParameters): array
    {
        // 获取函数或方法的参数列表
        $parameters = $reflection->getParameters();

        // 如果已解析参数不为空，则跳过已解析的参数
        if (!empty($resolvedParameters)) {
            $parameters = array_diff_key($parameters, $resolvedParameters);
        }

        // 遍历所有参数
        foreach ($parameters as $index => $parameter) {
            assert($parameter instanceof ReflectionParameter);

            // 检查参数是否有默认值可用
            if ($parameter->isDefaultValueAvailable()) {
                try {
                    // 获取默认值，并将其添加到已解析参数中
                    $resolvedParameters[$index] = $parameter->getDefaultValue();
                } catch (ReflectionException $e) {
                    // 无法从 PHP 内部类和函数获取默认值，捕获异常
                }
            } else {
                // 获取参数的类型信息
                $parameterType = $parameter->getType();

                // 检查参数类型是否允许 null 值
                if ($parameterType && $parameterType->allowsNull()) {
                    // 将 null 添加到已解析参数中
                    $resolvedParameters[$index] = null;
                }
            }
        }

        // 返回解析后的参数数组
        return $resolvedParameters;
    }
}
