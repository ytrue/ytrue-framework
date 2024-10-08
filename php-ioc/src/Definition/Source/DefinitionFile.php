<?php

namespace Ioc\Definition\Source;

use Exception;
use Ioc\Definition\Definition;

/**
 * 定义文件类，从文件中加载依赖注入定义。
 *
 * 该类继承自 DefinitionArray，通过文件懒加载的方式提高性能。
 */
class DefinitionFile extends DefinitionArray
{
    private bool $initialized = false; // 标志是否已初始化

    /**
     * 构造函数
     *
     * @param string $file 包含定义的文件路径
     * @param Autowiring|null $autowiring 自动装配对象（可选）
     * @throws Exception
     */
    public function __construct(
        private string $file, // 定义文件的路径
        ?Autowiring    $autowiring = null, // 自动装配对象，默认为 null
    )
    {
        // 使用懒加载的方式提高性能，初始化时不加载定义
        parent::__construct([], $autowiring);
    }

    /**
     * 获取指定名称的定义
     *
     * @param string $name 定义名称
     * @return Definition|null 返回定义对象，找不到时返回 null
     * @throws Exception
     */
    public function getDefinition(string $name): null|Definition
    {
        // 初始化定义文件
        $this->initialize();

        // 调用父类的 getDefinition 方法
        return parent::getDefinition($name);
    }

    /**
     * 获取所有定义
     *
     * @return array 返回定义数组
     * @throws Exception
     */
    public function getDefinitions(): array
    {
        // 初始化定义文件
        $this->initialize();

        // 调用父类的 getDefinitions 方法
        return parent::getDefinitions();
    }

    /**
     * 初始化定义文件
     *
     * 从文件中加载定义，并确保只初始化一次
     * @throws Exception
     */
    private function initialize(): void
    {
        // 如果已经初始化过，直接返回
        if ($this->initialized === true) {
            return;
        }

        // 从文件中加载定义数组
        $definitions = require $this->file;

        // 验证文件中是否返回了有效的定义数组
        if (!is_array($definitions)) {
            throw new Exception("文件 $this->file 应该返回一个定义数组");
        }

        // 添加定义
        $this->addDefinitions($definitions);

        // 设置初始化完成标志
        $this->initialized = true;
    }
}
