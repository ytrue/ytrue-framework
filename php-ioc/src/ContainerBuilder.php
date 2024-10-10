<?php

namespace Ioc;

use Exception;
use InvalidArgumentException;
use Ioc\Compiler\Compiler;
use Ioc\Definition\Source\AttributeBasedAutowiring;
use Ioc\Definition\Source\DefinitionArray;
use Ioc\Definition\Source\DefinitionFile;
use Ioc\Definition\Source\DefinitionSource;
use Ioc\Definition\Source\NoAutowiring;
use Ioc\Definition\Source\ReflectionBasedAutowiring;
use Ioc\Definition\Source\SourceCache;
use Ioc\Definition\Source\SourceChain;
use Ioc\Proxy\ProxyFactory;
use LogicException;
use Psr\Container\ContainerInterface;


/**
 * ContainerBuilder 是一个帮助创建和配置依赖注入容器的类。
 *
 * 默认情况下，它创建的容器适用于开发环境。通过提供配置选项，可以调整容器行为，比如启用或禁用自动注入、缓存、编译等特性。
 *
 * 使用示例:
 *
 *     $builder = new ContainerBuilder();
 *     $container = $builder->build();
 *
 */
class ContainerBuilder
{

    /**
     * @var string 容器类的名称，用于实例化容器。
     */
    private string $containerClass;

    /**
     * 容器父类的名称，编译容器时使用。
     *
     * @var string<Container>
     * @psalm-var class-string<ContainerClass>
     */
    private string $containerParentClass;

    /**
     * 是否启用自动注入（默认启用）。
     *
     * @var bool
     */
    private bool $useAutowiring = true;

    /**
     * 是否使用 PHP 8 的属性（Attributes）来配置注入（默认禁用）。
     *
     * @var bool
     */
    private bool $useAttributes = false;

    /**
     * 如果设置了目录，代理类将会写入该目录以提升性能。
     *
     * @var ?string
     */
    private ?string $proxyDirectory = null;

    /**
     * 如果 PHP-DI 被封装在另一个容器中，此属性引用该封装容器。
     *
     * @var ?ContainerInterface
     */
    private ?ContainerInterface $wrapperContainer = null;

    /**
     * 存储容器定义源，支持数组、文件或定义源类。
     *
     * @var DefinitionSource[]|string[]|array[]
     */
    private array $definitionSources = [];

    /**
     * 容器是否已被构建。
     *
     * @var bool
     */
    private bool $locked = false;

    /**
     * 编译容器的目录路径。
     *
     * @var ?string
     */
    private ?string $compileToDirectory = null;

    /**
     * 是否启用源缓存。
     *
     * @var bool
     */
    private bool $sourceCache = false;

    /**
     * 源缓存的命名空间。
     *
     * @var string
     */
    protected string $sourceCacheNamespace = '';

    /**
     * 构造函数，设置容器类的名称。
     *
     * @param string<Container> $containerClass 容器类的名称
     * @psalm-param class-string<ContainerClass> $containerClass
     */
    public function __construct(string $containerClass = Container::class)
    {
        $this->containerClass = $containerClass;
    }

    /**
     * 构建并返回一个容器实例。
     *
     * @return Container 返回容器实例
     * @psalm-return ContainerClass
     * @throws Exception
     */
    public function build(): Container
    {
        // 反转定义源数组，以便最早添加的定义最后解析
        $sources = array_reverse($this->definitionSources);

        // 根据配置选择自动注入方式
        if ($this->useAttributes) {
            $autowiring = new AttributeBasedAutowiring();
            $sources[] = $autowiring;
        } elseif ($this->useAutowiring) {
            $autowiring = new ReflectionBasedAutowiring();
            $sources[] = $autowiring;
        } else {
            $autowiring = new NoAutowiring();
        }

        // 将定义源转换为可用的定义数组或文件
        $sources = array_map(function ($definitions) use ($autowiring) {
            if (is_string($definitions)) {
                return new DefinitionFile($definitions, $autowiring);
            }
            if (is_array($definitions)) {
                return new DefinitionArray($definitions, $autowiring);
            }
            return $definitions;
        }, $sources);


        // 将多个定义源链式组合
        $source = new SourceChain($sources);

        // 设置可变定义源
        $source->setMutableDefinitionSource(new DefinitionArray([], $autowiring));


        // 如果启用了缓存，使用 SourceCache 包装源
        if ($this->sourceCache) {
            if (!SourceCache::isSupported()) {
                throw new Exception('APCu 未启用，无法使用缓存');
            }
            $source = new SourceCache($source, $this->sourceCacheNamespace);
        }

        // 创建代理工厂
        $proxyFactory = new ProxyFactory($this->proxyDirectory);

        // 锁定容器配置
        $this->locked = true;

        $containerClass = $this->containerClass;

        // 如果启用了编译，执行编译过程
        if ($this->compileToDirectory) {
            $compiler = new Compiler($proxyFactory);
            $compiledContainerFile = $compiler->compile(
                $source,
                $this->compileToDirectory,
                $this->containerClass,
                $this->containerParentClass,
                $this->useAutowiring
            );

            if (!class_exists($containerClass, false)) {
                require $compiledContainerFile;
            }
        }
        // 返回创建的容器实例
        return new $containerClass($source, $proxyFactory, $this->wrapperContainer);
    }


