<?php

namespace Ioc\Proxy;

use ProxyManager\Configuration;
use ProxyManager\Factory\LazyLoadingValueHolderFactory;
use ProxyManager\FileLocator\FileLocator;
use ProxyManager\GeneratorStrategy\EvaluatingGeneratorStrategy;
use ProxyManager\GeneratorStrategy\FileWriterGeneratorStrategy;
use ProxyManager\Proxy\LazyLoadingInterface;

/**
 * 代理类工厂，用于创建和管理懒加载代理类实例。
 *
 * 该类是对 Ocramius/ProxyManager 的 LazyLoadingValueHolderFactory 的封装。
 * 懒加载代理类的创建有助于提升应用程序的性能，只有在需要时才实例化对象。
 *
 * @see LazyLoadingValueHolderFactory
 *
 * @since  5.0
 * @author Matthieu Napoli <matthieu@mnapoli.fr>
 */
class ProxyFactory
{
    // 保存 LazyLoadingValueHolderFactory 的实例，用于生成代理类
    private ?LazyLoadingValueHolderFactory $proxyManager = null;

    /**
     * @param string|null $proxyDirectory 如果设置了该目录，则代理类将被写入到磁盘以提升性能。
     */
    public function __construct(
        private readonly ?string $proxyDirectory = null, // 代理类文件存储的目录，可选参数
    )
    {
    }

    /**
     * 创建指定类的懒加载代理实例，并传递初始化闭包。
     *
     * @param string $className 要生成代理类的类名
     * @param \Closure $initializer 初始化闭包，当代理对象首次被调用时执行
     * @return LazyLoadingInterface 返回代理类实例
     */
    public function createProxy(string $className, \Closure $initializer): LazyLoadingInterface
    {
        // 调用 proxyManager() 方法获取 LazyLoadingValueHolderFactory，并创建代理类
        return $this->proxyManager()->createProxy($className, $initializer);
    }

    /**
     * 生成并将代理类写入文件（如果指定了代理类存储目录）。
     *
     * @param string $className 要生成代理类的类名
     */
    public function generateProxyClass(string $className): void
    {
        // 如果设置了代理类文件的存储目录，则生成并预创建代理类文件
        if ($this->proxyDirectory) {
            $this->createProxy($className, function () {
            });
        }
    }

    /**
     * 获取 LazyLoadingValueHolderFactory 的实例，如果不存在则创建新的实例。
     *
     * @return LazyLoadingValueHolderFactory 返回 LazyLoadingValueHolderFactory 实例
     * @throws \RuntimeException 如果未安装 ocramius/proxy-manager，则抛出运行时异常
     */
    private function proxyManager(): LazyLoadingValueHolderFactory
    {
        // 如果 LazyLoadingValueHolderFactory 尚未实例化，则进行初始化
        if ($this->proxyManager === null) {
            // 检查是否安装了 ocramius/proxy-manager 库
            if (!class_exists(Configuration::class)) {
                // 如果未安装，抛出异常并提示安装指令
                throw new \RuntimeException('The ocramius/proxy-manager library is not installed. Lazy injection requires that library to be installed with Composer in order to work. Run "composer require ocramius/proxy-manager:~2.0".');
            }

            // 创建代理管理器的配置对象
            $config = new Configuration();

            // 如果设置了代理类存储目录，则将代理类写入文件
            if ($this->proxyDirectory) {
                $config->setProxiesTargetDir($this->proxyDirectory);
                $config->setGeneratorStrategy(new FileWriterGeneratorStrategy(new FileLocator($this->proxyDirectory)));
                // 注册自动加载器，用于自动加载代理类
                spl_autoload_register($config->getProxyAutoloader());
            } else {
                // 如果未设置目录，则使用动态生成策略
                $config->setGeneratorStrategy(new EvaluatingGeneratorStrategy());
            }

            // 创建 LazyLoadingValueHolderFactory 实例
            $this->proxyManager = new LazyLoadingValueHolderFactory($config);
        }

        return $this->proxyManager;
    }
}
