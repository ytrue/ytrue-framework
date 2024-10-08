<?php

namespace Ioc\Definition\Source;

use Exception;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use ReflectionException;

/**
 * 定义数组源类，用于存储并处理依赖注入的定义
 */
class DefinitionArray implements DefinitionSource, MutableDefinitionSource
{
    public const WILDCARD = '*'; // 通配符常量

    /**
     * 匹配任何除了 "\" 的字符
     */
    private const WILDCARD_PATTERN = '([^\\\\]+)';

    private array $definitions; // 保存定义的数组

    private ?array $wildcardDefinitions = null; // 缓存包含通配符的定义

    private DefinitionNormalizer $normalizer; // 定义标准化器，用于处理定义的标准化

    /**
     * 构造函数
     *
     * @param array $definitions DI definitions in a PHP array indexed by the definition name.
     * @param Autowiring|null $autowiring 自动装配对象（可选）
     * @throws Exception 当定义数组未按条目名称索引时抛出异常
     */
    public function __construct(array $definitions = [], ?Autowiring $autowiring = null)
    {
        if (isset($definitions[0])) {
            throw new Exception('PHP-DI 定义数组未按条目名称进行索引');
        }

        $this->definitions = $definitions;

        // 如果未提供 Autowiring 实例，则使用默认的 NoAutowiring
        $this->normalizer = new DefinitionNormalizer($autowiring ?: new NoAutowiring);
    }

    /**
     * 添加多个定义
     *
     * @param array $definitions DI definitions in a PHP array indexed by the definition name.
     * @throws Exception 当传入的定义数组未按条目名称索引时抛出异常
     */
    public function addDefinitions(array $definitions): void
    {
        if (isset($definitions[0])) {
            throw new Exception('PHP-DI 定义数组未按条目名称进行索引');
        }

        // 将新定义与现有定义合并
        $this->definitions = $definitions + $this->definitions;

        // 清空通配符定义缓存
        $this->wildcardDefinitions = null;
    }

    /**
     * 添加单个定义
     *
     * @param Definition $definition 要添加的定义
     */
    public function addDefinition(Definition $definition): void
    {
        // 根据定义名称将定义存储在数组中
        $this->definitions[$definition->getName()] = $definition;

        // 清空通配符定义缓存
        $this->wildcardDefinitions = null;
    }

    /**
     * 根据名称获取定义
     * @param string $name
     * @return Definition|null
     * @throws InvalidDefinition
     * @throws ReflectionException
     */
    public function getDefinition(string $name): ?Definition
    {
        // 如果定义存在，直接返回
        if (array_key_exists($name, $this->definitions)) {
            $definition = $this->definitions[$name];
            return $this->normalizer->normalizeRootDefinition($definition, $name);
        }

        // 如果通配符定义缓存为空，初始化通配符定义
        if ($this->wildcardDefinitions === null) {
            $this->wildcardDefinitions = [];

            // 查找包含通配符的定义
            foreach ($this->definitions as $key => $definition) {
                if (str_contains($key, self::WILDCARD)) {
                    $this->wildcardDefinitions[$key] = $definition;
                }
            }
        }

        // 处理通配符定义
        foreach ($this->wildcardDefinitions as $key => $definition) {
            $key = preg_quote($key, '#');
            $key = '#^' . str_replace('\\' . self::WILDCARD, self::WILDCARD_PATTERN, $key) . '#';

            // 使用正则表达式匹配通配符
            if (preg_match($key, $name, $matches) === 1) {
                array_shift($matches); // 移除匹配结果的第一个元素

                // 返回标准化后的定义
                return $this->normalizer->normalizeRootDefinition($definition, $name, $matches);
            }
        }

        return null; // 未找到定义，返回 null
    }

    /**
     * 获取所有定义
     *
     * @return array 返回不包含通配符的所有定义
     */
    public function getDefinitions(): array
    {
        $definitions = [];

        // 过滤掉包含通配符的定义
        foreach ($this->definitions as $key => $definition) {
            if (!str_contains($key, self::WILDCARD)) {
                $definitions[$key] = $definition;
            }
        }

        return $definitions;
    }
}
