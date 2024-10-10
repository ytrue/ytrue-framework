<?php

namespace Invoker\ParameterResolver;

use Override;
use ReflectionFunctionAbstract;

class NumericArrayResolver implements ParameterResolver
{
    #[Override] public function getParameters(ReflectionFunctionAbstract $reflection, array $providedParameters, array $resolvedParameters): array
    {
        // 如果已解析参数不为空，则跳过这些参数
        if (!empty($resolvedParameters)) {
            // 从提供的参数中排除已解析的参数
            $providedParameters = array_diff_key($providedParameters, $resolvedParameters);
        }
        // 遍历提供的参数
        foreach ($providedParameters as $key => $value) {
            // 检查参数的键是否为整数（即索引数组）
            if (is_int($key)) {
                // 将该值添加到已解析参数数组中
                $resolvedParameters[$key] = $value;
            }
        }

        // 返回已解析的参数数组
        return $resolvedParameters;
    }
}