    /**
     * 为了优化性能，编译容器。
     *
     * 请注意，容器在编译后是一次性构建的，之后不会再更新！
     *
     * 因此：
     *
     * - 在生产环境中，每次部署时都应该清理该目录
     * - 在开发环境中，您不应该编译容器
     *
     * @see https://php-di.org/doc/performances.html
     *
     * @psalm-template T of CompiledContainer
     *
     * @param string $directory 用于存放编译后容器的目录。
     * @param string $containerClass 编译后的类名称，只有在必要时才进行自定义。
     * @param string<Container> $containerParentClass 编译后容器的父类名称，只有在必要时才自定义。
     * @psalm-param class-string<T> $containerParentClass
     *
     * @psalm-return self<T>
     */
    public function enableCompilation(
        string $directory,
        string $containerClass = 'CompiledContainer',
        string $containerParentClass = "\\" . CompiledContainer::class,
    ): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 设置编译输出目录
        $this->compileToDirectory = $directory;
        // 设置容器类名
        $this->containerClass = $containerClass;
        // 设置容器父类
        $this->containerParentClass = $containerParentClass;

        return $this;
    }

    /**
     * 启用或禁用自动注入机制，用于推断依赖注入。
     *
     * 默认情况下启用。
     *
     * @param bool $bool 是否启用自动注入
     * @return $this
     */
    public function useAutowiring(bool $bool): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 设置是否使用自动注入
        $this->useAutowiring = $bool;

        return $this;
    }

    /**
     * 启用或禁用使用 PHP 8 属性来配置依赖注入。
     *
     * 默认情况下禁用。
     *
     * @param bool $bool 是否启用属性注入
     * @return $this
     */
    public function useAttributes(bool $bool): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 设置是否使用属性注入
        $this->useAttributes = $bool;

        return $this;
    }

    /**
     * 配置代理类的生成方式。
     *
     * 开发环境中，请使用 `writeProxiesToFile(false)`（默认配置）。
     * 生产环境中，请使用 `writeProxiesToFile(true, 'tmp/proxies')`。
     *
     * @see https://php-di.org/doc/lazy-injection.html
     *
     * @param bool $writeToFile 如果为 true，则将代理写入磁盘以提高性能
     * @param string|null $proxyDirectory 存放代理类的目录
     * @return $this
     * @throws InvalidArgumentException 当 writeToFile 为 true 且 proxyDirectory 为 null 时抛出异常
     */
    public function writeProxiesToFile(bool $writeToFile, ?string $proxyDirectory = null): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 如果要求写入文件但没有指定目录，抛出异常
        if ($writeToFile && $proxyDirectory === null) {
            throw new InvalidArgumentException(
                '如果希望将代理类写入磁盘，必须指定代理目录'
            );
        }

        // 配置代理类写入目录
        $this->proxyDirectory = $writeToFile ? $proxyDirectory : null;

        return $this;
    }

    /**
     * 如果 PHP-DI 的容器被其他容器包装，我们可以设置此方法
     * 让 PHP-DI 使用包装器来构建对象，而不是直接使用自己。
     *
     * @param ContainerInterface $otherContainer 外部容器实例
     * @return $this
     */
    public function wrapContainer(ContainerInterface $otherContainer): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 设置包装的容器
        $this->wrapperContainer = $otherContainer;

        return $this;
    }

    /**
     * 向容器添加定义。
     *
     * 可以是定义数组、包含定义的文件名或 DefinitionSource 对象。
     *
     * @param string|array|DefinitionSource ...$definitions 定义的数组、文件名或 DefinitionSource 对象
     * @return $this
     */
    public function addDefinitions(string|array|DefinitionSource ...$definitions): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 将定义源添加到容器
        foreach ($definitions as $definition) {
            $this->definitionSources[] = $definition;
        }

        return $this;
    }

    /**
     * 启用 APCu 缓存容器定义。
     *
     * 必须启用 APCu 才能使用此功能。
     *
     * 在使用此功能之前，建议先进行以下步骤：
     * 1. 如果尚未启用编译，请启用编译（参见 `enableCompilation()`）。
     * 2. 如果使用自动注入或属性注入，请将所有使用的类添加到配置中，这样 PHP-DI 可以知道它们并进行编译。
     *
     * 一旦完成这些步骤，您可以尝试进一步通过 APCu 优化性能。
     * 如果您使用 `Container::make()` 而不是 `get()`，APCu 可能也有用，因为 `make()` 调用无法编译，因此不会被优化。
     *
     * 请记住，每次部署时都要清除 APCu，否则应用程序将使用过期的缓存。在开发环境中，不要启用缓存：
     * 否则对代码的任何更改都会被缓存忽略。
     *
     * @see https://php-di.org/doc/performances.html
     *
     * @param string $cacheNamespace 使用唯一的命名空间来避免共享单个 APC 内存池时的缓存冲突
     * @return $this
     */
    public function enableDefinitionCache(string $cacheNamespace = ''): self
    {
        // 确保容器尚未构建
        $this->ensureNotLocked();

        // 启用源缓存并设置命名空间
        $this->sourceCache = true;
        $this->sourceCacheNamespace = $cacheNamespace;

        return $this;
    }

    /**
     * 判断是否启用了编译。
     *
     * @return bool 返回编译是否启用的状态
     */
    public function isCompilationEnabled(): bool
    {
        // 如果编译目录存在，则返回 true，否则返回 false
        return (bool)$this->compileToDirectory;
    }

    /**
     * 确保在容器构建之前调用的辅助方法。
     * 如果容器已经构建，任何修改都将抛出异常。
     *
     * @throws LogicException 如果容器已锁定，则抛出异常
     */
    private function ensureNotLocked(): void
    {
        // 检查容器是否已经被锁定
        if ($this->locked) {
            throw new LogicException('容器已构建，ContainerBuilder 无法在此之后进行修改');
        }
    }
}
