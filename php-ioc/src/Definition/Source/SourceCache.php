<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\AutowireDefinition;
use Ioc\Definition\Definition;
use Ioc\Definition\ObjectDefinition;
use LogicException;
use Override;

/**
 * SourceCache 类用于在获取依赖注入定义时提供缓存功能。
 * 它通过 APCu 缓存来优化性能，避免频繁从数据源获取定义。
 */
class SourceCache implements DefinitionSource, MutableDefinitionSource
{
    public const string CACHE_KEY = 'php-di.definitions.'; // 缓存键前缀

    /**
     * 构造函数
     *
     * @param DefinitionSource $cachedSource 源数据源，用于在缓存未命中时获取定义。
     * @param string $cacheNamespace 缓存命名空间，用于区分不同上下文的缓存。
     */
    public function __construct(
        private readonly DefinitionSource $cachedSource,
        private readonly string           $cacheNamespace = '',
    )
    {
    }

    /**
     * 获取指定名称的定义，并尝试从缓存中读取，如果缓存中没有则从源数据源获取并缓存。
     *
     * @param string $name 定义的名称。
     * @return Definition|null 如果找到定义则返回，否则返回 null。
     */
    #[Override] public function getDefinition(string $name): Definition|null
    {
        // 从缓存中获取定义
        $definition = apcu_fetch($this->getCacheKey($name));

        // 如果缓存中没有定义，从源数据源获取并存储到缓存
        if ($definition === false) {
            $definition = $this->cachedSource->getDefinition($name);

            // 如果该定义适合缓存，则存入缓存
            if ($this->shouldBeCached($definition)) {
                apcu_store($this->getCacheKey($name), $definition);
            }
        }

        return $definition;
    }

    /**
     * 判断定义是否应该缓存。
     *
     * @param Definition|null $definition 要判断的定义。
     * @return bool 是否应该缓存。
     */
    private function shouldBeCached(?Definition $definition = null): bool
    {
        return
            // 缓存未找到的定义
            ($definition === null)
            // 对象定义适用于 `make()`，应该缓存
            || ($definition instanceof ObjectDefinition)
            // 自动装配定义无法完全预编译，适用于 `make()`，应该缓存
            || ($definition instanceof AutowireDefinition);
    }

    /**
     * 检查是否支持 APCu 缓存。
     *
     * @return bool 是否支持 APCu。
     */
    public static function isSupported(): bool
    {
        return function_exists('apcu_fetch')  // 检查是否有 APCu 函数
            && ini_get('apc.enabled')         // 检查 APC 是否启用
            && !('cli' === \PHP_SAPI && !ini_get('apc.enable_cli'));  // 在 CLI 模式下是否启用 APC
    }

    /**
     * 获取缓存键，结合命名空间和定义名称。
     *
     * @param string $name 定义名称。
     * @return string 生成的缓存键。
     */
    public function getCacheKey(string $name): string
    {
        return self::CACHE_KEY . $this->cacheNamespace . $name;
    }

    /**
     * 获取所有定义。
     *
     * @return array 定义数组。
     */
    #[Override] public function getDefinitions(): array
    {
        return $this->cachedSource->getDefinitions();
    }

    /**
     * 添加定义。
     *
     * @throws LogicException 当试图在启用缓存时添加定义时抛出异常。
     */
    #[Override] public function addDefinition(Definition $definition): void
    {
        // 禁止在运行时添加定义，因为这可能导致缓存不一致
        throw new LogicException('无法在启用缓存的容器中运行时设置定义。这样做可能会缓存定义，导致下次执行时结果不同。可以将定义放入文件中，移除缓存，或直接设置原始值（PHP 对象、字符串、整数等）。');
    }
}
