<?php

namespace Invoker\ParameterResolver;

use ReflectionFunctionAbstract;

/**
 * ParameterResolver 接口用于解析调用可调用对象时使用的参数。
 *
 * 每个实现该接口的类必须能够解析某些参数，以便调用传入的可调用对象。解析过程可以链式进行，
 * 即多个 ParameterResolver 可以协同工作，每个解析器只需处理尚未解析的参数。
 */
interface ParameterResolver
{
    /**
     * 解析调用可调用对象时使用的参数。
     *
     * `$resolvedParameters` 包含已解析的参数。
     * 每个 ParameterResolver 必须解析尚未在 `$resolvedParameters` 中的参数，允许多个解析器链式工作。
     *
     * @param ReflectionFunctionAbstract $reflection 可调用对象的反射信息。
     * @param array $providedParameters 调用方提供的参数。
     * @param array $resolvedParameters 已解析的参数（按参数位置索引）。
     * @return array 返回解析后的参数数组。
     */
    public function getParameters(
        ReflectionFunctionAbstract $reflection,
        array                      $providedParameters,
        array                      $resolvedParameters
    ): array;
}
