<?php

namespace Ioc\Definition\Resolver;

use Exception;
use Ioc\Definition\ArrayDefinition;
use Ioc\Definition\Definition;
use Ioc\DependencyException;
use Override;

/**
 * Class ArrayResolver
 *
 * 该类实现了 DefinitionResolver 接口，负责解析数组类型的定义。它会解析数组中的每个元素，
 * 这些元素可能包含嵌套的定义。
 */
readonly class ArrayResolver implements DefinitionResolver
{


    /**
     * 构造函数，接收一个 DefinitionResolver 对象
     *
     * @param DefinitionResolver $definitionResolver 用于处理数组中各个定义的解析器
     */
    public function __construct(
        private DefinitionResolver $definitionResolver
    )
    {
    }

    /**
     * 解析数组定义
     *
     * @param ArrayDefinition $definition 需要解析的数组定义
     * @param array $parameters 传递的额外参数（默认空数组）
     * @return array 解析后的数组
     * @throws DependencyException 如果解析过程中出现依赖错误，则抛出该异常
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): array
    {
        // 获取数组的值
        $values = $definition->getValues();

        // 递归解析嵌套的定义
        array_walk_recursive($values, function (&$value, $key) use ($definition) {
            // 如果值是定义实例，则解析它
            if ($value instanceof Definition) {
                $value = $this->resolveDefinition($value, $definition, $key);
            }
        });

        return $values;
    }

    /**
     * 检查定义是否可以解析
     *
     * @param Definition $definition 需要检查的定义
     * @param array $parameters 传递的参数（默认空数组）
     * @return bool 返回 true 表示可以解析
     */
    #[Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        return true;
    }

    /**
     * 解析单个定义
     *
     * @param Definition $value 需要解析的定义
     * @param ArrayDefinition $definition 当前处理的数组定义
     * @param int|string $key 数组中的键
     * @return mixed 解析后的值
     * @throws DependencyException 如果解析过程中出现依赖错误，则抛出该异常
     */
    private function resolveDefinition(Definition $value, ArrayDefinition $definition, int|string $key): mixed
    {
        try {
            // 使用传入的解析器解析单个定义
            return $this->definitionResolver->resolve($value);
        } catch (DependencyException $e) {
            // 捕获依赖异常并重新抛出
            throw $e;
        } catch (Exception $e) {
            // 捕获其他异常并抛出自定义的 DependencyException，附带错误信息
            throw new DependencyException(sprintf(
                '在解析 %s[%s] 时发生错误。%s',
                $definition->getName(),
                $key,
                $e->getMessage()
            ), 0, $e);
        }
    }
}
