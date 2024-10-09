<?php

namespace Ioc\Definition\Resolver;

use Ioc\Definition\Definition;
use Ioc\Definition\EnvironmentVariableDefinition;
use Ioc\Definition\Exception\InvalidDefinition;
use Override;

class EnvironmentVariableResolver implements DefinitionResolver
{
    /**
     * @var callable 用于读取环境变量的回调函数
     */
    private $variableReader;

    /**
     * 构造函数
     *
     * @param DefinitionResolver $definitionResolver 用于解析嵌套定义的解析器
     * @param callable|null $variableReader 自定义的环境变量读取器（可选），默认为 getEnvVariable 方法
     */
    public function __construct(
        private readonly DefinitionResolver $definitionResolver,
        ?callable                           $variableReader = null
    )
    {
        // 如果未提供自定义的环境变量读取器，则使用默认的 getEnvVariable 方法
        $this->variableReader = $variableReader ?? [$this, 'getEnvVariable'];
    }

    /**
     * 解析环境变量定义为一个值。
     *
     * @param EnvironmentVariableDefinition $definition 环境变量的定义
     * @param array $parameters 传递的参数（默认空数组）
     * @return mixed 解析后的环境变量值
     * @throws InvalidDefinition 如果环境变量未定义且该变量不是可选的，则抛出异常
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): mixed
    {
        // 使用变量读取器获取环境变量的值
        $value = call_user_func($this->variableReader, $definition->getVariableName());

        // 如果环境变量存在，直接返回其值
        if (false !== $value) {
            return $value;
        }

        // 如果环境变量未定义且不是可选的，抛出异常
        if (!$definition->isOptional()) {
            throw new InvalidDefinition(sprintf(
                "环境变量 '%s' 未定义",
                $definition->getVariableName()
            ));
        }

        // 如果是可选的，获取默认值
        $value = $definition->getDefaultValue();

        // 如果默认值是一个嵌套的定义，则解析该定义
        if ($value instanceof Definition) {
            return $this->definitionResolver->resolve($value);
        }

        // 返回默认值
        return $value;
    }

    /**
     * 检查定义是否可以被解析
     *
     * @param Definition $definition 要检查的定义
     * @param array $parameters 传递的参数（默认空数组）
     * @return bool 返回 true 表示可以解析
     */
    #[Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        return true;
    }

    /**
     * 获取环境变量的值。
     *
     * @param string $variableName 环境变量的名称
     * @return mixed 环境变量的值，如果未定义则返回 null
     */
    protected function getEnvVariable(string $variableName): mixed
    {
        return $_ENV[$variableName] ?? $_SERVER[$variableName] ?? getenv($variableName);
    }
}
