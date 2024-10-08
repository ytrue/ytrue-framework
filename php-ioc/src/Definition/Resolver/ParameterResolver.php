<?php

namespace Ioc\Definition\Resolver;

use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use ReflectionMethod;
use ReflectionParameter;

readonly class ParameterResolver
{
    /**
     * ParameterResolver 构造函数
     *
     * @param DefinitionResolver $definitionResolver 用于解析嵌套定义的解析器
     */
    public function __construct(
        private DefinitionResolver $definitionResolver,
    )
    {
    }

    /**
     * 解析调用方法所需的参数
     *
     * @param MethodInjection|null $definition 方法注入定义，可选，表示该方法的依赖定义
     * @param ReflectionMethod|null $method 方法的反射对象，可选，表示需要解析的目标方法
     * @param array $parameters 传入的参数数组，关联数组，包含参数名和对应的值
     *
     * @return array 解析后的参数数组
     *
     * @throws InvalidDefinition 如果参数没有定义的值或无法推断的值
     */
    public function resolveParameters(
        ?MethodInjection  $definition = null,
        ?ReflectionMethod $method = null,
        array             $parameters = [],
    ): array
    {
        // 存储解析后的参数
        $args = [];

        // 如果方法为空，直接返回空参数数组
        if (!$method) {
            return $args;
        }

        // 获取方法定义的参数列表
        $definitionParameters = $definition ? $definition->getParameters() : [];

        // 遍历方法的所有参数
        foreach ($method->getParameters() as $index => $parameter) {
            // 检查参数名是否存在于传入的参数数组中
            if (array_key_exists($parameter->getName(), $parameters)) {
                // 从 $parameters 数组中获取参数值
                $value = &$parameters[$parameter->getName()];
            } elseif (array_key_exists($index, $definitionParameters)) {
                // 从定义的参数中获取值
                $value = &$definitionParameters[$index];
            } else {
                // 如果参数是可选的且未指定，则使用默认值
                if ($parameter->isDefaultValueAvailable() || $parameter->isOptional()) {
                    $args[] = $this->getParameterDefaultValue($parameter, $method);
                    continue;
                }

                // 如果没有找到值，抛出异常
                throw new InvalidDefinition(sprintf(
                    'Parameter $%s of %s has no value defined or guessable',
                    $parameter->getName(),
                    $this->getFunctionName($method)
                ));
            }

            // 处理嵌套定义
            if ($value instanceof Definition) {
                // 如果参数是可选的且容器无法解析该定义，使用默认参数值
                if ($parameter->isOptional() && !$this->definitionResolver->isResolvable($value)) {
                    $value = $this->getParameterDefaultValue($parameter, $method);
                } else {
                    // 使用 DefinitionResolver 解析参数值
                    $value = $this->definitionResolver->resolve($value);
                }
            }

            // 将解析后的值添加到参数数组中
            $args[] = &$value;
        }

        return $args;
    }

    /**
     * 返回函数参数的默认值
     *
     * @param ReflectionParameter $parameter 方法参数的反射对象
     * @param ReflectionMethod $function 方法的反射对象
     *
     * @return mixed 参数的默认值
     *
     * @throws InvalidDefinition 如果无法获取默认值，通常是因为参数是 PHP 内部类
     */
    private function getParameterDefaultValue(ReflectionParameter $parameter, ReflectionMethod $function): mixed
    {
        try {
            // 获取参数的默认值
            return $parameter->getDefaultValue();
        } catch (\ReflectionException) {
            // 如果无法读取默认值，抛出异常
            throw new InvalidDefinition(sprintf(
                'The parameter "%s" of %s has no type defined or guessable. It has a default value, '
                . 'but the default value can\'t be read through Reflection because it is a PHP internal class.',
                $parameter->getName(),
                $this->getFunctionName($function)
            ));
        }
    }

    /**
     * 获取方法的名称，用于生成异常消息
     *
     * @param ReflectionMethod $method 方法的反射对象
     *
     * @return string 方法名称，包含括号
     */
    private function getFunctionName(ReflectionMethod $method): string
    {
        return $method->getName() . '()';
    }
}
