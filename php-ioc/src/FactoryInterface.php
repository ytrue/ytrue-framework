<?php

namespace Ioc;

/**
 * 工场接口
 */
interface FactoryInterface
{

    /**
     * 创建对象
     * @param string $name 对象的class获取 实体名称
     * @param array $parameters 参数
     * @return mixed 创建的对象
     */
    public function make(string $name, array $parameters = []) : mixed;
}
