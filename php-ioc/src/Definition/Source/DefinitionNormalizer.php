<?php

namespace Ioc\Definition\Source;

use Closure;
use Ioc\Definition\ArrayDefinition;
use Ioc\Definition\AutowireDefinition;
use Ioc\Definition\DecoratorDefinition;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\FactoryDefinition;
use Ioc\Definition\Helper\DefinitionHelper;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ValueDefinition;
use ReflectionException;

/**
 * 定义归一化器类。
 *
 * 此类负责将不同类型的定义归一化为统一的 Definition 对象，
 * 支持数组、闭包和其他定义类型的转换，确保在 DI 容器中使用一致的定义格式。
 */
class DefinitionNormalizer
{
    /**
     * 构造函数。
     *
     * @param Autowiring $autowiring 自动装配实例。
     */
    public function __construct(
        private Autowiring $autowiring,
    )
    {
    }

    /**
     * 归一化根定义。
     *
     * 将不同类型的定义转换为 Definition 对象，并在必要时进行自动装配。
     *
     * @param mixed $definition 要归一化的定义，可以是各种类型。
     * @param string $name 定义的名称。
     * @param array|null $wildcardsReplacements 可选的通配符替换数组。
     *
     * @return Definition 归一化后的定义对象。
     * @throws InvalidDefinition 如果定义无效。
     *
     * @throws ReflectionException 如果反射操作失败。
     */
    public function normalizeRootDefinition(mixed $definition, string $name, ?array $wildcardsReplacements = null): Definition
    {
        // 根据传入的定义类型进行处理
        if ($definition instanceof DefinitionHelper) {
            // 如果是 DefinitionHelper，获取其定义
            $definition = $definition->getDefinition($name);
        } elseif (is_array($definition)) {
            // 如果是数组，转换为 ArrayDefinition
            $definition = new ArrayDefinition($definition);
        } elseif ($definition instanceof Closure) {
            // 如果是闭包，转换为 FactoryDefinition
            $definition = new FactoryDefinition($name, $definition);
        } elseif (!$definition instanceof Definition) {
            // 如果不是 Definition 的实例，转换为 ValueDefinition
            $definition = new ValueDefinition($definition);
        }

        // 如果有通配符替换且定义是 ObjectDefinition，进行替换
        if ($wildcardsReplacements && $definition instanceof ObjectDefinition) {
            $definition->replaceWildcards($wildcardsReplacements);
        }

        // 如果定义是 AutowireDefinition，执行自动装配
        if ($definition instanceof AutowireDefinition) {
            /** @var AutowireDefinition $definition */
            $definition = $this->autowiring->autowire($name, $definition);
        }

        // 设置定义的名称
        $definition->setName($name);

        try {
            // 递归处理嵌套定义
            $definition->replaceNestedDefinitions([$this, 'normalizeNestedDefinition']);
        } catch (InvalidDefinition $e) {
            // 捕获 InvalidDefinition 异常并抛出详细信息
            throw InvalidDefinition::create($definition, sprintf(
                'Definition "%s" contains an error: %s',
                $definition->getName(),
                $e->getMessage()
            ), $e);
        }

        return $definition; // 返回归一化后的定义
    }

    /**
     * 归一化嵌套定义。
     *
     * 处理嵌套在其他定义中的定义，确保它们也被正确转换为 Definition 对象。
     *
     * @param mixed $definition 要归一化的嵌套定义。
     *
     * @return mixed 归一化后的嵌套定义对象。
     * @throws InvalidDefinition 如果定义无效。
     *
     */
    public function normalizeNestedDefinition(mixed $definition): mixed
    {
        $name = '<nested definition>'; // 嵌套定义的名称

        // 根据传入的定义类型进行处理
        if ($definition instanceof DefinitionHelper) {
            // 如果是 DefinitionHelper，获取其定义
            $definition = $definition->getDefinition($name);
        } elseif (is_array($definition)) {
            // 如果是数组，转换为 ArrayDefinition
            $definition = new ArrayDefinition($definition);
        } elseif ($definition instanceof Closure) {
            // 如果是闭包，转换为 FactoryDefinition
            $definition = new FactoryDefinition($name, $definition);
        }

        // 如果定义是 DecoratorDefinition，抛出异常
        if ($definition instanceof DecoratorDefinition) {
            throw new InvalidDefinition('Decorators cannot be nested in another definition');
        }

        // 如果定义是 AutowireDefinition，执行自动装配
        if ($definition instanceof AutowireDefinition) {
            $definition = $this->autowiring->autowire($name, $definition);
        }

        // 如果是 Definition 的实例，设置名称并递归处理嵌套定义
        if ($definition instanceof Definition) {
            $definition->setName($name);
            // 递归遍历嵌套定义
            $definition->replaceNestedDefinitions([$this, 'normalizeNestedDefinition']);
        }

        return $definition; // 返回归一化后的嵌套定义
    }
}
