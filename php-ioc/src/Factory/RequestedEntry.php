<?php

namespace Ioc\Factory;

interface RequestedEntry
{
    /**
     * 返回容器请求的条目的名称
     * @return string
     */
    public function getName() : string;
}
